let stompClient = null;
const urlParams = new URLSearchParams(window.location.search);
const alertAnchorState = {
    targetId: urlParams.get('alertId'),
    timer: null
};

const state = {
    alerts: [],
    filteredAlerts: [],
    currentPage: 1,
    pageSize: 10,
    isLoading: false,
    filters: {
        status: '待处理',
        type: '',
        level: '',
        keyword: ''
    }
};

document.addEventListener('DOMContentLoaded', initPage);

async function initPage() {
    const userInfo = checkLogin();
    if (!userInfo || userInfo.role !== 'MEDICAL') {
        alert('权限不足');
        logout();
        return;
    }
    document.getElementById('welcomeText').textContent = `欢迎, ${userInfo.username}!`;
    bindControls();
    await loadAlerts(true);
    connectWebSocket(userInfo.userId);
}

function bindControls() {
    const statusTabs = document.querySelectorAll('#statusTabs .status-tab');
    statusTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            statusTabs.forEach(btn => btn.classList.remove('active'));
            tab.classList.add('active');
            state.filters.status = tab.dataset.status || '';
            state.currentPage = 1;
            applyFilters();
        });
    });

    document.getElementById('filterApplyBtn').addEventListener('click', () => {
        state.filters.keyword = document.getElementById('filterKeyword').value.trim();
        state.filters.type = document.getElementById('filterType').value;
        state.filters.level = document.getElementById('filterLevel').value;
        state.currentPage = 1;
        applyFilters();
    });

    document.getElementById('filterKeyword').addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            state.filters.keyword = event.target.value.trim();
            state.currentPage = 1;
            applyFilters();
        }
    });

    document.getElementById('filterResetBtn').addEventListener('click', () => {
        state.filters = { status: '', type: '', level: '', keyword: '' };
        document.getElementById('filterKeyword').value = '';
        document.getElementById('filterType').value = '';
        document.getElementById('filterLevel').value = '';
        document.querySelectorAll('#statusTabs .status-tab').forEach(tab => tab.classList.remove('active'));
        const defaultTab = document.querySelector('#statusTabs .status-tab[data-status="待处理"]');
        if (defaultTab) {
            defaultTab.classList.add('active');
            state.filters.status = '待处理';
        }
        state.currentPage = 1;
        applyFilters();
    });

    document.getElementById('refreshBtn').addEventListener('click', () => loadAlerts(true));
    document.getElementById('prevPageBtn').addEventListener('click', () => changePage(-1));
    document.getElementById('nextPageBtn').addEventListener('click', () => changePage(1));
}

async function loadAlerts(force = false) {
    if (state.isLoading) return;
    if (!force && state.alerts.length) {
        applyFilters();
        return;
    }

    state.isLoading = true;
    setTableMessage('加载中...');
    try {
        const response = await request('/medical/alerts/all', { method: 'GET' });
        const data = Array.isArray(response.data) ? dedupeAlerts(response.data) : [];
        state.alerts = data;
        state.currentPage = 1;
        applyFilters(true);
    } catch (error) {
        console.error('加载告警列表失败:', error);
        setTableMessage('加载失败，请稍后重试');
    } finally {
        state.isLoading = false;
    }
}

function applyFilters(resetPage = false) {
    const { status, type, level, keyword } = state.filters;
    state.filteredAlerts = state.alerts.filter(alert => {
        const matchesStatus = !status || alert.status === status;
        const matchesType = !type || alert.alertType === type;
        const matchesLevel = !level || alert.alertLevel === level;
        const kw = keyword.toLowerCase();
        const matchesKeyword = !kw || (alert.elderlyName || '').toLowerCase().includes(kw);
        return matchesStatus && matchesType && matchesLevel && matchesKeyword;
    });

    if (resetPage) {
        state.currentPage = 1;
    }

    renderSummary();
    renderTable();
}

