// 检查登录状态
function checkLogin() {
    const token = localStorage.getItem('token');
    const userInfo = localStorage.getItem('userInfo');

    if (!token || !userInfo) {
        // 未登录,跳转到登录页
        window.location.href = 'login.html';
        return null;
    }

    return JSON.parse(userInfo);
}

// 退出登录
function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    window.location.href = 'login.html';
}

// 获取角色名称
function getRoleName(role) {
    const roleMap = {
        'ADMIN': '管理员',
        'FAMILY': '子女',
        'MEDICAL': '医护人员',
        'ELDERLY': '老人'
    };
    return roleMap[role] || role;
}

// 获取角色对应的快捷操作
function getQuickActions(role) {
    const actions = {
        'ADMIN': [
            { title: '用户管理', desc: '管理系统用户', link: '/api/admin/user' },
            { title: '健康数据统计', desc: '查看健康数据汇总', link: '#' },
            { title: '设备管理', desc: '管理监测设备', link: '#' },
            { title: '预警处理', desc: '处理异常预警', link: '#' }
        ],
        'FAMILY': [
            { title: '老人健康', desc: '查看老人健康数据', link: '#' },
            { title: 'GPS定位', desc: '查看老人位置', link: '#' },
            { title: '视频通话', desc: '与老人视频沟通', link: '#' },
            { title: '预警通知', desc: '查看预警消息', link: '#' }
        ],
        'MEDICAL': [
            { title: '健康档案', desc: '查看老人健康档案', link: '#' },
            { title: '巡诊记录', desc: '录入巡诊记录', link: '#' },
            { title: '护理计划', desc: '安排护理计划', link: '#' },
            { title: '远程问诊', desc: '在线咨询服务', link: '#' }
        ],
        'ELDERLY': [
            { title: '我的健康', desc: '查看我的健康数据', link: '#' },
            { title: '联系子女', desc: '联系我的子女', link: '#' },
            { title: '医护服务', desc: '查看医护安排', link: '#' },
            { title: '紧急求助', desc: '紧急情况呼叫', link: '#' }
        ]
    };
    return actions[role] || [];
}

// 页面初始化
window.addEventListener('DOMContentLoaded', () => {
    // 检查登录状态
    const userInfo = checkLogin();
    if (!userInfo) return;

    // 管理员角色直接跳转到管理后台
    if (userInfo.role === 'ADMIN') {
        window.location.href = 'admin-dashboard.html';
        return;
    }

    // 显示欢迎信息
    const welcomeText = document.getElementById('welcomeText');
    welcomeText.textContent = `欢迎, ${userInfo.username}!`;

    // 显示用户详情
    const userDetails = document.getElementById('userDetails');
    userDetails.innerHTML = `
        <div class="detail-item">
            <span class="label">用户名:</span>
            <span class="value">${userInfo.username}</span>
        </div>
        <div class="detail-item">
            <span class="label">姓名:</span>
            <span class="value">${userInfo.realName || '未设置'}</span>
        </div>
        <div class="detail-item">
            <span class="label">角色:</span>
            <span class="value">
                <span class="role-badge ${userInfo.role.toLowerCase()}">${getRoleName(userInfo.role)}</span>
            </span>
        </div>
        <div class="detail-item">
            <span class="label">邮箱:</span>
            <span class="value">${userInfo.email || '未设置'}</span>
        </div>
        <div class="detail-item">
            <span class="label">手机:</span>
            <span class="value">${userInfo.phone || '未设置'}</span>
        </div>
    `;

    // 显示快捷操作
    const actionCards = document.getElementById('actionCards');
    const actions = getQuickActions(userInfo.role);
    actionCards.innerHTML = actions.map(action => `
        <div class="action-card" onclick="location.href='${action.link}'">
            <h4>${action.title}</h4>
            <p>${action.desc}</p>
        </div>
    `).join('');

    // 绑定退出登录按钮
    document.getElementById('logoutBtn').addEventListener('click', () => {
        if (confirm('确定要退出登录吗?')) {
            logout();
        }
    });
});
