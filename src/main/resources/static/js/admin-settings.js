// 系统设置页面脚本

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', () => {
    checkLogin();
    updateUserInfo();
    loadSystemStatistics();
    initTabs();
});

// 初始化标签页
function initTabs() {
    document.querySelectorAll('.tab-btn').forEach((btn, index) => {
        btn.addEventListener('click', function() {
            switchTab(this);
        });
    });
}

// 更新用户信息显示
function updateUserInfo() {
    const userInfo = getUserInfo();
    if (userInfo) {
        const welcomeText = document.getElementById('welcomeText');
        if (welcomeText) {
            welcomeText.textContent = `欢迎,${userInfo.username || '管理员'}!`;
        }
    }
}

// 切换标签页
function switchTab(btnElement) {
    // 获取标签名
    const tabName = btnElement.textContent.trim();
    let targetTab;

    if (tabName.includes('系统信息')) {
        targetTab = 'system';
    } else if (tabName.includes('操作日志')) {
        targetTab = 'logs';
    } else if (tabName.includes('关于系统')) {
        targetTab = 'about';
    }

    // 移除所有active类
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));

    // 添加active类到当前标签
    btnElement.classList.add('active');
    document.getElementById(`${targetTab}-tab`).classList.add('active');
}

// 加载系统统计数据
async function loadSystemStatistics() {
    await Promise.all([
        loadUserStatistics(),
        loadDeviceStatistics(),
        loadAlertStatistics()
    ]);
}

// 加载用户统计
async function loadUserStatistics() {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE_URL}/admin/statistics/overview`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();
        if (result.code === 200) {
            const data = result.data;
            document.getElementById('totalUsersCount').textContent = data.totalUsers || 0;
            document.getElementById('adminCount').textContent = data.adminCount || 0;
            document.getElementById('familyCount').textContent = data.familyCount || 0;
            document.getElementById('medicalCount').textContent = data.medicalCount || 0;
            document.getElementById('elderlyCount').textContent = data.elderlyCount || 0;
        }
    } catch (error) {
        console.error('加载用户统计失败:', error);
        // 显示默认值
        document.getElementById('totalUsersCount').textContent = '0';
        document.getElementById('adminCount').textContent = '0';
        document.getElementById('familyCount').textContent = '0';
        document.getElementById('medicalCount').textContent = '0';
        document.getElementById('elderlyCount').textContent = '0';
    }
}

// 加载设备统计
async function loadDeviceStatistics() {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE_URL}/admin/devices/statistics`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();
        if (result.code === 200) {
            const data = result.data;
            document.getElementById('deviceTotal').textContent = data.total || 0;
            document.getElementById('deviceOnline').textContent = data.online || 0;
            document.getElementById('deviceOffline').textContent = data.offline || 0;
            document.getElementById('deviceFault').textContent = data.fault || 0;
        }
    } catch (error) {
        console.error('加载设备统计失败:', error);
        document.getElementById('deviceTotal').textContent = '0';
        document.getElementById('deviceOnline').textContent = '0';
        document.getElementById('deviceOffline').textContent = '0';
        document.getElementById('deviceFault').textContent = '0';
    }
}

// 加载预警统计
async function loadAlertStatistics() {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE_URL}/admin/alerts/statistics`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();
        if (result.code === 200) {
            const data = result.data;
            document.getElementById('alertTotal').textContent = data.total || 0;
            document.getElementById('alertPending').textContent = data.pending || 0;
            document.getElementById('alertProcessing').textContent = data.processing || 0;
            document.getElementById('alertHandled').textContent = data.handled || 0;
        }
    } catch (error) {
        console.error('加载预警统计失败:', error);
        document.getElementById('alertTotal').textContent = '0';
        document.getElementById('alertPending').textContent = '0';
        document.getElementById('alertProcessing').textContent = '0';
        document.getElementById('alertHandled').textContent = '0';
    }
}
