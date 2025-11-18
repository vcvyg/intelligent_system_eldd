// D:\intelligent_system\src\main\resources\static\js\medical-leave.js

document.addEventListener('DOMContentLoaded', function() {
    const userInfo = checkLogin();
    if (!userInfo || userInfo.role !== 'MEDICAL') {
        alert('权限不足');
        logout();
        return;
    }
    document.getElementById('welcomeText').textContent = `欢迎, ${userInfo.username}!`;
    loadLeaveRequests(userInfo.id);
});

async function loadLeaveRequests(userId) {
    try {
        const response = await get(`/medical/leave/my`);
        const data = response.data;

        const tableBody = document.getElementById('leaveTableBody');
        tableBody.innerHTML = ''; // Clear existing data

        if (data.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="9" style="text-align: center;">暂无申请记录</td></tr>';
            return;
        }

        data.forEach(req => {
            // 处理请假类型的中文显示
            const leaveTypeMap = {
                'SICK_LEAVE': '病假',
                'PERSONAL_LEAVE': '事假',
                'ANNUAL_LEAVE': '年假',
                'ADJUSTMENT_LEAVE': '调休'
            };
            const leaveTypeText = leaveTypeMap[req.leave_type] || req.leave_type;
            
            // 处理状态的中文显示
            const statusMap = {
                'PENDING': '待审批',
                'APPROVED': '已同意',
                'REJECTED': '已拒绝'
            };
            const statusText = statusMap[req.status] || req.status;
            
            // 格式化时间
            const createTime = req.create_time ? req.create_time.replace('T', ' ').substring(0, 16) : '-';
            
            const row = `
                <tr>
                    <td>${leaveTypeText}</td>
                    <td>${req.start_date || '-'}</td>
                    <td>${req.end_date || '-'}</td>
                    <td>${req.days || 0}</td>
                    <td>${req.reason || '-'}</td>
                    <td><span class="status-${getStatusClass(req.status)}">${statusText}</span></td>
                    <td>${req.approver_remark || '-'}</td>
                    <td>${createTime}</td>
                    <td>
                        <button class="btn-danger btn-sm" onclick="cancelRequest(${req.id})" ${req.status !== 'PENDING' ? 'disabled' : ''}>撤销</button>
                    </td>
                </tr>
            `;
            tableBody.innerHTML += row;
        });
    } catch (error) {
        console.error('加载请假列表失败:', error);
        const tableBody = document.getElementById('leaveTableBody');
        tableBody.innerHTML = '<tr><td colspan="9" style="text-align: center;">加载失败</td></tr>';
    }
}

function getStatusClass(status) {
    switch (status) {
        case 'PENDING': return 'pending';
        case 'APPROVED': return 'approved';
        case 'REJECTED': return 'rejected';
        default: return '';
    }
}

function showAddModal() {
    document.getElementById('addLeaveModal').style.display = 'flex';
}

function closeModal() {
    document.getElementById('addLeaveModal').style.display = 'none';
    document.getElementById('leaveForm').reset();
    document.getElementById('daysCount').textContent = '0';
}

function calculateDays() {
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;

    if (startDate && endDate) {
        const start = new Date(startDate);
        const end = new Date(endDate);
        if (end < start) {
            alert('结束日期不能早于开始日期');
            document.getElementById('endDate').value = '';
            document.getElementById('daysCount').textContent = '0';
            return;
        }
        const diffTime = Math.abs(end - start);
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
        document.getElementById('daysCount').textContent = diffDays;
    }
}

async function submitLeaveRequest() {
    const leaveType = document.getElementById('leaveType').value;
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    const reason = document.getElementById('reason').value;
    const days = document.getElementById('daysCount').textContent;
    const userInfo = getUserInfo();

    if (!leaveType || !startDate || !endDate || !reason) {
        alert('请填写所有必填项');
        return;
    }

    const newRequest = {
        userId: userInfo.id,
        leaveType,
        startDate,
        endDate,
        days: parseInt(days),
        reason
    };

    try {
        await post('/medical/leave/submit', newRequest);
        alert('申请已提交');
        closeModal();
        loadLeaveRequests(userInfo.id);
    } catch (error) {
        console.error('提交申请失败:', error);
        alert('提交失败，请重试');
    }
}

async function cancelRequest(id) {
    if (confirm('确定要撤销此申请吗？')) {
        try {
            await put(`/medical/leave/${id}/cancel`);
            alert('申请已撤销');
            loadLeaveRequests(); // Reload the list
        } catch (error) {
            console.error('撤销申请失败:', error);
            alert('撤销失败，请重试');
        }
    }
}

// Mock logout function
function logout() {
    alert('已退出登录');
    window.location.href = 'login.html';
}
