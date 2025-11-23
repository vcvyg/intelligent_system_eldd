// 医护端群聊脚本 - 修复版

let currentGroupId = null;
let currentGroupName = '';
let stompClient = null;
let currentUser = null;

// DOM元素缓存，提高性能
const domCache = {
    welcomeText: null,
    chatInput: null,
    sendBtn: null,
    chatMessages: null,
    chatHeader: null,
    userList: null,
    recordBtn: null,
    
    init() {
        this.welcomeText = document.getElementById('welcomeText');
        this.chatInput = document.getElementById('chatInput');
        this.sendBtn = document.getElementById('sendBtn');
        this.chatMessages = document.getElementById('chatMessages');
        this.chatHeader = document.getElementById('chatHeader');
        this.userList = document.getElementById('userList');
        this.recordBtn = document.getElementById('recordBtn');
    }
};

document.addEventListener('DOMContentLoaded', async () => {
    // 初始化DOM缓存
    domCache.init();
    
    // 快速权限检查
    currentUser = checkLogin();
    if (!currentUser || currentUser.role !== 'MEDICAL') {
        alert('权限不足或登录已过期');
        logout();
        return;
    }
    
    // 设置欢迎文本
    if (domCache.welcomeText) {
        domCache.welcomeText.textContent = `欢迎，${currentUser.username}！`;
    }

    // 异步加载数据，避免阻塞页面渲染
    // 先加载群组列表，WebSocket连接可以稍后（即使失败也不影响页面使用）
    loadGroupList().catch(error => {
        console.error('加载群组列表失败:', error);
    });
    
    // WebSocket连接失败不应该阻止页面加载
    // 延迟一下再连接，避免页面加载时立即失败
    setTimeout(() => {
        updateConnectionStatus('connecting');
        connectWebSocket().catch(error => {
            // 静默处理，让重连机制处理
            // 只在控制台输出一次警告
            console.warn('WebSocket初始连接失败，将自动重试（最多' + maxReconnectAttempts + '次）');
        });
    }, 500);

    // 添加键盘事件监听
    if (domCache.chatInput) {
        domCache.chatInput.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });
    }
    
    // 页面卸载时清理资源
    window.addEventListener('beforeunload', function() {
        isManualDisconnect = true;
        stopConnectionHealthCheck();
        if (reconnectTimer) {
            clearTimeout(reconnectTimer);
        }
        if (stompClient && stompClient.connected) {
            stompClient.disconnect();
        }
    });
    
    // 监听网络状态变化，网络恢复时自动重连
    window.addEventListener('online', function() {
        console.log('网络已恢复，尝试重新连接WebSocket');
        if (!isManualDisconnect && (!stompClient || !stompClient.connected)) {
            reconnectAttempts = 0;
            shouldStopReconnecting = false;
            updateConnectionStatus('reconnecting');
            connectWebSocket().catch(err => {
                console.warn('网络恢复后重连失败:', err);
            });
        }
    });
    
    window.addEventListener('offline', function() {
        console.log('网络已断开');
        updateConnectionStatus('disconnected');
    });
});

// 重连相关变量
let reconnectAttempts = 0;
let maxReconnectAttempts = 10; // 增加重连次数
let reconnectTimer = null;
let isConnecting = false;
let isManualDisconnect = false;
let shouldStopReconnecting = false; // 标志：是否应该停止重连
let connectionHealthCheckInterval = null; // 连接健康检查定时器
let lastHeartbeatTime = null; // 最后一次收到心跳的时间

// 更新连接状态（仅输出到控制台）
function updateConnectionStatus(status) {
    const statusConfig = {
        'connected': { text: '✅ WebSocket已连接', emoji: '✅' },
        'connecting': { text: '🔄 WebSocket连接中...', emoji: '🔄' },
        'disconnected': { text: '❌ WebSocket未连接', emoji: '❌' },
        'reconnecting': { text: '🔄 WebSocket重连中...', emoji: '🔄' }
    };
    
    const config = statusConfig[status] || statusConfig['disconnected'];
    const timestamp = new Date().toLocaleTimeString('zh-CN');
    console.log(`[${timestamp}] ${config.text}`);
}

// 启动连接健康检查
function startConnectionHealthCheck() {
    if (connectionHealthCheckInterval) {
        clearInterval(connectionHealthCheckInterval);
    }
    
    connectionHealthCheckInterval = setInterval(() => {
        if (isManualDisconnect) return;
        
        // 检查连接状态
        if (!stompClient || !stompClient.connected) {
            console.log('健康检查：连接已断开，尝试重连...');
            updateConnectionStatus('reconnecting');
            // 重置重连次数，允许重新尝试
            if (reconnectAttempts >= maxReconnectAttempts) {
                reconnectAttempts = 0;
                shouldStopReconnecting = false;
            }
            connectWebSocket().catch(err => {
                console.warn('健康检查重连失败:', err);
            });
        } else {
            // 检查心跳（如果超过30秒没有收到心跳，认为连接异常）
            if (lastHeartbeatTime) {
                const timeSinceLastHeartbeat = Date.now() - lastHeartbeatTime;
                if (timeSinceLastHeartbeat > 30000) {
                    console.warn('健康检查：超过30秒未收到心跳，连接可能异常');
                    // 断开并重连
                    if (stompClient && stompClient.connected) {
                        stompClient.disconnect();
                    }
                    updateConnectionStatus('reconnecting');
                    reconnectAttempts = 0;
                    shouldStopReconnecting = false;
                    connectWebSocket().catch(err => {
                        console.warn('健康检查重连失败:', err);
                    });
                }
            }
        }
    }, 5000); // 每5秒检查一次
}

// 停止连接健康检查
function stopConnectionHealthCheck() {
    if (connectionHealthCheckInterval) {
        clearInterval(connectionHealthCheckInterval);
        connectionHealthCheckInterval = null;
    }
}

// 检查token是否过期
function isTokenExpired() {
    const token = localStorage.getItem('token');
    if (!token) return true;
    
    try {
        // 简单的JWT token过期检查（解析payload）
        const payload = JSON.parse(atob(token.split('.')[1]));
        const exp = payload.exp * 1000; // 转换为毫秒
        return Date.now() >= exp;
    } catch (e) {
        // 如果无法解析，假设token无效
        return true;
    }
}