function renderSummary() {
    const total = state.alerts.length;
    const pending = state.alerts.filter(a => a.status === '待处理').length;
    const processing = state.alerts.filter(a => a.status === '处理中').length;
    const handled = state.alerts.filter(a => a.status === '已处理' || a.status === '已忽略').length;

    document.getElementById('summaryTotal').textContent = total;
    document.getElementById('summaryPending').textContent = pending;
    document.getElementById('summaryProcessing').textContent = processing;
    document.getElementById('summaryHandled').textContent = handled;
}

function renderTable() {
    const tbody = document.getElementById('alertTableBody');
    const totalRecords = state.filteredAlerts.length;
    const totalPages = Math.max(1, Math.ceil(totalRecords / state.pageSize));
    ensureTargetInCurrentPage();
    state.currentPage = Math.min(state.currentPage, totalPages);

    document.getElementById('recordTotal').textContent = totalRecords;
    document.getElementById('paginationInfo').textContent = `第 ${totalRecords === 0 ? 0 : state.currentPage} / ${totalPages} 页`;
    document.getElementById('prevPageBtn').disabled = state.currentPage <= 1;
    document.getElementById('nextPageBtn').disabled = state.currentPage >= totalPages;

    if (!totalRecords) {
        setTableMessage('暂无告警记录');
        return;
    }

    const start = (state.currentPage - 1) * state.pageSize;
    const currentPageData = state.filteredAlerts.slice(start, start + state.pageSize);

    tbody.innerHTML = currentPageData.map(alert => `
        <tr id="alert-row-${alert.id || ''}">
            <td>${start + currentPageData.indexOf(alert) + 1}</td>
            <td>
                <div>${formatDateTime(alert.alertTime)}</div>
            </td>
            <td>
                <div>${alert.elderlyName || '-'}</div>
                <div class="text-muted">${alert.medicalName ? `责任人：${alert.medicalName}` : ''}</div>
            </td>
            <td>${alert.roomName || '-'}</td>
            <td>${alert.deviceName || '-'}</td>
            <td>${alert.alertType || '-'}</td>
            <td><span class="level-badge ${getLevelClass(alert.alertLevel)}">${alert.alertLevel || '-'}</span></td>
            <td>${alert.alertContent || '-'}</td>
            <td>${alert.alertValue || '-'}</td>
            <td>${renderStatusTag(alert.status)}</td>
            <td>${renderActions(alert)}</td>
        </tr>
    `).join('');

    highlightTargetRow();
}

function setTableMessage(message) {
    document.getElementById('alertTableBody').innerHTML = `
        <tr>
            <td colspan="10" class="empty-tip">${message}</td>
        </tr>
    `;
    document.getElementById('recordTotal').textContent = 0;
    document.getElementById('paginationInfo').textContent = '第 0 / 0 页';
    document.getElementById('prevPageBtn').disabled = true;
    document.getElementById('nextPageBtn').disabled = true;
}

function renderStatusTag(status) {
    const map = {
        '待处理': 'pending',
        '处理中': 'processing',
        '已处理': 'handled',
        '已忽略': 'ignored'
    };
    return `<span class="status-tag ${map[status] || ''}">${status || '-'}</span>`;
}

function renderActions(alert) {
    if (alert.status === '待处理') {
        return `
            <button class="action-btn primary" onclick="startProcessingAlert(${alert.id})">立即处理</button>
            <button class="action-btn secondary" onclick="ignoreAlert(${alert.id})">忽略</button>
        `;
    }
    if (alert.status === '处理中') {
        return `<button class="action-btn primary" onclick="finishProcessingAlert(${alert.id})">处理完成</button>`;
    }
    return '<span class="text-muted">-</span>';
}

function changePage(delta) {
    const totalPages = Math.max(1, Math.ceil(state.filteredAlerts.length / state.pageSize));
    const nextPage = state.currentPage + delta;
    if (nextPage < 1 || nextPage > totalPages) return;
    state.currentPage = nextPage;
    renderTable();
}

