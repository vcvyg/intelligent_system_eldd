// 子女端服务脚本

let elderlyList = [];

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
            const select = document.getElementById('elderlySelect');
            const appointmentSelect = document.getElementById('appointmentElderly');
            
            const options = elderlyList.map(elderly => {
                const id = elderly.elderly_id || elderly.id;
                const name = elderly.name || '-';
                return `<option value="${id}">${name}</option>`;
            }).join('');

            select.innerHTML = '<option value="">请选择...</option>' + options;
            appointmentSelect.innerHTML = '<option value="">请选择...</option>' + options;
        }
    } catch (error) {
        console.error('加载老人列表失败:', error);
    }
}

/**
 * 切换标签页
 */
function switchTab(tabName) {
    // 隐藏所有标签内容
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });
    document.querySelectorAll('.tab').forEach(tab => {
        tab.classList.remove('active');
    });

    // 显示选中的标签
    document.getElementById(tabName + 'Tab').classList.add('active');
    event.target.classList.add('active');

    // 根据标签加载数据
    if (tabName === 'services') {
        loadServiceProgress();
    } else if (tabName === 'appointment') {
        loadAppointmentList();
    } else if (tabName === 'payment') {
        loadPaymentList();
        loadPaymentHistory();
    }
}

/**
 * 加载服务进度
 */
