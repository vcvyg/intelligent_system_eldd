// 子女端群聊脚本

const MOBILE_BREAKPOINT = 900;
let currentGroupId = null;
let currentGroupName = '';
let stompClient = null;
let currentUser = null;
let mobileListPreferred = true;

// UI Elements
let chatInput, sendBtn, fileBtn, fileInput, imageBtn, imageInput, voiceBtn, chatMessages, contextMenu, deleteMessageBtn, chatHeaderContent, chatContainer, chatBackBtn, toggleExtrasBtn, chatExtraPanel;

// Voice Recording
let mediaRecorder = null;
let audioChunks = [];
let isRecording = false;

// Context Menu
let currentMessageElement = null;

document.addEventListener('DOMContentLoaded', async () => {
    currentUser = checkLogin();
    if (!currentUser || currentUser.role !== 'FAMILY') {
        alert('权限不足或登录已过期');
        logout();
        return;
    }
    document.getElementById('welcomeText').textContent = `欢迎，${currentUser.username}！`;

    // Initialize UI Elements
    chatInput = document.getElementById('chatInput');
    sendBtn = document.getElementById('sendBtn');
    fileBtn = document.getElementById('file-btn');
    fileInput = document.getElementById('file-input');
    imageBtn = document.getElementById('image-btn');
    imageInput = document.getElementById('image-input');
    voiceBtn = document.getElementById('voice-btn');
    chatMessages = document.getElementById('chatMessages');
    contextMenu = document.getElementById('context-menu');
    deleteMessageBtn = document.getElementById('delete-message');
    chatHeaderContent = document.getElementById('chatHeaderContent');
    chatContainer = document.querySelector('.chat-container');
    chatBackBtn = document.getElementById('chatBackBtn');
    toggleExtrasBtn = document.getElementById('toggleExtrasBtn');
    chatExtraPanel = document.getElementById('chatExtraPanel');

    if (chatBackBtn) {
        chatBackBtn.addEventListener('click', () => {
            mobileListPreferred = true;
            exitMobileChatView();
        });
    }

    window.addEventListener('resize', handleResponsiveLayout);
    handleResponsiveLayout();

    await loadGroupList();
    connectWebSocket();

    // Event Listeners
    sendBtn.addEventListener('click', sendTextMessage);
    chatInput.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendTextMessage();
        }
    });

    if (fileBtn) {
        fileBtn.addEventListener('click', (event) => {
            event.stopPropagation();
            if (fileBtn.disabled) return;
            closeExtraPanel();
            fileInput.click();
        });
    }
    fileInput.addEventListener('change', uploadFile);
    if (imageBtn) {
        imageBtn.addEventListener('click', (event) => {
            event.stopPropagation();
            if (imageBtn.disabled) return;
            closeExtraPanel();
            imageInput.click();
        });
    }
    imageInput.addEventListener('change', uploadImage);
    voiceBtn.addEventListener('click', function () {
        console.log('Voice button clicked, isRecording:', isRecording);
        toggleRecording();
    });

    if (toggleExtrasBtn && chatExtraPanel) {
        toggleExtrasBtn.addEventListener('click', (event) => {
            if (toggleExtrasBtn.disabled) return;
            event.stopPropagation();
            const isOpening = !chatExtraPanel.classList.contains('active');
            chatExtraPanel.classList.toggle('active', isOpening);
            toggleExtrasBtn.classList.toggle('active', isOpening);
            toggleExtrasBtn.setAttribute('aria-expanded', isOpening ? 'true' : 'false');
        });
    }

    // Context Menu Listeners
    chatMessages.addEventListener('contextmenu', showContextMenu);
    document.addEventListener('click', (event) => {
        if (contextMenu) {
            contextMenu.style.display = 'none';
            currentMessageElement = null;
        }
        if (chatExtraPanel && toggleExtrasBtn) {
            const clickedInsidePanel = chatExtraPanel.contains(event.target);
            const clickedToggle = toggleExtrasBtn.contains(event.target);
            if (!clickedInsidePanel && !clickedToggle) {
                closeExtraPanel();
            }
        }
    });
    deleteMessageBtn.addEventListener('click', deleteMessage);
});

let currentGroupSubscription = null;

