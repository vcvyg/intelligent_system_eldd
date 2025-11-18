// API基础URL
const API_BASE_URL = 'http://localhost:8080/api';

// 初始化Swiper轮播
document.addEventListener('DOMContentLoaded', () => {
    // 初始化全屏轮播
    const heroSwiper = new Swiper('.hero-swiper', {
        direction: 'horizontal',
        loop: true,
        autoplay: {
            delay: 5000,
            disableOnInteraction: false,
        },
        pagination: {
            el: '.swiper-pagination',
            clickable: true,
        },
        effect: 'fade',
        fadeEffect: {
            crossFade: true
        },
        speed: 1000,
    });

    // 向下滚动提示点击事件
    const scrollIndicator = document.getElementById('scrollIndicator');
    const loginSection = document.getElementById('loginSection');

    scrollIndicator.addEventListener('click', () => {
        loginSection.scrollIntoView({ behavior: 'smooth' });
    });

    // 监听滚动,自动隐藏滚动提示
    let lastScrollTop = 0;
    window.addEventListener('scroll', () => {
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
        if (scrollTop > 100) {
            scrollIndicator.style.opacity = '0';
            scrollIndicator.style.pointerEvents = 'none';
        } else {
            scrollIndicator.style.opacity = '1';
            scrollIndicator.style.pointerEvents = 'auto';
        }
        lastScrollTop = scrollTop;
    }, false);
});

// 获取DOM元素
const loginForm = document.getElementById('loginForm');
const messageDiv = document.getElementById('message');

// 显示消息
function showMessage(message, type) {
    messageDiv.textContent = message;
    messageDiv.className = `message ${type}`;
    messageDiv.style.display = 'block';

    // 3秒后自动隐藏
    setTimeout(() => {
        messageDiv.style.display = 'none';
    }, 3000);
}

// 登录表单提交
loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    // 获取表单数据
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;
    const role = document.getElementById('role').value;

    // 验证表单
    if (!username || !password || !role) {
        showMessage('请填写完整的登录信息', 'error');
        return;
    }

    // 禁用提交按钮,防止重复提交
    const submitBtn = loginForm.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.classList.add('loading');

    try {
        // 调用登录API
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                username: username,
                password: password
            })
        });

        const result = await response.json();

        if (response.ok && result.code === 200) {
            // 验证角色是否匹配
            if (result.data.userInfo.role !== role) {
                showMessage('登录身份不匹配,请重新选择', 'error');
                submitBtn.disabled = false;
                submitBtn.classList.remove('loading');
                return;
            }

            // 登录成功
            showMessage('登录成功,正在跳转...', 'success');

            // 保存Token和用户信息
            localStorage.setItem('token', result.data.token);
            localStorage.setItem('userInfo', JSON.stringify(result.data.userInfo));

            // 记住我功能
            const rememberMe = document.getElementById('rememberMe').checked;
            if (rememberMe) {
                localStorage.setItem('rememberedUsername', username);
            } else {
                localStorage.removeItem('rememberedUsername');
            }

            // 1.5秒后根据角色跳转
            setTimeout(() => {
                switch (result.data.userInfo.role) {
                    case 'ADMIN':
                        window.location.href = 'admin-dashboard.html';
                        break;
                    case 'MEDICAL':
                        window.location.href = 'medical-dashboard.html';
                        break;
                    // case 'FAMILY':
                    //     window.location.href = 'family-dashboard.html'; // 未来可以为子女角色添加
                    //     break;
                    default:
                        // 对于其他角色或未定义角色，可以跳转到通用页面
                        window.location.href = 'index.html';
                        break;
                }
            }, 1500);

        } else {
            // 登录失败
            showMessage(result.message || '登录失败,请检查用户名和密码', 'error');
            submitBtn.disabled = false;
            submitBtn.classList.remove('loading');
        }

    } catch (error) {
        console.error('登录错误:', error);
        showMessage('网络错误,请检查后端服务是否启动', 'error');
        submitBtn.disabled = false;
        submitBtn.classList.remove('loading');
    }
});

// 页面加载时,填充记住的用户名
window.addEventListener('DOMContentLoaded', () => {
    const rememberedUsername = localStorage.getItem('rememberedUsername');
    if (rememberedUsername) {
        document.getElementById('username').value = rememberedUsername;
        document.getElementById('rememberMe').checked = true;
    }
});
