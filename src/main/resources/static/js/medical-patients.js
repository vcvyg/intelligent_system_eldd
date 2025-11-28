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
    let currentPage = 1;
    const pageSize = 10;

    // 加载患者列表
    async function loadPatients() {
        try {
            tableBody.innerHTML = '<tr><td colspan="8" class="loading">加载中...</td></tr>';
            const response = await get(`/medical/patients?page=${currentPage}&size=${pageSize}`);
            if (response && response.code === 200) {
                // mybatis-plus分页插件返回格式通常为 { records: [...], total: xx, size: xx, current: xx, pages: xx }
                if (response.data && Array.isArray(response.data.records)) {
                    allPatients = response.data.records;
                    renderTable(allPatients);
                    updatePagination(response.data);
                    // 显示分页
                    const pagination = document.querySelector('.pagination');
                    if (pagination) pagination.style.display = '';
                    return;
                }
                // 兼容无分页的情况
                if (Array.isArray(response.data)) {
                    allPatients = response.data;
                    renderTable(allPatients);
                    // 总是显示分页控件，但内容为第 1 页 / 共 1 页 (共 N 条)
                    const pagination = document.querySelector('.pagination');
                    if (pagination) {
                        pagination.style.display = '';
                        const pageInfo = document.getElementById('pageInfo');
                        if (pageInfo) pageInfo.textContent = `第 1 页 / 共 1 页 (共 ${allPatients.length} 条)`;
                        const prevBtn = document.getElementById('prevBtn');
                        const nextBtn = document.getElementById('nextBtn');
                        if (prevBtn) prevBtn.disabled = true;
                        if (nextBtn) nextBtn.disabled = true;
                    }
                    return;
                }
                console.error('Invalid response format:', response);
                tableBody.innerHTML = '<tr><td colspan="8" class="error">数据格式错误</td></tr>';
                return;
            } else {
                tableBody.innerHTML = '<tr><td colspan="8" class="error">加载老人列表失败</td></tr>';
            }
        } catch (error) {
            console.error('加载老人列表失败:', error);
            tableBody.innerHTML = '<tr><td colspan="8" class="error">加载老人列表失败</td></tr>';
        }
    }

    // 更新分页信息
    function updatePagination(pageData) {
        const pageInfo = document.getElementById('pageInfo');
        const prevBtn = document.getElementById('prevBtn');
        const nextBtn = document.getElementById('nextBtn');
        const pagination = document.querySelector('.pagination');
        if (!pageInfo || !prevBtn || !nextBtn || !pagination) return;
        // 总是显示分页控件，无论页数多少
        pagination.style.display = '';
        pageInfo.textContent = `第 ${pageData.current} 页 / 共 ${pageData.pages} 页 (共 ${pageData.total} 条)`;
        prevBtn.disabled = pageData.current === 1;
        nextBtn.disabled = pageData.current === pageData.pages;
    }

    // 上一页
    window.prevPage = function() {
        if (currentPage > 1) {
            currentPage--;
            loadPatients();
        }
    }

    // 下一页
    window.nextPage = function() {
        currentPage++;
        loadPatients();
    }

    // 渲染表格
    function renderTable(patients) {
        tableBody.innerHTML = ''; // 清空表格
        if (patients.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="8">没有找到任何老人信息</td></tr>';
            return;
        }

        if (!Array.isArray(patients)) {
            console.error('Invalid data format: patients is not an array', patients);
            tableBody.innerHTML = '<tr><td colspan="8" class="error">数据格式错误</td></tr>';
            return;
        }

        patients.sort((a, b) => a.id - b.id); // 按 ID 升序排序

        patients.forEach(patient => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${patient.id}</td>
                <td>${patient.name || '-'}</td>
                <td>${patient.age || '-'}</td>
                <td>${patient.gender || '-'}</td>
                <td>${patient.roomNumber || '-'}</td>
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
