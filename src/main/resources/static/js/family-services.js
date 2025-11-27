// 子女端服务脚本

let elderlyList = [];

const appointmentStatusMap = {
    PENDING: { text: '待确认', className: 'status-warning' },
    APPROVED: { text: '已确认', className: 'status-success' },
    REJECTED: { text: '已驳回', className: 'status-danger' },
    CANCELLED: { text: '已取消', className: 'status-secondary' }
};

const serviceStatusMap = {
    PENDING: { text: '待执行', className: 'status-warning' },
    PROCESSING: { text: '进行中', className: 'status-info' },
    COMPLETED: { text: '已完成', className: 'status-success' }
};

const paymentStatusMap = {
    PENDING: { text: '待支付', className: 'status-warning' },
    PAID: { text: '已支付', className: 'status-success' },
    CANCELLED: { text: '已取消', className: 'status-secondary' }
};

document.addEventListener('DOMContentLoaded', async () => {
    const userInfo = checkLogin();
    if (!userInfo || userInfo.role !== 'FAMILY') {
        alert('权限不足或登录已过期');
        logout();
        return;
    }
    document.getElementById('welcomeText').textContent = `欢迎，${userInfo.username}！`;

    await loadElderlyList();
    await loadAppointmentList();
    await loadPaymentList();
    await loadPaymentHistory();
});

/**
 * 加载老人列表
 */
async function loadElderlyList() {
    try {
        const result = await get('/family/relation/my-elderly');
        if (result.code === 200 && result.data) {
            elderlyList = result.data;
            const options = elderlyList.map(elderly => {
                const id = elderly.elderly_id || elderly.id;
                const name = elderly.name || '-';
                return `<option value="${id}">${name}</option>`;
            }).join('');

            const appointmentSelect = document.getElementById('appointmentElderly');
            if (appointmentSelect) {
                appointmentSelect.innerHTML = '<option value="">请选择...</option>' + options;
            }

            const progressSelect = document.getElementById('progressElderly');
            if (progressSelect) {
                progressSelect.innerHTML = '<option value="">请选择老人以查看服务进度</option>' + options;
            }

            if (elderlyList.length === 1) {
                const defaultId = elderlyList[0].elderly_id || elderlyList[0].id;
                if (appointmentSelect) {
                    appointmentSelect.value = defaultId;
                }
                if (progressSelect) {
                    progressSelect.value = defaultId;
                    await loadServiceProgress(defaultId);
                }
            } else {
                await loadServiceProgress();
            }
        }
    } catch (error) {
        console.error('加载老人列表失败:', error);
        await loadServiceProgress();
    }
}

/**
 * 切换标签页
 */
function switchTab(event, tabName) {
    // 隐藏所有标签内容
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });
    document.querySelectorAll('.tab').forEach(tab => {
        tab.classList.remove('active');
    });

    // 显示选中的标签
    document.getElementById(tabName + 'Tab').classList.add('active');
    if (event && event.target) {
        event.target.classList.add('active');
    }

    // 根据标签加载数据
    if (tabName === 'appointment') {
        loadAppointmentList();
    } else if (tabName === 'payment') {
        loadPaymentList();
        loadPaymentHistory();
    }
}

/**
 * 提交预约
 */
async function submitAppointment() {
    const elderlyId = document.getElementById('appointmentElderly').value;
    const date = document.getElementById('appointmentDate').value;
    const time = document.getElementById('appointmentTime').value;
    const purpose = document.getElementById('appointmentPurpose').value;
    const note = document.getElementById('appointmentNote').value;

    if (!elderlyId || !date || !time || !purpose) {
        alert('请填写完整信息');
        return;
    }

    try {
        await post('/family/services/appointment', {
            elderlyId: parseInt(elderlyId, 10),
            appointmentDate: date,
            appointmentTime: time,
            purpose: purpose,
            note: note
        });

        alert('预约提交成功');
        document.getElementById('appointmentForm').reset();
        await loadAppointmentList();
    } catch (error) {
        console.error('提交预约失败:', error);
        alert('提交失败，请稍后重试');
    }
}

/**
 * 加载预约列表
 */
