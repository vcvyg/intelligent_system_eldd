// 老人信息管理

// 分页参数
let currentPage = 1;
let pageSize = 10;
let totalPages = 1;

// 房间列表缓存
let roomList = [];
let roomMap = new Map(); // 房间ID到房间对象的映射

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

    // 加载房间列表
    loadRooms();
    // 加载老人列表
    loadElderly();
});

// 加载房间列表
async function loadRooms() {
    try {
        const response = await get('/admin/rooms/available');
        if (response && response.data) {
            roomList = response.data;
            // 构建房间映射
            roomMap.clear();
            roomList.forEach(room => {
                roomMap.set(room.id, room);
            });
        }
    } catch (error) {
        console.error('加载房间列表失败:', error);
    }
}

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
        tbody.innerHTML = '<tr><td colspan="9" class="loading">暂无数据</td></tr>';
        return;
    }

    tbody.innerHTML = elderlyList.map(elderly => {
        // 获取房间号
        const room = elderly.roomId ? roomMap.get(elderly.roomId) : null;
        const roomNumber = room ? room.roomNumber : '-';

        return `
        <tr>
            <td>${elderly.id}</td>
            <td>${elderly.name}</td>
            <td>${elderly.age || '-'}</td>
            <td>${elderly.gender || '-'}</td>
            <td>${roomNumber}</td>
            <td>${formatDate(elderly.birthday)}</td>
            <td>${elderly.emergencyContact || '-'}</td>
            <td>${elderly.emergencyPhone || '-'}</td>
            <td class="action-btns">
                <button class="btn-view" onclick="viewElderlyDetail(${elderly.id})">查看</button>
                <button class="btn-edit" onclick="editElderly(${elderly.id})">编辑</button>
                <button class="btn-relation" onclick="manageRelations(${elderly.id})">关联管理</button>
                <button class="btn-delete" onclick="deleteElderly(${elderly.id})">删除</button>
            </td>
        </tr>
        `;
    }).join('');
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

    // 填充房间下拉列表
    populateRoomSelect();

    document.getElementById('elderlyModal').style.display = 'block';
}

// 填充房间下拉列表
function populateRoomSelect(selectedRoomId = null) {
    const roomSelect = document.getElementById('roomId');
    roomSelect.innerHTML = '<option value="">未分配</option>';

    roomList.forEach(room => {
        const option = document.createElement('option');
        option.value = room.id;
        option.textContent = `${room.roomNumber} - ${room.roomType} (${room.occupiedBeds}/${room.bedCount})`;
        if (selectedRoomId && room.id === selectedRoomId) {
            option.selected = true;
        }
        roomSelect.appendChild(option);
    });
}

