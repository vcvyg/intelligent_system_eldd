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
    chatInput.addEventListener('keydown', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendTextMessage();
        }
    });

    fileBtn.addEventListener('click', () => fileInput.click());
    fileInput.addEventListener('change', uploadFile);
    voiceBtn.addEventListener('click', toggleRecording);

    // Context Menu Listeners
    chatMessages.addEventListener('contextmenu', showContextMenu);
    document.addEventListener('click', () => {
        contextMenu.style.display = 'none';
        currentMessageElement = null;
    });
    deleteMessageBtn.addEventListener('click', deleteMessage);
});

function connectWebSocket() {
    const socket = new SockJS('/ws-chat');
    stompClient = Stomp.over(socket);
    const token = localStorage.getItem('token');
    stompClient.connect({ 'Authorization': `Bearer ${token}` }, function (frame) {
        stompClient.subscribe('/user/queue/group-messages', function (message) {
            const msg = JSON.parse(message.body);
            handleNewMessage(msg);
        });
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
    div.dataset.messageId = msg.id; // Store message ID for deletion

    const senderName = msg.me ? '' : `<div class="sender-name">${escapeHtml(msg.senderName)} (${escapeHtml(msg.senderRole)})</div>`;
    let messageBubble = '';

    switch (msg.messageType) {
        case 'VOICE':
            messageBubble = `<div class="bubble">
                                <audio controls src="${msg.audioUrl}"></audio>
                             </div>`;
            break;
        case 'FILE':
            messageBubble = `<div class="bubble">
                                <a href="${msg.fileUrl}" target="_blank" download>
                                    <i class="fas fa-file"></i> ${escapeHtml(msg.fileName || '附件')}
                                </a>
                             </div>`;
            break;
        case 'IMAGE':
            messageBubble = `<div class="bubble">
                                <img src="${msg.imageUrl}" alt="图片" style="max-width: 200px; border-radius: 8px; cursor: pointer;" onclick="showImageModal('${msg.imageUrl}')">
                             </div>`;
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
        await post(`/api/chat/groups/${groupId}/read`);
        updateUnreadBadge(groupId, 0, true);
    } catch (error) {
        console.error("Failed to mark as read:", error);
    }

    await loadGroupMessages();
}

async function loadGroupMessages() {
    if (!currentGroupId) return;
    try {
        // This endpoint should return messages with their IDs
        const res = await get(`/medical/chat/group/${currentGroupId}/messages`);
        const messagePage = res.data || { records: [] };
        chatMessages.innerHTML = '';
        messagePage.records.forEach(appendMessage);
    } catch (e) {
        chatMessages.innerHTML = '<div style="color:red;">消息加载失败</div>';
    }
}

function sendMessage(messageObject) {
     if (!currentGroupId || !stompClient || !stompClient.connected) {
        return;
    }
    try {
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

    const formData = new FormData();
    formData.append('file', file);

    try {
        const result = await post('/upload/file', formData, true); // true for FormData
        sendMessage({
            messageType: 'FILE',
            content: `[文件] ${result.data.fileName}`,
            fileName: result.data.fileName,
            fileUrl: result.data.url
        });
    } catch (error) {
        console.error('File upload failed:', error);
        alert('文件上传失败');
    }
    fileInput.value = ''; // Reset input
}

function toggleRecording() {
    if (isRecording) {
        stopRecording();
    } else {
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
        sendMessage({
            messageType: 'VOICE',
            content: '[语音消息]',
            audioUrl: result.data.url,
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
        await del(`/api/chat/message/${messageId}`);
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
    return str.replace(/[&<>\"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;','\'':'&#39;'}[c]));
}

function formatTime(t) {
    if (!t) return '';
    return t.replace('T', ' ').substring(5, 16);
}