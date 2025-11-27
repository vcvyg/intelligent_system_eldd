// 子女端仪表盘脚本

const TIME_RANGE_LABELS = {
    today: '今日数据',
    '7d': '近一周',
    '14d': '近两周',
    '30d': '近一月'
};

let currentRange = 'today';
let fullRangeVisible = false;

function formatGender(value) {
    if (value === null || value === undefined) {
        return '-';
    }
    const normalized = String(value).trim();
    if (!normalized) {
        return '-';
    }
    const upper = normalized.toUpperCase();
    if (upper === 'M' || normalized === '男' || upper === 'MALE' || upper === '1') {
        return '男';
    }
    if (upper === 'F' || normalized === '女' || upper === 'FEMALE' || upper === '0') {
        return '女';
    }
    return normalized;
}

function maskIdCard(idCard) {
    if (!idCard || idCard.length < 8) {
        return idCard || '-';
    }
    const middleLength = idCard.length - 8;
    return `${idCard.slice(0, 6)}${'*'.repeat(middleLength)}${idCard.slice(-2)}`;
}

document.addEventListener('DOMContentLoaded', async () => {
    const userInfo = checkLogin();
    if (!userInfo || userInfo.role !== 'FAMILY') {
        alert('权限不足或登录已过期');
        logout();
        return;
    }
    document.getElementById('welcomeText').textContent = `欢迎，${userInfo.username}！`;
    updateRangeLabel();
    highlightRangeButtons();
    applyFullRangeVisibility();

    const searchResults = document.getElementById('searchResults');
    if (searchResults) {
        searchResults.addEventListener('click', event => {
            const bindBtn = event.target.closest('[data-bind-id]');
            if (!bindBtn) {
                return;
            }
            const { bindId, bindName, bindIdcard } = bindBtn.dataset;
            if (bindId) {
                bindElderly(bindId, bindName, bindIdcard || '');
            }
        });
    }

    await loadDashboardData(currentRange);
});

/**
 * 加载仪表盘数据
 */
