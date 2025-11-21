// 子女端群聊脚本

let currentGroupId = null;
let currentGroupName = '';
let stompClient = null;
let currentUser = null;

document.addEventListener('DOMContentLoaded', async () => {
    currentUser = checkLogin();
    if (!currentUser || currentUser.role !== 'FAMILY') {
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
    if (msg.groupId === currentGroupId) {
        appendMessage(msg);
    } else {
        showNewMsgTip(msg);
        updateUnreadBadge(msg.groupId, 1); // Increment count by 1
    }
}

function appendMessage(msg) {
    const box = document.getElementById('chatMessages');
    const div = document.createElement('div');
    div.className = 'message' + (msg.me ? ' me' : '');
    const senderName = msg.me ? '' : `<div class="sender-name">${escapeHtml(msg.senderName)} (${escapeHtml(msg.senderRole)})</div>`;
    div.innerHTML = `${senderName}<div class="bubble">${escapeHtml(msg.content)}</div><div class="meta">${formatTime(msg.time)}</div>`;
    box.appendChild(div);
    box.scrollTop = box.scrollHeight;
}

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
        const res = await get('/family/chat/groups'); // Family-specific endpoint
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
    document.getElementById('chatHeader').textContent = `与 ${groupName} 的家庭群聊`;
    document.getElementById('chatInput').disabled = false;
    document.getElementById('sendBtn').disabled = false;

    document.querySelectorAll('.user-item').forEach(item => {
        item.classList.remove('active');
        if (item.dataset.groupId == groupId) {
            item.classList.add('active');
        }
    });

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
        const res = await get(`/medical/chat/group/${currentGroupId}/messages`);
        const messagePage = res.data || { records: [] };
        const box = document.getElementById('chatMessages');
        box.innerHTML = '';
        messagePage.records.forEach(appendMessage);
    } catch (e) {
        document.getElementById('chatMessages').innerHTML = '<div style="color:red;">消息加载失败</div>';
    }
}

function sendMessage() {
    const input = document.getElementById('chatInput');
    const content = input.value.trim();
    if (!content || !currentGroupId || !stompClient || !stompClient.connected) {
        return;
    }
    try {
        stompClient.send(`/app/chat/group/${currentGroupId}`, {}, content);
        input.value = '';
    }
    catch (e) {
        console.error('Send message error:', e);
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;','\'':'&#39;'}[c]));
}

function formatTime(t) {
    if (!t) return '';
    return t.replace('T', ' ').substring(5, 16);
}