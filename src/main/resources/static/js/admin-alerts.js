// 预警管理页面脚本

let currentPage = 1;
const pageSize = 10;
let totalPages = 1;

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', () => {
    checkLogin();
    loadAlerts();
    loadStatistics();
});

// 加载预警列表
async function loadAlerts() {
    const alertType = document.getElementById('typeFilter').value;
    const alertLevel = document.getElementById('levelFilter').value;
    const status = document.getElementById('statusFilter').value;

    try {
        const token = localStorage.getItem('token');
        const params = new URLSearchParams({
            current: currentPage,
            size: pageSize
        });

        if (alertType) params.append('alertType', alertType);
        if (alertLevel) params.append('alertLevel', alertLevel);
        if (status) params.append('status', status);

        const response = await fetch(`${API_BASE_URL}/admin/alerts?${params}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();
        console.log('API响应:', result); // 调试日志
        
        if (result.code === 200 && result.data) {
            console.log('预警数据:', result.data); // 调试日志
            const pageData = result.data;
            const records = Array.isArray(pageData.records) ? pageData.records : [];
            renderAlertTable(records);
            updatePagination({
                current: pageData.current,
                pages: pageData.pages,
                total: pageData.total,
                records
            });
        } else {
            showError(result.message);
        }
    } catch (error) {
        console.error('加载预警列表失败:', error);
        showError('加载预警列表失败');
    }
}

// 渲染预警表格
function renderAlertTable(alerts) {
    const tbody = document.getElementById('alertTableBody');

    if (alerts.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center">暂无数据</td></tr>';
        return;
    }

    tbody.innerHTML = alerts.map(alert => `
        <tr>
            <td>${alert.id}</td>
            <td>${alert.elderlyName || '-'}</td>
            <td>${alert.alertType}</td>
            <td>
                <span class="level-badge ${getLevelClass(alert.alertLevel)}">
                    ${alert.alertLevel}
                </span>
            </td>
            <td>${alert.alertContent}</td>
            <td>${alert.alertValue || '-'}</td>
            <td>${formatDateTime(alert.alertTime)}</td>
            <td>
                <span class="status-badge ${getStatusClass(alert.status)}">
                    ${alert.status}
                </span>
            </td>
            <td class="action-btns">
                ${getActionButtons(alert)}
            </td>
        </tr>
    `).join('');
}

// 获取等级样式类
function getLevelClass(level) {
    const levelMap = {
        '低': 'level-low',
        '中': 'level-medium',
        '高': 'level-high',
        '紧急': 'level-urgent'
    };
    return levelMap[level] || 'level-low';
}

// 获取状态样式类
function getStatusClass(status) {
    const statusMap = {
        '待处理': 'status-warning',
        '处理中': 'status-info',
        '已处理': 'status-success',
        '已忽略': 'status-disabled'
    };
    return statusMap[status] || '';
}

// 获取操作按钮
function getActionButtons(alert) {
    if (alert.status === '待处理') {
        return `
            <button class="btn-handle" onclick="startProcessingAlert(${alert.id})">立即处理</button>
            <button class="btn-ignore" onclick="ignoreAlert(${alert.id})">忽略</button>
            <button class="btn-delete" onclick="deleteAlert(${alert.id})">删除</button>
        `;
    } else if (alert.status === '处理中') {
        return `
            <button class="btn-handle" onclick="handleAlert(${alert.id})">处理完成</button>
            <button class="btn-delete" onclick="deleteAlert(${alert.id})">删除</button>
        `;
    } else {
        return `
            <button class="btn-view" onclick="viewAlert(${alert.id})">查看</button>
            <button class="btn-delete" onclick="deleteAlert(${alert.id})">删除</button>
        `;
    }
}

// 加载统计数据
async function loadStatistics() {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE_URL}/admin/alerts/statistics`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();
        if (result.code === 200) {
            const stats = result.data;
            document.getElementById('totalAlerts').textContent = stats.total || 0;
            document.getElementById('pendingAlerts').textContent = stats.pending || 0;
            document.getElementById('processingAlerts').textContent = stats.processing || 0;
            document.getElementById('handledAlerts').textContent = stats.handled || 0;

            // 更新侧边栏铃铛徽章
            updateAlertBadge(stats.pending || 0);
        }
    } catch (error) {
        console.error('加载统计数据失败:', error);
    }
}

// 更新预警徽章数量
function updateAlertBadge(count) {
    const badge = document.getElementById('pendingAlertBadge');
    if (badge) {
        if (count > 0) {
            badge.textContent = count > 99 ? '99+' : count;
            badge.style.display = 'inline-block';
        } else {
            badge.style.display = 'none';
        }
    }
}

// 显示新增预警模态框
function showCreateAlertModal() {
    alert('新增预警功能通常由系统自动触发,如需手动测试可通过API调用');
}

// 处理预警
function handleAlert(id) {
    document.getElementById('handleAlertId').value = id;
    document.getElementById('handleResult').value = '';
    document.getElementById('handleModal').style.display = 'block';
}

// 立即处理预警（将待处理改为处理中）
async function startProcessingAlert(id) {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE_URL}/admin/alerts/${id}/process`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();
        if (result.code === 200) {
            showSuccess('告警状态已更新为处理中');
            loadAlerts();
            loadStatistics();
        } else {
            showError(result.message);
        }
    } catch (error) {
        console.error('更新告警状态失败:', error);
        showError('更新告警状态失败');
    }
}

// 提交处理结果
async function submitHandle() {
    const alertId = document.getElementById('handleAlertId').value;
    const handleResult = document.getElementById('handleResult').value;

    if (!handleResult.trim()) {
        showError('请输入处理结果');
        return;
    }

    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE_URL}/admin/alerts/handle`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                alertId: parseInt(alertId),
                handleResult: handleResult
            })
        });

        const result = await response.json();
        if (result.code === 200) {
            showSuccess('预警处理成功');
            closeHandleModal();
            loadAlerts();
            loadStatistics(); // 更新统计数据和徽章
        } else {
            showError(result.message);
        }
    } catch (error) {
        console.error('处理预警失败:', error);
        showError('处理预警失败');
    }
}