// 确保WebSocket连接的函数（改进版：即使达到最大重连次数，在用户操作时也要尝试）
async function ensureWebSocketConnected(forceReconnect = false) {
    // 检查token是否过期
    if (isTokenExpired()) {
        console.warn('Token已过期，无法连接WebSocket');
        updateConnectionStatus('disconnected');
        alert('登录已过期，请重新登录');
        logout();
        return false;
    }
    
    if (stompClient && stompClient.connected && !forceReconnect) {
        return true;
    }
    
    // 如果之前达到最大重连次数，但在用户操作时，重置计数器并重试
    if (forceReconnect || reconnectAttempts >= maxReconnectAttempts) {
        console.log('用户操作触发，重置重连计数器并尝试连接');
        reconnectAttempts = 0;
        shouldStopReconnecting = false;
    }
    
    if (isConnecting) {
        // 如果正在连接，等待一下
        await new Promise(resolve => setTimeout(resolve, 500));
        return stompClient && stompClient.connected;
    }
    
    // 尝试连接
    updateConnectionStatus('connecting');
    await connectWebSocket();
    return stompClient && stompClient.connected;
}

function connectWebSocket() {
    const token = localStorage.getItem('token');
    if (!token) {
        console.warn('登录信息不存在，无法连接WebSocket');
        return Promise.reject(new Error('No token'));
    }

    // 如果已经连接，直接返回
    if (stompClient && stompClient.connected) {
        return Promise.resolve();
    }

    // 如果正在连接，等待连接完成
    if (isConnecting) {
        return new Promise((resolve) => {
            const checkInterval = setInterval(() => {
                if (!isConnecting) {
                    clearInterval(checkInterval);
                    resolve();
                } else if (stompClient && stompClient.connected) {
                    clearInterval(checkInterval);
                    resolve();
                }
            }, 100);
            
            // 最多等待5秒
            setTimeout(() => {
                clearInterval(checkInterval);
                resolve();
            }, 5000);
        });
    }

    isConnecting = true;
    console.log('正在连接WebSocket... (尝试 ' + (reconnectAttempts + 1) + '/' + maxReconnectAttempts + ')');
    
    return new Promise((resolve, reject) => {
        try {
            const wsUrl = `/ws-chat?token=${encodeURIComponent(token)}`;
            console.log('连接WebSocket URL:', wsUrl.replace(/token=[^&]+/, 'token=***'));
            
            const socket = new SockJS(wsUrl);
            // 使用Stomp.over，但传入一个factory函数来避免警告
            if (typeof Stomp !== 'undefined') {
                // 检查是否有新的API
                if (Stomp.Stomp && typeof Stomp.Stomp.over === 'function') {
                    stompClient = Stomp.Stomp.over(() => socket);
                } else if (typeof Stomp.over === 'function') {
                    // 旧版API，直接使用
                    stompClient = Stomp.over(socket);
                } else {
                    // 如果都不支持，创建一个包装器
                    stompClient = Stomp.over(socket);
                }
            } else {
                throw new Error('Stomp库未加载');
            }

            // 配置心跳，与服务器端保持一致
            stompClient.heartbeat.outgoing = 10000; // 客户端每10秒发送一次心跳
            stompClient.heartbeat.incoming = 10000; // 客户端期望每10秒收到一次心跳

            // 覆盖默认的断开连接处理
            stompClient.onWebSocketClose = function() {
                console.warn('WebSocket连接已关闭');
                isConnecting = false;
                updateConnectionStatus('disconnected');
                
                // 如果是手动断开，不重连
                if (isManualDisconnect) {
                    stopConnectionHealthCheck();
                    return;
                }
                
                // 尝试自动重连
                updateConnectionStatus('reconnecting');
                attemptReconnect();
            };

            // 添加SockJS错误处理
            socket.onerror = function(error) {
                // 只在第一次失败或达到最大重连次数时输出错误
                if (reconnectAttempts === 0 || reconnectAttempts >= maxReconnectAttempts - 1) {
                    console.error('SockJS连接错误:', error);
                }
                isConnecting = false;
                updateConnectionStatus('disconnected');
                if (!isManualDisconnect) {
                    updateConnectionStatus('reconnecting');
                    attemptReconnect();
                }
                reject(new Error('SockJS connection error'));
            };

            // 添加SockJS关闭处理
            socket.onclose = function(event) {
                console.warn('SockJS连接关闭:', event);
                isConnecting = false;
                updateConnectionStatus('disconnected');
                
                if (!isManualDisconnect) {
                    updateConnectionStatus('reconnecting');
                    attemptReconnect();
                }
            };

            stompClient.connect({}, function (frame) {
                console.log('WebSocket连接成功:', frame);
                isConnecting = false;
                reconnectAttempts = 0; // 重置重连次数
                shouldStopReconnecting = false; // 重置停止标志
                lastHeartbeatTime = Date.now(); // 记录连接时间
                updateConnectionStatus('connected');
                
                // 启动连接健康检查
                startConnectionHealthCheck();
                
                // 监听心跳消息
                const originalOnMessage = stompClient.ws.onmessage;
                stompClient.ws.onmessage = function(event) {
                    lastHeartbeatTime = Date.now();
                    if (originalOnMessage) {
                        originalOnMessage.call(this, event);
                    }
                };
                
                // Subscribe to personal message queue
                stompClient.subscribe('/user/queue/group-messages', function (message) {
                    console.log('收到个人队列消息:', message.body);
                    lastHeartbeatTime = Date.now(); // 收到消息也更新心跳时间
                    const msg = JSON.parse(message.body);
                    handleNewMessage(msg);
                });
                
                // Subscribe to current group topic for immediate message display
                if (currentGroupId) {
                    subscribeToGroupTopic(currentGroupId);
                }
                
                resolve();
            }, function(error) {
                // 只在第一次失败或达到最大重连次数时输出错误
                if (reconnectAttempts === 0 || reconnectAttempts >= maxReconnectAttempts - 1) {
                    console.error('STOMP连接失败:', error);
                }
                isConnecting = false;
                updateConnectionStatus('disconnected');
                
                // 尝试自动重连
                if (!isManualDisconnect) {
                    updateConnectionStatus('reconnecting');
                    attemptReconnect();
                }
                reject(error);
            });
        } catch (error) {
            console.error('创建WebSocket连接时出错:', error);
            isConnecting = false;
            attemptReconnect();
            reject(error);
        }
    });
}