function connectWebSocket() {
    const token = localStorage.getItem('token');
    console.log('正在连接WebSocket...');
    // 通过URL参数传递token，与其他聊天页面保持一致
    const socket = new SockJS(`/ws-chat?token=${encodeURIComponent(token)}`);
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('WebSocket连接成功:', frame);
        
        // Subscribe to personal message queue
        stompClient.subscribe('/user/queue/group-messages', function (message) {
            console.log('收到个人队列消息:', message.body);
            const msg = JSON.parse(message.body);
            handleNewMessage(msg);
        });
        
        // Subscribe to current group topic for immediate message display
        if (currentGroupId) {
            subscribeToGroupTopic(currentGroupId);
        }
    }, function (error) {
        console.error('WebSocket连接失败:', error);
        // 重连逻辑
        setTimeout(connectWebSocket, 5000);
    });
}

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
        const elementToRemove = document.querySelector(`[data-message-id='${msg.id}']`);
        if (elementToRemove) {
            elementToRemove.remove();
        }
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

function appendMessage(msg) {
    const box = chatMessages;
    const div = document.createElement('div');
    div.className = 'message' + (msg.me ? ' me' : '');
    // 确保消息ID存在
    if (msg.id) {
        div.dataset.messageId = msg.id; // Store message ID for deletion
    }

    const senderName = msg.me ? '' : `<div class="sender-name">${escapeHtml(msg.senderName)} (${escapeHtml(msg.senderRole)})</div>`;
    let messageBubble = '';

    switch (msg.messageType) {
        case 'VOICE':
            let duration = msg.duration || 0;
            let audioUrl = msg.audioUrl || '';

            if (!audioUrl && msg.content && msg.content.startsWith('{')) {
                try {
                    const contentData = JSON.parse(msg.content);
                    audioUrl = contentData.audioUrl || '';
                    duration = contentData.duration || duration;
                } catch (e) {
                    console.warn('解析语音消息content失败:', e);
                }
            }

            if (audioUrl) {
                messageBubble = `
                    <div class="bubble voice-bubble">
                        <button type="button" class="voice-play-btn" onclick="toggleVoicePlayback('${audioUrl}', this)">
                            <i class="fas fa-play"></i>
                        </button>
                        <div class="voice-wave">
                            <span></span>
                            <span></span>
                            <span></span>
                        </div>
                        ${duration ? `<span class="duration">${duration}"</span>` : ''}
                    </div>
                `;
            } else {
                messageBubble = `<div class="bubble">${escapeHtml(msg.content)}</div>`;
            }
            break;
        case 'FILE':
            if (msg.fileUrl) {
                messageBubble = `<div class="bubble file-bubble">
                                    <i class="fas fa-file-alt"></i>
                                    <a href="/download?path=${encodeURIComponent(msg.fileUrl)}" target="_blank" download="${escapeHtml(msg.fileName || '附件')}">
                                        ${escapeHtml(msg.fileName || '附件')}
                                    </a>
                                 </div>`;
            } else {
                messageBubble = `<div class="bubble">${escapeHtml(msg.content)}</div>`;
            }
            break;
        case 'IMAGE':
            if (msg.imageUrl) {
                messageBubble = `<div class="image-bubble">
                                    <img src="${msg.imageUrl}" alt="图片" onclick="showImageModal('${msg.imageUrl}')">
                                 </div>`;
            } else {
                messageBubble = `<div class="bubble">${escapeHtml(msg.content)}</div>`;
            }
            break;
        default: // TEXT
            messageBubble = `<div class="bubble">${escapeHtml(msg.content)}</div>`;
            break;
    }

    // 为临时消息添加视觉标识
    const tempIndicator = msg.isTemporary ? '<span class="temp-indicator" style="opacity: 0.6;">发送中...</span>' : '';
    div.innerHTML = `${senderName}${messageBubble}<div class="meta">${formatTime(msg.time)}${tempIndicator}</div>`;
    
    // 为临时消息添加样式
    if (msg.isTemporary) {
        div.style.opacity = '0.7';
        div.classList.add('temporary-message');
    }
    
    box.appendChild(div);
    box.scrollTop = box.scrollHeight;
}

function showImageModal(imageUrl) {
    const modal = document.createElement('div');
    modal.style.cssText = `position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.7); display:flex; align-items:center; justify-content:center; z-index:1001;`;
    modal.innerHTML = `<img src="${imageUrl}" style="max-width:90%; max-height:90%;">`;
    modal.onclick = () => modal.remove();
    document.body.appendChild(modal);
}


function showNewMsgTip(msg) {
    const box = document.createElement('div');
    box.textContent = `收到来自 [${msg.senderName}] 的新消息: ${msg.content}`;
    box.style.cssText = `position:fixed; right:30px; bottom:30px; background:#667eea; color:#fff; padding:14px 22px; border-radius:8px; z-index:9999; box-shadow:0 2px 8px rgba(0,0,0,0.15);`;
    document.body.appendChild(box);
    setTimeout(() => box.remove(), 3500);
}

async function loadGroupList() {
    try {
        const res = await get('/family/chat/groups');
        const groups = res.data || [];
        const userList = document.getElementById('userList');
        userList.innerHTML = groups.length === 0 ? '<div style="padding:20px;color:#999;">暂无关联的家人</div>' : '';

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
    }
    catch (error) {
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
        groupItem.appendChild(badge);
    }

    let newCount = isAbsolute ? count : (parseInt(badge.textContent || '0') + count);

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
    mobileListPreferred = false;
    closeExtraPanel();
    
    // 尝试获取群组详细信息，包括成员列表
    try {
        const groupInfoRes = await get(`/family/chat/group/${groupId}/info`);
        const groupInfo = groupInfoRes.data;
        
        const memberCount = groupInfo.members ? groupInfo.members.length : 0;
        const memberNames = groupInfo.members ? 
            groupInfo.members.map(m => m.realName || m.username).join('、') : '';
        
        if (chatHeaderContent) {
            const displayTitle = (groupInfo.groupName || '')
                .replace(/的沟通群$/, '老人的沟通群');
            const memberNamesHtml = memberNames
                ? `<span title="${memberNames}">${memberNames}</span>`
                : '';
            chatHeaderContent.innerHTML = `
                <div class="chat-header-title">${displayTitle || `${groupInfo.groupName} 老人沟通群`}</div>
                <div class="chat-header-meta">
                    <span>共${memberCount}人</span>
                    ${memberNamesHtml}
                </div>
            `;
        }
    } catch (error) {
        console.error("Failed to load group info:", error);
        // 如果获取群组信息失败，使用原来的简单显示方式
        if (chatHeaderContent) {
            const fallbackTitle = (groupName || '').replace(/的沟通群$/, '老人的沟通群');
            chatHeaderContent.textContent = fallbackTitle || `${groupName} 老人沟通群`;
        }
    }

    // Enable inputs
    chatInput.disabled = false;
    sendBtn.disabled = false;
    fileBtn.disabled = false;
    imageBtn.disabled = false;
    voiceBtn.disabled = false;
    if (toggleExtrasBtn) {
        toggleExtrasBtn.disabled = false;
    }

    document.querySelectorAll('.user-item').forEach(item => {
        item.classList.remove('active');
        if (item.dataset.groupId == groupId) {
            item.classList.add('active');
        }
    });

    // Subscribe to the new group topic for real-time messages
    if (stompClient && stompClient.connected) {
        subscribeToGroupTopic(groupId);
    }

    try {
        await post(`/chat/groups/${groupId}/read`);
        updateUnreadBadge(groupId, 0, true);
    } catch (error) {
        console.error("Failed to mark as read:", error);
    }

    await loadGroupMessages();
    handleResponsiveLayout();
}

async function loadGroupMessages() {
    if (!currentGroupId) return;
    try {
        // 使用正确的API路径
        const res = await get(`/family/chat/group/${currentGroupId}/messages`);
        const messagePage = res.data || { records: [] };
        chatMessages.innerHTML = '';
        messagePage.records.forEach(appendMessage);
    } catch (e) {
        console.error('Load messages error:', e);
        chatMessages.innerHTML = '<div style="color:red;">消息加载失败</div>';
    }
}

function sendMessage(messageObject) {
    if (!currentGroupId || !stompClient || !stompClient.connected) {
        console.error('无法发送消息: currentGroupId=', currentGroupId, 'stompClient=', stompClient, 'connected=', stompClient?.connected);
        return;
    }
    try {
        console.log('发送消息到群组', currentGroupId, ':', messageObject);
        
        // 生成唯一的临时ID
        const tempId = 'temp_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
        
        // 立即在本地显示消息（乐观更新）
        const localMessage = {
            ...messageObject,
            me: true,
            senderName: currentUser.username,
            senderRole: currentUser.role,
            time: new Date().toISOString(),
            id: tempId,
            isTemporary: true // 标记为临时消息
        };
        appendMessage(localMessage);
        
        // 发送到服务器
        stompClient.send(`/app/chat/group/${currentGroupId}`, {}, JSON.stringify(messageObject));
        console.log('消息已发送到服务器');
    } catch (e) {
        console.error('Send message error:', e);
    }
}

function sendTextMessage() {
    const content = chatInput.value.trim();
    if (!content) return;
    sendMessage({ messageType: 'TEXT', content: content });
    chatInput.value = '';
    closeExtraPanel();
}

// --- File and Voice Upload Functions ---



function toggleRecording() {
    console.log('toggleRecording called, isRecording:', isRecording);
    if (isRecording) {
        console.log('Stopping recording...');
        stopRecording();
    } else {
        console.log('Starting recording...');
        startRecording();
    }
}

async function startRecording() {
    try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        mediaRecorder = new MediaRecorder(stream);
        audioChunks = [];
        isRecording = true;
        voiceBtn.classList.add('recording');

        mediaRecorder.ondataavailable = event => {
            audioChunks.push(event.data);
        };

        mediaRecorder.onstop = async () => {
            const audioBlob = new Blob(audioChunks, { type: 'audio/wav' });
            await uploadVoiceMessage(audioBlob);
            stream.getTracks().forEach(track => track.stop());
        };

        mediaRecorder.start();
    } catch (err) {
        console.error('Error starting recording:', err);
        alert('无法启动录音功能，请检查麦克风权限。');
    }
}

