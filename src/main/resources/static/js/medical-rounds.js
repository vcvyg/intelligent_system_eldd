document.addEventListener('DOMContentLoaded', function() {
    const userInfo = checkLogin();
    if (!userInfo || userInfo.role !== 'MEDICAL') {
        alert('权限不足');
        logout();
        return;
    }
    document.getElementById('welcomeText').textContent = `欢迎, ${userInfo.username}!`;

    const recordDateInput = document.getElementById('recordDate');
    const elderlySelect = document.getElementById('elderlySelect');
    const keywordSearch = document.getElementById('keywordSearch');
    const searchBtn = document.getElementById('searchBtn');

    recordDateInput.valueAsDate = new Date();

    // Event Listeners
    recordDateInput.addEventListener('change', loadDailyRecords);
    elderlySelect.addEventListener('change', () => {
        keywordSearch.value = ''; // 清空搜索框
        loadDailyRecords();
    });
    searchBtn.addEventListener('click', () => {
        elderlySelect.value = ''; // 清空下拉菜单
        loadDailyRecords();
    });
    keywordSearch.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            searchBtn.click();
        }
    });

    loadElderlyOptions();
    // 初始不加载任何数据，等待用户选择
    document.getElementById('recordsContainer').innerHTML = '<p style="text-align: center; padding: 20px;">请选择一位老人或通过姓名搜索以查看记录。</p>';
});

async function loadElderlyOptions() {
    try {
        const result = await get('/admin/elderly/all');
        if (result.code === 200 && result.data) {
            const select = document.getElementById('elderlySelect');
            select.innerHTML = '<option value="">选择一位老人...</option>';
            result.data.forEach(elderly => {
                select.innerHTML += `<option value="${elderly.id}">${elderly.name}</option>`;
            });
        }
    } catch (error) {
        console.error('加载老人列表失败:', error);
    }
}

async function loadDailyRecords() {
    const date = document.getElementById('recordDate').value;
    const elderlyId = document.getElementById('elderlySelect').value;
    const keyword = document.getElementById('keywordSearch').value;

    if (!date || (!elderlyId && !keyword)) {
        document.getElementById('recordsContainer').innerHTML = '<p style="text-align: center; padding: 20px;">请选择一位老人或通过姓名搜索以查看记录。</p>';
        return;
    }

    const container = document.getElementById('recordsContainer');
    container.innerHTML = '<p class="loading">正在加载老人健康记录...</p>';

    let url = `/medical/rounds/daily-summary?date=${date}`;
    if (elderlyId) {
        url += `&elderlyId=${elderlyId}`;
    } else if (keyword) {
        url += `&keyword=${keyword}`;
    }

    try {
        const result = await get(url);
        if (result.code === 200) {
            renderRecords(result.data);
        } else {
            container.innerHTML = '<p class="error">加载失败: ' + result.message + '</p>';
        }
    } catch (error) {
        container.innerHTML = '<p class="error">加载记录时发生错误。</p>';
        console.error('加载记录失败:', error);
    }
}

function renderRecords(data) {
    const container = document.getElementById('recordsContainer');
    if (!data || data.length === 0) {
        container.innerHTML = '<p style="text-align: center; padding: 20px;">该日期没有需要记录健康数据的老人。</p>';
        return;
    }

    container.innerHTML = data.map(elderlyData => createElderlyCard(elderlyData)).join('');
}

function createElderlyCard(elderlyData) {
    const timePeriods = ['MORNING', 'NOON', 'AFTERNOON', 'EVENING'];
    const periodForms = timePeriods.map(period => {
        const record = elderlyData.records[period] || {};
        return createPeriodForm(elderlyData.elderlyId, elderlyData.elderlyName, period, record);
    }).join('');

    const dailyRecord = elderlyData.records['DAILY'] || {};
    const dailyForm = createDailySummaryForm(elderlyData.elderlyId, elderlyData.elderlyName, dailyRecord);

    return `
        <div class="elderly-record-card" id="card-${elderlyData.elderlyId}">
            <div class="elderly-record-header">
                <h3>${elderlyData.elderlyName}</h3>
            </div>
            <div class="time-periods-grid">
                ${periodForms}
            </div>
            <div class="daily-summary-section">
                ${dailyForm}
            </div>
        </div>
    `;
}