// 尝试重连
function attemptReconnect() {
    // 如果已经达到最大重连次数，停止重连
    if (reconnectAttempts >= maxReconnectAttempts || shouldStopReconnecting) {
        if (reconnectAttempts === maxReconnectAttempts && !shouldStopReconnecting) {
            console.warn('已达到最大重连次数，停止重连。WebSocket连接失败，但消息仍可通过HTTP API发送');
            shouldStopReconnecting = true;
            updateConnectionStatus('disconnected');
            
            // 显示用户友好的提示
            const notification = document.createElement('div');
            notification.style.cssText = 'position:fixed;top:20px;right:20px;background:#ff9800;color:white;padding:15px 20px;border-radius:8px;z-index:10000;box-shadow:0 4px 12px rgba(0,0,0,0.15);max-width:300px;';
            notification.innerHTML = '<strong>提示</strong><br>WebSocket连接失败，但您仍可通过HTTP API发送消息。点击发送按钮时会自动尝试重连。';
            document.body.appendChild(notification);
            setTimeout(() => notification.remove(), 5000);
        }
        return;
    }
    
    reconnectAttempts++;
    const delay = Math.min(1000 * Math.pow(2, reconnectAttempts - 1), 10000); // 指数退避，最多10秒
    
    // 只在第一次和最后一次重连时输出日志，减少控制台噪音
    if (reconnectAttempts === 1 || reconnectAttempts === maxReconnectAttempts) {
        console.log(`将在 ${delay}ms 后尝试第 ${reconnectAttempts} 次重连...`);
    }
    
    if (reconnectTimer) {
        clearTimeout(reconnectTimer);
    }
    
    reconnectTimer = setTimeout(() => {
        if (!shouldStopReconnecting) {
            connectWebSocket().catch(err => {
                // 只在达到最大重连次数时输出错误
                if (reconnectAttempts >= maxReconnectAttempts) {
                    console.error('重连失败:', err);
                }
            });
        }
    }, delay);
}

let currentGroupSubscription = null;

function subscribeToGroupTopic(groupId) {
    // Unsubscribe from previous group if any
    if (currentGroupSubscription) {
        currentGroupSubscription.unsubscribe();
    }
    
    // Subscribe to the new group topic
    if (stompClient && stompClient.connected) {
        currentGroupSubscription = stompClient.subscribe('/topic/group/' + groupId, function (message) {
            const msg = JSON.parse(message.body);
            console.log('收到群组话题消息:', msg);
            
            // 群组话题只用于接收自己发送的消息的服务器确认
            // 这样可以替换临时消息为正式消息
            if (msg.groupId === currentGroupId && msg.me) {
                console.log('收到自己消息的服务器确认，替换临时消息');
                
                // 查找并移除最近的临时消息
                const tempMessages = document.querySelectorAll('.temporary-message');
                if (tempMessages.length > 0) {
                    // 移除最后一个临时消息（最新的）
                    const lastTempMessage = tempMessages[tempMessages.length - 1];
                    lastTempMessage.remove();
                    console.log('已移除临时消息');
                }
                
                // 显示正式消息
                appendMessage(msg);
            }
        });
        console.log('已订阅群组话题:', '/topic/group/' + groupId);
    }
}

function handleNewMessage(msg) {
    console.log('处理新消息:', msg);
    
    if (msg.messageType === 'delete') {
        // 处理删除消息通知
        handleDeleteNotification(msg);
        return;
    }

    if (msg.groupId === currentGroupId) {
        // 如果是当前群组的消息
        if (msg.me) {
            // 自己发送的消息：不在这里处理，由群组话题订阅处理
            console.log('收到自己的消息，由群组话题处理');
            return;
        } else {
            // 其他人发送的消息：直接显示
            console.log('收到其他人的消息，直接显示');
            appendMessage(msg);
        }
    } else {
        // 其他群组的消息：显示通知
        console.log('消息属于其他群组，显示提示');
        showNewMsgTip(msg);
        updateUnreadBadge(msg.groupId, 1);
    }
}

// 处理删除消息通知
function handleDeleteNotification(msg) {
    console.log('收到删除消息通知:', msg);
    
    // 使用消息ID删除
    const messageId = msg.id || msg.messageId;
    if (messageId) {
        const messageElement = document.querySelector(`[data-message-id="${messageId}"]`);
        if (messageElement) {
            messageElement.style.transition = 'opacity 0.3s ease';
            messageElement.style.opacity = '0';
            setTimeout(() => {
                if (messageElement.parentNode) {
                    messageElement.remove();
                }
            }, 300);
            console.log('已删除消息元素:', messageId);
        } else {
            console.log('未找到要删除的消息元素:', messageId);
        }
    }
}

