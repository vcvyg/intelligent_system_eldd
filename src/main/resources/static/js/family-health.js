// 子女端健康详情脚本

let currentElderlyId = null;
let currentElderlyName = '';
let heartRateChart = null;
let bloodPressureChart = null;
let temperatureChart = null;

document.addEventListener('DOMContentLoaded', async () => {
    const userInfo = checkLogin();
    if (!userInfo || userInfo.role !== 'FAMILY') {
        alert('权限不足或登录已过期');
        logout();
        return;
    }

    // 从URL参数获取老人ID和姓名
    const urlParams = new URLSearchParams(window.location.search);
    currentElderlyId = urlParams.get('elderlyId');
    currentElderlyName = decodeURIComponent(urlParams.get('name') || '');

    if (!currentElderlyId) {
        alert('缺少老人ID参数');
        window.location.href = 'family-dashboard.html';
        return;
    }

    document.getElementById('elderlyName').textContent = currentElderlyName || '健康数据详情';

    // 初始化图表
    initCharts();
    
    // 加载数据
    await loadHealthData();
});

/**
 * 初始化图表
 */
function initCharts() {
    const heartRateCtx = document.getElementById('heartRateChart').getContext('2d');
    heartRateChart = new Chart(heartRateCtx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: '心率 (bpm)',
                data: [],
                borderColor: 'rgb(75, 192, 192)',
                backgroundColor: 'rgba(75, 192, 192, 0.2)',
                tension: 0.1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: false,
                    min: 50,
                    max: 120
                }
            }
        }
    });

    const bloodPressureCtx = document.getElementById('bloodPressureChart').getContext('2d');
    bloodPressureChart = new Chart(bloodPressureCtx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: '收缩压 (mmHg)',
                data: [],
                borderColor: 'rgb(255, 99, 132)',
                backgroundColor: 'rgba(255, 99, 132, 0.2)',
                tension: 0.1
            }, {
                label: '舒张压 (mmHg)',
                data: [],
                borderColor: 'rgb(54, 162, 235)',
                backgroundColor: 'rgba(54, 162, 235, 0.2)',
                tension: 0.1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: false,
                    min: 50,
                    max: 180
                }
            }
        }
    });

    const temperatureCtx = document.getElementById('temperatureChart').getContext('2d');
    temperatureChart = new Chart(temperatureCtx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: '体温 (°C)',
                data: [],
                borderColor: 'rgb(255, 159, 64)',
                backgroundColor: 'rgba(255, 159, 64, 0.2)',
                tension: 0.1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: false,
                    min: 35,
                    max: 39
                }
            }
        }
    });
}

/**
 * 加载健康数据
 */
async function loadHealthData() {
    const days = document.getElementById('daysSelect').value;
    
    try {
        // 加载最新数据
        const latestResult = await get(`/family/health/latest/${currentElderlyId}`);
        if (latestResult.code === 200 && latestResult.data) {
            renderLatestHealth(latestResult.data);
        }

        // 加载历史数据列表
        const listResult = await get(`/family/health/list/${currentElderlyId}?days=${days}`);
        if (listResult.code === 200 && listResult.data) {
            renderHealthDataList(listResult.data);
            updateCharts(listResult.data);
        } else {
            console.error('加载数据失败:', listResult.message);
            showError('加载数据失败: ' + (listResult.message || '未知错误'));
        }
    } catch (error) {
        console.error('加载健康数据失败:', error);
        showError('加载数据失败，请稍后重试');
    }
}

/**
 * 渲染最新健康数据卡片
 */
function renderLatestHealth(data) {
    const cardsContainer = document.getElementById('latestHealthCards');
    
    const heartRate = data.heartRate != null ? data.heartRate.toFixed(0) : '-';
    const heartRateStatus = evaluateHeartRateStatus(data.heartRate);
    
    const bloodPressureHigh = data.bloodPressureHigh != null ? data.bloodPressureHigh.toFixed(0) : '-';
    const bloodPressureLow = data.bloodPressureLow != null ? data.bloodPressureLow.toFixed(0) : '-';
    const bloodPressureStatus = evaluateBloodPressureStatus(data.bloodPressureHigh, data.bloodPressureLow);
    
    const temperature = data.temperature != null ? data.temperature.toFixed(1) : '-';
    const temperatureStatus = evaluateTemperatureStatus(data.temperature);
    
    const bloodSugar = data.bloodSugar != null ? data.bloodSugar.toFixed(1) : '-';
    const steps = data.steps != null ? data.steps : '-';
    const sleepDuration = data.sleepDuration != null ? Math.floor(data.sleepDuration / 60) + '小时' + (data.sleepDuration % 60) + '分钟' : '-';

    cardsContainer.innerHTML = `
        <div class="health-card">
            <div class="label">心率</div>
            <div class="value">${heartRate}</div>
            <div class="unit">bpm</div>
            <div class="status ${heartRateStatus.class}">${heartRateStatus.text}</div>
        </div>
        <div class="health-card">
            <div class="label">血压</div>
            <div class="value">${bloodPressureHigh}/${bloodPressureLow}</div>
            <div class="unit">mmHg</div>
            <div class="status ${bloodPressureStatus.class}">${bloodPressureStatus.text}</div>
        </div>
        <div class="health-card">
            <div class="label">体温</div>
            <div class="value">${temperature}</div>
            <div class="unit">°C</div>
            <div class="status ${temperatureStatus.class}">${temperatureStatus.text}</div>
        </div>
        <div class="health-card">
            <div class="label">血糖</div>
            <div class="value">${bloodSugar}</div>
            <div class="unit">mmol/L</div>
        </div>
        <div class="health-card">
            <div class="label">步数</div>
            <div class="value">${steps}</div>
            <div class="unit">步</div>
        </div>
        <div class="health-card">
            <div class="label">睡眠时长</div>
            <div class="value">${sleepDuration}</div>
        </div>
    `;
}