function createPeriodForm(elderlyId, elderlyName, period, record) {
    const periodName = getPeriodName(period);
    const isNew = !record.id;
    const readonly = !isNew ? 'readonly' : '';

    return `
        <form class="period-form" id="form-${elderlyId}-${period}" onsubmit="return false;">
            <h4>${periodName}</h4>
            <input type="hidden" name="id" value="${record.id || ''}">
            <input type="hidden" name="elderlyId" value="${elderlyId}">
            <input type="hidden" name="timePeriod" value="${period}">
            
            <div class="form-group">
                <label for="heartRate-${elderlyId}-${period}">心率</label>
                <input type="number" id="heartRate-${elderlyId}-${period}" name="heartRate" value="${record.heartRate || ''}" ${readonly} placeholder="bpm">
            </div>
            <div class="form-group">
                <label for="bloodPressure-${elderlyId}-${period}">血压</label>
                <input type="text" id="bloodPressure-${elderlyId}-${period}" name="bloodPressure" value="${record.bloodPressureHigh && record.bloodPressureLow ? `${record.bloodPressureHigh}/${record.bloodPressureLow}` : ''}" ${readonly} placeholder="收缩压/舒张压">
            </div>
            <div class="form-group">
                <label for="temperature-${elderlyId}-${period}">体温</label>
                <input type="number" step="0.1" id="temperature-${elderlyId}-${period}" name="temperature" value="${record.temperature || ''}" ${readonly} placeholder="°C">
            </div>
            <div class="form-group">
                <label for="bloodSugar-${elderlyId}-${period}">血糖</label>
                <input type="number" step="0.1" id="bloodSugar-${elderlyId}-${period}" name="bloodSugar" value="${record.bloodSugar || ''}" ${readonly} placeholder="mmol/L">
            </div>
            <div class="form-group">
                <label for="notes-${elderlyId}-${period}">备注</label>
                <input type="text" id="notes-${elderlyId}-${period}" name="notes" value="${record.notes || ''}" ${readonly} placeholder="可选">
            </div>

            <div class="action-buttons">
                ${isNew 
                    ? `<button class="btn-primary btn-save" onclick="saveRecord('${elderlyId}', '${period}')">保存</button>`
                    : `<button class="btn-secondary btn-edit" onclick="toggleEdit(this, '${elderlyId}', '${period}')">编辑</button>`
                }
            </div>
        </form>
    `;
}

function toggleEdit(button, elderlyId, period) {
    const form = document.getElementById(`form-${elderlyId}-${period}`);
    const inputs = form.querySelectorAll('input[type="number"], input[type="text"]');
    const isReadonly = inputs[0].hasAttribute('readonly');

    if (isReadonly) {
        inputs.forEach(input => input.removeAttribute('readonly'));
        button.textContent = '保存';
        button.classList.remove('btn-secondary');
        button.classList.add('btn-primary');
        button.setAttribute('onclick', `saveRecord('${elderlyId}', '${period}')`);
    }
}

function createDailySummaryForm(elderlyId, elderlyName, record) {
    const isNew = !record.id;
    const readonly = !isNew ? 'readonly' : '';
    return `
        <form class="period-form daily-summary-form" id="form-${elderlyId}-DAILY" onsubmit="return false;">
            <h4>每日数据 (设备同步)</h4>
            <input type="hidden" name="id" value="${record.id || ''}">
            <input type="hidden" name="elderlyId" value="${elderlyId}">
            <input type="hidden" name="timePeriod" value="DAILY">

            <div class="form-group">
                <label for="steps-${elderlyId}-DAILY">今日总步数</label>
                <input type="number" id="steps-${elderlyId}-DAILY" name="steps" value="${record.steps || ''}" ${readonly} placeholder="步">
            </div>
            <div class="form-group">
                <label for="sleepDuration-${elderlyId}-DAILY">昨晚睡眠</label>
                <input type="number" id="sleepDuration-${elderlyId}-DAILY" name="sleepDuration" value="${record.sleepDuration || ''}" ${readonly} placeholder="分钟">
            </div>

            <div class="action-buttons">
                ${isNew
                    ? `<button class="btn-primary btn-save" onclick="saveDailySummary('${elderlyId}')">保存</button>`
                    : `<button class="btn-secondary btn-edit" onclick="toggleDailyEdit(this, '${elderlyId}')">编辑</button>`
                }
            </div>
        </form>
    `;
}

