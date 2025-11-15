// 医护排班管理JS

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', function() {
    // 检查登录状态
    const userInfo = checkLogin();
    if (!userInfo || userInfo.role !== 'ADMIN') {
        alert('权限不足');
        logout();
        return;
    }

    // 显示欢迎信息
    document.getElementById('welcomeText').textContent = `欢迎，${userInfo.username}！`;

    // 设置默认日期范围(本周)
    const today = new Date();
    const startOfWeek = new Date(today);
    startOfWeek.setDate(today.getDate() - today.getDay());
    const endOfWeek = new Date(startOfWeek);
    endOfWeek.setDate(startOfWeek.getDate() + 6);

    document.getElementById('startDate').valueAsDate = startOfWeek;
    document.getElementById('endDate').valueAsDate = endOfWeek;

    // 加载排班列表
    loadSchedules();
});

// 加载排班列表
async function loadSchedules() {
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    const tbody = document.getElementById('scheduleTableBody');
    tbody.innerHTML = '<tr><td colspan="9" class="loading">加载中...</td></tr>';

    try {
        let url = '/admin/schedule/list';
        if (startDate && endDate) {
            url = `/admin/schedule/range?startDate=${startDate}&endDate=${endDate}`;
        }

        const result = await get(url);
        if (result.code === 200 && result.data) {
            if (result.data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;color:#999;">暂无排班数据</td></tr>';
            } else {
                tbody.innerHTML = result.data.map(item => `
                    <tr>
                        <td>${item.id}</td>
                        <td>${item.username || '-'}</td>
                        <td>${item.real_name || '-'}</td>
                        <td>${formatDate(item.schedule_date)}</td>
                        <td>${item.shift_type || '-'}</td>
                        <td>${formatTime(item.start_time)}</td>
                        <td>${formatTime(item.end_time)}</td>
                        <td><span class="status-badge">${item.status || '正常'}</span></td>
                        <td class="action-btns">
                            <button class="btn-edit" onclick="editSchedule(${item.id})">编辑</button>
                            <button class="btn-danger" onclick="deleteSchedule(${item.id})">删除</button>
                        </td>
                    </tr>
                `).join('');
            }
        }
    } catch (error) {
        console.error('加载排班列表失败:', error);
        tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;color:red;">加载失败</td></tr>';
    }
}

// 显示新增排班模态框
function showAddScheduleModal() {
    document.getElementById('modalTitle').textContent = '新增排班';
    document.getElementById('scheduleForm').reset();
    document.getElementById('scheduleId').value = '';

    // 设置默认日期为今天
    document.getElementById('scheduleDate').valueAsDate = new Date();
    // 设置默认状态
    document.getElementById('status').value = '正常';

    document.getElementById('scheduleModal').style.display = 'flex';
}

// 关闭排班模态框
function closeScheduleModal() {
    document.getElementById('scheduleModal').style.display = 'none';
}

// 保存排班
async function saveSchedule() {
    const scheduleId = document.getElementById('scheduleId').value;
    const formData = {
        medicalUserId: document.getElementById('medicalUserId').value,
        scheduleDate: document.getElementById('scheduleDate').value,
        shiftType: document.getElementById('shiftType').value,
        startTime: document.getElementById('startTime').value,
        endTime: document.getElementById('endTime').value,
        status: document.getElementById('status').value,
        remark: document.getElementById('remark').value
    };

    // 验证必填项
    if (!formData.medicalUserId || !formData.scheduleDate || !formData.shiftType ||
        !formData.startTime || !formData.endTime) {
        alert('请填写所有必填项！');
        return;
    }

    try {
        if (scheduleId) {
            // 编辑
            formData.id = scheduleId;
            await put('/admin/schedule/update', formData);
            alert('更新成功！');
        } else {
            // 新增
            await post('/admin/schedule/add', formData);
            alert('新增成功！');
        }
        closeScheduleModal();
        loadSchedules();
    } catch (error) {
        console.error('保存排班失败:', error);
        alert('保存失败：' + (error.message || '未知错误'));
    }
}

// 编辑排班
async function editSchedule(id) {
    try {
        // 从表格中获取数据
        const rows = document.querySelectorAll('#scheduleTableBody tr');
        let scheduleData = null;

        for (let row of rows) {
            const firstCell = row.cells[0];
            if (firstCell && firstCell.textContent == id) {
                scheduleData = {
                    id: id,
                    medicalUserId: row.cells[1].textContent,
                    scheduleDate: row.cells[3].textContent,
                    shiftType: row.cells[4].textContent,
                    startTime: row.cells[5].textContent,
                    endTime: row.cells[6].textContent,
                    status: row.cells[7].querySelector('.status-badge').textContent
                };
                break;
            }
        }

        if (!scheduleData) {
            alert('未找到排班数据');
            return;
        }

        document.getElementById('modalTitle').textContent = '编辑排班';
        document.getElementById('scheduleId').value = scheduleData.id;
        document.getElementById('medicalUserId').value = scheduleData.medicalUserId;
        document.getElementById('scheduleDate').value = scheduleData.scheduleDate;
        document.getElementById('shiftType').value = scheduleData.shiftType;
        document.getElementById('startTime').value = scheduleData.startTime;
        document.getElementById('endTime').value = scheduleData.endTime;
        document.getElementById('status').value = scheduleData.status;

        document.getElementById('scheduleModal').style.display = 'flex';
    } catch (error) {
        console.error('编辑排班失败:', error);
        alert('加载排班数据失败');
    }
}

// 删除排班
async function deleteSchedule(id) {
    if (!confirm('确定要删除这条排班记录吗？')) return;

    try {
        await del(`/admin/schedule/${id}`);
        alert('删除成功！');
        loadSchedules();
    } catch (error) {
        console.error('删除排班失败:', error);
        alert('删除失败：' + (error.message || '未知错误'));
    }
}

// 格式化日期
function formatDate(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleDateString('zh-CN');
}

// 格式化时间
function formatTime(timeStr) {
    if (!timeStr) return '-';
    // 如果是时间字符串,直接返回
    if (typeof timeStr === 'string' && timeStr.includes(':')) {
        return timeStr.substring(0, 5); // 只显示HH:mm
    }
    return timeStr;
}

// 退出登录
function logout() {
    if (confirm('确定要退出登录吗？')) {
        localStorage.removeItem('token');
        sessionStorage.removeItem('token');
        window.location.href = 'login.html';
    }
}