function appendMessage(msg) {
    console.log('Appending message:', msg);
    
    const box = domCache.chatMessages;
    
    // 简单直接的逻辑：使用消息的真实ID
    const messageId = msg.id || `msg-${msg.senderId}-${new Date(msg.time).getTime()}`;
    
    // 检查是否已经存在相同的消息
    if (document.querySelector(`[data-message-id="${messageId}"]`)) {
        console.log('消息已存在，跳过:', messageId);
        return;
    }
    
    const div = document.createElement('div');
    div.className = 'message' + (msg.me ? ' me' : '');
    div.setAttribute('data-message-id', messageId);
    // 只有数字ID才能删除
    div.setAttribute('data-has-real-id', (msg.id && !isNaN(msg.id)).toString());
    div.dataset.messageData = JSON.stringify(msg);
    
    let messageContent = '';
    const senderName = msg.me ? '' : `<div class="sender-name" style="font-size: 12px; color: #666; margin-bottom: 4px;">${escapeHtml(msg.senderName)} (${escapeHtml(msg.senderRole)})</div>`;
    
    // 兼容不同的消息类型格式
    const messageType = msg.messageType.toLowerCase();
    
    if (messageType === 'voice' || messageType === 'audio') {
        // 语音消息 - 兼容VOICE和audio两种类型
        let duration = msg.duration || 0;
        let audioUrl = msg.audioUrl || '';
        
        // 如果content是JSON字符串，尝试解析
        if (!audioUrl && msg.content && msg.content.startsWith('{')) {
            try {
                const contentData = JSON.parse(msg.content);
                audioUrl = contentData.audioUrl || '';
                duration = contentData.duration || duration;
            } catch (e) {
                console.warn('解析语音消息content失败:', e);
            }
        }
        
        console.log('语音消息 - URL:', audioUrl, 'Duration:', duration);
        
        messageContent = `
            <div class="bubble voice-bubble">
                <i class="fas fa-play-circle"></i>
                <audio controls src="${audioUrl}">
                    您的浏览器不支持音频播放
                </audio>
                ${duration ? `<span class="duration">${duration}"</span>` : ''}
            </div>
        `;
    } else if (messageType === 'image') {
        // 图片消息
        let imageUrl = msg.imageUrl || '';
        
        // 如果content是JSON字符串，尝试解析
        if (!imageUrl && msg.content && msg.content.startsWith('{')) {
            try {
                const contentData = JSON.parse(msg.content);
                imageUrl = contentData.imageUrl || '';
            } catch (e) {
                console.warn('解析图片消息content失败:', e);
            }
        }
        
        messageContent = `
            <div class="image-bubble">
                <img src="${imageUrl}" alt="图片" onclick="showImageModal('${imageUrl}')">
            </div>
        `;
    } else if (messageType === 'file') {
        // 文件消息 - 兼容历史消息和JSON格式
        console.log('医护端收到文件消息:', msg);
        console.log('原始fileName:', msg.fileName);
        console.log('原始fileUrl:', msg.fileUrl);
        console.log('原始content:', msg.content);
        
        let displayName = msg.fileName || '附件';
        let fileUrl = msg.fileUrl || '';
        
        // 如果content是JSON字符串，尝试解析
        if (msg.content && msg.content.startsWith('{')) {
            try {
                const contentData = JSON.parse(msg.content);
                displayName = contentData.fileName || displayName;
                fileUrl = contentData.fileUrl || fileUrl;
                console.log('从content解析出的fileName:', contentData.fileName);
                console.log('从content解析出的fileUrl:', contentData.fileUrl);
            } catch (e) {
                console.warn('解析文件消息content失败:', e);
            }
        }
        
        console.log('最终使用的displayName:', displayName);
        console.log('最终使用的fileUrl:', fileUrl);
        
        // 如果没有文件名，尝试从URL中提取
        if (!displayName || displayName === '附件') {
            if (fileUrl) {
                const urlParts = fileUrl.split('/');
                const filename = urlParts[urlParts.length - 1];
                if (filename && filename.includes('.')) {
                    displayName = decodeURIComponent(filename);
                } else if (fileUrl.includes('.pdf')) {
                    displayName = '文档.pdf';
                } else if (fileUrl.includes('.jpg') || fileUrl.includes('.png')) {
                    displayName = '图片文件';
                } else {
                    displayName = '文件';
                }
            }
        }
        
        messageContent = `
            <div class="bubble file-bubble">
                <i class="fas fa-file-alt"></i>
                <a href="/download?path=${encodeURIComponent(fileUrl)}" target="_blank" download="${escapeHtml(displayName)}">
                    ${escapeHtml(displayName)}
                </a>
            </div>
        `;
    } else {
        // 文本消息
        messageContent = `<div class="bubble">${escapeHtml(msg.content)}</div>`;
    }
    
    // 为临时消息添加视觉标识
    const tempIndicator = msg.isTemporary ? '<span class="temp-indicator" style="opacity: 0.6;">发送中...</span>' : '';
    div.innerHTML = `${senderName}${messageContent}<div class="meta">${formatTime(msg.time)}${tempIndicator}</div>`;
    
    // 为临时消息添加样式
    if (msg.isTemporary) {
        div.style.opacity = '0.7';
        div.classList.add('temporary-message');
    }
    
    // 添加右键菜单功能
    div.addEventListener('contextmenu', function(e) {
        e.preventDefault();
        const msgData = JSON.parse(div.dataset.messageData);
        showContextMenu(e, msgData, div);
    });
    
    box.appendChild(div);
    box.scrollTop = box.scrollHeight;
}

// 其他函数保持不变...
function showNewMsgTip(msg) {
    const box = document.createElement('div');
    box.textContent = `收到来自 [${msg.senderName}] 的新消息: ${msg.content}`;
    box.style.position = 'fixed';
    box.style.right = '30px';
    box.style.bottom = '30px';
    box.style.background = '#667eea';
    box.style.color = '#fff';
    box.style.padding = '14px 22px';
    box.style.borderRadius = '8px';
    box.style.zIndex = 9999;
    box.style.boxShadow = '0 2px 8px rgba(0,0,0,0.15)';
    document.body.appendChild(box);
    setTimeout(() => box.remove(), 3500);
}

async function loadGroupList() {
    try {
        const res = await get('/medical/chat/groups');
        const groups = res.data || [];
        const userList = domCache.userList;
        userList.innerHTML = groups.length === 0 ? '<div style="padding:20px;color:#999;">暂无负责的老人</div>' : '';
        
        groups.forEach(group => {
            const div = document.createElement('div');
            div.className = 'user-item';
            const memberCount = group.members ? group.members.length : 0;
            div.innerHTML = `
                <div style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
                    <span style="flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${group.groupName}</span>
                    <span style="font-size: 11px; color: #999; margin-left: 8px;">${memberCount}人</span>
                </div>
            `;
            div.onclick = () => selectGroup(group.groupId, group.groupName);
            div.dataset.groupId = group.groupId;
            userList.appendChild(div);
        });

        await loadUnreadCounts();
    } catch (e) {
        domCache.userList.innerHTML = '<div style="padding:20px;color:red;">加载失败</div>';
    }
}

async function loadUnreadCounts() {
    try {
        const res = await get('/chat/unread-counts');
        const unreadMap = res.data || {};
        for (const groupId in unreadMap) {
            if (unreadMap.hasOwnProperty(groupId)) {
                const count = unreadMap[groupId];
                if (count > 0) {
                    updateUnreadBadge(groupId, count, true);
                } 
            }
        }
    } catch (error) {
        console.error("Failed to load unread counts:", error);
    }
}

function updateUnreadBadge(groupId, count, isAbsolute = false) {
    const groupItem = document.querySelector(`.user-item[data-group-id='${groupId}']`);
    if (!groupItem) return;

    let badge = groupItem.querySelector('.unread-badge');
    if (!badge) {
        badge = document.createElement('span');
        badge.className = 'unread-badge';
        badge.style.cssText = `
            position: absolute;
            top: 8px;
            right: 8px;
            background: #ff4757;
            color: white;
            border-radius: 10px;
            padding: 2px 6px;
            font-size: 11px;
            min-width: 16px;
            text-align: center;
            display: none;
            z-index: 1;
        `;
        groupItem.style.position = 'relative';
        groupItem.appendChild(badge);
    }

    let newCount;
    if (isAbsolute) {
        newCount = count;
    } else {
        const currentCount = parseInt(badge.textContent || '0');
        newCount = currentCount + count;
    }

    if (newCount > 0) {
        badge.textContent = newCount > 99 ? '99+' : newCount;
        badge.style.display = 'block';
    } else {
        badge.style.display = 'none';
    }
}

async function selectGroup(groupId, groupName) {
    currentGroupId = groupId;
    currentGroupName = groupName;
    
    try {
        const groupInfoRes = await get(`/medical/chat/group/${groupId}/info`);
        const groupInfo = groupInfoRes.data;
        
        const memberCount = groupInfo.members ? groupInfo.members.length : 0;
        const memberNames = groupInfo.members ? 
            groupInfo.members.map(m => m.realName || m.username).join('、') : '';
        
        document.getElementById('chatHeader').innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
                <div style="flex: 1;">
                    <div style="font-size: 16px; font-weight: 600; color: #333; margin-bottom: 4px;">${groupInfo.groupName}</div>
                    <div style="font-size: 12px; color: #666;">共${memberCount}人在群聊中</div>
                </div>
                <div style="text-align: right; max-width: 250px;">
                    <div style="font-size: 11px; color: #999; margin-bottom: 2px;">群成员</div>
                    <div style="font-size: 12px; color: #666; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${memberNames}">
                        ${memberNames}
                    </div>
                </div>
            </div>
        `;
    } catch (error) {
        console.error("Failed to load group info:", error);
        document.getElementById('chatHeader').textContent = `${groupName}`;
    }
    
    document.getElementById('chatInput').disabled = false;
    document.getElementById('sendBtn').disabled = false;

    document.querySelectorAll('.user-item').forEach(item => {
        item.classList.remove('active');
        if (item.dataset.groupId == groupId) {
            item.classList.add('active');
        }
    });

    subscribeToGroupTopic(groupId);

    try {
        await post(`/chat/groups/${groupId}/read`);
        const badge = document.querySelector(`.user-item[data-group-id='${groupId}'] .unread-badge`);
        if (badge) {
            badge.style.display = 'none';
            badge.textContent = '0';
        }
    } catch (error) {
        console.error("Failed to mark as read:", error);
    }

    await loadGroupMessages();
}