function toggleDailyEdit(button, elderlyId) {
    const form = document.getElementById(`form-${elderlyId}-DAILY`);
    const inputs = form.querySelectorAll('input[type="number"]');
    const isReadonly = inputs[0].hasAttribute('readonly');

    if (isReadonly) {
        inputs.forEach(input => input.removeAttribute('readonly'));
        button.textContent = '保存';
        button.classList.remove('btn-secondary');
        button.classList.add('btn-primary');
        button.setAttribute('onclick', `saveDailySummary('${elderlyId}')`);
    }
}

async function saveDailySummary(elderlyId) {
    const form = document.getElementById(`form-${elderlyId}-DAILY`);
    const date = document.getElementById('recordDate').value;

    const record = {
        id: form.querySelector('[name="id"]').value || null,
        elderlyId: elderlyId,
        measureTime: `${date}T12:00:00`, // Use noon as a standard time for daily records
        timePeriod: 'DAILY',
        steps: form.querySelector('[name="steps"]').value,
        sleepDuration: form.querySelector('[name="sleepDuration"]').value,
    };

    try {
        const result = await post('/medical/rounds/record', record);
        if (result.code === 200) {
            alert('每日数据保存成功');
            loadDailyRecords(); // Reload to update state
        } else {
            alert('保存失败: ' + result.message);
        }
    } catch (error) {
        console.error('保存每日数据失败:', error);
        alert('保存每日数据时发生错误。');
    }
}

async function saveRecord(elderlyId, period) {
    const form = document.getElementById(`form-${elderlyId}-${period}`);
    const date = document.getElementById('recordDate').value;
    const bpInput = form.querySelector('[name="bloodPressure"]').value.split('/');

    const record = {
        id: form.querySelector('[name="id"]').value || null,
        elderlyId: elderlyId,
        measureTime: getMeasureTimeForPeriod(date, period),
        timePeriod: period,
        heartRate: form.querySelector('[name="heartRate"]').value,
        bloodPressureHigh: bpInput[0] || null,
        bloodPressureLow: bpInput[1] || null,
        temperature: form.querySelector('[name="temperature"]').value,
        bloodSugar: form.querySelector('[name="bloodSugar"]').value,
        notes: form.querySelector('[name="notes"]').value,
    };

    try {
        const result = await post('/medical/rounds/record', record);
        if (result.code === 200) {
            alert('保存成功');
            loadDailyRecords(); // 重新加载以更新状态
        } else {
            alert('保存失败: ' + result.message);
        }
    } catch (error) {
        console.error('保存失败:', error);
        alert('保存记录时发生错误。');
    }
}

function getPeriodName(period) {
    switch (period) {
        case 'MORNING': return '上午 (08:00)';
        case 'NOON': return '中午 (12:00)';
        case 'AFTERNOON': return '下午 (16:00)';
        case 'EVENING': return '晚上 (20:00)';
        default: return '未知时段';
    }
}

function getMeasureTimeForPeriod(date, period) {
    const timeMap = {
        'MORNING': '08:00:00',
        'NOON': '12:00:00',
        'AFTERNOON': '16:00:00',
        'EVENING': '20:00:00'
    };
    // 构造并返回 ISO 格式的字符串，例如 "2023-11-21T08:00:00"
    return `${date}T${timeMap[period]}`;
}
