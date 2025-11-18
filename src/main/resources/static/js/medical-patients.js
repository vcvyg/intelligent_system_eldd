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

    const tableBody = document.getElementById('patientsTableBody');
    let allPatients = []; // 存储所有患者数据以用于搜索

    // 加载患者列表
    async function loadPatients() {
        try {
            tableBody.innerHTML = '<tr><td colspan="8" class="loading">加载中...</td></tr>';
            const response = await get('/medical/patients');
            if (response && response.code === 200) {
                allPatients = response.data;
                renderTable(allPatients);
            } else {
                tableBody.innerHTML = '<tr><td colspan="8" class="error">加载老人列表失败</td></tr>';
            }
        } catch (error) {
            console.error('加载老人列表失败:', error);
            tableBody.innerHTML = '<tr><td colspan="8" class="error">加载老人列表失败</td></tr>';
        }
    }

    // 渲染表格
    function renderTable(patients) {
        tableBody.innerHTML = ''; // 清空表格
        if (patients.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="8">没有找到任何老人信息</td></tr>';
            return;
        }

        patients.forEach(patient => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${patient.id}</td>
                <td>${patient.name || '-'}</td>
                <td>${patient.age || '-'}</td>
                <td>${patient.gender || '-'}</td>
                <td>${patient.roomId || '未分配'}</td>
                <td>${patient.emergencyContact || '-'}</td>
                <td>${patient.emergencyPhone || '-'}</td>
                <td>
                    <button class="btn-primary btn-sm" onclick="viewHealthRecord(${patient.id})">健康档案</button>
                </td>
            `;
            tableBody.appendChild(row);
        });
    }

    // 搜索功能
    window.searchPatients = function() {
        const keyword = document.getElementById('searchInput').value.trim().toLowerCase();
        if (!keyword) {
            renderTable(allPatients);
            return;
        }
        const filteredPatients = allPatients.filter(p => 
            p.name.toLowerCase().includes(keyword)
        );
        renderTable(filteredPatients);
    }
    
    // 查看健康档案
    window.viewHealthRecord = function(patientId) {
        window.location.href = `medical-patient-detail.html?id=${patientId}`;
    }

    // 初始加载
    loadPatients();
});
