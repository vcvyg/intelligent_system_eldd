// 管理员仪表盘

let elderlyListPopulated = false;

// 页面加载完成
window.addEventListener('DOMContentLoaded', () => {
    // 检查登录状态
    const userInfo = checkLogin();
    if (!userInfo || userInfo.role !== 'ADMIN') {
        alert('权限不足');
        logout();
        return;
    }

    // 显示欢迎信息
    document.getElementById('welcomeText').textContent = `欢迎，${userInfo.username}！`;

    // 加载统计数据
    loadStatistics();

    // 为筛选按钮绑定事件
    document.getElementById('applyFiltersBtn').addEventListener('click', loadHealthCharts);
    document.getElementById('resetFiltersBtn').addEventListener('click', resetFiltersAndLoadCharts);
});

// 加载统计数据
async function loadStatistics() {
    try {
        const response = await get('/admin/statistics');
        if (response && response.data) {
            const stats = response.data;
            document.getElementById('totalUsers').textContent = stats.totalUsers || 0;
            document.getElementById('totalElderly').textContent = stats.totalElderly || 0;
            document.getElementById('activeUsers').textContent = stats.activeUsers || 0;
            document.getElementById('todayHealthRecords').textContent = stats.todayHealthRecords || 0;
            document.getElementById('adminCount').textContent = stats.adminCount || 0;
            document.getElementById('familyCount').textContent = stats.familyCount || 0;
            document.getElementById('medicalCount').textContent = stats.medicalCount || 0;
        }
    } catch (error) {
        console.error('加载统计数据失败:', error);
    }
}

// 填充老人信息下拉框
async function populateElderlyFilter() {
    if (elderlyListPopulated) return;
    try {
        // 我们假设 /api/admin/elderly/all 接口能返回所有老人的列表
        const response = await get('/admin/elderly/all');
        if (response && response.data) {
            const select = document.getElementById('elderlyFilter');
            select.innerHTML = '<option value="">所有老人</option>'; // 默认选项
            response.data.forEach(elderly => {
                const option = document.createElement('option');
                option.value = elderly.id;
                option.textContent = elderly.name;
                select.appendChild(option);
            });
            elderlyListPopulated = true;
        }
    } catch (error) {
        console.error('加载老人列表失败:', error);
    }
}

// 显示健康数据图表模态框
function showHealthChartsModal() {
    document.getElementById('healthChartsModal').style.display = 'block';
    // 填充老人列表
    populateElderlyFilter();
    // 加载默认图表数据
    loadHealthCharts();
}

// 关闭健康数据图表模态框
function closeHealthChartsModal() {
    document.getElementById('healthChartsModal').style.display = 'none';
}

// 点击模态框外部关闭
window.onclick = function(event) {
    const modal = document.getElementById('healthChartsModal');
    if (event.target == modal) {
        closeHealthChartsModal();
    }
}

// 重置筛选并重新加载图表
function resetFiltersAndLoadCharts() {
    document.getElementById('elderlyFilter').value = '';
    document.getElementById('dateFilter').value = '';
    loadHealthCharts();
}

// 加载健康数据图表
async function loadHealthCharts() {
    const elderlyId = document.getElementById('elderlyFilter').value;
    const date = document.getElementById('dateFilter').value;

    let url = '';
    let params = new URLSearchParams();

    if (date) {
        // 如果选择了日期，我们获取单日详细数据
        url = '/admin/health/daily';
        params.append('date', date);
        if (elderlyId) {
            params.append('elderlyId', elderlyId);
        }
    } else {
        // 否则，获取趋势数据
        url = '/admin/health/trend';
        params.append('days', 7); // 默认7天
        if (elderlyId) {
            params.append('elderlyId', elderlyId);
        }
    }
    
    const fullUrl = `${url}?${params.toString()}`;

    try {
        const response = await get(fullUrl);
        if (response && response.data) {
            const data = response.data;
            const elderlyName = document.getElementById('elderlyFilter').selectedOptions[0]?.textContent || '所有老人';
            
            // 根据选择的日期更新图表标题和类型
            if (date) {
                document.querySelector('.chart-card h3').textContent = `${elderlyName} - ${date} 健康数据`;
                // 渲染单日数据图表 (可能需要不同的渲染方式)
                renderDailyCharts(data);
            } else {
                document.querySelector('.chart-card h3').textContent = `${elderlyName} - 近7天健康趋势`;
                // 渲染趋势图表
                renderTrendCharts(data);
            }
        } else {
             // 清空图表或显示无数据
            clearCharts();
            console.warn('未获取到图表数据', response);
        }
    } catch (error) {
        console.error('加载健康数据图表失败:', error);
        clearCharts();
    }
}