async function loadGroupMessages() {
    if (!currentGroupId) return;
    try {
        console.log('loadGroupMessages - 开始加载群组消息, groupId:', currentGroupId);
        console.log('loadGroupMessages - 当前用户:', currentUser);
        console.log('loadGroupMessages - Token:', localStorage.getItem('token'));
        
        const apiUrl = `/medical/chat/group/${currentGroupId}/messages`;
        console.log('loadGroupMessages - 调用API:', apiUrl);
        
        const res = await get(apiUrl);
        console.log('loadGroupMessages - API响应:', res);
        
        const messagePage = res.data || { records: [] };
        console.log('loadGroupMessages - 消息数据:', messagePage);
        
        const box = document.getElementById('chatMessages');
        box.innerHTML = '';
        
        if (messagePage.records && messagePage.records.length > 0) {
            console.log('loadGroupMessages - 渲染消息数量:', messagePage.records.length);
            messagePage.records.forEach(appendMessage);
        } else {
            console.log('loadGroupMessages - 没有消息记录');
            box.innerHTML = '<div style="color:#999;text-align:center;padding:20px;">暂无消息</div>';
        }
    } catch (e) {
        console.error('loadGroupMessages - 加载消息失败:', e);
        console.error('loadGroupMessages - 错误详情:', e.message, e.stack);
        document.getElementById('chatMessages').innerHTML = `<div style="color:red;">消息加载失败: ${e.message}</div>`;
    }
}

function sendMessage() {
    const input = document.getElementById('chatInput');
    const content = input.value.trim();
    if (!content) return;
    
    const messageObject = {
        messageType: 'TEXT',
        content: content
    };
    
    sendMessageWithContent(messageObject);
    input.value = '';
}

// 语音录制相关变量
let mediaRecorder = null;
let audioChunks = [];
let isRecording = false;

// 语音录制功能
async function startRecordAudio() {
    const recordBtn = document.getElementById('recordBtn');
    
    if (!isRecording) {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            mediaRecorder = new MediaRecorder(stream);
            audioChunks = [];
            
            mediaRecorder.ondataavailable = (event) => {
                audioChunks.push(event.data);
            };
            
            mediaRecorder.onstop = async () => {
                const audioBlob = new Blob(audioChunks, { type: 'audio/wav' });
                await sendAudioMessage(audioBlob);
                stream.getTracks().forEach(track => track.stop());
            };
            
            mediaRecorder.start();
            isRecording = true;
            
            recordBtn.classList.add('recording');
            recordBtn.title = '停止录制';
            recordBtn.innerHTML = `
                <svg class="icon" viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg">
                    <rect x="320" y="320" width="384" height="384" rx="32" fill="#fff"/>
                </svg>
            `;
            
        } catch (error) {
            console.error('无法访问麦克风:', error);
            alert('无法访问麦克风，请检查浏览器权限设置');
        }
    } else {
        if (mediaRecorder && mediaRecorder.state === 'recording') {
            mediaRecorder.stop();
        }
        isRecording = false;
        
        recordBtn.classList.remove('recording');
        recordBtn.title = '录制语音';
        recordBtn.innerHTML = `
            <svg class="icon" viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg">
                <path d="M512 704c70.7 0 128-57.3 128-128V320c0-70.7-57.3-128-128-128s-128 57.3-128 128v256c0 70.7 57.3 128 128 128z m256-128c0 141.4-114.6 256-256 256s-256-114.6-256-256h64c0 105.9 86.1 192 192 192s192-86.1 192-192h64z" fill="#666"/>
            </svg>
        `;
    }
}

// 发送语音消息 - 修复版
async function sendAudioMessage(audioBlob) {
    if (!currentGroupId) return;

    try {
        // 显示发送中状态
        const recordBtn = document.getElementById('recordBtn');
        recordBtn.style.opacity = '0.5';
        recordBtn.disabled = true;

        const formData = new FormData();
        formData.append('audio', audioBlob, 'voice_message.wav');
        formData.append('groupId', currentGroupId);

        const response = await fetch('/upload-audio', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            },
            body: formData
        });

        if (response.ok) {
            const result = await response.json();
            if (!result.data || !result.data.audioUrl) {
                throw new Error('上传响应中缺少audioUrl');
            }

            // 通过WebSocket发送语音消息
            const audioMessage = {
                messageType: 'VOICE',
                content: '[语音消息]',
                audioUrl: result.data.audioUrl,
                duration: result.data.duration || 0
            };
            
            console.log('发送语音消息:', audioMessage);
            sendMessageWithContent(audioMessage);

        } else {
            const errorText = await response.text();
            throw new Error(`语音上传失败: ${errorText}`);
        }

    } catch (error) {
        console.error('发送语音消息失败:', error);
        alert('语音消息发送失败: ' + error.message);
    } finally {
        // 恢复录音按钮状态
        const recordBtn = document.getElementById('recordBtn');
        recordBtn.style.opacity = '1';
        recordBtn.disabled = false;
    }
}