async function loadAppointmentList() {
    const tbody = document.getElementById('appointmentListBody');
    tbody.innerHTML = '<tr><td colspan="7" class="loading">加载中...</td></tr>';

    try {
        const result = await get('/family/services/appointments');
        const list = result.data || [];

        if (list.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; padding: 40px;">暂无预约记录</td></tr>';
            return;
        }

        tbody.innerHTML = list.map(apt => {
            const statusInfo = appointmentStatusMap[apt.status] || appointmentStatusMap.PENDING;
            const canCancel = apt.status === 'PENDING' || apt.status === 'APPROVED';
            return `
                <tr>
                    <td>${apt.elderlyName || '-'}</td>
                    <td>${apt.appointmentDate || '-'}</td>
                    <td>${apt.appointmentTime || '-'}</td>
                    <td>${apt.purpose || '-'}</td>
                    <td><span class="status-badge ${statusInfo.className}">${statusInfo.text}</span></td>
                    <td>${apt.reviewRemark || '-'}</td>
                    <td>
                        <div class="action-btns">
                            ${canCancel ? `<button class="btn-secondary btn-sm" onclick="cancelAppointment(${apt.id})">取消</button>` : '-'}
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        console.error('加载预约列表失败:', error);
        tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; padding: 40px;">加载失败</td></tr>';
    }
}

/**
 * 取消预约
 */
async function cancelAppointment(id) {
    if (!confirm('确定要取消这个预约吗？')) {
        return;
    }

    try {
        await del(`/family/services/appointment/${id}`);
        alert('预约已取消');
        await loadAppointmentList();
    } catch (error) {
        console.error('取消预约失败:', error);
        alert('取消失败，请稍后重试');
    }
}

/**
 * 加载待支付列表
 */
async function loadPaymentList() {
    const container = document.getElementById('paymentList');
    container.innerHTML = '<div style="text-align: center; padding: 40px; color: var(--color-text-gray);">加载中...</div>';

    try {
        const result = await get('/family/services/payments/pending');
        const payments = result.data || [];

        if (payments.length === 0) {
            container.innerHTML = '<div style="text-align: center; padding: 40px; color: var(--color-text-gray);">暂无待支付项目</div>';
            return;
        }

        container.innerHTML = payments.map(payment => `
            <div class="payment-item">
                <div class="info">
                    <div style="font-weight: 600; margin-bottom: 5px;">${payment.itemName}</div>
                    <div style="font-size: 12px; color: var(--color-text-gray);">${payment.elderlyName || '-'}</div>
                    ${payment.dueDate ? `<div style="font-size: 12px; color: var(--color-text-gray);">到期：${payment.dueDate}</div>` : ''}
                </div>
                <div style="display: flex; align-items: center; gap: 15px;">
                    <div class="amount">¥${formatAmount(payment.amount)}</div>
                    <button class="btn-primary btn-sm" onclick="payNow(${payment.id})">立即支付</button>
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('加载待支付列表失败:', error);
        container.innerHTML = '<div style="text-align: center; padding: 40px; color: var(--color-text-gray);">加载失败</div>';
    }
}

/**
 * 立即支付
 */
async function payNow(id) {
    const payMethod = prompt('请输入支付方式（如 WeChat、AliPay、Bank）：', 'WeChat');
    if (payMethod === null) {
        return;
    }

    try {
        await post(`/family/services/payments/${id}/pay`, { payMethod });
        alert('支付成功');
        await loadPaymentList();
        await loadPaymentHistory();
    } catch (error) {
        console.error('支付失败:', error);
        alert('支付失败，请稍后再试');
    }
}

async function loadPaymentHistory() {
    const tbody = document.getElementById('paymentHistoryBody');
    tbody.innerHTML = '<tr><td colspan="6" class="loading">加载中...</td></tr>';

    try {
        const result = await get('/family/services/payments/history');
        const history = result.data || [];

        if (history.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 40px;">暂无支付记录</td></tr>';
            return;
        }

        tbody.innerHTML = history.map(record => {
            const statusInfo = paymentStatusMap[record.status] || paymentStatusMap.PAID;
            return `
                <tr>
                    <td>${formatDateTime(record.payTime)}</td>
                    <td>${record.itemName}</td>
                    <td>${record.elderlyName}</td>
                    <td>¥${formatAmount(record.amount)}</td>
                    <td>${record.payMethod || '-'}</td>
                    <td><span class="status-badge ${statusInfo.className}">${statusInfo.text}</span></td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        console.error('加载支付历史失败:', error);
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 40px;">加载失败</td></tr>';
    }
}

function formatAmount(value) {
    const num = Number(value || 0);
    return num.toFixed(2);
}

async function loadServiceProgress(forceElderlyId) {
    const select = document.getElementById('progressElderly');
    const elderlyId = forceElderlyId || (select ? select.value : '');
    const tbody = document.getElementById('serviceProgressBody');
    if (!tbody) {
        return;
    }
    if (!elderlyId) {
        setProgressPlaceholder('请选择老人');
        return;
    }
    if (select && !select.value) {
        select.value = elderlyId;
    }
    tbody.innerHTML = '<tr><td colspan="5" class="loading">加载中...</td></tr>';
    try {
        const result = await get(`/family/services/progress?elderlyId=${elderlyId}`);
        const records = result.data || [];
        if (records.length === 0) {
            setProgressPlaceholder('暂无服务记录');
            return;
        }
        tbody.innerHTML = records.map(record => {
            const statusInfo = serviceStatusMap[record.status] || serviceStatusMap.PENDING;
            return `
                <tr>
                    <td>${record.serviceDate || '-'}</td>
                    <td>${record.serviceType || '-'}</td>
                    <td>${record.medicalStaff || '-'}</td>
                    <td>${record.description || '-'}</td>
                    <td><span class="status-badge ${statusInfo.className}">${statusInfo.text}</span></td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        console.error('加载服务进度失败:', error);
        setProgressPlaceholder('加载失败');
    }
}

function setProgressPlaceholder(text) {
    const tbody = document.getElementById('serviceProgressBody');
    if (!tbody) return;
    tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:24px;color:var(--color-text-gray);">${text}</td></tr>`;
}

