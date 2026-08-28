let patients = [];
let selectedPatient = null;
let sessionId = null;
let sending = false;

const patientList = document.getElementById('patientList');
const patientCount = document.getElementById('patientCount');
const patientSearch = document.getElementById('patientSearch');
const contextPatient = document.getElementById('contextPatient');
const contextRoom = document.getElementById('contextRoom');
const messageList = document.getElementById('messageList');
const questionInput = document.getElementById('questionInput');
const sendBtn = document.getElementById('sendBtn');
const resetBtn = document.getElementById('resetBtn');

window.addEventListener('DOMContentLoaded', async () => {
    const user = checkLogin();
    if (!user) return;
    if (user.role && user.role !== 'MEDICAL') {
        window.location.href = 'index.html';
        return;
    }

    bindEvents();
    await loadPatients();
});

function bindEvents() {
    patientSearch.addEventListener('input', () => renderPatients(patientSearch.value));
    sendBtn.addEventListener('click', () => sendQuestion());
    resetBtn.addEventListener('click', resetConversation);

    questionInput.addEventListener('keydown', event => {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            sendQuestion();
        }
    });

    questionInput.addEventListener('input', autoResizeInput);

    document.querySelectorAll('#starterPrompts [data-question]').forEach(button => {
        button.addEventListener('click', () => sendQuestion(button.dataset.question));
    });
}

async function loadPatients() {
    try {
        const response = await get('/medical/ai-assistant/patients');
        patients = response?.data || [];
        patientCount.textContent = patients.length;
        renderPatients('');
        if (patients.length === 1) selectPatient(patients[0]);
    } catch (error) {
        patientList.innerHTML = '<div class="empty-patients">无法加载当前权限范围，请确认后端服务与登录状态。</div>';
    }
}

function renderPatients(keyword) {
    const query = (keyword || '').trim().toLowerCase();
    const filtered = patients.filter(patient => {
        const text = `${patient.name || ''} ${patient.roomNumber || ''}`.toLowerCase();
        return !query || text.includes(query);
    });

    if (!filtered.length) {
        patientList.innerHTML = '<div class="empty-patients">没有匹配的老人。</div>';
        return;
    }

    patientList.innerHTML = filtered.map(patient => {
        const active = selectedPatient?.id === patient.id ? ' active' : '';
        const initial = escapeHtml((patient.name || '老').slice(0, 1));
        return `
            <button class="patient-card${active}" data-id="${patient.id}">
                <span class="patient-avatar">${initial}</span>
                <span class="patient-copy">
                    <span class="patient-name">${escapeHtml(patient.name || '未命名老人')}</span>
                    <span class="patient-room">${escapeHtml(patient.roomNumber ? `${patient.roomNumber} 房` : '房间暂未登记')}</span>
                </span>
            </button>`;
    }).join('');

    patientList.querySelectorAll('.patient-card').forEach(card => {
        card.addEventListener('click', () => {
            const patient = patients.find(item => String(item.id) === card.dataset.id);
            if (patient) selectPatient(patient);
        });
    });
}

function selectPatient(patient) {
    const changed = selectedPatient && selectedPatient.id !== patient.id;
    selectedPatient = patient;
    contextPatient.textContent = patient.name || '当前老人';
    contextRoom.textContent = patient.roomNumber ? `· ${patient.roomNumber} 房` : '· 房间未登记';
    renderPatients(patientSearch.value);

    if (changed) {
        appendNotice(`已切换到 ${patient.name || '当前老人'}。后续“她/他/TA”的追问会沿用这个上下文。`);
    }
    questionInput.focus();
}

async function sendQuestion(preset) {
    if (sending) return;
    const question = (preset || questionInput.value || '').trim();
    if (!question) return;

    if (!preset) questionInput.value = '';
    autoResizeInput();
    appendUserMessage(question);
    const typingId = appendTyping();
    setSending(true);

    try {
        const response = await post('/medical/ai-assistant/chat', {
            sessionId,
            elderlyId: selectedPatient?.id || null,
            message: question
        });
        removeTyping(typingId);

        const answer = response?.data;
        if (!answer) throw new Error('AI 助手未返回有效结果');
        sessionId = answer.sessionId || sessionId;

        if (answer.elderlyId) {
            const resolved = patients.find(item => item.id === answer.elderlyId);
            if (resolved && selectedPatient?.id !== resolved.id) selectPatient(resolved);
        }
        appendAssistantMessage(answer);
    } catch (error) {
        removeTyping(typingId);
        appendErrorMessage(error.message || '请求失败，请稍后重试。');
    } finally {
        setSending(false);
        questionInput.focus();
    }
}

function appendUserMessage(text) {
    const node = document.createElement('div');
    node.className = 'message user-message';
    node.innerHTML = `
        <div class="avatar user-avatar">我</div>
        <div class="message-body">
            <div class="message-meta">医护人员</div>
            <div class="bubble">${escapeHtml(text)}</div>
        </div>`;
    messageList.appendChild(node);
    scrollToBottom();
}