function stopRecording() {
    if (mediaRecorder && isRecording) {
        mediaRecorder.stop();
        isRecording = false;
        voiceBtn.classList.remove('recording');
    }
}

async function uploadVoiceMessage(blob) {
    const formData = new FormData();
    formData.append('audio', blob, 'voice.wav');

    try {
        const result = await post('/upload/audio', formData, true);
        console.log('Voice upload result:', result);
        sendMessage({
            messageType: 'VOICE',
            content: '[语音消息]',
            audioUrl: result.data.audioUrl || result.data.url,
            duration: result.data.duration
        });
    } catch (error) {
        console.error('Voice upload failed:', error);
        alert('语音上传失败');
    }
}


// --- Context Menu and Deletion ---

function showContextMenu(e) {
    const messageElement = e.target.closest('.message');
    if (!messageElement) return;

    const msgId = messageElement.dataset.messageId;
    const isMyMessage = messageElement.classList.contains('me');

    if (!isMyMessage || !msgId) return;

    e.preventDefault();
    currentMessageElement = messageElement;
    contextMenu.style.top = `${e.pageY}px`;
    contextMenu.style.left = `${e.pageX}px`;
    contextMenu.style.display = 'block';
}

async function deleteMessage() {
    if (!currentMessageElement) return;

    const messageId = currentMessageElement.dataset.messageId;
    if (!messageId) return;

    const elementToRemove = currentMessageElement; // 保存引用
    
    try {
        console.log('开始删除消息，ID:', messageId);
        const result = await del(`/chat/message/${messageId}`);
        console.log('删除API返回结果:', result);
        
        // 删除成功，移除元素
        if (elementToRemove && elementToRemove.parentNode) {
            elementToRemove.remove();
        }
        currentMessageElement = null;
        console.log('消息删除成功');
        
        // 不显示任何提示，因为删除成功了
    } catch (error) {
        console.error('删除消息失败:', error);
        // 只有真正失败时才显示错误
        if (error.message && !error.message.includes('null')) {
            alert('删除失败: ' + error.message);
        }
    }
    contextMenu.style.display = 'none';
}