// 播放音频 - 修复版
function playAudio(audioUrl, button) {
    if (!audioUrl) {
        console.error('音频URL为空');
        alert('音频文件不存在');
        return;
    }
    
    const isPlaying = button.getAttribute('data-playing') === 'true';
    
    if (isPlaying) {
        if (window.currentAudio) {
            window.currentAudio.pause();
            window.currentAudio.currentTime = 0;
        }
        button.innerHTML = '<i class="fas fa-play"></i>';
        button.setAttribute('data-playing', 'false');
        return;
    }
    
    // 停止其他正在播放的音频
    if (window.currentAudio) {
        window.currentAudio.pause();
        document.querySelectorAll('.play-btn[data-playing="true"]').forEach(btn => {
            btn.innerHTML = '<i class="fas fa-play"></i>';
            btn.setAttribute('data-playing', 'false');
        });
    }
    
    const audio = new Audio(audioUrl);
    window.currentAudio = audio;
    
    button.innerHTML = '<i class="fas fa-pause"></i>';
    button.setAttribute('data-playing', 'true');
    
    audio.onended = () => {
        button.innerHTML = '<i class="fas fa-play"></i>';
        button.setAttribute('data-playing', 'false');
        window.currentAudio = null;
    };
    
    audio.onerror = (error) => {
        console.error('音频播放失败:', error);
        button.innerHTML = '<i class="fas fa-play"></i>';
        button.setAttribute('data-playing', 'false');
        window.currentAudio = null;
        alert('音频播放失败，请检查文件是否存在');
    };
    
    audio.play().catch(error => {
        console.error('音频播放失败:', error);
        button.innerHTML = '<i class="fas fa-play"></i>';
        button.setAttribute('data-playing', 'false');
        window.currentAudio = null;
        alert('音频播放失败: ' + error.message);
    });
}

// 删除消息 - 修复版
async function deleteMessage(message, messageElement) {
    if (!confirm('确定要删除这条消息吗？')) {
        return;
    }
    
    try {
        // 获取消息ID - 必须是数字类型的数据库ID
        let messageId = message.id;
        
        // 如果message.id不存在或不是数字，尝试从data-message-id获取
        if (!messageId || isNaN(messageId)) {
            const dataMessageId = messageElement.getAttribute('data-message-id');
            // 检查data-message-id是否是数字
            if (dataMessageId && !isNaN(dataMessageId)) {
                messageId = dataMessageId;
            }
        }
        
        // 验证messageId是否为有效的数字
        if (!messageId || isNaN(messageId)) {
            console.log('无效的消息ID:', messageId, '消息对象:', message);
            alert('无法删除：这是临时消息或消息ID无效');
            return;
        }
        
        messageId = parseInt(messageId);
        console.log('准备删除消息:', messageId, message);
        
        // 立即从界面移除
        if (messageElement) {
            messageElement.style.opacity = '0.5';
            messageElement.style.pointerEvents = 'none';
        }
        
        // 使用原有的REST API删除消息（逻辑删除）
        try {
            console.log('调用REST API删除消息:', messageId);
            const response = await fetch(`/api/chat/message/${messageId}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('token')}`,
                    'Content-Type': 'application/json'
                }
            });
            
            const result = await response.json();
            console.log('删除API响应:', result);
            
            if (result.code === 200) {
                console.log('消息逻辑删除成功');
                // 通过WebSocket通知其他用户
                if (stompClient && stompClient.connected) {
                    const deleteNotification = {
                        messageType: 'delete',
                        messageId: parseInt(messageId),
                        groupId: currentGroupId
                    };
                    stompClient.send(`/app/chat/group/${currentGroupId}`, {}, JSON.stringify(deleteNotification));
                }
            } else {
                throw new Error(result.message || '删除失败');
            }
        } catch (apiError) {
            console.error('REST API删除失败，尝试WebSocket方式:', apiError);
            // 如果REST API失败，回退到WebSocket方式
            if (stompClient && stompClient.connected) {
                const deleteNotification = {
                    messageType: 'delete',
                    messageId: parseInt(messageId),
                    groupId: currentGroupId
                };
                stompClient.send(`/app/chat/group/${currentGroupId}`, {}, JSON.stringify(deleteNotification));
            }
        }
        
        // 立即移除元素（不等待服务器响应）
        setTimeout(() => {
            if (messageElement && messageElement.parentNode) {
                messageElement.remove();
                console.log('消息元素已移除');
            }
        }, 300);
        
    } catch (error) {
        console.error('删除消息失败:', error);
        alert('删除消息失败: ' + error.message);
        
        if (messageElement) {
            messageElement.style.opacity = '1';
            messageElement.style.pointerEvents = 'auto';
        }
    }
}

// 显示右键菜单
function showContextMenu(event, message, messageElement) {
    const existingMenu = document.getElementById('contextMenu');
    if (existingMenu) {
        existingMenu.remove();
    }
    
    if (!message.me) {
        return;
    }
    
    // 检查消息是否有真实的数据库ID
    const hasRealId = messageElement.getAttribute('data-has-real-id') === 'true';
    if (!hasRealId) {
        console.log('消息没有有效的数据库ID，无法删除');
        return;
    }
    
    const menu = document.createElement('div');
    menu.id = 'contextMenu';
    menu.style.cssText = `
        position: fixed;
        left: ${event.pageX}px;
        top: ${event.pageY}px;
        background: white;
        border: 1px solid #ddd;
        border-radius: 4px;
        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        z-index: 10000;
        min-width: 100px;
    `;
    
    const deleteOption = document.createElement('div');
    deleteOption.textContent = '删除消息';
    deleteOption.style.cssText = `
        padding: 8px 12px;
        cursor: pointer;
        color: #ff4757;
        font-size: 14px;
        border-radius: 4px;
    `;
    
    deleteOption.addEventListener('mouseenter', function() {
        this.style.background = '#f8f9fa';
    });
    
    deleteOption.addEventListener('mouseleave', function() {
        this.style.background = 'white';
    });
    
    deleteOption.addEventListener('click', function() {
        deleteMessage(message, messageElement);
        menu.remove();
    });
    
    menu.appendChild(deleteOption);
    document.body.appendChild(menu);
    
    document.addEventListener('click', function closeMenu(e) {
        if (!menu.contains(e.target)) {
            menu.remove();
            document.removeEventListener('click', closeMenu);
        }
    });
}