function appendAssistantMessage(answer) {
    const plan = (answer.plan || []).map((tool, index) => `
        <span class="tool-chip" title="计划步骤 ${index + 1}">
            ${index + 1}. ${escapeHtml(toolDisplayName(tool))}
        </span>`).join('');

    const tools = (answer.tools || []).map(tool => {
        const latency = Number.isFinite(Number(tool.elapsedMs)) ? ` · ${Number(tool.elapsedMs)} ms` : '';
        const title = `${tool.summary || ''}${latency}`;
        return `
        <span class="tool-chip ${escapeHtml(tool.status || '')}" title="${escapeHtml(title)}">
            ${escapeHtml(toolDisplayName(tool.tool || 'tool'))}${latency ? `<small>${escapeHtml(latency)}</small>` : ''}
        </span>`;
    }).join('');

    const sources = (answer.sources || []).map(source => `
        <span class="source-chip">${escapeHtml(source)}</span>`).join('');

    const suggestions = (answer.suggestions || []).map((suggestion, index) => `
        <button class="suggestion-btn" data-suggestion-index="${index}">${escapeHtml(suggestion)}</button>`).join('');

    const hasFailedTool = (answer.tools || []).some(tool => tool.status === 'failed');
    const runMeta = [
        answer.traceId ? `Trace ${String(answer.traceId).slice(0, 8)}` : null,
        Number.isFinite(Number(answer.elapsedMs)) ? `${Number(answer.elapsedMs)} ms` : null,
        hasFailedTool ? '部分结果' : null,
        answer.modelEnhanced ? '模型润色' : '事实工具模式'
    ].filter(Boolean).join(' · ');

    const node = document.createElement('div');
    node.className = 'message assistant-message';
    node.innerHTML = `
        <div class="avatar ai-avatar">AI</div>
        <div class="message-body">
            <div class="message-meta">
                医护 AI 助手${answer.elderlyName ? ` · ${escapeHtml(answer.elderlyName)}` : ''}
                ${runMeta ? `<span style="margin-left:8px;font-weight:400;color:#9299a8;">${escapeHtml(runMeta)}</span>` : ''}
            </div>
            <div class="bubble">${escapeHtml(answer.answer || '')}</div>
            <div class="trace-block">
                ${answer.planReason ? `<div class="trace-row"><span class="trace-label">规划</span><span class="source-chip">${escapeHtml(answer.planReason)}</span></div>` : ''}
                ${plan ? `<div class="trace-row"><span class="trace-label">计划</span>${plan}</div>` : ''}
                ${tools ? `<div class="trace-row"><span class="trace-label">执行</span>${tools}</div>` : ''}
                ${sources ? `<div class="source-row"><span class="trace-label">来源</span>${sources}</div>` : ''}
            </div>
            ${hasFailedTool ? '<div class="safety-strip">部分业务数据源暂时不可用，本轮仅展示成功查询到的系统事实。</div>' : ''}
            ${answer.safetyNote ? `<div class="safety-strip">${escapeHtml(answer.safetyNote)}</div>` : ''}
            ${suggestions ? `<div class="suggestion-row">${suggestions}</div>` : ''}
        </div>`;

    node.querySelectorAll('[data-suggestion-index]').forEach(button => {
        button.addEventListener('click', () => {
            const text = answer.suggestions[Number(button.dataset.suggestionIndex)];
            if (text) sendQuestion(text);
        });
    });

    messageList.appendChild(node);
    scrollToBottom();
}

function toolDisplayName(tool) {
    const names = {
        patient_access: '权限校验',
        patient_scope: '患者范围',
        room_lookup: '房间查询',
        patient_profile: '老人档案',
        health_recent: '近期健康',
        alerts_recent: '近期告警',
        care_schedule: '照护安排',
        recommendation_preview: '关怀推荐',
        medical_safety_guard: '医疗安全',
        llm_polish: '语言润色'
    };
    return names[tool] || tool;
}

function appendErrorMessage(text) {
    const node = document.createElement('div');
    node.className = 'message assistant-message';
    node.innerHTML = `
        <div class="avatar ai-avatar">AI</div>
        <div class="message-body">
            <div class="message-meta">医护 AI 助手</div>
            <div class="bubble">${escapeHtml(text)}</div>
        </div>`;
    messageList.appendChild(node);
    scrollToBottom();
}

function appendNotice(text) {
    const node = document.createElement('div');
    node.style.cssText = 'text-align:center;color:#9299a8;font-size:11px;margin:0 auto 18px;';
    node.textContent = text;
    messageList.appendChild(node);
    scrollToBottom();
}

function appendTyping() {
    const id = `typing-${Date.now()}`;
    const node = document.createElement('div');
    node.id = id;
    node.className = 'message assistant-message';
    node.innerHTML = `
        <div class="avatar ai-avatar">AI</div>
        <div class="message-body">
            <div class="message-meta">正在规划并查询系统工具…</div>
            <div class="bubble typing-bubble"><i></i><i></i><i></i></div>
        </div>`;
    messageList.appendChild(node);
    scrollToBottom();
    return id;
}

function removeTyping(id) {
    document.getElementById(id)?.remove();
}

async function resetConversation() {
    if (sessionId) {
        try {
            await del(`/medical/ai-assistant/sessions/${encodeURIComponent(sessionId)}`);
        } catch (error) {
            console.warn('reset session failed', error);
        }
    }
    sessionId = null;
    selectedPatient = null;
    contextPatient.textContent = '暂未选择老人';
    contextRoom.textContent = '';
    patientSearch.value = '';
    renderPatients('');

    messageList.innerHTML = `
        <div class="message assistant-message welcome-message">
            <div class="avatar ai-avatar">AI</div>
            <div class="message-body">
                <div class="message-meta">医护 AI 助手</div>
                <div class="bubble">新会话已创建。请选择老人，或直接在问题里说出你负责的老人姓名。</div>
                <div class="safety-strip">会话只记忆当前老人上下文，不保存完整医疗回答。</div>
            </div>
        </div>`;
}

function setSending(value) {
    sending = value;
    sendBtn.disabled = value;
    questionInput.disabled = value;
}

function autoResizeInput() {
    questionInput.style.height = 'auto';
    questionInput.style.height = `${Math.min(questionInput.scrollHeight, 120)}px`;
}

function scrollToBottom() {
    requestAnimationFrame(() => {
        messageList.scrollTop = messageList.scrollHeight;
    });
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