async function loadServiceProgress() {
    const elderlyId = document.getElementById('elderlySelect').value;
    const container = document.getElementById('serviceProgressList');

    if (!elderlyId) {
        container.innerHTML = '<div style="text-align: center; padding: 40px; color: var(--color-text-gray);">请选择老人查看服务进度</div>';
        return;
    }

    // TODO: 实现真实的服务进度接口
        // const result = await get(`/family/services/progress?elderlyId=${elderlyId}`);

    // 模拟数据
    const mockServices = [
        {
            id: 1,
            serviceType: '巡诊服务',
            serviceDate: '2024-01-15',
            serviceTime: '10:00',
            medicalStaff: '张医生',
            status: 'completed',
            description: '定期健康检查，各项指标正常'
        },
        {
            id: 2,
            serviceType: '护理服务',
            serviceDate: '2024-01-16',
            serviceTime: '14:00',
            medicalStaff: '李护士',
            status: 'processing',
            description: '日常护理，协助生活起居'
        },
        {
            id: 3,
            serviceType: '用药提醒',
            serviceDate: '2024-01-17',
            serviceTime: '09:00',
            medicalStaff: '系统自动',
            status: 'pending',
            description: '定时服药提醒服务'
        }
    ];

    if (mockServices.length === 0) {
        container.innerHTML = '<div style="text-align: center; padding: 40px; color: var(--color-text-gray);">暂无服务记录</div>';
        return;
    }

    container.innerHTML = mockServices.map(service => {
        const statusClass = {
            'pending': 'status-pending',
            'processing': 'status-processing',
            'completed': 'status-completed'
        }[service.status] || '';

        const statusText = {
            'pending': '待处理',
            'processing': '进行中',
            'completed': '已完成'
        }[service.status] || service.status;

        return `
            <div class="service-card">
                <div class="service-header">
                    <h3>${service.serviceType}</h3>
                    <span class="service-status ${statusClass}">${statusText}</span>
                </div>
                <div class="service-info">
                    <div class="info-item">
                        <span class="label">服务日期：</span>
                        <span class="value">${service.serviceDate}</span>
                    </div>
                    <div class="info-item">
                        <span class="label">服务时间：</span>
                        <span class="value">${service.serviceTime}</span>
                    </div>
                    <div class="info-item">
                        <span class="label">医护人员：</span>
                        <span class="value">${service.medicalStaff}</span>
                    </div>
                </div>
                <div style="margin-top: 10px; font-size: 14px; color: var(--color-text-gray);">
                    ${service.description}
                </div>
            </div>
        `;
    }).join('');
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

    // TODO: 实现真实的预约接口
    try {
        // const result = await post('/family/services/appointment', {
        //     elderlyId: elderlyId,
        //     appointmentDate: date,
        //     appointmentTime: time,
        //     purpose: purpose,
        //     note: note
        // });

        // 模拟提交成功
        alert('预约提交成功（当前为模拟数据）');
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
    // TODO: 实现真实的预约列表接口
        // const result = await get('/family/services/appointments');

    // 模拟数据
    const mockAppointments = [
        {
            id: 1,
            elderlyName: '张爷爷',
            appointmentDate: '2024-01-20',
            appointmentTime: '10:00',
            purpose: '日常探访',
            status: '待确认'
        },
        {
            id: 2,
            elderlyName: '李奶奶',
            appointmentDate: '2024-01-18',
            appointmentTime: '14:00',
            purpose: '陪同就医',
            status: '已确认'
        }
    ];

    const tbody = document.getElementById('appointmentListBody');
    
    if (mockAppointments.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 40px;">暂无预约记录</td></tr>';
        return;
    }

    tbody.innerHTML = mockAppointments.map(apt => `
        <tr>
            <td>${apt.elderlyName}</td>
            <td>${apt.appointmentDate}</td>
            <td>${apt.appointmentTime}</td>
            <td>${apt.purpose}</td>
            <td><span class="status-badge ${apt.status === '已确认' ? 'status-success' : 'status-warning'}">${apt.status}</span></td>
            <td>
                <div class="action-btns">
                    <button class="btn-secondary btn-sm" onclick="cancelAppointment(${apt.id})">取消</button>
                </div>
            </td>
        </tr>
    `).join('');
}

/**
 * 取消预约
 */
async function cancelAppointment(id) {
    if (!confirm('确定要取消这个预约吗？')) {
        return;
    }

    // TODO: 实现真实的取消接口
    try {
        // const result = await del(`/family/services/appointment/${id}`);
        alert('预约已取消（当前为模拟数据）');
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
    // TODO: 实现真实的支付列表接口
        // const result = await get('/family/services/payments/pending');

    // 模拟数据
    const mockPayments = [
        {
            id: 1,
            itemName: '1月份餐食费',
            elderlyName: '张爷爷',
            amount: 800.00
        },
        {
            id: 2,
            itemName: '护理服务费',
            elderlyName: '李奶奶',
            amount: 1200.00
        }
    ];

    const container = document.getElementById('paymentList');
    
    if (mockPayments.length === 0) {
        container.innerHTML = '<div style="text-align: center; padding: 40px; color: var(--color-text-gray);">暂无待支付项目</div>';
        return;
    }

    container.innerHTML = mockPayments.map(payment => `
        <div class="payment-item">
            <div class="info">
                <div style="font-weight: 600; margin-bottom: 5px;">${payment.itemName}</div>
                <div style="font-size: 12px; color: var(--color-text-gray);">${payment.elderlyName}</div>
            </div>
            <div style="display: flex; align-items: center; gap: 15px;">
                <div class="amount">¥${payment.amount.toFixed(2)}</div>
                <button class="btn-primary btn-sm" onclick="payNow(${payment.id})">立即支付</button>
            </div>
        </div>
    `).join('');
}

/**
 * 立即支付
 */
function payNow(id) {
    alert('支付功能待实现（支付项目ID: ' + id + '）\n可以集成支付宝、微信支付等第三方支付平台');
}

/**
 * 加载支付历史
 */
async function loadPaymentHistory() {
    // TODO: 实现真实的支付历史接口
        // const result = await get('/family/services/payments/history');

    // 模拟数据
    const mockHistory = [
        {
            payTime: '2024-01-10 15:30:25',
            itemName: '12月份餐食费',
            elderlyName: '张爷爷',
            amount: 800.00,
            payMethod: '微信支付',
            status: '已支付'
        },
        {
            payTime: '2024-01-05 10:20:15',
            itemName: '护理服务费',
            elderlyName: '李奶奶',
            amount: 1200.00,
            payMethod: '支付宝',
            status: '已支付'
        }
    ];

    const tbody = document.getElementById('paymentHistoryBody');
    
    if (mockHistory.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 40px;">暂无支付记录</td></tr>';
        return;
    }

    tbody.innerHTML = mockHistory.map(record => `
        <tr>
            <td>${record.payTime}</td>
            <td>${record.itemName}</td>
            <td>${record.elderlyName}</td>
            <td>¥${record.amount.toFixed(2)}</td>
            <td>${record.payMethod}</td>
            <td><span class="status-badge status-success">${record.status}</span></td>
        </tr>
    `).join('');
}

