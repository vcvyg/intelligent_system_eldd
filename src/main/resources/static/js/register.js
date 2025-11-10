// API基础URL
const API_BASE_URL = 'http://localhost:8080/api';

// 防抖函数
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// 显示验证提示
function showValidationHint(inputId, message, type) {
    const hintElement = document.getElementById(inputId + 'Hint');
    const inputElement = document.getElementById(inputId);

    if (hintElement) {
        hintElement.textContent = message;
        hintElement.className = 'validation-hint ' + type;
    }

    if (inputElement) {
        inputElement.classList.remove('valid', 'invalid');
        if (type === 'success') {
            inputElement.classList.add('valid');
        } else if (type === 'error') {
            inputElement.classList.add('invalid');
        }
    }
}

// 验证用户名
async function validateUsername(username) {
    if (!username || username.length === 0) {
        showValidationHint('username', '', '');
        return false;
    }

    if (username.length < 3) {
        showValidationHint('username', '用户名至少3个字符', 'error');
        return false;
    }

    if (username.length > 20) {
        showValidationHint('username', '用户名不能超过20个字符', 'error');
        return false;
    }

    if (!/^[a-zA-Z0-9_\u4e00-\u9fa5]+$/.test(username)) {
        showValidationHint('username', '用户名只能包含字母、数字、下划线和中文', 'error');
        return false;
    }

    // 检查用户名是否已存在
    try {
        const response = await fetch(`${API_BASE_URL}/auth/checkUsername?username=${encodeURIComponent(username)}`);

        // 检查网络层面是否成功
        if (!response.ok) {
            throw new Error('Network response was not ok.');
        }

        const result = await response.json();

        if (result.code === 200 && result.data === true) {
            // 后端返回true代表可用
            showValidationHint('username', '✓ 用户名可用', 'success');
            return true;
        } else {
            showValidationHint('username', '用户名已被使用', 'error');
            return false;
        }
    } catch (error) {
        console.error('无法验证用户名:', error);
        // 当API请求失败时,给出错误提示
        showValidationHint('username', '无法验证用户名,请稍后重试', 'error');
        return false;
    }
}

// 验证真实姓名
function validateRealName(realName) {
    if (!realName || realName.length === 0) {
        showValidationHint('realName', '', '');
        return false;
    }

    if (realName.length < 2) {
        showValidationHint('realName', '姓名至少2个字符', 'error');
        return false;
    }

    if (realName.length > 20) {
        showValidationHint('realName', '姓名不能超过20个字符', 'error');
        return false;
    }

    if (!/^[\u4e00-\u9fa5a-zA-Z\s]+$/.test(realName)) {
        showValidationHint('realName', '姓名只能包含中文、字母和空格', 'error');
        return false;
    }

    showValidationHint('realName', '✓ 格式正确', 'success');
    return true;
}

// 计算密码强度
function calculatePasswordStrength(password) {
    if (!password || password.length === 0) {
        return { strength: 0, text: '' };
    }

    let strength = 0;

    // 长度加分
    if (password.length >= 6) strength += 1;
    if (password.length >= 10) strength += 1;

    // 包含小写字母
    if (/[a-z]/.test(password)) strength += 1;

    // 包含大写字母
    if (/[A-Z]/.test(password)) strength += 1;

    // 包含数字
    if (/[0-9]/.test(password)) strength += 1;

    // 包含特殊字符
    if (/[^a-zA-Z0-9]/.test(password)) strength += 1;

    if (strength <= 2) {
        return { strength: 1, text: '弱', level: 'weak' };
    } else if (strength <= 4) {
        return { strength: 2, text: '中等', level: 'medium' };
    } else {
        return { strength: 3, text: '强', level: 'strong' };
    }
}

// 验证密码
function validatePassword(password) {
    const strengthIndicator = document.querySelector('.password-strength');
    const strengthBar = document.querySelector('.strength-bar-fill');
    const strengthText = document.querySelector('.strength-text');

    if (!password || password.length === 0) {
        showValidationHint('password', '', '');
        strengthIndicator.classList.remove('show');
        return false;
    }

    // 检查长度
    if (password.length < 8) {
        showValidationHint('password', '密码至少8个字符', 'error');
        strengthIndicator.classList.remove('show');
        return false;
    }

    // 检查是否包含数字
    if (!/\d/.test(password)) {
        showValidationHint('password', '密码必须包含数字', 'error');
        strengthIndicator.classList.remove('show');
        return false;
    }

    // 检查是否包含小写字母
    if (!/[a-z]/.test(password)) {
        showValidationHint('password', '密码必须包含小写字母', 'error');
        strengthIndicator.classList.remove('show');
        return false;
    }

    // 检查是否包含大写字母
    if (!/[A-Z]/.test(password)) {
        showValidationHint('password', '密码必须包含大写字母', 'error');
        strengthIndicator.classList.remove('show');
        return false;
    }

    // 显示密码强度
    const { strength, text, level } = calculatePasswordStrength(password);
    strengthIndicator.classList.add('show');
    strengthBar.className = 'strength-bar-fill ' + level;
    strengthText.className = 'strength-text ' + level;
    strengthText.textContent = '密码强度: ' + text;

    showValidationHint('password', '✓ 密码符合要求', 'success');
    return true;
}

