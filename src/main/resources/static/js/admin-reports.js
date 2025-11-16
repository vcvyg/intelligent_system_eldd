// admin-reports.js

let staffListPopulated = false;

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

    // 默认填充老人下拉框
    populateElderlyFilter();

    // 设置月份输入框默认值为上一个月
    setDefaultMonth();

    // 绑定事件
    document.getElementById('reportType').addEventListener('change', handleReportTypeChange);
    document.getElementById('generateReportBtn').addEventListener('click', generateReport);
});

/**
 * 根据报告类型切换筛选条件
 */
function handleReportTypeChange() {
    const reportType = document.getElementById('reportType').value;
    const filterForElderly = document.getElementById('filterForElderly');
    const filterForStaff = document.getElementById('filterForStaff');

    if (reportType === 'monthly_health') {
        filterForElderly.style.display = 'block';
        filterForStaff.style.display = 'none';
    } else if (reportType === 'staff_attendance') {
        filterForElderly.style.display = 'none';
        filterForStaff.style.display = 'block';
        // 首次选择时填充医护人员列表
        if (!staffListPopulated) {
            populateStaffFilter();
        }
    }
}

/**
 * 填充老人信息下拉框
 */
async function populateElderlyFilter() {
    try {
        const response = await get('/admin/elderly/all');
        if (response && response.data) {
            const select = document.getElementById('elderlyFilter');
            select.innerHTML = '<option value="">全部老人</option>'; // 重置
            response.data.forEach(elderly => {
                const option = document.createElement('option');
                option.value = elderly.id;
                option.textContent = elderly.name;
                select.appendChild(option);
            });
        }
    } catch (error) {
        console.error('加载老人列表失败:', error);
    }
}

/**
 * 填充医护人员下拉框
 */
async function populateStaffFilter() {
    try {
        const response = await get('/admin/user/by-role?role=MEDICAL');
        if (response && response.data) {
            const select = document.getElementById('staffFilter');
            select.innerHTML = '<option value="">全部医护</option>'; // 重置
            response.data.forEach(staff => {
                const option = document.createElement('option');
                option.value = staff.id;
                option.textContent = staff.username;
                select.appendChild(option);
            });
            staffListPopulated = true;
        }
    } catch (error) {
        console.error('加载医护人员列表失败:', error);
    }
}


/**
 * 设置月份输入框默认值为上一个月
 */
function setDefaultMonth() {
    const reportMonthInput = document.getElementById('reportMonth');
    const today = new Date();
    today.setMonth(today.getMonth() - 1);
    const year = today.getFullYear();
    const month = (today.getMonth() + 1).toString().padStart(2, '0');
    reportMonthInput.value = `${year}-${month}`;
}

/**
 * 生成报告 - 总入口
 */
async function generateReport() {
    const reportType = document.getElementById('reportType').value;
    const monthValue = document.getElementById('reportMonth').value;

    if (!monthValue) {
        alert('请选择要生成报告的月份！');
        return;
    }

    // 显示加载状态，并隐藏所有报告区域
    const reportPlaceholder = document.getElementById('reportPlaceholder');
    document.querySelectorAll('.report-display-area').forEach(el => el.style.display = 'none');
    reportPlaceholder.innerHTML = '<p>正在生成报告，请稍候...</p>';
    reportPlaceholder.style.display = 'block';

    try {
        let response;
        let params = new URLSearchParams({ month: monthValue });

        if (reportType === 'monthly_health') {
            const elderlyId = document.getElementById('elderlyFilter').value;
            if (elderlyId) params.append('elderlyId', elderlyId);
            response = await get(`/admin/reports/monthly_health?${params.toString()}`);
            
            if (response && response.data) {
                const elderlyName = document.getElementById('elderlyFilter').selectedOptions[0].textContent;
                renderHealthReport(response.data, monthValue, elderlyName);
            } else {
                throw new Error('未能获取到健康报告数据');
            }

        } else if (reportType === 'staff_attendance') {
            const staffId = document.getElementById('staffFilter').value;
            if (staffId) params.append('staffId', staffId);
            response = await get(`/admin/reports/staff_attendance?${params.toString()}`);

            if (response && response.data) {
                renderAttendanceReport(response.data, monthValue);
            } else {
                throw new Error('未能获取到考勤报告数据');
            }
        }

        reportPlaceholder.style.display = 'none';

    } catch (error) {
        console.error('生成报告失败:', error);
        reportPlaceholder.innerHTML = `<p style="color: red;">生成报告失败: ${error.message}</p>`;
    }
}

/**
 * 渲染健康报告
 */
