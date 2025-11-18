document.addEventListener('DOMContentLoaded', function() {
    const userInfo = checkLogin();
    if (!userInfo || userInfo.role !== 'MEDICAL') {
        alert('请以医护人员身份登录');
        logout();
        return;
    }

    const welcomeText = document.getElementById('welcomeText');
    if (welcomeText) {
        welcomeText.textContent = `欢迎，${userInfo.username} (医护人员)`;
    }

    const profileGrid = document.getElementById('profileGrid');
    const healthRecordsTableBody = document.getElementById('healthRecordsTableBody');

    // 从URL获取患者ID
    const params = new URLSearchParams(window.location.search);
    const patientId = params.get('id');

    if (!patientId) {
        alert('未指定老人ID');
        window.location.href = 'medical-patients.html';
        return;
    }

    // 加载所有数据
    async function loadAllDetails() {
        try {
            const response = await get(`/medical/patients/${patientId}/health-details`);
            if (response && response.code === 200) {
                const { elderlyInfo, healthDataRecords } = response.data;
                renderProfile(elderlyInfo);
                renderHealthRecords(healthDataRecords);
                renderCharts(healthDataRecords);
            } else {
                profileGrid.innerHTML = '<div class="error">加载信息失败</div>';
                healthRecordsTableBody.innerHTML = '<tr><td colspan="7" class="error">加载数据失败</td></tr>';
            }
        } catch (error) {
            console.error('加载详情失败:', error);
            profileGrid.innerHTML = '<div class="error">加载信息失败</div>';
            healthRecordsTableBody.innerHTML = '<tr><td colspan="7" class="error">加载数据失败</td></tr>';
        }
    }

    // 渲染基本信息
    function renderProfile(patient) {
        profileGrid.innerHTML = `
            <div class="profile-item"><h4>姓名</h4><p>${patient.name || '-'}</p></div>
            <div class="profile-item"><h4>年龄</h4><p>${patient.age || '-'}</p></div>
            <div class="profile-item"><h4>性别</h4><p>${patient.gender || '-'}</p></div>
            <div class="profile-item"><h4>房间号</h4><p>${patient.roomNumber ? `${patient.roomNumber} (${patient.roomType || '未知类型'})` : '未分配'}</p></div>
            <div class="profile-item"><h4>紧急联系人</h4><p>${patient.emergencyContact || '-'}</p></div>
            <div class="profile-item"><h4>紧急电话</h4><p>${patient.emergencyPhone || '-'}</p></div>
        `;
        // 更新页面标题
        document.title = `${patient.name}的健康档案 - 智慧养老系统`;
    }

    // 渲染历史记录表格
    function renderHealthRecords(records) {
        healthRecordsTableBody.innerHTML = '';
        if (records.length === 0) {
            healthRecordsTableBody.innerHTML = '<tr><td colspan="7">暂无健康数据记录</td></tr>';
            return;
        }
        records.forEach(record => {
            const row = document.createElement('tr');
            const sleepHours = record.sleepDuration ? (record.sleepDuration / 60).toFixed(1) : '-';
            row.innerHTML = `
                <td>${formatDateTime(record.measureTime)}</td>
                <td>${record.heartRate || '-'}</td>
                <td>${record.bloodPressureHigh || '-'}/${record.bloodPressureLow || '-'}</td>
                <td>${record.temperature || '-'}</td>
                <td>${record.bloodSugar || '-'}</td>
                <td>${sleepHours}</td>
                <td>${record.steps || '-'}</td>
            `;
            healthRecordsTableBody.appendChild(row);
        });
    }

    // 渲染图表
    function renderCharts(records) {
        // 反转数组，让图表时间从左到右为从早到晚
        const sortedRecords = records.slice().reverse();

        const dates = sortedRecords.map(r => formatDateTime(r.measureTime));
        
        // 图表1: 心率和血压
        const heartRateChart = echarts.init(document.getElementById('heartRateBloodPressureChart'));
        const heartRateData = sortedRecords.map(r => r.heartRate);
        const bpHighData = sortedRecords.map(r => r.bloodPressureHigh);
        const bpLowData = sortedRecords.map(r => r.bloodPressureLow);

        const option1 = {
            tooltip: { trigger: 'axis' },
            legend: { data: ['心率', '收缩压', '舒张压'] },
            xAxis: { type: 'category', data: dates },
            yAxis: { type: 'value' },
            series: [
                { name: '心率', type: 'line', data: heartRateData, smooth: true },
                { name: '收缩压', type: 'line', data: bpHighData, smooth: true },
                { name: '舒张压', type: 'line', data: bpLowData, smooth: true }
            ]
        };
        heartRateChart.setOption(option1);

        // 图表2: 体温和血糖
        const tempChart = echarts.init(document.getElementById('tempBloodSugarChart'));
        const tempData = sortedRecords.map(r => r.temperature);
        const sugarData = sortedRecords.map(r => r.bloodSugar);

        const option2 = {
            tooltip: { trigger: 'axis' },
            legend: { data: ['体温', '血糖'] },
            xAxis: { type: 'category', data: dates },
            yAxis: { type: 'value' },
            series: [
                { name: '体温', type: 'line', data: tempData, smooth: true },
                { name: '血糖', type: 'line', data: sugarData, smooth: true }
            ]
        };
        tempChart.setOption(option2);
    }

    // 初始加载
    loadAllDetails();
});
