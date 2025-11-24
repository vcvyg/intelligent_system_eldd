let currentStatus = '';
let appointmentCache = [];

const appointmentStatusDict = {
    PENDING: { text: '待审批', className: 'status-warning' },
    APPROVED: { text: '已同意', className: 'status-success' },
    REJECTED: { text: '已拒绝', className: 'status-danger' },
    CANCELLED: { text: '已取消', className: 'status-disabled' }
};

document.addEventListener('DOMContentLoaded', async () => {
    const userInfo = checkLogin();
    if (!userInfo || userInfo.role !== 'ADMIN') {
        alert('请以管理员身份登录');
        logout();
        return;
    }
    document.getElementById('welcomeText').textContent = `欢迎,${userInfo.username}!`;
    await loadAppointments();
});

async function loadAppointments() {
    const tbody = document.getElementById('appointmentTableBody');
    tbody.innerHTML = '<tr><td colspan="9" class="loading">加载中...</td></tr>';
    try {
        const url = currentStatus ? `/admin/family-services/appointments?status=${currentStatus}` : '/admin/family-services/appointments';
        const result = await get(url);
        appointmentCache = result.data || [];

        if (appointmentCache.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;color:var(--color-text-gray);padding:40px;">暂无记录</td></tr>';
            return;
        }

        tbody.innerHTML = appointmentCache.map(item => {
            const statusInfo = appointmentStatusDict[item.status] || appointmentStatusDict.PENDING;
            const disabled = item.status !== 'PENDING';
            return `
                <tr>
                    <td>${item.elderlyName || '-'}</td>
                    <td>${item.familyUsername || '-'}</td>
                    <td>${item.appointmentDate || '-'}</td>
                    <td>${item.appointmentTime || '-'}</td>
                    <td>${item.purpose || '-'}</td>
                    <td>${item.note || '-'}</td>
                    <td><span class="status-badge ${statusInfo.className}">${statusInfo.text}</span></td>
                    <td>${item.reviewRemark || '-'}</td>
                    <td>
                        ${disabled ? '-' : `<button class="btn-primary btn-sm" onclick="showReviewModal(${item.id})">审批</button>`}
                    </td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        console.error('加载探访预约失败:', error);
        tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;color:red;padding:40px;">加载失败</td></tr>';
    }
}

function filterAppointments(event, status) {
    currentStatus = status;
    document.querySelectorAll('.btn-group .btn-secondary').forEach(btn => btn.classList.remove('active'));
    if (event && event.target) {
        event.target.classList.add('active');
    }
    loadAppointments();
}

function showReviewModal(id) {
    const record = appointmentCache.find(item => item.id === id);
    if (!record) {
        return;
    }
    document.getElementById('currentAppointmentId').value = record.id;
    document.getElementById('appointmentRemark').value = '';
    document.getElementById('appointmentSummary').innerHTML = `
        <p><strong>老人：</strong>${record.elderlyName || '-'}</p>
        <p><strong>家属：</strong>${record.familyUsername || '-'}</p>
        <p><strong>探访时间：</strong>${record.appointmentDate || '-'} ${record.appointmentTime || ''}</p>
        <p><strong>探访目的：</strong>${record.purpose || '-'}</p>
        <p><strong>家属备注：</strong>${record.note || '-'}</p>
    `;
    document.getElementById('appointmentModal').style.display = 'flex';
}

function closeModal() {
    document.getElementById('appointmentModal').style.display = 'none';
}

async function submitReview(status) {
    const id = document.getElementById('currentAppointmentId').value;
    if (!id) {
        return;
    }
    try {
        const remark = document.getElementById('appointmentRemark').value;
        await put(`/admin/family-services/appointments/${id}/review`, {
            status,
            remark
        });
        alert('审批完成');
        closeModal();
        await loadAppointments();
    } catch (error) {
        console.error('审批失败:', error);
        alert(error.message || '审批失败，请稍后再试');
    }
}

window.filterAppointments = filterAppointments;
window.showReviewModal = showReviewModal;
window.closeModal = closeModal;
window.submitReview = submitReview;