function dedupeAlerts(alerts) {
    const map = new Map();
    alerts.forEach(alert => {
        const key = alert.id ?? `${alert.alertTime || ''}_${alert.elderlyId || ''}_${alert.alertType || ''}_${alert.alertValue || ''}_${alert.alertContent || ''}`;
        if (!map.has(key)) {
            map.set(key, alert);
        } else {
            const existing = map.get(key);
            if (new Date(alert.alertTime || 0) > new Date(existing.alertTime || 0)) {
                map.set(key, alert);
            }
        }
    });
    return Array.from(map.values());
}

function highlightTargetRow() {
    if (!alertAnchorState.targetId) return;
    const row = document.getElementById(`alert-row-${alertAnchorState.targetId}`);
    if (row) {
        row.classList.add('row-highlight');
        alertAnchorState.timer = setTimeout(() => {
            row.classList.remove('row-highlight');
        }, 8000);
        alertAnchorState.targetId = null;
    }
}

function ensureTargetInCurrentPage() {
    if (!alertAnchorState.targetId) return;
    const index = state.filteredAlerts.findIndex(alert => alert.id === Number(alertAnchorState.targetId) || alert.id === alertAnchorState.targetId);
    if (index === -1) return;
    state.currentPage = Math.floor(index / state.pageSize) + 1;
}

function connectWebSocket(userId) {
    const token = localStorage.getItem('authToken');
    const socket = new SockJS('/ws-chat?token=' + encodeURIComponent(token));
    stompClient = Stomp.over(socket);

    stompClient.connect({}, () => {
        stompClient.subscribe('/topic/alerts', (message) => {
            const newAlert = JSON.parse(message.body);
            alertAnchorState.targetId = newAlert.id;
            playNotificationSound();
            loadAlerts(true);
        });
    }, (error) => {
        console.error('WebSocket连接失败:', error);
        setTimeout(() => connectWebSocket(userId), 5000);
    });
}

function playNotificationSound() {
    const audio = new Audio('data:audio/wav;base64,UklGRigAAABXQVZFZm10IBIAAAABAAEARKwAAIhYAQACABAAAABkYXRhAgAAAAEA');
    audio.play().catch(() => {});
}

function getLevelClass(level) {
    const map = {
        '低': 'level-low',
        '中': 'level-medium',
        '高': 'level-high',
        '紧急': 'level-critical'
    };
    return map[level] || 'level-low';
}

function showToast(message, type = 'success') {
    const existingToast = document.querySelector('.toast');
    if (existingToast) existingToast.remove();

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);

    setTimeout(() => toast.classList.add('show'), 50);
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 2500);
}

async function startProcessingAlert(alertId) {
    try {
        await request(`/medical/alerts/${alertId}/process`, { method: 'PUT' });
        showToast('告警已开始处理');
        alertAnchorState.targetId = alertId;
        await loadAlerts(true);
    } catch (error) {
        console.error('开始处理告警失败:', error);
        showToast('处理失败，请稍后重试', 'error');
    }
}

async function finishProcessingAlert(alertId) {
    const result = prompt('请输入处理结果：');
    if (result === null) return;
    if (!result.trim()) {
        showToast('处理结果不能为空', 'error');
        return;
    }

    try {
        await request('/medical/alerts/handle', {
            method: 'PUT',
            body: JSON.stringify({
                alertId,
                handleResult: result.trim(),
                status: '已处理'
            })
        });
        showToast('告警处理完成');
        alertAnchorState.targetId = alertId;
        await loadAlerts(true);
    } catch (error) {
        console.error('完成处理告警失败:', error);
        showToast('处理失败，请稍后重试', 'error');
    }
}

async function ignoreAlert(alertId) {
    if (!confirm('确定要忽略这条告警吗？')) return;
    try {
        await request(`/medical/alerts/${alertId}/ignore`, { method: 'PUT' });
        showToast('告警已忽略');
        await loadAlerts(true);
    } catch (error) {
        console.error('忽略告警失败:', error);
        showToast('操作失败，请稍后重试', 'error');
    }
}