// --- File Upload Functions ---

function uploadFile(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    if (file.size > 10 * 1024 * 1024) {
        alert('文件大小不能超过10MB');
        return;
    }
    
    sendFileMessage(file);
    event.target.value = ''; // 清空文件输入
}

function uploadFile(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    if (file.size > 10 * 1024 * 1024) {
        alert('文件大小不能超过10MB');
        return;
    }
    
    sendFileMessage(file);
    event.target.value = ''; // 清空文件输入
}

function uploadImage(event) {
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
            sendMessage(fileMessage);
        } else {
            throw new Error('文件上传响应格式错误');
        }
        
    } catch (error) {
        console.error('发送文件消息失败:', error);
        alert('文件发送失败: ' + error.message);
    }
}

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
            sendMessage(imageMessage);
        } else {
            throw new Error('图片上传响应格式错误');
        }
        
    } catch (error) {
        console.error('发送图片消息失败:', error);
        alert('图片发送失败: ' + error.message);
    }
}

function isMobileView() {
    return window.innerWidth <= MOBILE_BREAKPOINT;
}

function enterMobileChatView() {
    if (!chatContainer || !isMobileView()) return;
    chatContainer.classList.add('mobile-chat-active');
    toggleChatBackBtn(true);
}

function exitMobileChatView() {
    if (!chatContainer) return;
    chatContainer.classList.remove('mobile-chat-active');
    toggleChatBackBtn(false);
}

