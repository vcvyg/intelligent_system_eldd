// 设备管理页面脚本

let currentPage = 1;
const pageSize = 10;
let totalPages = 1;

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    loadDevices();
    loadStatistics();
});

// 加载设备列表
async function loadDevices() {
    const keyword = document.getElementById('searchInput').value;
    const deviceType = document.getElementById('typeFilter').value;
    const status = document.getElementById('statusFilter').value;

    try {
        const token = localStorage.getItem('token');
        const params = new URLSearchParams({
            current: currentPage,
            size: pageSize
        });

        if (keyword) params.append('keyword', keyword);
        if (deviceType) params.append('deviceType', deviceType);
        if (status) params.append('status', status);

        const response = await fetch(`${API_BASE_URL}/admin/devices?${params}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();
        if (result.code === 200) {
            renderDeviceTable(result.data.records);
            updatePagination(result.data);
        } else {
            showError(result.message);
        }
    } catch (error) {
        console.error('加载设备列表失败:', error);
        showError('加载设备列表失败');
    }
}

// 渲染设备表格
function renderDeviceTable(devices) {
    const tbody = document.getElementById('deviceTableBody');

    if (devices.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center">暂无数据</td></tr>';
        return;
    }

    tbody.innerHTML = devices.map(device => `
        <tr>
            <td>${device.id}</td>
            <td>${device.deviceCode}</td>
            <td>${device.deviceName}</td>
            <td>${device.deviceType}</td>
            <td>${device.manufacturer || '-'}</td>
            <td>${device.elderlyName || '未绑定'}</td>
            <td>
                <span class="status-badge ${getStatusClass(device.status)}">
                    ${device.status}
                </span>
            </td>
            <td>${device.lastSyncTime ? formatDateTime(device.lastSyncTime) : '-'}</td>
            <td class="action-btns">
                <button class="btn-small btn-info" onclick="viewDevice(${device.id})">查看</button>
                <button class="btn-small btn-warning" onclick="editDevice(${device.id})">编辑</button>
                <button class="btn-small btn-danger" onclick="deleteDevice(${device.id})">删除</button>
            </td>
        </tr>
    `).join('');
}

// 获取状态样式类
function getStatusClass(status) {
    const statusMap = {
        '在线': 'status-active',
        '离线': 'status-warning',
        '故障': 'status-danger'
    };
    return statusMap[status] || '';
}

// 加载统计数据
async function loadStatistics() {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE_URL}/admin/devices/statistics`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();
        if (result.code === 200) {
            const stats = result.data;
            document.getElementById('totalDevices').textContent = stats.total || 0;
            document.getElementById('onlineDevices').textContent = stats.online || 0;
            document.getElementById('offlineDevices').textContent = stats.offline || 0;
            document.getElementById('faultDevices').textContent = stats.fault || 0;
        }
    } catch (error) {
        console.error('加载统计数据失败:', error);
    }
}

// 搜索设备
function searchDevices() {
    currentPage = 1;
    loadDevices();
}

// 显示新增设备模态框
function showCreateDeviceModal() {
    document.getElementById('modalTitle').textContent = '新增设备';
    document.getElementById('deviceForm').reset();
    document.getElementById('deviceId').value = '';
    document.getElementById('deviceModal').style.display = 'block';
}

// 编辑设备
async function editDevice(id) {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE_URL}/admin/devices/${id}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();
        if (result.code === 200) {
            const device = result.data;
            document.getElementById('modalTitle').textContent = '编辑设备';
            document.getElementById('deviceId').value = device.id;
            document.getElementById('deviceCode').value = device.deviceCode;
            document.getElementById('deviceCode').disabled = true; // 编辑时不能修改设备编号
            document.getElementById('deviceName').value = device.deviceName;
            document.getElementById('deviceType').value = device.deviceType;
            document.getElementById('manufacturer').value = device.manufacturer || '';
            document.getElementById('model').value = device.model || '';
            document.getElementById('status').value = device.status;
            document.getElementById('purchaseDate').value = device.purchaseDate || '';
            document.getElementById('warrantyExpireDate').value = device.warrantyExpireDate || '';
            document.getElementById('remark').value = device.remark || '';

            document.getElementById('deviceModal').style.display = 'block';
        } else {
            showError(result.message);
        }
    } catch (error) {
        console.error('获取设备信息失败:', error);
        showError('获取设备信息失败');
    }
}

// 保存设备
async function saveDevice() {
    const deviceId = document.getElementById('deviceId').value;
    const isEdit = !!deviceId;

    const deviceData = {
        deviceCode: document.getElementById('deviceCode').value,
        deviceName: document.getElementById('deviceName').value,
        deviceType: document.getElementById('deviceType').value,
        manufacturer: document.getElementById('manufacturer').value,
        model: document.getElementById('model').value,
        status: document.getElementById('status').value,
        purchaseDate: document.getElementById('purchaseDate').value || null,
        warrantyExpireDate: document.getElementById('warrantyExpireDate').value || null,
        remark: document.getElementById('remark').value
    };

    if (isEdit) {
        deviceData.id = parseInt(deviceId);
    }

    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE_URL}/admin/devices`, {
            method: isEdit ? 'PUT' : 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(deviceData)
        });

        const result = await response.json();
        if (result.code === 200) {
            showSuccess(result.message);
            closeDeviceModal();
            loadDevices();
            loadStatistics();
        } else {
            showError(result.message);
        }
    } catch (error) {
        console.error('保存设备失败:', error);
        showError('保存设备失败');
    }
}

// 删除设备
async function deleteDevice(id) {
    if (!confirm('确定要删除这个设备吗?')) {
        return;
    }

    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE_URL}/admin/devices/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();
        if (result.code === 200) {
            showSuccess('设备删除成功');
            loadDevices();
            loadStatistics();
        } else {
            showError(result.message);
        }
    } catch (error) {
        console.error('删除设备失败:', error);
        showError('删除设备失败');
    }
}

// 查看设备详情
async function viewDevice(id) {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE_URL}/admin/devices/${id}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();
        if (result.code === 200) {
            const device = result.data;
            alert(`设备详情:\n设备编号: ${device.deviceCode}\n设备名称: ${device.deviceName}\n设备类型: ${device.deviceType}\n生产厂商: ${device.manufacturer || '无'}\n状态: ${device.status}`);
        } else {
            showError(result.message);
        }
    } catch (error) {
        console.error('获取设备详情失败:', error);
        showError('获取设备详情失败');
    }
}

// 关闭模态框
function closeDeviceModal() {
    document.getElementById('deviceModal').style.display = 'none';
    document.getElementById('deviceCode').disabled = false;
}

// 更新分页信息
function updatePagination(pageData) {
    currentPage = pageData.current;
    totalPages = pageData.pages;

    document.getElementById('pageInfo').textContent = `第 ${currentPage} / ${totalPages} 页`;
    document.getElementById('prevBtn').disabled = currentPage <= 1;
    document.getElementById('nextBtn').disabled = currentPage >= totalPages;
}

// 上一页
function prevPage() {
    if (currentPage > 1) {
        currentPage--;
        loadDevices();
    }
}

// 下一页
function nextPage() {
    if (currentPage < totalPages) {
        currentPage++;
        loadDevices();
    }
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
