let elderlyOptions = [];

const serviceStatusMap = {
    COMPLETED: { text: '已完成', className: 'status-success' },
    PROCESSING: { text: '进行中', className: 'status-warning' },
    PENDING: { text: '待执行', className: 'status-secondary' }
};

const paymentStatusMap = {
    PENDING: { text: '待支付', className: 'status-warning' },
    PAID: { text: '已支付', className: 'status-success' },
    CANCELLED: { text: '已取消', className: 'status-secondary' }
};

document.addEventListener('DOMContentLoaded', async () => {
    const userInfo = checkLogin();
    if (!userInfo || userInfo.role !== 'MEDICAL') {
        alert('请以医护身份登录');
        logout();
        return;
    }
    document.getElementById('welcomeText').textContent = `欢迎，${userInfo.username} (医护人员)`;
    setDefaultDates();
    await Promise.all([loadSummaryStats(), loadElderlyOptions()]);
});

function setDefaultDates() {
    const today = new Date();
    const dateStr = today.toISOString().split('T')[0];
    const timeStr = today.toTimeString().substring(0, 5);
    const nextWeek = new Date(today.getTime() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];

    document.getElementById('serviceDate').value = dateStr;
    document.getElementById('serviceTime').value = timeStr;
    document.getElementById('dueDate').value = nextWeek;
}

async function loadElderlyOptions() {
    try {
        const result = await get('/medical/patients');
        elderlyOptions = result.data || [];
        fillSelectOptions('serviceElderly');
        fillSelectOptions('paymentElderly');
        fillSelectOptions('recordElderly', true);
    } catch (error) {
        console.error('加载老人列表失败:', error);
        alert('无法加载老人列表，请稍后再试');
    }
}

async function loadSummaryStats() {
    const serviceCountEl = document.getElementById('todayServiceCount');
    const pendingAmountEl = document.getElementById('pendingPaymentAmount');
    try {
        const result = await get('/medical/family-services/summary');
        const data = result.data || {};
        if (serviceCountEl) {
            serviceCountEl.textContent = data.todayServiceCount != null ? data.todayServiceCount : 0;
        }
        if (pendingAmountEl) {
            pendingAmountEl.textContent = `¥${formatAmount(data.pendingPaymentAmount)}`;
        }
    } catch (error) {
        console.error('加载统计数据失败:', error);
        if (serviceCountEl) {
            serviceCountEl.textContent = '--';
        }
        if (pendingAmountEl) {
            pendingAmountEl.textContent = '--';
        }
    }
}

function fillSelectOptions(selectId, includePlaceholder = false) {
    const select = document.getElementById(selectId);
    if (!select) return;
    const placeholder = includePlaceholder ? '<option value="">请选择老人以查看记录</option>' : '<option value="">请选择...</option>';
    select.innerHTML = placeholder + elderlyOptions.map(item => `<option value="${item.id}">${item.name || '-'}</option>`).join('');
}

async function submitServiceRecord() {
    const elderlyId = document.getElementById('serviceElderly').value;
    const serviceType = document.getElementById('serviceType').value.trim();
    const serviceDate = document.getElementById('serviceDate').value;
    const serviceTime = document.getElementById('serviceTime').value;
    const status = document.getElementById('serviceStatus').value;
    const description = document.getElementById('serviceDescription').value.trim();

    if (!elderlyId || !serviceType || !serviceDate || !serviceTime) {
        alert('请完整填写服务记录信息');
        return;
    }

    try {
        await post('/medical/family-services/service-records', {
            elderlyId: Number(elderlyId),
            serviceType,
            serviceDate,
            serviceTime,
            status,
            description
        });
        alert('服务记录提交成功');
        document.getElementById('serviceDescription').value = '';
        syncRecordElderly(elderlyId);
        await reloadRecords(elderlyId);
    } catch (error) {
        console.error('提交服务记录失败:', error);
        alert(error.message || '提交失败，请稍后再试');
    }
}

async function submitPaymentRecord() {
    const elderlyId = document.getElementById('paymentElderly').value;
    const familyUserId = document.getElementById('familyContact').value;
    const itemName = document.getElementById('itemName').value.trim();
    const amount = document.getElementById('amount').value;
    const dueDate = document.getElementById('dueDate').value;
    const remark = document.getElementById('paymentRemark').value.trim();

    if (!elderlyId || !itemName || !amount || !dueDate) {
        alert('请完整填写缴费信息');
        return;
    }

    try {
        await post('/medical/family-services/payment-records', {
            elderlyId: Number(elderlyId),
            familyUserId: familyUserId ? Number(familyUserId) : null,
            itemName,
            amount: Number(amount),
            dueDate,
            remark
        });
        alert('缴费通知创建成功');
        document.getElementById('itemName').value = '';
        document.getElementById('amount').value = '';
        document.getElementById('paymentRemark').value = '';
        syncRecordElderly(elderlyId);
        await reloadRecords(elderlyId);
    } catch (error) {
        console.error('创建缴费记录失败:', error);
        alert(error.message || '创建失败，请稍后再试');
    }
}