// 查看老人详情
async function viewElderlyDetail(id) {
    try {
        const response = await get(`/admin/elderly/${id}`);
        if (response && response.data) {
            const elderly = response.data;

            // 获取房间信息
            const room = elderly.roomId ? roomMap.get(elderly.roomId) : null;
            const roomInfo = room ? `${room.roomNumber} (${room.roomType})` : '未分配';

            const detailHTML = `
                <div class="detail-grid">
                    <div class="detail-item"><strong>姓名：</strong>${elderly.name}</div>
                    <div class="detail-item"><strong>年龄：</strong>${elderly.age || '-'}</div>
                    <div class="detail-item"><strong>性别：</strong>${elderly.gender || '-'}</div>
                    <div class="detail-item"><strong>出生日期：</strong>${formatDate(elderly.birthday)}</div>
                    <div class="detail-item"><strong>身份证号：</strong>${elderly.idCard || '-'}</div>
                    <div class="detail-item"><strong>房间号：</strong>${roomInfo}</div>
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

            // 填充房间下拉列表并选中当前房间
            populateRoomSelect(elderly.roomId);

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
    const roomIdValue = document.getElementById('roomId').value;

    const formData = {
        userId: parseInt(document.getElementById('userId').value),
        name: document.getElementById('name').value.trim(),
        age: parseInt(document.getElementById('age').value),
        gender: document.getElementById('gender').value,
        birthday: document.getElementById('birthday').value || null,
        idCard: document.getElementById('idCard').value.trim() || null,
        address: document.getElementById('address').value.trim() || null,
        roomId: roomIdValue ? parseInt(roomIdValue) : null,
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
        // 重新加载房间列表和老人列表
        await loadRooms();
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

// ========== 关联管理功能 ==========

let currentElderlyId = null;

// 打开关联管理弹窗
async function manageRelations(elderlyId) {
    currentElderlyId = elderlyId;
    document.getElementById('relationModal').style.display = 'flex';
    document.getElementById('relationModalTitle').textContent = `关联管理 - 老人ID: ${elderlyId}`;

    // 默认显示子女关联标签页
    switchRelationTab('family');
}

// 关闭关联管理弹窗
function closeRelationModal() {
    document.getElementById('relationModal').style.display = 'none';
    currentElderlyId = null;
}

// 切换关联管理标签页
function switchRelationTab(tabName) {
    // 移除所有标签页的active类
    document.querySelectorAll('.relation-tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.relation-tab-content').forEach(content => content.classList.remove('active'));

    // 添加当前标签页的active类
    event.target.classList.add('active');
    document.getElementById(`${tabName}-tab`).classList.add('active');

    // 加载对应数据
    if (tabName === 'family') {
        loadFamilyRelations();
    } else if (tabName === 'medical') {
        loadMedicalRelations();
    } else if (tabName === 'device') {
        loadDeviceRelations();
    }
}

// 加载子女关联列表
async function loadFamilyRelations() {
    const tbody = document.getElementById('familyRelationTableBody');
    tbody.innerHTML = '<tr><td colspan="7" class="loading">加载中...</td></tr>';

    try {
        const result = await get(`/admin/elderly/${currentElderlyId}/family`);
        if (result.code === 200 && result.data) {
            if (result.data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:#999;">暂无子女关联</td></tr>';
            } else {
                tbody.innerHTML = result.data.map(item => `
                    <tr>
                        <td>${item.username || '-'}</td>
                        <td>${item.real_name || '-'}</td>
                        <td>${item.phone || '-'}</td>
                        <td>${item.email || '-'}</td>
                        <td>${item.relation_type || '-'}</td>
                        <td>${item.is_primary_contact == 1 ? '是' : '否'}</td>
                        <td class="action-btns">
                            <button class="btn-delete" onclick="removeFamilyRelation(${item.id})">删除</button>
                        </td>
                    </tr>
                `).join('');
            }
        }
    } catch (error) {
        console.error('加载子女关联失败:', error);
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:red;">加载失败</td></tr>';
    }
}

// 加载医护分配列表
async function loadMedicalRelations() {
    const tbody = document.getElementById('medicalRelationTableBody');
    tbody.innerHTML = '<tr><td colspan="6" class="loading">加载中...</td></tr>';

    try {
        const result = await get(`/admin/elderly/${currentElderlyId}/medical`);
        if (result.code === 200 && result.data) {
            if (result.data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:#999;">暂无医护分配</td></tr>';
            } else {
                tbody.innerHTML = result.data.map(item => `
                    <tr>
                        <td>${item.username || '-'}</td>
                        <td>${item.real_name || '-'}</td>
                        <td>${item.phone || '-'}</td>
                        <td>${item.is_primary_doctor == 1 ? '是' : '否'}</td>
                        <td>${formatDateTime(item.assign_date) || '-'}</td>
                        <td class="action-btns">
                            <button class="btn-delete" onclick="removeMedicalRelation(${item.id})">删除</button>
                        </td>
                    </tr>
                `).join('');
            }
        }
    } catch (error) {
        console.error('加载医护分配失败:', error);
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:red;">加载失败</td></tr>';
    }
}

// 加载设备绑定列表
async function loadDeviceRelations() {
    const tbody = document.getElementById('deviceRelationTableBody');
    tbody.innerHTML = '<tr><td colspan="6" class="loading">加载中...</td></tr>';

    try {
        const result = await get(`/admin/elderly/${currentElderlyId}/devices`);
        if (result.code === 200 && result.data) {
            if (result.data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:#999;">暂无设备绑定</td></tr>';
            } else {
                tbody.innerHTML = result.data.map(item => `
                    <tr>
                        <td>${item.device_code || '-'}</td>
                        <td>${item.device_name || '-'}</td>
                        <td>${item.device_type || '-'}</td>
                        <td><span class="status-badge">${item.status || '-'}</span></td>
                        <td>${formatDateTime(item.last_sync_time) || '-'}</td>
                        <td class="action-btns">
                            <button class="btn-view" onclick="viewDevice(${item.id})">查看</button>
                            <button class="btn-danger" onclick="unbindDevice(${item.id})">解绑</button>
                        </td>
                    </tr>
                `).join('');
            }
        }
    } catch (error) {
        console.error('加载设备绑定失败:', error);
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:red;">加载失败</td></tr>';
    }
}

// 显示添加子女表单
async function showAddFamilyForm() {
    // 加载子女用户列表
    try {
        const response = await get('/admin/users/by-role?role=FAMILY');
        if (response && response.data) {
            const familyUsers = response.data;
            const select = document.getElementById('familyUserSelect');
            select.innerHTML = '<option value="">请选择子女用户</option>';

            familyUsers.forEach(user => {
                const option = document.createElement('option');
                option.value = user.id;
                option.textContent = `${user.realName || user.username} (${user.phone || '无手机号'})`;
                select.appendChild(option);
            });

            // 重置表单
            document.getElementById('addFamilyForm').reset();
            document.getElementById('addFamilyModal').style.display = 'block';
        }
    } catch (error) {
        console.error('加载子女用户列表失败:', error);
        alert('加载子女用户列表失败: ' + (error.message || '未知错误'));
    }
}

// 关闭添加子女模态框
function closeAddFamilyModal() {
    document.getElementById('addFamilyModal').style.display = 'none';
}

// 确认添加子女
async function confirmAddFamily() {
    const familyUserId = document.getElementById('familyUserSelect').value;
    const relationType = document.getElementById('relationType').value;
    const isPrimary = document.getElementById('isPrimaryContact').checked;

    if (!familyUserId) {
        alert('请选择子女用户');
        return;
    }

    if (!relationType) {
        alert('请选择关系类型');
        return;
    }

    try {
        await post(`/admin/elderly/${currentElderlyId}/family?familyUserId=${familyUserId}&relationType=${encodeURIComponent(relationType)}&isPrimaryContact=${isPrimary ? 1 : 0}`);
        alert('添加成功');
        closeAddFamilyModal();
        loadFamilyRelations();
    } catch (error) {
        console.error('添加子女失败:', error);
        alert('添加失败:' + (error.message || '未知错误'));
    }
}

// 显示分配医护表单
async function showAddMedicalForm() {
    // 加载医护人员列表
    try {
        const response = await get('/admin/users/by-role?role=MEDICAL');
        if (response && response.data) {
            const medicalUsers = response.data;
            const select = document.getElementById('medicalUserSelect');
            select.innerHTML = '<option value="">请选择医护人员</option>';

            medicalUsers.forEach(user => {
                const option = document.createElement('option');
                option.value = user.id;
                option.textContent = `${user.realName || user.username} (${user.phone || '无手机号'})`;
                select.appendChild(option);
            });

            // 重置表单
            document.getElementById('addMedicalForm').reset();
            document.getElementById('addMedicalModal').style.display = 'block';
        }
    } catch (error) {
        console.error('加载医护人员列表失败:', error);
        alert('加载医护人员列表失败: ' + (error.message || '未知错误'));
    }
}

// 关闭添加医护模态框
function closeAddMedicalModal() {
    document.getElementById('addMedicalModal').style.display = 'none';
}

// 确认分配医护
async function confirmAddMedical() {
    const medicalUserId = document.getElementById('medicalUserSelect').value;
    const isPrimary = document.getElementById('isPrimaryDoctor').checked;

    if (!medicalUserId) {
        alert('请选择医护人员');
        return;
    }

    try {
        await post(`/admin/elderly/${currentElderlyId}/medical?medicalUserId=${medicalUserId}&isPrimaryDoctor=${isPrimary ? 1 : 0}`);
        alert('分配成功');
        closeAddMedicalModal();
        loadMedicalRelations();
    } catch (error) {
        console.error('分配医护失败:', error);
        alert('分配失败:' + (error.message || '未知错误'));
    }
}

// 显示绑定设备表单
async function showBindDeviceForm() {
    const deviceId = prompt('请输入要绑定的设备ID\n(提示:可在设备管理页面查看未绑定的设备列表)');
    if (!deviceId) return;

    try {
        await post(`/admin/elderly/${currentElderlyId}/devices/bind?deviceId=${deviceId}`);
        alert('绑定成功');
        loadDeviceRelations();
    } catch (error) {
        console.error('绑定设备失败:', error);
        alert('绑定失败:' + (error.message || '未知错误'));
    }
}

// 解绑设备
async function unbindDevice(deviceId) {
    if (!confirm('确定要解绑该设备吗?')) return;

    try {
        await del(`/admin/elderly/${currentElderlyId}/devices/${deviceId}`);
        alert('解绑成功');
        loadDeviceRelations();
    } catch (error) {
        console.error('解绑设备失败:', error);
        alert('解绑失败:' + (error.message || '未知错误'));
    }
}

// 删除子女关联
async function removeFamilyRelation(relationId) {
    if (!confirm('确定要删除该子女关联吗?')) return;

    try {
        await del(`/admin/elderly/${currentElderlyId}/family/${relationId}`);
        alert('删除成功');
        loadFamilyRelations();
    } catch (error) {
        console.error('删除子女关联失败:', error);
        alert('删除失败:' + (error.message || '未知错误'));
    }
}

// 删除医护分配
async function removeMedicalRelation(relationId) {
    if (!confirm('确定要删除该医护分配吗?')) return;

    try {
        await del(`/admin/elderly/${currentElderlyId}/medical/${relationId}`);
        alert('删除成功');
        loadMedicalRelations();
    } catch (error) {
        console.error('删除医护分配失败:', error);
        alert('删除失败:' + (error.message || '未知错误'));
    }
}

// 查看设备详情(跳转到设备管理页面)
function viewDevice(deviceId) {
    window.location.href = `admin-devices.html?deviceId=${deviceId}`;
}