// 验证确认密码
function validateConfirmPassword(password, confirmPassword) {
    if (!confirmPassword || confirmPassword.length === 0) {
        showValidationHint('confirmPassword', '', '');
        return false;
    }

    if (password !== confirmPassword) {
        showValidationHint('confirmPassword', '两次密码输入不一致', 'error');
        return false;
    }

    showValidationHint('confirmPassword', '✓ 密码一致', 'success');
    return true;
}

// 初始化Swiper轮播
document.addEventListener('DOMContentLoaded', () => {
    // 初始化特性卡片轮播
    const swiper = new Swiper('.slider-features', {
        slidesPerView: 1,
        spaceBetween: 30,
        loop: true,
        autoplay: {
            delay: 4000,
            disableOnInteraction: false,
        },
        pagination: {
            el: '.swiper-pagination',
            clickable: true,
        },
        effect: 'slide',
        speed: 600,
    });
});

// 获取DOM元素
const registerForm = document.getElementById('registerForm');
const messageDiv = document.getElementById('message');
const sendCodeBtn = document.getElementById('sendCodeBtn');
const emailInput = document.getElementById('email');

// 倒计时变量
let countdown = 60;
let countdownTimer = null;

// 显示消息
function showMessage(message, type) {
    messageDiv.textContent = message;
    messageDiv.className = `message-toast ${type}`;
    messageDiv.style.display = 'block';

    // 5秒后自动隐藏
    setTimeout(() => {
        messageDiv.style.display = 'none';
    }, 5000);
}

// 验证手机号
async function validatePhone(phone) {
    if (!phone) {
        showValidationHint('phone', '', '');
        return true; // 手机号是可选的
    }

    const phoneRegex = /^1[3-9]\d{9}$/;

    if (!phoneRegex.test(phone)) {
        showValidationHint('phone', '手机号格式不正确', 'error');
        return false;
    }

    // 检查手机号是否已存在
    try {
        const response = await fetch(`${API_BASE_URL}/auth/checkPhone?phone=${encodeURIComponent(phone)}`);

        // 检查网络层面是否成功
        if (!response.ok) {
            throw new Error('Network response was not ok.');
        }

        const result = await response.json();

        if (result.code === 200 && result.data === true) {
            // 后端返回true代表可用
            showValidationHint('phone', '✓ 手机号可用', 'success');
            return true;
        } else {
            showValidationHint('phone', '该手机号已被注册', 'error');
            return false;
        }
    } catch (error) {
        console.error('无法验证手机号:', error);
        // 当API请求失败时,给出错误提示
        showValidationHint('phone', '无法验证手机号,请稍后重试', 'error');
        return false;
    }
}

// 验证邮箱
async function validateEmail(email) {
    if (!email || email.length === 0) {
        showValidationHint('email', '邮箱为必填项', 'error');
        return false;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        showValidationHint('email', '邮箱格式不正确', 'error');
        return false;
    }

    // 检查邮箱是否已存在
    try {
        const response = await fetch(`${API_BASE_URL}/auth/checkEmail?email=${encodeURIComponent(email)}`);

        // 检查网络层面是否成功
        if (!response.ok) {
            throw new Error('Network response was not ok.');
        }

        const result = await response.json();

        if (result.code === 200 && result.data === true) {
            // 后端返回true代表可用
            showValidationHint('email', '✓ 邮箱可用', 'success');
            return true;
        } else {
            showValidationHint('email', '该邮箱已被注册', 'error');
            return false;
        }
    } catch (error) {
        console.error('无法验证邮箱:', error);
        // 当API请求失败时,给出错误提示
        showValidationHint('email', '无法验证邮箱,请稍后重试', 'error');
        return false;
    }
}

// 初始化Swiper轮播
document.addEventListener('DOMContentLoaded', () => {
    // 初始化特性卡片轮播
    const swiper = new Swiper('.slider-features', {
        slidesPerView: 1,
        spaceBetween: 30,
        loop: true,
        autoplay: {
            delay: 4000,
            disableOnInteraction: false,
        },
        pagination: {
            el: '.swiper-pagination',
            clickable: true,
        },
        effect: 'slide',
        speed: 600,
    });

    // 绑定实时验证事件
    setupRealtimeValidation();
});

