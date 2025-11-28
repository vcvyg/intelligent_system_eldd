document.addEventListener('DOMContentLoaded', function() {
    const userInfo = checkLogin();
    if (!userInfo || userInfo.role !== 'MEDICAL') {
        alert('权限不足');
        logout();
        return;
    }
    document.getElementById('welcomeText').textContent = `欢迎, ${userInfo.username}!`;

    loadDashboardData();
});

async function loadDashboardData() {
    try {
        const result = await get('/medical/dashboard');
        if (result.code === 200 && result.data) {
            const data = result.data;
            renderStats(data);
            renderTodaySchedules(data.todaySchedules);
            renderPendingAlerts(data.pendingAlerts);
        }
    } catch (error) {
        console.error('加载工作台数据失败:', error);
    }
}

function renderStats(data) {
    const statsGrid = document.getElementById('statsGrid');
    statsGrid.innerHTML = `
        <div class="stat-card">
            <div class="stat-icon users">👴</div>
            <div class="stat-info">
                <h3>负责老人数量</h3>
                <p class="stat-value">${data.assignedPatientsCount}</p>
            </div>
        </div>
        <div class="stat-card">
            <div class="stat-icon health">📅</div>
            <div class="stat-info">
                <h3>今日排班数</h3>
                <p class="stat-value">${data.todaySchedulesCount}</p>
            </div>
        </div>
        <div class="stat-card">
            <div class="stat-icon alerts-pending">⚠️</div>
            <div class="stat-info">
                <h3>待处理告警</h3>
                <p class="stat-value">${data.pendingAlertsCount}</p>
            </div>
        </div>
    `;
}

function renderTodaySchedules(schedules) {
    const tbody = document.getElementById('todaySchedulesBody');
    if (!schedules || schedules.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align: center;">今日无排班</td></tr>';
        return;
    }
    tbody.innerHTML = schedules.map(s => `
        <tr>
            <td>${s.startTime.substring(0, 5)} - ${s.endTime.substring(0, 5)}</td>
            <td>${s.shiftType}</td>
            <td><span class="badge ${s.status === '正常' ? 'badge-green' : 'badge-yellow'}">${s.status}</span></td>
            <td>${s.roomNumber || '-'}</td>
        </tr>
    `).join('');
}

const pendingAlertsState = {
    data: [],
    currentPage: 1,
    pageSize: 5
};

let pendingControlsBound = false;

function renderPendingAlerts(alerts) {
    pendingAlertsState.data = alerts || [];
    pendingAlertsState.currentPage = 1;
    updatePendingAlertsView();
}

function updatePendingAlertsView() {
    const tbody = document.getElementById('pendingAlertsBody');
    const totalEl = document.getElementById('pendingAlertsTotal');
    const pagination = document.getElementById('pendingAlertsPagination');
    const pageInfo = document.getElementById('pendingPageInfo');
    const prevBtn = document.getElementById('pendingPrevBtn');
    const nextBtn = document.getElementById('pendingNextBtn');

    const total = pendingAlertsState.data.length;
    if (totalEl) totalEl.textContent = total;

    if (!total) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align: center;">无待处理告警</td></tr>';
        if (pagination) pagination.style.display = 'none';
        return;
    }

    const totalPages = Math.ceil(total / pendingAlertsState.pageSize);
    pendingAlertsState.currentPage = Math.min(pendingAlertsState.currentPage, totalPages);
    const start = (pendingAlertsState.currentPage - 1) * pendingAlertsState.pageSize;
    const visibleAlerts = pendingAlertsState.data.slice(start, start + pendingAlertsState.pageSize);

    tbody.innerHTML = visibleAlerts.map((a, idx) => `
        <tr>
            <td>${start + idx + 1}</td>
            <td>${formatDateTime(a.alertTime)}</td>
            <td>${a.elderlyName}</td>
            <td>${a.alertContent}</td>
            <td><a href="medical-alerts.html?alertId=${a.id}" class="btn-primary btn-sm">去处理</a></td>
        </tr>
    `).join('');

    if (pagination) {
        pagination.style.display = totalPages > 1 ? 'flex' : 'none';
        if (pageInfo) pageInfo.textContent = `第 ${pendingAlertsState.currentPage} / ${totalPages} 页`;
        if (prevBtn) prevBtn.disabled = pendingAlertsState.currentPage === 1;
        if (nextBtn) nextBtn.disabled = pendingAlertsState.currentPage === totalPages;
        if (!pendingControlsBound) {
            prevBtn?.addEventListener('click', () => changePendingPage(-1));
            nextBtn?.addEventListener('click', () => changePendingPage(1));
            pendingControlsBound = true;
        }
    }
}

function changePendingPage(delta) {
    const totalPages = Math.ceil(pendingAlertsState.data.length / pendingAlertsState.pageSize);
    const nextPage = pendingAlertsState.currentPage + delta;
    if (nextPage < 1 || nextPage > totalPages) return;
    pendingAlertsState.currentPage = nextPage;
    updatePendingAlertsView();
}

