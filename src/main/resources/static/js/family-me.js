const ACCOUNT_STORAGE_KEY = 'familyAccountSettings';
const SYSTEM_STORAGE_KEY = 'familySystemSettings';

let currentUserInfo = null;

document.addEventListener('DOMContentLoaded', () => {
    currentUserInfo = checkLogin();
    if (!currentUserInfo || currentUserInfo.role !== 'FAMILY') {
        alert('权限不足或登录已过期');
        logout();
        return;
    }

    const welcome = document.getElementById('welcomeText');
    if (welcome) {
        welcome.textContent = `欢迎，${currentUserInfo.username}！`;
    }

    initActionSwitching();
    initAccountForm();
    initSystemSettings();
    initBindSection();
    initQuickLinks();
});

function initActionSwitching() {
    const actions = document.querySelectorAll('.settings-action');
    const panels = document.querySelectorAll('.settings-panel');
    actions.forEach(action => {
        action.addEventListener('click', () => {
            const target = action.dataset.panel;
            actions.forEach(btn => btn.classList.toggle('active', btn === action));
            panels.forEach(panel => panel.classList.toggle('active', panel.dataset.panel === target));
        });
    });
}

function initAccountForm() {
    const saved = safeParseLocalStorage(ACCOUNT_STORAGE_KEY);
    const nicknameInput = document.getElementById('accountNickname');
    const phoneInput = document.getElementById('accountPhone');
    const emailInput = document.getElementById('accountEmail');
    const emergencyContactInput = document.getElementById('emergencyContact');
    const emergencyPhoneInput = document.getElementById('emergencyPhone');

    if (nicknameInput) {
        nicknameInput.value = saved.nickname || currentUserInfo.realName || currentUserInfo.username || '';
    }
    if (phoneInput) {
        phoneInput.value = saved.phone || '';
    }
    if (emailInput) {
        emailInput.value = saved.email || '';
    }
    if (emergencyContactInput) {
        emergencyContactInput.value = saved.emergencyContact || '';
    }
    if (emergencyPhoneInput) {
        emergencyPhoneInput.value = saved.emergencyPhone || '';
    }

    const form = document.getElementById('accountForm');
    if (form) {
        form.addEventListener('submit', event => {
            event.preventDefault();
            const payload = {
                nickname: nicknameInput.value.trim(),
                phone: phoneInput.value.trim(),
                email: emailInput.value.trim(),
                emergencyContact: emergencyContactInput.value.trim(),
                emergencyPhone: emergencyPhoneInput.value.trim()
            };

            if (payload.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(payload.email)) {
                setHint('accountSaveHint', '邮箱格式不正确', false);
                return;
            }

            localStorage.setItem(ACCOUNT_STORAGE_KEY, JSON.stringify(payload));
            setHint('accountSaveHint', '保存成功', true);
        });
    }
}

function initSystemSettings() {
    const saved = safeParseLocalStorage(SYSTEM_STORAGE_KEY, {
        notifications: true,
        sms: false,
        darkMode: false
    });
    const notificationToggle = document.getElementById('settingNotifications');
    const smsToggle = document.getElementById('settingSms');
    const darkModeToggle = document.getElementById('settingDarkMode');

    if (notificationToggle) {
        notificationToggle.checked = !!saved.notifications;
    }
    if (smsToggle) {
        smsToggle.checked = !!saved.sms;
    }
    if (darkModeToggle) {
        darkModeToggle.checked = !!saved.darkMode;
    }

    const saveBtn = document.getElementById('saveSystemSettings');
    if (saveBtn) {
        saveBtn.addEventListener('click', () => {
            const newSettings = {
                notifications: notificationToggle.checked,
                sms: smsToggle.checked,
                darkMode: darkModeToggle.checked
            };
            localStorage.setItem(SYSTEM_STORAGE_KEY, JSON.stringify(newSettings));
            setHint('systemSaveHint', '偏好已同步', true);
        });
    }
}

function initBindSection() {
    const searchBtn = document.getElementById('bindSearchBtn');
    const searchInput = document.getElementById('bindSearchInput');
    const resultsContainer = document.getElementById('bindSearchResults');

    if (!searchBtn || !searchInput || !resultsContainer) {
        return;
    }

    searchBtn.addEventListener('click', () => {
        triggerBindSearch();
    });

    searchInput.addEventListener('keypress', event => {
        if (event.key === 'Enter') {
            event.preventDefault();
            triggerBindSearch();
        }
    });

    resultsContainer.addEventListener('click', event => {
        const bindBtn = event.target.closest('[data-bind-id]');
        if (!bindBtn) {
            return;
        }
        const elderlyId = bindBtn.dataset.bindId;
        const elderlyName = bindBtn.dataset.bindName;
        if (elderlyId) {
            bindElderlyFromMePage(elderlyId, elderlyName);
        }
    });
}

