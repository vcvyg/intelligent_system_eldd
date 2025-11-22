// 医护端群聊脚本 - 修复版

let currentGroupId = null;
let currentGroupName = '';
let stompClient = null;
let currentUser = null;

document.addEventListener('DOMContentLoaded', async () => {
    currentUser = checkLogin();
    if (!currentUser || currentUser.role !== 'MEDICAL') {
        alert('权限不足或登录已过期');
        logout();
        return;
    }
    document.getElementById('welcomeText').textContent = `欢迎，${currentUser.username}！`;

    await loadGroupList();
    connectWebSocket();

    document.getElementById('chatInput').addEventListener('keydown', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });
});

function connectWebSocket() {
    const token = localStorage.getItem('token');
    const socket = new SockJS(`/ws-chat?token=${encodeURIComponent(token)}`);
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('WebSocket connected:', frame);
        
        // Subscribe to personal message queue
        stompClient.subscribe('/user/queue/group-messages', function (message) {
            const msg = JSON.parse(message.body);
            handleNewMessage(msg);
        });
        
        // Subscribe to current group topic for immediate message display
        if (currentGroupId) {
            subscribeToGroupTopic(currentGroupId);
        }
    }, function(error) {
        console.error('WebSocket connection error:', error);
    });
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
            console.log('Received group message:', msg);
            
            if (msg.groupId === currentGroupId) {
                // 对于语音、图片、文件消息，总是显示
                // 对于文本消息，只显示其他人发送的（避免重复）
                if (msg.messageType === 'VOICE' || msg.messageType === 'IMAGE' || msg.messageType === 'FILE' || !msg.me) {
                    appendMessage(msg);
                }
            }
        });
        console.log('Subscribed to group topic:', '/topic/group/' + groupId);
    }
}

function handleNewMessage(msg) {
    if (msg.groupId === currentGroupId) {
        if (msg.messageType === 'delete') {
            // 处理删除消息通知
            handleDeleteNotification(msg);
        } else {
            appendMessage(msg);
        }
    } else {
        if (msg.messageType !== 'delete') {
            showNewMsgTip(msg);
            updateUnreadBadge(msg.groupId, 1);
        }
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
    
    const box = document.getElementById('chatMessages');
    
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
    
    div.innerHTML = `${senderName}${messageContent}<div class="meta">${formatTime(msg.time)}</div>`;
    
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
        const userList = document.getElementById('userList');
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
        document.getElementById('userList').innerHTML = '<div style="padding:20px;color:red;">加载失败</div>';
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
    if (!content || !currentGroupId || !stompClient || !stompClient.connected) {
        return;
    }
    try {
        // 发送消息到服务器，等待WebSocket返回后显示
        const messageObject = {
            messageType: 'TEXT',
            content: content
        };
        stompClient.send(`/app/chat/group/${currentGroupId}`, {}, JSON.stringify(messageObject));
        input.value = '';
    } catch (e) {
        console.error('Send message error:', e);
    }
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
                audioUrl: result.data.audioUrl,
                duration: result.data.duration || 0
            };
            
            console.log('发送语音消息:', audioMessage);
            stompClient.send(`/app/chat/group/${currentGroupId}`, {}, JSON.stringify(audioMessage));

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