// 其他功能函数...
function onImageSelected(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    if (!file.type.startsWith('image/')) {
        alert('请选择图片文件');
        return;
    }
    
    if (file.size > 5 * 1024 * 1024) {
        alert('图片文件不能超过5MB');
        return;
    }
    
    sendImageMessage(file);
}

async function sendImageMessage(imageFile) {
    if (!currentGroupId) return;
    
    try {
        const formData = new FormData();
        formData.append('image', imageFile);
        formData.append('groupId', currentGroupId);
        
        const response = await fetch('/upload-image', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            },
            body: formData
        });
        
        if (response.ok) {
            const result = await response.json();
            const imageMessage = {
                messageType: 'IMAGE',
                imageUrl: result.data.imageUrl
            };
            stompClient.send(`/app/chat/group/${currentGroupId}`, {}, JSON.stringify(imageMessage));
        } else {
            console.error('图片上传失败');
            alert('图片发送失败');
        }
        
    } catch (error) {
        console.error('发送图片消息失败:', error);
        alert('图片发送失败');
    }
}

function onFileSelected(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    if (file.size > 10 * 1024 * 1024) {
        alert('文件大小不能超过10MB');
        return;
    }
    
    sendFileMessage(file);
}

async function sendFileMessage(file) {
    if (!currentGroupId) return;
    
    try {
        const formData = new FormData();
        formData.append('file', file);
        
        const response = await post('/upload/file', formData, true);
        
        if (response && response.data) {
            const fileMessage = {
                messageType: 'FILE',
                fileName: response.data.fileName || response.data.originalFilename,
                fileUrl: response.data.fileUrl || response.data.url || response.data.imageUrl || response.data.audioUrl
            };
            stompClient.send(`/app/chat/group/${currentGroupId}`, {}, JSON.stringify(fileMessage));
        } else {
            throw new Error('文件上传响应格式错误');
        }
        
    } catch (error) {
        console.error('发送文件消息失败:', error);
        alert('文件发送失败: ' + error.message);
    }
}

