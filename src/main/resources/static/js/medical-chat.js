// 假设后端API: /api/medical/chat/users 获取子女列表
// /api/medical/chat/messages?userId=xxx 获取与该子女的历史消息
// POST /api/medical/chat/send 发送消息 {toUserId, content}

let currentUserId = null;
let currentUserName = '';

// WebSocket实时消息
let stompClient = null;
function connectWebSocket(userId) {
    if (!userId) return;
    const socket = new SockJS('/ws-chat');
    stompClient = Stomp.over(socket);
    const token = localStorage.getItem('authToken');
    console.log('Token being sent:', token);
    stompClient.connect({ Authorization: `Bearer ${token}` }, function () {
        stompClient.subscribe('/topic/medical-chat-' + userId, function (msg) {
            const data = JSON.parse(msg.body);
            // 新消息提示
            if (data && !data.me) {
                showNewMsgTip(data);
                loadMessages();
            }
        });
    });
}

function showNewMsgTip(msg) {
    const box = document.createElement('div');
    box.textContent = `收到来自${msg.fromName || '子女'}的新消息：${msg.content}`;
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

// 加载子女列表
async function loadUserList() {
    try {
        const res = await get('/medical/chat/users');
        const users = res.data || [];
        const userList = document.getElementById('userList');
        userList.innerHTML = users.length === 0 ? '<div style="padding:20px;color:#999;">暂无子女</div>' : '';
        users.forEach(user => {
            const div = document.createElement('div');
            div.className = 'user-item';
            div.textContent = user.name + (user.elderlyName ? `（${user.elderlyName}）` : '');
            div.onclick = () => selectUser(user.id, user.name);
            div.dataset.id = user.id;
            userList.appendChild(div);
        });
    } catch (e) {
        document.getElementById('userList').innerHTML = '<div style="padding:20px;color:red;">加载失败</div>';
    }
}

// 选择子女
async function selectUser(userId, userName) {
    currentUserId = userId;
    currentUserName = userName;
    document.getElementById('chatHeader').textContent = `与 ${userName} 聊天`;
    document.getElementById('chatInput').disabled = false;
    document.getElementById('sendBtn').disabled = false;
    // 高亮
    document.querySelectorAll('.user-item').forEach(item => {
        item.classList.toggle('active', item.dataset.id === userId); // 使用严格比较
    });
    await loadMessages();
}

// 加载消息
async function loadMessages() {
    if (!currentUserId) return;
    try {
        const res = await get(`/medical/chat/messages?userId=${currentUserId}`);
        const messages = res.data || [];
        const box = document.getElementById('chatMessages');
        box.innerHTML = '';
        messages.forEach(msg => {
            const div = document.createElement('div');
            div.className = 'message' + (msg.me ? ' me' : '');
            let bubble;
            if (msg.type === 'image') {
                bubble = `<img src="${escapeHtml(msg.content)}" alt="图片" style="max-width:180px;max-height:120px;border-radius:8px;">`;
            } else if (msg.type === 'audio') {
                bubble = `<audio controls src="${escapeHtml(msg.content)}" style="width:160px;"></audio>`;
            } else {
                bubble = escapeHtml(msg.content);
            }
            div.innerHTML = `<div class="bubble">${bubble}</div><div class="meta">${formatTime(msg.time)}</div>`;
            box.appendChild(div);
        });
        box.scrollTop = box.scrollHeight;
    } catch (e) {
        document.getElementById('chatMessages').innerHTML = '<div style=\"color:red;\">消息加载失败</div>';
    }
}

// 处理图片选择
function onImageSelected(event) {
    const file = event.target.files[0];
    if (!file) return;
    const maxSize = 2 * 1024 * 1024;
    const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
    if (file.size > maxSize) {
        alert('图片不能超过2MB');
        return;
    }
    if (!allowedTypes.includes(file.type)) {
        alert('仅支持JPG/PNG/GIF/WEBP格式图片');
        return;
    }
    const reader = new FileReader();
    reader.onload = async function(e) {
        const base64 = e.target.result;
        if (!currentUserId) return;
        await post('/medical/chat/send', {
            toUserId: currentUserId,
            content: base64,
            type: 'image',
            fileName: file.name
        });
        await loadMessages();
    };
    reader.readAsDataURL(file);
}

// 语音录制相关
let mediaRecorder = null;
let audioChunks = [];
function startRecordAudio() {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        alert('当前浏览器不支持语音录制');
        return;
    }
    navigator.mediaDevices.getUserMedia({ audio: true }).then(stream => {
        mediaRecorder = new MediaRecorder(stream);
        audioChunks = [];
        mediaRecorder.ondataavailable = e => {
            if (e.data.size > 0) audioChunks.push(e.data);
        };
        mediaRecorder.onstop = async () => {
            const blob = new Blob(audioChunks, { type: 'audio/webm' });
            if (blob.size > 3 * 1024 * 1024) {
                alert('语音不能超过3MB');
                return;
            }
            const reader = new FileReader();
            reader.onload = async function(e) {
                const base64 = e.target.result;
                if (!currentUserId) return;
                await post('/medical/chat/send', {
                    toUserId: currentUserId,
                    content: base64,
                    type: 'audio',
                    fileName: 'audio.webm'
                });
                await loadMessages();
            };
            reader.readAsDataURL(blob);
        };
        mediaRecorder.start();
        document.getElementById('recordBtn').textContent = '⏹️';
        document.getElementById('recordBtn').onclick = stopRecordAudio;
    }).catch(() => {
        alert('无法获取麦克风权限');
    });
}
function stopRecordAudio() {
    if (mediaRecorder) {
        mediaRecorder.stop();
        document.getElementById('recordBtn').textContent = '🎤';
        document.getElementById('recordBtn').onclick = startRecordAudio;
    }
}

// 发送消息
async function sendMessage() {
    const input = document.getElementById('chatInput');
    const content = input.value.trim();
    if (!content || !currentUserId) return;
    try {
        await post('/medical/chat/send', { toUserId: currentUserId, content, type: 'text' });
        input.value = '';
        await loadMessages();
    } catch (e) {
        alert('发送失败');
    }
}

// 处理语音文件选择（备用，支持直接上传音频文件）
function onAudioSelected(event) {
    const file = event.target.files[0];
    if (!file) return;
    const maxSize = 3 * 1024 * 1024;
    const allowedTypes = ['audio/webm', 'audio/wav', 'audio/mp3', 'audio/mpeg'];
    if (file.size > maxSize) {
        alert('语音不能超过3MB');
        return;
    }
    if (!allowedTypes.includes(file.type)) {
        alert('仅支持webm/mp3/wav格式语音');
        return;
    }
    const reader = new FileReader();
    reader.onload = async function(e) {
        const base64 = e.target.result;
        if (!currentUserId) return;
        await post('/medical/chat/send', {
            toUserId: currentUserId,
            content: base64,
            type: 'audio',
            fileName: file.name
        });
        await loadMessages();
    };
    reader.readAsDataURL(file);
}

function escapeHtml(str) {
    return str.replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;','\'':'&#39;'}[c]));
}

function formatTime(t) {
    if (!t) return '';
    return t.replace('T', ' ').substring(0, 16);
}

document.addEventListener('DOMContentLoaded', () => {
    loadUserList();
    // 假设医护userId为1，实际应从登录信息获取
    connectWebSocket(1);
    document.getElementById('chatInput').addEventListener('keydown', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });
});