function renderDailyCharts(data) {
    // 对于单日数据，我们可能需要展示不同的图表，例如使用仪表盘或简单的值
    // 这里我们暂时还用趋势图的格式来展示，但X轴只有一个点
    renderTrendCharts(data); 
}

function renderTrendCharts(data) {
    renderHeartRateChart(data);
    renderBloodPressureChart(data);
    renderBloodSugarChart(data);
    renderStepsChart(data);
}

function clearCharts() {
    const chartIds = ['heartRateChart', 'bloodPressureChart', 'bloodSugarChart', 'stepsChart'];
    chartIds.forEach(id => {
        const chartDom = document.getElementById(id);
        if(chartDom) {
            // 获取 ECharts 实例并销毁
            const chartInstance = echarts.getInstanceByDom(chartDom);
            if (chartInstance) {
                chartInstance.dispose();
            }
            // 你也可以选择清空内容或显示 "无数据"
            chartDom.innerHTML = '<div style="text-align:center; color:#999; padding-top: 50px;">无可用数据</div>';
        }
    });
}


// 渲染心率趋势图
function renderHeartRateChart(data) {
    const chartDom = document.getElementById('heartRateChart');
    echarts.dispose(chartDom); // 销毁旧实例
    const chart = echarts.init(chartDom);
    const option = {
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: data.dates || [] },
        yAxis: { type: 'value', name: '心率(bpm)' },
        series: [{
            name: '心率',
            type: 'line',
            data: data.heartRates || [],
            smooth: true,
            itemStyle: { color: '#ef4444' },
            areaStyle: { opacity: 0.3 }
        }]
    };
    chart.setOption(option);
    window.addEventListener('resize', () => chart.resize());
}

// 渲染血压趋势图
function renderBloodPressureChart(data) {
    const chartDom = document.getElementById('bloodPressureChart');
    echarts.dispose(chartDom);
    const chart = echarts.init(chartDom);
    const option = {
        tooltip: { trigger: 'axis' },
        legend: { data: ['收缩压', '舒张压'], top: 'bottom' },
        xAxis: { type: 'category', data: data.dates || [] },
        yAxis: { type: 'value', name: '血压(mmHg)' },
        series: [
            {
                name: '收缩压',
                type: 'line',
                data: data.bloodPressureHigh || [],
                smooth: true,
                itemStyle: { color: '#3b82f6' }
            },
            {
                name: '舒张压',
                type: 'line',
                data: data.bloodPressureLow || [],
                smooth: true,
                itemStyle: { color: '#8b5cf6' }
            }
        ]
    };
    chart.setOption(option);
    window.addEventListener('resize', () => chart.resize());
}

// 渲染血糖趋势图
function renderBloodSugarChart(data) {
    const chartDom = document.getElementById('bloodSugarChart');
    echarts.dispose(chartDom);
    const chart = echarts.init(chartDom);
    const option = {
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: data.dates || [] },
        yAxis: { type: 'value', name: '血糖(mmol/L)' },
        series: [{
            name: '血糖',
            type: 'line',
            data: data.bloodSugars || [],
            smooth: true,
            itemStyle: { color: '#f59e0b' },
            areaStyle: { opacity: 0.3 }
        }]
    };
    chart.setOption(option);
    window.addEventListener('resize', () => chart.resize());
}

// 渲染步数统计图
function renderStepsChart(data) {
    const chartDom = document.getElementById('stepsChart');
    echarts.dispose(chartDom);
    const chart = echarts.init(chartDom);
    const option = {
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: data.dates || [] },
        yAxis: { type: 'value', name: '步数' },
        series: [{
            name: '步数',
            type: 'bar',
            data: data.steps || [],
            itemStyle: { color: '#10b981' }
        }]
    };
    chart.setOption(option);
    window.addEventListener('resize', () => chart.resize());
}

// 退出登录
function logout() {
    localStorage.removeItem('userInfo');
    window.location.href = 'login.html';
}