function showImageModal(imageUrl) {
    if (!imageUrl) return;
    
    const modal = document.createElement('div');
    modal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0,0,0,0.8);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 10000;
        cursor: pointer;
    `;
    
    const img = document.createElement('img');
    img.src = imageUrl;
    img.style.cssText = `
        max-width: 90%;
        max-height: 90%;
        border-radius: 8px;
    `;
    
    modal.appendChild(img);
    modal.onclick = () => modal.remove();
    document.body.appendChild(modal);
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;','\'':'&#39;'}[c]));
}

function formatTime(t, returnDate = false) {
    if (!t) return returnDate ? new Date() : '';
    
    try {
        let date;
        
        if (typeof t === 'string') {
            if (t.includes('T') && !t.includes('Z') && !t.includes('+')) {
                const localTimeStr = t.replace('T', ' ');
                date = new Date(localTimeStr);
            } else {
                date = new Date(t);
            }
        } else {
            date = new Date(t);
        }
        
        if (isNaN(date.getTime())) {
            console.warn('Invalid date format:', t);
            return returnDate ? new Date() : '';
        }
        
        if (returnDate) {
            return date;
        }
        
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        
        return `${month}-${day} ${hours}:${minutes}`;
    } catch (error) {
        console.error('Error formatting time:', error, 'Input:', t);
        return returnDate ? new Date() : '';
    }
}
// 图片和文件上传功能

// 图片选择处理
function onImageSelected(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    if (!file.type.startsWith('image/')) {
        alert('请选择图片文件');
        return;
    }
    
    if (file.size > 5 * 1024 * 1024) {
        alert('图片文件不能超过5MB');
        return;
    }
    
    sendImageMessage(file);
    event.target.value = ''; // 清空文件输入
}

// 文件选择处理
function onFileSelected(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    if (file.size > 10 * 1024 * 1024) {
        alert('文件大小不能超过10MB');
        return;
    }
    
    sendFileMessage(file);
    event.target.value = ''; // 清空文件输入
}

// 发送图片消息
async function sendImageMessage(imageFile) {
    if (!currentGroupId) return;
    
    try {
        const formData = new FormData();
        formData.append('file', imageFile);
        
        const response = await post('/upload/file', formData, true);
        
        if (response && response.data) {
            const imageMessage = {
                messageType: 'IMAGE',
                content: `[图片] ${response.data.fileName || response.data.originalFilename}`,
                fileName: response.data.fileName || response.data.originalFilename,
                imageUrl: response.data.imageUrl || response.data.url
            };
            // 使用sendMessageWithContent函数来立即显示消息并发送到服务器
            sendMessageWithContent(imageMessage);
        } else {
            throw new Error('图片上传响应格式错误');
        }
        
    } catch (error) {
        console.error('发送图片消息失败:', error);
        alert('图片发送失败: ' + error.message);
    }
}

// 发送文件消息
async function sendFileMessage(file) {
    if (!currentGroupId) return;
    
    try {
        const formData = new FormData();
        formData.append('file', file);
        
        const response = await post('/upload/file', formData, true);
        
        if (response && response.data) {
            const fileMessage = {
                messageType: 'FILE',
                content: `[文件] ${response.data.fileName || response.data.originalFilename}`,
                fileName: response.data.fileName || response.data.originalFilename,
                fileUrl: response.data.fileUrl || response.data.url
            };
            // 使用sendMessageWithContent函数来立即显示消息并发送到服务器
            sendMessageWithContent(fileMessage);
        } else {
            throw new Error('文件上传响应格式错误');
        }
        
    } catch (error) {
        console.error('发送文件消息失败:', error);
        alert('文件发送失败: ' + error.message);
    }
}

// 继续原有的图片选择处理逻辑
function onImageSelected_old(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    if (!file.type.startsWith('image/')) {
        alert('请选择图片文件');
        return;
    }
    
    if (file.size > 5 * 1024 * 1024) {
        alert('图片文件不能超过5MB');
        return;
    }
    
    sendImageMessage(file);
    event.target.value = ''; // 清空文件输入
}

// 文件选择处理
function onFileSelected(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    if (file.size > 10 * 1024 * 1024) {
        alert('文件大小不能超过10MB');
        return;
    }
    
    sendFileMessage(file);
    event.target.value = ''; // 清空文件输入
}

// 发送图片消息
async function sendImageMessage(imageFile) {
    if (!currentGroupId) return;
    
    try {
        const formData = new FormData();
        formData.append('file', imageFile);
        
        const response = await post('/upload/file', formData, true);
        
        if (response && response.data) {
            const imageMessage = {
                messageType: 'IMAGE',
                content: `[图片] ${response.data.fileName || response.data.originalFilename}`,
                fileName: response.data.fileName || response.data.originalFilename,
                imageUrl: response.data.imageUrl || response.data.url
            };
            // 使用sendMessage函数来立即显示消息并发送到服务器
            sendMessageWithContent(imageMessage);
        } else {
            throw new Error('图片上传响应格式错误');
        }
        
    } catch (error) {
        console.error('发送图片消息失败:', error);
        alert('图片发送失败: ' + error.message);
    }
}

// 发送文件消息
async function sendFileMessage(file) {
    if (!currentGroupId) return;
    
    try {
        const formData = new FormData();
        formData.append('file', file);
        
        const response = await post('/upload/file', formData, true);
        
        if (response && response.data) {
            const fileMessage = {
                messageType: 'FILE',
                content: `[文件] ${response.data.fileName || response.data.originalFilename}`,
                fileName: response.data.fileName || response.data.originalFilename,
                fileUrl: response.data.fileUrl || response.data.url
            };
            // 使用sendMessage函数来立即显示消息并发送到服务器
            sendMessageWithContent(fileMessage);
        } else {
            throw new Error('文件上传响应格式错误');
        }
        
    } catch (error) {
        console.error('发送文件消息失败:', error);
        alert('文件发送失败: ' + error.message);
    }
}

// 发送带内容的消息（用于图片、文件、语音等）
async function sendMessageWithContent(messageObject) {
    if (!currentGroupId) {
        console.error('无法发送消息: 未选择群组');
        alert('请先选择一个群组');
        return;
    }
    
    try {
        // 生成唯一的临时ID
        const tempId = 'temp_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
        
        // 立即在本地显示消息（乐观更新）
        const localMessage = {
            ...messageObject,
            me: true,
            senderName: currentUser.realName || currentUser.username,
            senderRole: currentUser.role,
            time: new Date().toISOString(),
            id: tempId,
            isTemporary: true // 标记为临时消息
        };
        appendMessage(localMessage);
        
        // 尝试通过WebSocket发送（强制重连，确保连接可用）
        const connected = await ensureWebSocketConnected(true); // 强制重连
        let sendSuccess = false;
        
        if (connected && stompClient && stompClient.connected) {
            try {
                stompClient.send(`/app/chat/group/${currentGroupId}`, {}, JSON.stringify(messageObject));
                console.log('消息已通过WebSocket发送到服务器');
                sendSuccess = true;
                
                // 更新临时消息状态
                setTimeout(() => {
                    const tempElement = document.getElementById(tempId);
                    if (tempElement) {
                        tempElement.classList.remove('temporary-message');
                        tempElement.style.opacity = '1';
                        const indicator = tempElement.querySelector('.temp-indicator');
                        if (indicator) {
                            indicator.remove();
                        }
                    }
                }, 500);
            } catch (sendError) {
                console.warn('WebSocket发送失败，将使用HTTP API:', sendError);
                sendSuccess = false;
            }
        }
        
        // 如果WebSocket发送失败，使用HTTP API作为降级方案
        if (!sendSuccess) {
            console.log('使用HTTP API发送消息（WebSocket降级方案）');
            try {
                const response = await fetch(`/api/medical/chat/group/${currentGroupId}/send`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${localStorage.getItem('token')}`
                    },
                    body: JSON.stringify(messageObject)
                });
                
                if (response.ok) {
                    const result = await response.json();
                    if (result.code === 200 && result.data) {
                        console.log('消息已通过HTTP API发送成功');
                        
                        // 更新临时消息，使用服务器返回的真实消息数据
                        const tempElement = document.getElementById(tempId);
                        if (tempElement) {
                            const serverMessage = result.data;
                            // 更新消息ID
                            tempElement.dataset.messageData = JSON.stringify(serverMessage);
                            tempElement.id = serverMessage.id || tempId;
                            
                            // 移除临时标识
                            tempElement.classList.remove('temporary-message');
                            tempElement.style.opacity = '1';
                            const indicator = tempElement.querySelector('.temp-indicator');
                            if (indicator) {
                                indicator.remove();
                            }
                        }
                        
                        // 重新加载消息列表以确保同步
                        setTimeout(() => {
                            loadGroupMessages();
                        }, 500);
                    } else {
                        throw new Error(result.message || '发送失败');
                    }
                } else {
                    const errorText = await response.text();
                    throw new Error(`HTTP请求失败: ${errorText}`);
                }
            } catch (httpError) {
                console.error('HTTP API发送消息失败:', httpError);
                // 标记消息为发送失败
                const tempElement = document.getElementById(tempId);
                if (tempElement) {
                    tempElement.style.opacity = '0.5';
                    const indicator = tempElement.querySelector('.temp-indicator');
                    if (indicator) {
                        indicator.textContent = '发送失败';
                        indicator.style.color = 'red';
                    }
                }
                alert('消息发送失败: ' + httpError.message);
            }
        }
    } catch (e) {
        console.error('Send message error:', e);
        alert('发送消息失败: ' + e.message);
    }
}

// 显示图片模态框
function showImageModal(imageUrl) {
    const modal = document.createElement('div');
    modal.style.cssText = `position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.7); display:flex; align-items:center; justify-content:center; z-index:1001;`;
    modal.innerHTML = `<img src="${imageUrl}" style="max-width:90%; max-height:90%;">`;
    modal.onclick = () => modal.remove();
    document.body.appendChild(modal);
}

// 工具函数
function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>\"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', '\'': '&#39;' }[c]));
}

function formatTime(t) {
    if (!t) return '';
    try {
        const date = new Date(t);
        if (isNaN(date.getTime())) {
            console.error('Invalid date:', t);
            return '';
        }
        
        const now = new Date();
        const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const messageDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());
        
        const timeStr = date.toLocaleTimeString('zh-CN', { 
            hour: '2-digit', 
            minute: '2-digit',
            hour12: false 
        });
        
        if (messageDate.getTime() === today.getTime()) {
            return timeStr;
        } else if (messageDate.getTime() === today.getTime() - 24 * 60 * 60 * 1000) {
            return `昨天 ${timeStr}`;
        } else {
            return `${date.getMonth() + 1}/${date.getDate()} ${timeStr}`;
        }
    } catch (error) {
        console.error('Error formatting time:', error, 'Input:', t);
        return '';
    }
}