// 忽略预警
async function ignoreAlert(id) {
    if (!confirm('确定要忽略这条预警吗?')) {
        return;
    }

    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE_URL}/admin/alerts/${id}/ignore`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();
        if (result.code === 200) {
            showSuccess('预警已忽略');
            loadAlerts();
            loadStatistics(); // 更新统计数据和徽章
        } else {
            showError(result.message);
        }
    } catch (error) {
        console.error('忽略预警失败:', error);
        showError('忽略预警失败');
    }
}

// 删除预警
async function deleteAlert(id) {
    if (!confirm('确定要删除这条预警记录吗?')) {
        return;
    }

    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE_URL}/admin/alerts/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();
        if (result.code === 200) {
            showSuccess('预警删除成功');
            loadAlerts();
            loadStatistics(); // 更新统计数据和徽章
        } else {
            showError(result.message);
        }
    } catch (error) {
        console.error('删除预警失败:', error);
        showError('删除预警失败');
    }
}

// 查看预警详情
async function viewAlert(id) {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE_URL}/admin/alerts/${id}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();
        if (result.code === 200) {
            const alert = result.data;
            alert(`预警详情:\n类型: ${alert.alertType}\n等级: ${alert.alertLevel}\n内容: ${alert.alertContent}\n状态: ${alert.status}\n处理结果: ${alert.handleResult || '无'}`);
        } else {
            showError(result.message);
        }
    } catch (error) {
        console.error('获取预警详情失败:', error);
        showError('获取预警详情失败');
    }
}

// 关闭处理模态框
function closeHandleModal() {
    document.getElementById('handleModal').style.display = 'none';
}

// 更新分页信息
function updatePagination(pageData) {
    console.log('分页数据:', pageData); // 调试日志

    const recordList = Array.isArray(pageData.records) ? pageData.records : [];
    const totalFromApi = parsePositiveNumber(pageData.total);
    const pagesFromApi = parsePositiveNumber(pageData.pages);
    const currentFromApi = parsePositiveNumber(pageData.current);
    const recordCount = totalFromApi || recordList.length;

    totalPages = pagesFromApi > 0
        ? pagesFromApi
        : Math.ceil(recordCount / pageSize);

    if ((totalPages === 0 || !Number.isFinite(totalPages)) && recordList.length > 0) {
        totalPages = 1;
    }

    if (totalPages === 0) {
        currentPage = 1;
        document.getElementById('pageInfo').textContent = '暂无数据';
        document.getElementById('prevBtn').disabled = true;
        document.getElementById('nextBtn').disabled = true;
        return;
    }

    currentPage = currentFromApi > 0
        ? Math.min(currentFromApi, totalPages)
        : Math.min(currentPage, totalPages);

    const totalDisplay = Math.max(recordCount, recordList.length);
    document.getElementById('pageInfo').textContent = `第 ${currentPage} 页 / 共 ${totalPages} 页 (共 ${totalDisplay} 条)`;
    document.getElementById('prevBtn').disabled = currentPage <= 1;
    document.getElementById('nextBtn').disabled = currentPage >= totalPages;
}

// 上一页
function prevPage() {
    if (currentPage > 1) {
        currentPage--;
        loadAlerts();
    }
}

// 下一页
function nextPage() {
    if (currentPage < totalPages) {
        currentPage++;
        loadAlerts();
    }
}

function parsePositiveNumber(value) {
    const num = Number(value);
    return Number.isFinite(num) && num > 0 ? Math.floor(num) : 0;
}

// 格式化日期时间
function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '-';
    const date = new Date(dateTimeStr);
    return date.toLocaleString('zh-CN');
}

// 显示成功消息
function showSuccess(message) {
    alert(message);
}

// 显示错误消息
function showError(message) {
    alert('错误: ' + message);
}