// 设置实时验证
function setupRealtimeValidation() {
    const usernameInput = document.getElementById('username');
    const realNameInput = document.getElementById('realName');
    const passwordInput = document.getElementById('password');
    const confirmPasswordInput = document.getElementById('confirmPassword');
    const emailInput = document.getElementById('email');
    const phoneInput = document.getElementById('phone');

    // 用户名验证(使用防抖,避免频繁请求)
    const debouncedUsernameValidation = debounce(async (e) => {
        await validateUsername(e.target.value.trim());
    }, 500);
    usernameInput.addEventListener('input', debouncedUsernameValidation);

    // 真实姓名验证
    realNameInput.addEventListener('input', (e) => {
        validateRealName(e.target.value.trim());
    });

    // 密码验证
    passwordInput.addEventListener('input', (e) => {
        const password = e.target.value;
        validatePassword(password);
        // 同时验证确认密码
        const confirmPassword = confirmPasswordInput.value;
        if (confirmPassword) {
            validateConfirmPassword(password, confirmPassword);
        }
    });

    // 确认密码验证
    confirmPasswordInput.addEventListener('input', (e) => {
        const password = passwordInput.value;
        const confirmPassword = e.target.value;
        validateConfirmPassword(password, confirmPassword);
    });

    // 邮箱验证(使用防抖)
    const debouncedEmailValidation = debounce(async (e) => {
        await validateEmail(e.target.value.trim());
    }, 500);
    emailInput.addEventListener('input', debouncedEmailValidation);

    // 手机号验证(使用防抖)
    const debouncedPhoneValidation = debounce(async (e) => {
        await validatePhone(e.target.value.trim());
    }, 500);
    phoneInput.addEventListener('input', debouncedPhoneValidation);
}

// 发送验证码
sendCodeBtn.addEventListener('click', async () => {
    const email = emailInput.value.trim();

    // 验证邮箱
    const isValid = await validateEmail(email);
    if (!isValid) {
        emailInput.focus();
        return;
    }

    // 禁用按钮
    sendCodeBtn.disabled = true;
    sendCodeBtn.textContent = '发送中...';

    try {
        // 调用发送验证码API
        const response = await fetch(`${API_BASE_URL}/auth/sendEmailCode`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                email: email
            })
        });

        const result = await response.json();

        if (response.ok && result.code === 200) {
            // 发送成功
            showMessage('验证码已发送到您的邮箱,请查收!(有效期5分钟)', 'success');

            // 开始倒计时
            countdown = 60;
            sendCodeBtn.textContent = `${countdown}秒后重新获取`;

            countdownTimer = setInterval(() => {
                countdown--;
                if (countdown > 0) {
                    sendCodeBtn.textContent = `${countdown}秒后重新获取`;
                } else {
                    clearInterval(countdownTimer);
                    sendCodeBtn.disabled = false;
                    sendCodeBtn.textContent = '获取验证码';
                }
            }, 1000);

        } else {
            showMessage(result.message || '发送失败,请重试', 'error');
            sendCodeBtn.disabled = false;
            sendCodeBtn.textContent = '获取验证码';
        }

    } catch (error) {
        console.error('发送验证码错误:', error);
        showMessage('网络错误,请检查后端服务是否启动', 'error');
        sendCodeBtn.disabled = false;
        sendCodeBtn.textContent = '获取验证码';
    }
});

// 注册表单提交
registerForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    // 获取表单数据
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    const realName = document.getElementById('realName').value.trim();
    const phone = document.getElementById('phone').value.trim();
    const code = document.getElementById('code').value.trim();
    const email = document.getElementById('email').value.trim();
    const role = document.getElementById('role').value;
    const agree = document.getElementById('agree').checked;

    // 验证表单
    if (!username || !password || !realName || !email || !code || !role) {
        showMessage('请填写所有必填项', 'error');
        return;
    }

    if (password !== confirmPassword) {
        showMessage('两次密码输入不一致', 'error');
        return;
    }

    if (!validateEmail(email)) {
        showMessage('邮箱格式不正确', 'error');
        return;
    }

    if (phone && !validatePhone(phone)) {
        showMessage('手机号格式不正确', 'error');
        return;
    }

    if (!agree) {
        showMessage('请先同意用户协议和隐私政策', 'error');
        return;
    }

    // 禁用提交按钮,防止重复提交
    const submitBtn = registerForm.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.classList.add('loading');
    submitBtn.querySelector('span').textContent = '注册中...';

    try {
        // 调用注册API
        const response = await fetch(`${API_BASE_URL}/auth/register`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                username: username,
                password: password,
                realName: realName,
                phone: phone || null,
                email: email,
                code: code,
                role: role
            })
        });

        const result = await response.json();

        if (response.ok && result.code === 200) {
            // 注册成功
            showMessage('注册成功! 3秒后自动跳转到登录页...', 'success');

            // 3秒后跳转到登录页
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 3000);

        } else {
            // 注册失败
            showMessage(result.message || '注册失败,请重试', 'error');
            submitBtn.disabled = false;
            submitBtn.classList.remove('loading');
            submitBtn.querySelector('span').textContent = '立即注册';
        }

    } catch (error) {
        console.error('注册错误:', error);
        showMessage('网络错误,请检查后端服务是否启动', 'error');
        submitBtn.disabled = false;
        submitBtn.classList.remove('loading');
        submitBtn.querySelector('span').textContent = '立即注册';
    }
});