async function triggerBindSearch() {
    const searchInput = document.getElementById('bindSearchInput');
    const placeholder = document.getElementById('bindResultsPlaceholder');
    const resultsContainer = document.getElementById('bindSearchResults');
    if (!searchInput || !placeholder || !resultsContainer) {
        return;
    }
    const keyword = searchInput.value.trim();
    if (!keyword) {
        alert('请输入搜索关键词');
        return;
    }

    placeholder.textContent = '搜索中...';
    resultsContainer.innerHTML = '';

    try {
        const result = await get(`/family/relation/search-elderly?keyword=${encodeURIComponent(keyword)}`);
        if (result.code !== 200) {
            placeholder.textContent = `搜索失败：${result.message || '未知错误'}`;
            return;
        }
        const list = result.data || [];
        if (list.length === 0) {
            placeholder.textContent = '未找到相关老人';
            return;
        }
        placeholder.textContent = '';
        resultsContainer.innerHTML = list.map(elderly => {
            const name = elderly.name || '-';
            const age = elderly.age != null ? elderly.age : '-';
            const gender = elderly.gender === 'M' ? '男' : elderly.gender === 'F' ? '女' : '-';
            const idCard = elderly.id_card || '-';
            return `
                <div class="bind-result">
                    <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;">
                        <div style="flex:1;">
                            <h4>${name}</h4>
                            <p style="margin:4px 0;color:var(--color-text-gray);font-size:14px;">
                                <span>年龄：${age}</span>
                                <span style="margin-left:12px;">性别：${gender}</span>
                            </p>
                            <p style="margin:4px 0;color:var(--color-text-gray);font-size:14px;">
                                身份证：${idCard}
                            </p>
                        </div>
                        <button class="btn-primary btn-sm" data-bind-id="${elderly.id}" data-bind-name="${name}">绑定</button>
                    </div>
                </div>
            `;
        }).join('');
    } catch (error) {
        console.error('搜索老人失败:', error);
        placeholder.textContent = '搜索失败，请稍后重试';
    }
}

async function bindElderlyFromMePage(elderlyId, elderlyName) {
    const relationType = prompt(`请输入与 ${elderlyName || '该老人'} 的关系（如：子女、配偶等）：`);
    if (!relationType || !relationType.trim()) {
        alert('请输入关系类型');
        return;
    }
    const isPrimary = confirm('是否设为主要联系人？');
    try {
        const formData = new FormData();
        formData.append('elderlyId', elderlyId);
        formData.append('relationType', relationType.trim());
        formData.append('isPrimaryContact', isPrimary ? 1 : 0);
        const result = await post('/family/relation/bind-elderly', formData, true);
        if (result.code === 200) {
            alert('绑定成功！');
            triggerBindSearch();
        } else {
            alert('绑定失败：' + (result.message || '未知错误'));
        }
    } catch (error) {
        console.error('绑定老人失败:', error);
        alert('绑定失败，请稍后再试');
    }
}

function initQuickLinks() {
    const loginBtn = document.getElementById('goLoginBtn');
    if (loginBtn) {
        loginBtn.addEventListener('click', () => {
            window.location.href = 'login.html';
        });
    }
    const logoutBtn = document.getElementById('logoutNowBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => logout());
    }
    const resetBtn = document.getElementById('goResetBtn');
    if (resetBtn) {
        resetBtn.addEventListener('click', () => {
            window.location.href = 'reset-password.html';
        });
    }
}

function setHint(elementId, message, success) {
    const el = document.getElementById(elementId);
    if (!el) {
        return;
    }
    el.textContent = message;
    el.style.color = success ? '#059669' : '#dc2626';
}

function safeParseLocalStorage(key, defaultValue = {}) {
    try {
        const raw = localStorage.getItem(key);
        return raw ? JSON.parse(raw) : defaultValue;
    } catch (error) {
        console.warn(`无法解析本地存储 ${key}:`, error);
        return defaultValue;
    }
}

window.bindElderlyFromMePage = bindElderlyFromMePage;