function toggleChatBackBtn(visible) {
    if (!chatBackBtn) return;
    chatBackBtn.style.display = visible ? 'inline-flex' : 'none';
}

function handleResponsiveLayout() {
    if (!chatContainer) return;
    if (isMobileView()) {
        if (currentGroupId && !mobileListPreferred) {
            enterMobileChatView();
        } else {
            exitMobileChatView();
        }
    } else {
        chatContainer.classList.remove('mobile-chat-active');
        toggleChatBackBtn(false);
    }
}



// --- Utility Functions ---

function closeExtraPanel() {
    if (chatExtraPanel) {
        chatExtraPanel.classList.remove('active');
    }
    if (toggleExtrasBtn) {
        toggleExtrasBtn.classList.remove('active');
        toggleExtrasBtn.setAttribute('aria-expanded', 'false');
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>\"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', '\'': '&#39;' }[c]));
}

function formatTime(t) {
    if (!t) return '';
    return t.replace('T', ' ').substring(5, 16);
}

let currentVoiceAudio = null;
let currentVoiceButton = null;

function toggleVoicePlayback(audioUrl, button) {
    if (!audioUrl) {
        alert('音频资源不存在');
        return;
    }
    const isPlaying = button.dataset.playing === 'true';

    if (isPlaying) {
        if (currentVoiceAudio) {
            currentVoiceAudio.pause();
            currentVoiceAudio = null;
        }
        button.dataset.playing = 'false';
        button.innerHTML = '<i class="fas fa-play"></i>';
        currentVoiceButton = null;
        return;
    }

    if (currentVoiceAudio && currentVoiceButton) {
        currentVoiceAudio.pause();
        currentVoiceButton.dataset.playing = 'false';
        currentVoiceButton.innerHTML = '<i class="fas fa-play"></i>';
    }

    const audio = new Audio(audioUrl);
    currentVoiceAudio = audio;
    currentVoiceButton = button;
    button.dataset.playing = 'true';
    button.innerHTML = '<i class="fas fa-pause"></i>';

    audio.onended = () => {
        button.dataset.playing = 'false';
        button.innerHTML = '<i class="fas fa-play"></i>';
        if (currentVoiceAudio === audio) {
            currentVoiceAudio = null;
            currentVoiceButton = null;
        }
    };

    audio.onerror = () => {
        alert('语音播放失败');
        button.dataset.playing = 'false';
        button.innerHTML = '<i class="fas fa-play"></i>';
        currentVoiceAudio = null;
        currentVoiceButton = null;
    };

    audio.play().catch(error => {
        console.error('播放语音失败:', error);
        alert('语音播放失败');
        button.dataset.playing = 'false';
        button.innerHTML = '<i class="fas fa-play"></i>';
        currentVoiceAudio = null;
        currentVoiceButton = null;
    });
}

