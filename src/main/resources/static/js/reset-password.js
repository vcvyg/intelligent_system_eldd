// API基础URL
const API_BASE_URL = 'http://localhost:8080/api';

// 获取DOM元素
const resetForm = document.getElementById('resetForm');
const messageDiv = document.getElementById('message');
const sendCodeBtn = document.getElementById('sendCodeBtn');
const emailInput = document.getElementById('email');
const codeInput = document.getElementById('code');
const newPasswordInput = document.getElementById('newPassword');
const confirmPasswordInput = document.getElementById('confirmPassword');

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

// 验证邮箱格式
function validateEmail(email) {
    if (!email || email.length === 0) {
        showValidationHint('email', '邮箱为必填项', 'error');
        return false;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        showValidationHint('email', '邮箱格式不正确', 'error');
        return false;
    }

    showValidationHint('email', '✓ 格式正确', 'success');
    return true;
}

// 计算密码强度
function calculatePasswordStrength(password) {
    if (!password || password.length === 0) {
        return { strength: 0, text: '' };
    }

    let strength = 0;

    if (password.length >= 6) strength += 1;
    if (password.length >= 10) strength += 1;
    if (/[a-z]/.test(password)) strength += 1;
    if (/[A-Z]/.test(password)) strength += 1;
    if (/[0-9]/.test(password)) strength += 1;
    if (/[^a-zA-Z0-9]/.test(password)) strength += 1;

    if (strength <= 2) {
        return { strength: 1, text: '弱', level: 'weak' };
    } else if (strength <= 4) {
        return { strength: 2, text: '中等', level: 'medium' };
    } else {
        return { strength: 3, text: '强', level: 'strong' };
    }
}

// 验证新密码
function validateNewPassword(password) {
    const strengthIndicator = document.querySelector('.password-strength');
    const strengthBar = document.querySelector('.strength-bar-fill');
    const strengthText = document.querySelector('.strength-text');

    if (!password || password.length === 0) {
        showValidationHint('newPassword', '', '');
        strengthIndicator.classList.remove('show');
        return false;
    }

    if (password.length < 6) {
        showValidationHint('newPassword', '密码至少6个字符', 'error');
        strengthIndicator.classList.remove('show');
        return false;
    }

    const { strength, text, level } = calculatePasswordStrength(password);
    strengthIndicator.classList.add('show');
    strengthBar.className = 'strength-bar-fill ' + level;
    strengthText.className = 'strength-text ' + level;
    strengthText.textContent = '密码强度: ' + text;

    if (level === 'weak') {
        showValidationHint('newPassword', '建议使用字母、数字和特殊字符组合', 'info');
    } else if (level === 'medium') {
        showValidationHint('newPassword', '✓ 密码强度中等', 'success');
    } else {
        showValidationHint('newPassword', '✓ 密码强度很高', 'success');
    }

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

// 绑定实时验证
emailInput.addEventListener('input', (e) => {
    validateEmail(e.target.value.trim());
});

newPasswordInput.addEventListener('input', (e) => {
    const password = e.target.value;
    validateNewPassword(password);
    const confirmPassword = confirmPasswordInput.value;
    if (confirmPassword) {
        validateConfirmPassword(password, confirmPassword);
    }
});

confirmPasswordInput.addEventListener('input', (e) => {
    const password = newPasswordInput.value;
    const confirmPassword = e.target.value;
    validateConfirmPassword(password, confirmPassword);
});

// 发送验证码
sendCodeBtn.addEventListener('click', async () => {
    const email = emailInput.value.trim();

    // 验证邮箱
    if (!validateEmail(email)) {
        emailInput.focus();
        return;
    }

    // 禁用按钮
    sendCodeBtn.disabled = true;
    sendCodeBtn.textContent = '发送中...';

    try {
        // 调用发送重置密码验证码API
        const response = await fetch(`${API_BASE_URL}/auth/sendResetPasswordCode`, {
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

// 表单提交
resetForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const email = emailInput.value.trim();
    const code = codeInput.value.trim();
    const newPassword = newPasswordInput.value;
    const confirmPassword = confirmPasswordInput.value;

    // 验证表单
    if (!email || !code || !newPassword || !confirmPassword) {
        showMessage('请填写所有必填项', 'error');
        return;
    }

    if (!validateEmail(email)) {
        showMessage('邮箱格式不正确', 'error');
        return;
    }

    if (newPassword.length < 6) {
        showMessage('密码至少6个字符', 'error');
        return;
    }

    if (newPassword !== confirmPassword) {
        showMessage('两次密码输入不一致', 'error');
        return;
    }

    // 禁用提交按钮
    const submitBtn = resetForm.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.classList.add('loading');
    submitBtn.querySelector('span').textContent = '重置中...';

    try {
        const response = await fetch(`${API_BASE_URL}/auth/resetPassword`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                email: email,
                code: code,
                newPassword: newPassword
            })
        });

        const result = await response.json();

        if (response.ok && result.code === 200) {
            showMessage('密码重置成功! 3秒后自动跳转到登录页...', 'success');

            setTimeout(() => {
                window.location.href = 'login.html';
            }, 3000);

        } else {
            showMessage(result.message || '重置失败,请重试', 'error');
            submitBtn.disabled = false;
            submitBtn.classList.remove('loading');
            submitBtn.querySelector('span').textContent = '重置密码';
        }

    } catch (error) {
        console.error('重置密码错误:', error);
        showMessage('网络错误,请检查后端服务是否启动', 'error');
        submitBtn.disabled = false;
        submitBtn.classList.remove('loading');
        submitBtn.querySelector('span').textContent = '重置密码';
    }
});
