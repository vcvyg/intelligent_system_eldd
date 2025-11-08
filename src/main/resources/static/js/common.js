// 公共函数库

// API基础URL
const API_BASE_URL = '';

// 获取token
function getToken() {
    return localStorage.getItem('token');
}

// 获取用户信息
function getUserInfo() {
    const userInfo = localStorage.getItem('userInfo');
    return userInfo ? JSON.parse(userInfo) : null;
}

// 检查登录状态
function checkLogin() {
    const token = getToken();
    const userInfo = getUserInfo();

    if (!token || !userInfo) {
        window.location.href = 'login.html';
        return null;
    }

    return userInfo;
}

// 退出登录
function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    window.location.href = 'login.html';
}

// HTTP请求封装
async function request(url, options = {}) {
    const token = getToken();

    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json',
            ...(token && { 'Authorization': `Bearer ${token}` })
        }
    };

    const finalOptions = {
        ...defaultOptions,
        ...options,
        headers: {
            ...defaultOptions.headers,
            ...options.headers
        }
    };

    try {
        const response = await fetch(API_BASE_URL + url, finalOptions);
        const data = await response.json();

        // 处理未授权
        if (response.status === 401 || response.status === 403) {
            alert('登录已过期，请重新登录');
            logout();
            return null;
        }

        // 处理业务错误
        if (!data.success) {
            throw new Error(data.message || '请求失败');
        }

        return data;
    } catch (error) {
        console.error('Request error:', error);
        alert(error.message || '网络请求失败');
        throw error;
    }
}

// GET请求
async function get(url) {
    return request(url, { method: 'GET' });
}

// POST请求
async function post(url, data) {
    return request(url, {
        method: 'POST',
        body: JSON.stringify(data)
    });
}

// PUT请求
async function put(url, data) {
    return request(url, {
        method: 'PUT',
        body: JSON.stringify(data)
    });
}

// DELETE请求
async function del(url) {
    return request(url, { method: 'DELETE' });
}

// 格式化日期
function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('zh-CN');
}

// 格式化日期时间
function formatDateTime(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('zh-CN');
}

// 获取角色中文名
function getRoleName(role) {
    const roleMap = {
        'ADMIN': '管理员',
        'FAMILY': '子女',
        'MEDICAL': '医护人员',
        'ELDERLY': '老人'
    };
    return roleMap[role] || role;
}

// 获取状态文本
function getStatusText(status) {
    return status === 1 ? '启用' : '禁用';
}

// 显示加载中
function showLoading(message = '加载中...') {
    // 简单实现,可以使用更复杂的loading组件
    console.log(message);
}

// 隐藏加载中
function hideLoading() {
    console.log('Loading hidden');
}