function renderHealthReport(data, monthValue, elderlyName) {
    const reportContent = document.getElementById('healthReportContent');
    reportContent.style.display = 'block';

    document.getElementById('reportTitle').textContent = `${elderlyName} - ${monthValue}月度健康报告`;
    document.getElementById('reportSubtitle').textContent = `报告生成时间：${new Date().toLocaleString()}`;

    const summaryGrid = document.getElementById('summaryGrid');
    summaryGrid.innerHTML = '';
    
    const summaries = [
        { title: '月平均心率', value: data.summary?.avgHeartRate?.toFixed(1) || '-', unit: 'bpm' },
        { title: '最高收缩压', value: data.summary?.maxBloodPressureHigh || '-', unit: 'mmHg' },
        { title: '最低舒张压', value: data.summary?.minBloodPressureLow || '-', unit: 'mmHg' },
        { title: '月平均血糖', value: data.summary?.avgBloodSugar?.toFixed(1) || '-', unit: 'mmol/L' },
        { title: '月总步数', value: data.summary?.totalSteps || '-', unit: '步' }
    ];

    summaries.forEach(s => {
        const card = document.createElement('div');
        card.className = 'summary-card';
        card.innerHTML = `<h4>${s.title}</h4><p>${s.value} <span style="font-size: 14px; color: #888;">${s.unit}</span></p>`;
        summaryGrid.appendChild(card);
    });

    renderLineChart('heartRateTrendChart', '心率月度趋势 (bpm)', data.dailyTrends?.dates, [
        { name: '日均心率', type: 'line', data: data.dailyTrends?.heartRates, smooth: true }
    ]);

    renderLineChart('bloodPressureTrendChart', '血压月度趋势 (mmHg)', data.dailyTrends?.dates, [
        { name: '日均收缩压', type: 'line', data: data.dailyTrends?.bloodPressureHighs, smooth: true },
        { name: '日均舒张压', type: 'line', data: data.dailyTrends?.bloodPressureLows, smooth: true }
    ]);
}

/**
 * 渲染考勤报告
 */
function renderAttendanceReport(data, monthValue) {
    const reportContent = document.getElementById('attendanceReportContent');
    reportContent.style.display = 'block';

    document.getElementById('attendanceReportTitle').textContent = `${monthValue} 医护人员考勤表`;
    document.getElementById('attendanceReportSubtitle').textContent = `报告生成时间：${new Date().toLocaleString()}`;

    const table = document.getElementById('attendanceTable');
    table.innerHTML = ''; // 清空旧数据

    if (!data.attendance || data.attendance.length === 0) {
        table.innerHTML = '<tbody><tr><td style="text-align: center; padding: 40px;">没有找到相关考勤数据</td></tr></tbody>';
        return;
    }

    // 将概要数据转换为Map，方便查找
    const summaryMap = new Map(data.summary.map(s => [s.staffId, s]));

    // 创建表头
    const thead = table.createTHead();
    const headerRow = thead.insertRow();
    const headers = ['医护姓名', '在班', '请假', '休息', ...data.daysInMonth];
    headers.forEach(text => {
        const th = document.createElement('th');
        th.textContent = text;
        headerRow.appendChild(th);
    });

    // 创建表体
    const tbody = table.createTBody();
    data.attendance.forEach(staffData => {
        const summary = summaryMap.get(staffData.staffId);
        const row = tbody.insertRow();
        
        // 填充数据 (姓名, 概要统计, 每日状态)
        row.insertCell().textContent = staffData.staffName;
        row.insertCell().textContent = summary ? summary.onDutyDays : 'N/A';
        row.insertCell().textContent = summary ? summary.onLeaveDays : 'N/A';
        row.insertCell().textContent = summary ? summary.offDutyDays : 'N/A';

        staffData.statusByDay.forEach(status => {
            const statusCell = row.insertCell();
            statusCell.textContent = getStatusText(status);
            statusCell.className = `status-${status.toLowerCase()}`;
        });
    });
}

function getStatusText(status) {
    const map = {
        'ON_DUTY': '在班',
        'OFF_DUTY': '休息',
        'ON_LEAVE': '请假'
    };
    return map[status] || '未知';
}


/**
 * 通用折线图渲染函数
 */
function renderLineChart(elementId, title, dates, series) {
    const chartDom = document.getElementById(elementId);
    echarts.dispose(chartDom);
    const chart = echarts.init(chartDom);
    const option = {
        title: { text: title, left: 'center', textStyle: { fontSize: 16, fontWeight: 'normal' } },
        tooltip: { trigger: 'axis' },
        legend: { top: 'bottom' },
        xAxis: { type: 'category', boundaryGap: false, data: dates || [] },
        yAxis: { type: 'value' },
        series: series,
        grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true }
    };
    chart.setOption(option);
    window.addEventListener('resize', () => chart.resize());
}
