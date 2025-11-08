// 管理员仪表盘

// 页面加载完成
window.addEventListener('DOMContentLoaded', () => {
    // 检查登录状态
    const userInfo = checkLogin();
    if (!userInfo || userInfo.role !== 'ADMIN') {
        alert('权限不足');
        logout();
        return;
    }

    // 显示欢迎信息
    document.getElementById('welcomeText').textContent = `欢迎，${userInfo.realName || userInfo.username}！`;

    // 加载统计数据
    loadStatistics();
});

// 加载统计数据
async function loadStatistics() {
    try {
        const response = await get('/api/admin/statistics');
        if (response && response.data) {
            const stats = response.data;

            // 更新统计卡片
            document.getElementById('totalUsers').textContent = stats.totalUsers || 0;
            document.getElementById('totalElderly').textContent = stats.totalElderly || 0;
            document.getElementById('activeUsers').textContent = stats.activeUsers || 0;
            document.getElementById('todayHealthRecords').textContent = stats.todayHealthRecords || 0;

            // 更新角色分布
            document.getElementById('adminCount').textContent = stats.adminCount || 0;
            document.getElementById('familyCount').textContent = stats.familyCount || 0;
            document.getElementById('medicalCount').textContent = stats.medicalCount || 0;
            document.getElementById('elderlyCount').textContent = stats.elderlyCount || 0;
        }
    } catch (error) {
        console.error('加载统计数据失败:', error);
    }
}