async function loadFamilyContacts() {
    const elderlyId = document.getElementById('paymentElderly').value;
    const contactSelect = document.getElementById('familyContact');
    contactSelect.innerHTML = '<option value="">自动选择主要联系人</option>';
    if (!elderlyId) {
        return;
    }
    try {
        const result = await get(`/medical/family-services/elderly/${elderlyId}/family-contacts`);
        const contacts = result.data || [];
        contactSelect.innerHTML = '<option value="">自动选择主要联系人</option>' +
            contacts.map(contact => `<option value="${contact.userId || ''}">
                ${contact.realName || contact.username} (${contact.relationType || '家属'})
            </option>`).join('');
    } catch (error) {
        console.error('加载家属联系人失败:', error);
        alert('无法加载家属联系人，请稍后再试');
    }
}

async function reloadRecords(forceElderlyId) {
    const select = document.getElementById('recordElderly');
    const elderlyId = forceElderlyId || select.value;
    if (!elderlyId) {
        setTablePlaceholder('serviceRecordBody', 4, '请选择老人');
        setTablePlaceholder('paymentRecordBody', 4, '请选择老人');
        if (select && forceElderlyId) {
            select.value = elderlyId;
        }
        return;
    }
    if (select && !select.value) {
        select.value = elderlyId;
    }
    await Promise.all([loadServiceRecords(elderlyId), loadPaymentRecords(elderlyId)]);
}

async function loadServiceRecords(elderlyId) {
    const tbody = document.getElementById('serviceRecordBody');
    tbody.innerHTML = '<tr><td colspan="4" class="loading">加载中...</td></tr>';
    try {
        const result = await get(`/medical/family-services/elderly/${elderlyId}/service-records`);
        const records = result.data || [];
        if (records.length === 0) {
            setTablePlaceholder('serviceRecordBody', 4, '暂无记录');
            return;
        }
        tbody.innerHTML = records.map(record => {
            const statusInfo = serviceStatusMap[record.status] || serviceStatusMap.COMPLETED;
            return `
                <tr>
                    <td>${record.serviceDate || '-'}</td>
                    <td>${record.serviceType || '-'}</td>
                    <td>${record.medicalStaff || '-'}</td>
                    <td><span class="status-badge ${statusInfo.className}">${statusInfo.text}</span></td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        console.error('加载服务记录失败:', error);
        setTablePlaceholder('serviceRecordBody', 4, '加载失败');
    }
}

async function loadPaymentRecords(elderlyId) {
    const tbody = document.getElementById('paymentRecordBody');
    tbody.innerHTML = '<tr><td colspan="4" class="loading">加载中...</td></tr>';
    try {
        const result = await get(`/medical/family-services/elderly/${elderlyId}/payment-records`);
        const records = result.data || [];
        if (records.length === 0) {
            setTablePlaceholder('paymentRecordBody', 4, '暂无记录');
            return;
        }
        tbody.innerHTML = records.map(record => {
            const statusInfo = paymentStatusMap[record.status] || paymentStatusMap.PENDING;
            return `
                <tr>
                    <td>${record.itemName || '-'}</td>
                    <td>¥${formatAmount(record.amount)}</td>
                    <td><span class="status-badge ${statusInfo.className}">${statusInfo.text}</span></td>
                    <td>${record.dueDate || '-'}</td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        console.error('加载缴费记录失败:', error);
        setTablePlaceholder('paymentRecordBody', 4, '加载失败');
    }
}

function setTablePlaceholder(tbodyId, colspan, text) {
    const tbody = document.getElementById(tbodyId);
    if (!tbody) return;
    tbody.innerHTML = `<tr><td colspan="${colspan}" style="text-align:center;color:var(--color-text-gray);padding:24px;">${text}</td></tr>`;
}

function syncRecordElderly(elderlyId) {
    const select = document.getElementById('recordElderly');
    if (select && !select.value) {
        select.value = elderlyId;
    }
}

function formatAmount(value) {
    const num = Number(value || 0);
    return num.toFixed(2);
}

window.submitServiceRecord = submitServiceRecord;
window.submitPaymentRecord = submitPaymentRecord;
window.loadFamilyContacts = loadFamilyContacts;
window.reloadRecords = () => reloadRecords();