/**
 * 渲染健康数据列表
 */
function renderHealthDataList(dataList) {
    const tbody = document.getElementById('healthDataTableBody');
    
    if (!dataList || dataList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; padding: 40px;">暂无数据</td></tr>';
        return;
    }

    tbody.innerHTML = dataList.map(data => {
        const measureTime = data.measureTime ? formatDateTime(data.measureTime) : '-';
        const heartRate = data.heartRate != null ? data.heartRate.toFixed(0) : '-';
        const bloodPressureHigh = data.bloodPressureHigh != null ? data.bloodPressureHigh.toFixed(0) : '-';
        const bloodPressureLow = data.bloodPressureLow != null ? data.bloodPressureLow.toFixed(0) : '-';
        const temperature = data.temperature != null ? data.temperature.toFixed(1) : '-';
        const bloodSugar = data.bloodSugar != null ? data.bloodSugar.toFixed(1) : '-';
        const steps = data.steps != null ? data.steps : '-';
        const sleepDuration = data.sleepDuration != null ? Math.floor(data.sleepDuration / 60) + 'h' + (data.sleepDuration % 60) + 'm' : '-';

        return `
            <tr>
                <td>${measureTime}</td>
                <td>${heartRate}</td>
                <td>${bloodPressureHigh}</td>
                <td>${bloodPressureLow}</td>
                <td>${temperature}</td>
                <td>${bloodSugar}</td>
                <td>${steps}</td>
                <td>${sleepDuration}</td>
            </tr>
        `;
    }).join('');
}

/**
 * 更新图表
 */
function updateCharts(dataList) {
    if (!dataList || dataList.length === 0) {
        return;
    }

    // 按时间排序
    dataList.sort((a, b) => {
        const timeA = new Date(a.measureTime);
        const timeB = new Date(b.measureTime);
        return timeA - timeB;
    });

    const labels = dataList.map(d => {
        const date = new Date(d.measureTime);
        return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    });

    // 更新心率图表
    heartRateChart.data.labels = labels;
    heartRateChart.data.datasets[0].data = dataList.map(d => d.heartRate);
    heartRateChart.update();

    // 更新血压图表
    bloodPressureChart.data.labels = labels;
    bloodPressureChart.data.datasets[0].data = dataList.map(d => d.bloodPressureHigh);
    bloodPressureChart.data.datasets[1].data = dataList.map(d => d.bloodPressureLow);
    bloodPressureChart.update();

    // 更新体温图表
    temperatureChart.data.labels = labels;
    temperatureChart.data.datasets[0].data = dataList.map(d => d.temperature);
    temperatureChart.update();
}

/**
 * 评估心率状态
 */
function evaluateHeartRateStatus(heartRate) {
    if (heartRate == null) {
        return { class: 'status-normal', text: '暂无数据' };
    }
    if (heartRate >= 60 && heartRate <= 100) {
        return { class: 'status-normal', text: '正常' };
    }
    return { class: 'status-abnormal', text: '异常' };
}

/**
 * 评估血压状态
 */
function evaluateBloodPressureStatus(high, low) {
    if (high == null || low == null) {
        return { class: 'status-normal', text: '暂无数据' };
    }
    if (high >= 90 && high <= 140 && low >= 60 && low <= 90) {
        return { class: 'status-normal', text: '正常' };
    }
    return { class: 'status-abnormal', text: '异常' };
}

/**
 * 评估体温状态
 */
function evaluateTemperatureStatus(temperature) {
    if (temperature == null) {
        return { class: 'status-normal', text: '暂无数据' };
    }
    if (temperature >= 36.0 && temperature <= 37.5) {
        return { class: 'status-normal', text: '正常' };
    }
    return { class: 'status-abnormal', text: '异常' };
}

/**
 * 显示错误信息
 */
function showError(message) {
    const tbody = document.getElementById('healthDataTableBody');
    tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--color-danger); padding: 40px;">${message}</td></tr>`;
}