async function loadDashboardData(range = currentRange) {
    try {
        const result = await get(`/family/health/dashboard?range=${range}`);
        if (result.code === 200 && result.data) {
            const data = result.data;
            renderStats(data);
            renderElderlyList(data.elderlyList);
            if (data.range) {
                currentRange = data.range;
                updateRangeLabel();
                highlightRangeButtons();
            }
        } else {
            console.error('加载数据失败:', result.message);
            showError('加载数据失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        console.error('加载仪表盘数据失败:', error);
        showError('加载数据失败，请稍后重试');
    }
}

/**
 * 渲染统计卡片
 */
function renderStats(data) {
    const statsGrid = document.getElementById('statsGrid');
    const abnormalCount = data.abnormalStatus || 0;
    const abnormalBadge = abnormalCount > 0 ? `<span class="abnormal-badge">${abnormalCount}</span>` : '';
    
    statsGrid.innerHTML = `
        <div class="stat-card">
            <div class="stat-icon users">👴</div>
            <div class="stat-info">
                <h3>关联老人数量</h3>
                <p class="stat-value">${data.totalElderly || 0}</p>
            </div>
        </div>
        <div class="stat-card">
            <div class="stat-icon health">📊</div>
            <div class="stat-info">
                <h3>有健康数据</h3>
                <p class="stat-value">${data.withHealthData || 0}</p>
            </div>
        </div>
        <div class="stat-card">
            <div class="stat-icon active">✅</div>
            <div class="stat-info">
                <h3>健康状态正常</h3>
                <p class="stat-value">${data.normalStatus || 0}</p>
            </div>
        </div>
        <div class="stat-card ${abnormalCount > 0 ? 'stat-card-warning' : ''}">
            <div class="stat-icon warning">⚠️</div>
            <div class="stat-info">
                <h3>异常状态 ${abnormalBadge}</h3>
                <p class="stat-value">${abnormalCount}</p>
            </div>
        </div>
    `;
}

/**
 * 渲染老人列表
 */
function renderElderlyList(elderlyList) {
    const tbody = document.getElementById('elderlyListBody');
    
    if (!elderlyList || elderlyList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; padding: 40px;">暂无关联老人</td></tr>';
        return;
    }

    tbody.innerHTML = elderlyList.map(elderly => {
        const name = elderly.name || '-';
        const age = elderly.age || '-';
        const relationType = elderly.relation_type || '-';
        const heartRate = elderly.latestHeartRate != null ? elderly.latestHeartRate + ' bpm' : '-';
        const bloodPressure = (elderly.latestBloodPressureHigh != null && elderly.latestBloodPressureLow != null)
            ? `${elderly.latestBloodPressureHigh}/${elderly.latestBloodPressureLow} mmHg`
            : '-';
        const temperature = elderly.latestTemperature != null ? elderly.latestTemperature + ' °C' : '-';
        const healthStatus = elderly.healthStatus || '暂无数据';
        const elderlyId = elderly.elderly_id || elderly.id;

        // 健康状态样式
        let statusClass = 'status-success';
        if (healthStatus === '异常') {
            statusClass = 'status-warning';
        } else if (healthStatus === '暂无数据') {
            statusClass = 'status-disabled';
        }

        return `
            <tr>
                <td>${name}</td>
                <td>${age}</td>
                <td>${relationType}</td>
                <td>${heartRate}</td>
                <td>${bloodPressure}</td>
                <td>${temperature}</td>
                <td><span class="status-badge ${statusClass}">${healthStatus}</span></td>
                <td>
                    <div class="action-btns">
                        <button class="btn-view btn-sm" onclick="viewHealthDetail(${elderlyId}, '${name}')">查看详情</button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

/**
 * 查看健康详情
 */
function viewHealthDetail(elderlyId, elderlyName) {
    // 跳转到健康详情页面（后续创建）
    window.location.href = `family-health.html?elderlyId=${elderlyId}&name=${encodeURIComponent(elderlyName)}`;
}

/**
 * 显示错误信息
 */
function showError(message) {
    const tbody = document.getElementById('elderlyListBody');
    tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--color-danger); padding: 40px;">${message}</td></tr>`;
}

/**
 * 显示绑定老人模态框
 */
function showBindElderlyModal() {
    const modal = document.getElementById('bindElderlyModal');
    modal.style.display = 'flex';
    document.getElementById('searchElderlyInput').value = '';
    document.getElementById('searchResults').innerHTML = '<p style="text-align: center; color: var(--color-text-gray); padding: 20px;">请输入搜索关键词查找老人</p>';
}

/**
 * 关闭绑定老人模态框
 */
function closeBindElderlyModal() {
    const modal = document.getElementById('bindElderlyModal');
    modal.style.display = 'none';
}

/**
 * 搜索老人
 */
async function searchElderly() {
    const keyword = document.getElementById('searchElderlyInput').value.trim();
    const resultsDiv = document.getElementById('searchResults');
    
    if (!keyword) {
        alert('请输入搜索关键词');
        return;
    }
    
    resultsDiv.innerHTML = '<p style="text-align: center; padding: 20px;">搜索中...</p>';
    
    try {
        const result = await get(`/family/relation/search-elderly?keyword=${encodeURIComponent(keyword)}`);
        if (result.code === 200 && result.data) {
            if (result.data.length === 0) {
                resultsDiv.innerHTML = '<p style="text-align: center; color: var(--color-text-gray); padding: 20px;">未找到相关老人</p>';
            } else {
                resultsDiv.innerHTML = result.data.map(elderly => {
                    const elderlyId = elderly.id;
                    const name = elderly.name || '-';
                    const age = elderly.age != null ? elderly.age : '-';
                    const gender = formatGender(elderly.gender);
                    const idCard = elderly.id_card || '';
                    const maskedIdCard = maskIdCard(idCard) || '-';
                    
                    return `
                        <div style="border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 15px; margin-bottom: 10px; background: var(--color-bg-lighter);">
                            <div style="display: flex; justify-content: space-between; align-items: start;">
                                <div style="flex: 1;">
                                    <h4 style="margin: 0 0 8px 0; font-size: 16px;">${name}</h4>
                                    <p style="margin: 4px 0; color: var(--color-text-gray); font-size: 14px;">
                                        <span>年龄：${age}</span>
                                        <span style="margin-left: 15px;">性别：${gender}</span>
                                    </p>
                                    <p style="margin: 4px 0; color: var(--color-text-gray); font-size: 14px;">
                                        身份证：${maskedIdCard}
                                    </p>
                                </div>
                                <button class="btn-primary btn-sm" data-bind-id="${elderlyId}" data-bind-name="${name}" data-bind-idcard="${idCard}">绑定</button>
                            </div>
                        </div>
                    `;
                }).join('');
            }
        } else {
            resultsDiv.innerHTML = `<p style="text-align: center; color: var(--color-danger); padding: 20px;">搜索失败：${result.message || '未知错误'}</p>`;
        }
    } catch (error) {
        console.error('搜索老人失败:', error);
        resultsDiv.innerHTML = '<p style="text-align: center; color: var(--color-danger); padding: 20px;">搜索失败，请稍后重试</p>';
    }
}

/**
 * 绑定老人
 */
async function bindElderly(elderlyId, elderlyName, elderlyIdCard = '') {
    if (elderlyIdCard) {
        const confirmIdCard = prompt(`为确保信息安全，请输入 ${elderlyName || '该老人'} 的完整身份证号码：`);
        if (!confirmIdCard || confirmIdCard.trim() !== elderlyIdCard) {
            alert('身份证号码不匹配，无法绑定');
            return;
        }
    }

    // 弹出关系类型选择
    const relationType = prompt(`请输入与 ${elderlyName} 的关系（如：子女、配偶、其他亲属等）：`);
    if (!relationType || !relationType.trim()) {
        alert('请输入关系类型');
        return;
    }
    
    // 询问是否设为主要联系人
    const isPrimary = confirm('是否设为主要联系人？');
    const isPrimaryContact = isPrimary ? 1 : 0;
    
    try {
        // 使用FormData发送表单数据
        const formData = new FormData();
        formData.append('elderlyId', elderlyId);
        formData.append('relationType', relationType.trim());
        formData.append('isPrimaryContact', isPrimaryContact);
        
        const result = await post('/family/relation/bind-elderly', formData, true);
        
        if (result.code === 200) {
            alert('绑定成功！');
            closeBindElderlyModal();
            // 重新加载数据
            await loadDashboardData();
        } else {
            alert('绑定失败：' + (result.message || '未知错误'));
        }
    } catch (error) {
        console.error('绑定老人失败:', error);
        alert('绑定失败：' + (error.message || '请稍后重试'));
    }
}

// 点击模态框外部关闭
window.onclick = function(event) {
    const modal = document.getElementById('bindElderlyModal');
    if (event.target === modal) {
        closeBindElderlyModal();
    }
}

function setTimeRange(range) {
    if (!range || currentRange === range) {
        if (range === 'today') {
            fullRangeVisible = false;
            applyFullRangeVisibility();
        }
        return;
    }

    currentRange = range;
    fullRangeVisible = range !== 'today';
    updateRangeLabel();
    highlightRangeButtons();
    applyFullRangeVisibility();
    loadDashboardData(range);
}

function toggleFullRangeFilters() {
    fullRangeVisible = !fullRangeVisible;
    applyFullRangeVisibility();
}

function applyFullRangeVisibility() {
    const container = document.getElementById('fullRangeFilters');
    const toggleBtn = document.getElementById('viewFullRangeBtn');
    if (container) {
        container.style.display = fullRangeVisible ? 'flex' : 'none';
    }
    if (toggleBtn) {
        toggleBtn.textContent = fullRangeVisible ? '收起完整数据' : '查看完整数据';
    }
}

function updateRangeLabel() {
    const label = document.getElementById('currentRangeLabel');
    if (label) {
        label.textContent = TIME_RANGE_LABELS[currentRange] || '今日数据';
    }
}

function highlightRangeButtons() {
    const chips = document.querySelectorAll('#fullRangeFilters .btn-chip');
    chips.forEach(chip => {
        if (chip.dataset.range === currentRange) {
            chip.classList.add('active');
        } else {
            chip.classList.remove('active');
        }
    });
    const todayBtn = document.getElementById('todayRangeBtn');
    if (todayBtn) {
        if (currentRange === 'today') {
            todayBtn.classList.add('active');
        } else {
            todayBtn.classList.remove('active');
        }
    }
}

window.setTimeRange = setTimeRange;
window.toggleFullRangeFilters = toggleFullRangeFilters;
