// 子女端群聊脚本

let currentGroupId = null;
let currentGroupName = '';
let stompClient = null;
let currentUser = null;

// UI Elements
let chatInput, sendBtn, fileBtn, fileInput, voiceBtn, chatMessages, contextMenu, deleteMessageBtn;

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
    voiceBtn = document.getElementById('voice-btn');
    chatMessages = document.getElementById('chatMessages');
    contextMenu = document.getElementById('context-menu');
    deleteMessageBtn = document.getElementById('delete-message');

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

    fileBtn.addEventListener('click', () => fileInput.click());
    fileInput.addEventListener('change', uploadFile);
    voiceBtn.addEventListener('click', function () {
        console.log('Voice button clicked, isRecording:', isRecording);
        toggleRecording();
    });

    // Context Menu Listeners
    chatMessages.addEventListener('contextmenu', showContextMenu);
    document.addEventListener('click', () => {
        contextMenu.style.display = 'none';
        currentMessageElement = null;
    });
    deleteMessageBtn.addEventListener('click', deleteMessage);
});

function connectWebSocket() {
    const token = localStorage.getItem('token');
    // 通过URL参数传递token，与其他聊天页面保持一致
    const socket = new SockJS(`/ws-chat?token=${encodeURIComponent(token)}`);
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('WebSocket connected:', frame);
        stompClient.subscribe('/user/queue/group-messages', function (message) {
            const msg = JSON.parse(message.body);
            handleNewMessage(msg);
        });
    }, function (error) {
        console.error('WebSocket connection failed:', error);
        // 重连逻辑
        setTimeout(connectWebSocket, 5000);
    });
}

function handleNewMessage(msg) {
    if (msg.messageType === 'delete') {
        const elementToRemove = document.querySelector(`[data-message-id='${msg.id}']`);
        if (elementToRemove) {
            elementToRemove.remove();
        }
        return;
    }

    if (msg.groupId === currentGroupId) {
        // 如果是自己发送的消息，先移除临时消息再显示正式消息
        if (msg.me) {
            // 移除可能存在的临时消息
            const tempMessages = document.querySelectorAll('[data-message-id^="temp_"]');
            if (tempMessages.length > 0) {
                tempMessages[tempMessages.length - 1].remove(); // 移除最后一个临时消息
            }
        }
        appendMessage(msg);
    } else {
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
            // 兼容不同的数据格式
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
            
            if (audioUrl) {
                messageBubble = `<div class="bubble voice-bubble">
                                    <i class="fas fa-play-circle"></i>
                                    <audio controls src="${audioUrl}">
                                        您的浏览器不支持音频播放
                                    </audio>
                                    ${duration ? `<span class="duration">${duration}"</span>` : ''}
                                 </div>`;
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

    div.innerHTML = `${senderName}${messageBubble}<div class="meta">${formatTime(msg.time)}</div>`;
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
            div.innerHTML = `<span>${group.groupName}</span>`;
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
    document.getElementById('chatHeader').textContent = `与 ${groupName} 的家庭群聊`;

    // Enable inputs
    chatInput.disabled = false;
    sendBtn.disabled = false;
    fileBtn.disabled = false;
    voiceBtn.disabled = false;

    document.querySelectorAll('.user-item').forEach(item => {
        item.classList.remove('active');
        if (item.dataset.groupId == groupId) {
            item.classList.add('active');
        }
    });

    try {
        await post(`/chat/groups/${groupId}/read`);
        updateUnreadBadge(groupId, 0, true);
    } catch (error) {
        console.error("Failed to mark as read:", error);
    }

    await loadGroupMessages();
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
        return;
    }
    try {
        // 立即在本地显示消息（乐观更新）
        const localMessage = {
            ...messageObject,
            me: true,
            senderName: currentUser.username,
            senderRole: currentUser.role,
            time: new Date().toISOString(),
            id: 'temp_' + Date.now() // 临时ID
        };
        appendMessage(localMessage);
        
        // 发送到服务器
        stompClient.send(`/app/chat/group/${currentGroupId}`, {}, JSON.stringify(messageObject));
    } catch (e) {
        console.error('Send message error:', e);
    }
}

function sendTextMessage() {
    const content = chatInput.value.trim();
    if (!content) return;
    sendMessage({ messageType: 'TEXT', content: content });
    chatInput.value = '';
}

// --- File and Voice Upload Functions ---

async function uploadFile() {
    const file = fileInput.files[0];
    if (!file) return;

    // 检查文件大小 (限制为10MB)
    if (file.size > 10 * 1024 * 1024) {
        alert('文件大小不能超过10MB');
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    console.log('上传文件:', file.name, '类型:', file.type, '大小:', file.size);

    try {
        const result = await post('/upload/file', formData, true); // true for FormData
        console.log('File upload result:', result);

        // 强制所有上传的文件都识别为FILE类型，确保正确显示
        const fileMessage = {
            messageType: 'FILE',
            content: `[文件] ${result.data.fileName || result.data.originalFilename}`,
            fileName: result.data.fileName || result.data.originalFilename,
            fileUrl: result.data.fileUrl || result.data.url || result.data.imageUrl || result.data.audioUrl
        };
        console.log('子女端发送文件消息:', fileMessage);
        sendMessage(fileMessage);
    } catch (error) {
        console.error('File upload failed:', error);
        alert('文件上传失败');
    }
    fileInput.value = ''; // Reset input
}

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

    try {
        await del(`/chat/message/${messageId}`);
        // Optimistic removal
        currentMessageElement.remove();
        currentMessageElement = null;
    } catch (error) {
        console.error('Failed to delete message:', error);
        alert('删除失败');
    }
    contextMenu.style.display = 'none';
}


// --- Utility Functions ---

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>\"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', '\'': '&#39;' }[c]));
}

function formatTime(t) {
    if (!t) return '';
    return t.replace('T', ' ').substring(5, 16);
}

