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

function renderPendingAlerts(alerts) {
    const tbody = document.getElementById('pendingAlertsBody');
    if (!alerts || alerts.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align: center;">无待处理告警</td></tr>';
        return;
    }
    tbody.innerHTML = alerts.map(a => `
        <tr>
            <td>${formatDateTime(a.alertTime)}</td>
            <td>${a.elderlyName}</td>
            <td>${a.alertContent}</td>
            <td><a href="medical-alerts.html?alertId=${a.id}" class="btn-primary btn-sm">去处理</a></td>
        </tr>
    `).join('');
}

