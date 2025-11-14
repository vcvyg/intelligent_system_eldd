// 老人信息管理

// 分页参数
let currentPage = 1;
let pageSize = 10;
let totalPages = 1;

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
    document.getElementById('welcomeText').textContent = `欢迎，${userInfo.realName || userInfo.username}！`;

    // 加载老人列表
    loadElderly();
});

// 加载老人列表
async function loadElderly() {
    const keyword = document.getElementById('searchInput').value.trim();

    try {
        let url = `/admin/elderly/list?current=${currentPage}&size=${pageSize}`;
        if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;

        const response = await get(url);
        if (response && response.data) {
            const data = response.data;
            totalPages = Math.ceil(data.total / pageSize);

            renderElderlyTable(data.records);
            updatePagination();
        }
    } catch (error) {
        console.error('加载老人列表失败:', error);
    }
}

// 渲染老人表格
function renderElderlyTable(elderlyList) {
    const tbody = document.getElementById('elderlyTableBody');

    if (!elderlyList || elderlyList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="loading">暂无数据</td></tr>';
        return;
    }

    tbody.innerHTML = elderlyList.map(elderly => `
        <tr>
            <td>${elderly.id}</td>
            <td>${elderly.name}</td>
            <td>${elderly.age || '-'}</td>
            <td>${elderly.gender || '-'}</td>
            <td>${formatDate(elderly.birthday)}</td>
            <td>${elderly.emergencyContact || '-'}</td>
            <td>${elderly.emergencyPhone || '-'}</td>
            <td class="action-btns">
                <button class="btn-view" onclick="viewElderlyDetail(${elderly.id})">查看</button>
                <button class="btn-edit" onclick="editElderly(${elderly.id})">编辑</button>
                <button class="btn-delete" onclick="deleteElderly(${elderly.id})">删除</button>
            </td>
        </tr>
    `).join('');
}

// 更新分页
function updatePagination() {
    document.getElementById('pageInfo').textContent = `第 ${currentPage} 页 / 共 ${totalPages} 页`;
    document.getElementById('prevBtn').disabled = currentPage === 1;
    document.getElementById('nextBtn').disabled = currentPage === totalPages || totalPages === 0;
}

// 搜索老人
function searchElderly() {
    currentPage = 1;
    loadElderly();
}

// 上一页
function prevPage() {
    if (currentPage > 1) {
        currentPage--;
        loadElderly();
    }
}

// 下一页
function nextPage() {
    if (currentPage < totalPages) {
        currentPage++;
        loadElderly();
    }
}

// 显示新增老人信息模态框
function showCreateElderlyModal() {
    document.getElementById('modalTitle').textContent = '新增老人信息';
    document.getElementById('elderlyForm').reset();
    document.getElementById('elderlyId').value = '';
    document.getElementById('elderlyModal').style.display = 'block';
}

// 查看老人详情
async function viewElderlyDetail(id) {
    try {
        const response = await get(`/admin/elderly/${id}`);
        if (response && response.data) {
            const elderly = response.data;

            const detailHTML = `
                <div class="detail-grid">
                    <div class="detail-item"><strong>姓名：</strong>${elderly.name}</div>
                    <div class="detail-item"><strong>年龄：</strong>${elderly.age || '-'}</div>
                    <div class="detail-item"><strong>性别：</strong>${elderly.gender || '-'}</div>
                    <div class="detail-item"><strong>出生日期：</strong>${formatDate(elderly.birthday)}</div>
                    <div class="detail-item"><strong>身份证号：</strong>${elderly.idCard || '-'}</div>
                    <div class="detail-item"><strong>居住地址：</strong>${elderly.address || '-'}</div>
                    <div class="detail-item"><strong>紧急联系人：</strong>${elderly.emergencyContact || '-'}</div>
                    <div class="detail-item"><strong>紧急电话：</strong>${elderly.emergencyPhone || '-'}</div>
                    <div class="detail-item full-width"><strong>病史：</strong>${elderly.medicalHistory || '无'}</div>
                    <div class="detail-item full-width"><strong>过敏史：</strong>${elderly.allergyHistory || '无'}</div>
                </div>
            `;

            document.getElementById('detailContent').innerHTML = detailHTML;
            document.getElementById('detailModal').style.display = 'block';
        }
    } catch (error) {
        console.error('加载老人详情失败:', error);
    }
}

// 编辑老人信息
async function editElderly(id) {
    try {
        const response = await get(`/admin/elderly/${id}`);
        if (response && response.data) {
            const elderly = response.data;

            document.getElementById('modalTitle').textContent = '编辑老人信息';
            document.getElementById('elderlyId').value = elderly.id;
            document.getElementById('userId').value = elderly.userId || '';
            document.getElementById('name').value = elderly.name || '';
            document.getElementById('age').value = elderly.age || '';
            document.getElementById('gender').value = elderly.gender || '';
            document.getElementById('birthday').value = elderly.birthday || '';
            document.getElementById('idCard').value = elderly.idCard || '';
            document.getElementById('address').value = elderly.address || '';
            document.getElementById('emergencyContact').value = elderly.emergencyContact || '';
            document.getElementById('emergencyPhone').value = elderly.emergencyPhone || '';
            document.getElementById('medicalHistory').value = elderly.medicalHistory || '';
            document.getElementById('allergyHistory').value = elderly.allergyHistory || '';

            document.getElementById('elderlyModal').style.display = 'block';
        }
    } catch (error) {
        console.error('加载老人信息失败:', error);
    }
}

// 关闭老人信息模态框
function closeElderlyModal() {
    document.getElementById('elderlyModal').style.display = 'none';
}

// 关闭详情模态框
function closeDetailModal() {
    document.getElementById('detailModal').style.display = 'none';
}

// 保存老人信息
async function saveElderly() {
    const id = document.getElementById('elderlyId').value;
    const formData = {
        userId: parseInt(document.getElementById('userId').value),
        name: document.getElementById('name').value.trim(),
        age: parseInt(document.getElementById('age').value),
        gender: document.getElementById('gender').value,
        birthday: document.getElementById('birthday').value || null,
        idCard: document.getElementById('idCard').value.trim() || null,
        address: document.getElementById('address').value.trim() || null,
        emergencyContact: document.getElementById('emergencyContact').value.trim() || null,
        emergencyPhone: document.getElementById('emergencyPhone').value.trim() || null,
        medicalHistory: document.getElementById('medicalHistory').value.trim() || null,
        allergyHistory: document.getElementById('allergyHistory').value.trim() || null
    };

    try {
        if (id) {
            // 更新老人信息
            await put(`/admin/elderly/${id}`, formData);
            alert('更新成功');
        } else {
            // 新增老人信息
            await post('/admin/elderly', formData);
            alert('新增成功');
        }

        closeElderlyModal();
        loadElderly();
    } catch (error) {
        console.error('保存老人信息失败:', error);
    }
}

// 删除老人信息
async function deleteElderly(id) {
    if (!confirm('确定要删除该老人信息吗？')) {
        return;
    }

    try {
        await del(`/admin/elderly/${id}`);
        alert('删除成功');
        loadElderly();
    } catch (error) {
        console.error('删除老人信息失败:', error);
    }
}
