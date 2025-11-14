// 用户管理

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

    // 加载用户列表
    loadUsers();
});

// 加载用户列表
async function loadUsers() {
    const keyword = document.getElementById('searchInput').value.trim();
    const role = document.getElementById('roleFilter').value;
    const status = document.getElementById('statusFilter').value;

    try {
        let url = `/admin/user/list?current=${currentPage}&size=${pageSize}`;
        if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;
        if (role) url += `&role=${role}`;
        if (status) url += `&status=${status}`;

        const response = await get(url);
        if (response && response.data) {
            const data = response.data;
            totalPages = Math.ceil(data.total / pageSize);

            renderUserTable(data.records);
            updatePagination();
        }
    } catch (error) {
        console.error('加载用户列表失败:', error);
    }
}

// 渲染用户表格
function renderUserTable(users) {
    const tbody = document.getElementById('userTableBody');

    if (!users || users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="loading">暂无数据</td></tr>';
        return;
    }

    tbody.innerHTML = users.map(user => `
        <tr>
            <td>${user.id}</td>
            <td>${user.username}</td>
            <td>${user.realName || '-'}</td>
            <td>${user.phone || '-'}</td>
            <td>${user.email || '-'}</td>
            <td><span class="role-badge">${getRoleName(user.role)}</span></td>
            <td>${getStatusText(user.status)}</td>
            <td class="action-btns">
                <button class="btn-edit" onclick="editUser(${user.id})">编辑</button>
                <button class="btn-delete" onclick="deleteUser(${user.id})">删除</button>
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

// 搜索用户
function searchUsers() {
    currentPage = 1;
    loadUsers();
}

// 上一页
function prevPage() {
    if (currentPage > 1) {
        currentPage--;
        loadUsers();
    }
}

// 下一页
function nextPage() {
    if (currentPage < totalPages) {
        currentPage++;
        loadUsers();
    }
}

// 显示新增用户模态框
function showCreateUserModal() {
    document.getElementById('modalTitle').textContent = '新增用户';
    document.getElementById('userForm').reset();
    document.getElementById('userId').value = '';
    document.getElementById('passwordGroup').style.display = 'block';
    document.getElementById('userModal').style.display = 'block';
}

// 编辑用户
async function editUser(id) {
    try {
        const response = await get(`/admin/user/${id}`);
        if (response && response.data) {
            const user = response.data;

            document.getElementById('modalTitle').textContent = '编辑用户';
            document.getElementById('userId').value = user.id;
            document.getElementById('username').value = user.username;
            document.getElementById('username').disabled = true;
            document.getElementById('realName').value = user.realName || '';
            document.getElementById('phone').value = user.phone || '';
            document.getElementById('email').value = user.email || '';
            document.getElementById('role').value = user.role || '';
            document.getElementById('status').value = user.status;
            document.getElementById('passwordGroup').style.display = 'none';

            document.getElementById('userModal').style.display = 'block';
        }
    } catch (error) {
        console.error('加载用户信息失败:', error);
    }
}

// 关闭用户模态框
function closeUserModal() {
    document.getElementById('userModal').style.display = 'none';
    document.getElementById('username').disabled = false;
}

// 保存用户
async function saveUser() {
    const id = document.getElementById('userId').value;
    const formData = {
        username: document.getElementById('username').value.trim(),
        realName: document.getElementById('realName').value.trim(),
        phone: document.getElementById('phone').value.trim(),
        email: document.getElementById('email').value.trim(),
        role: document.getElementById('role').value,
        status: parseInt(document.getElementById('status').value)
    };

    // 新增用户需要密码
    if (!id) {
        formData.password = document.getElementById('password').value.trim();
        if (!formData.password) {
            alert('请输入密码');
            return;
        }
    }

    try {
        if (id) {
            // 更新用户
            await put(`/admin/user/${id}`, formData);
            alert('更新成功');
        } else {
            // 新增用户
            await post('/admin/user', formData);
            alert('新增成功');
        }

        closeUserModal();
        loadUsers();
    } catch (error) {
        console.error('保存用户失败:', error);
    }
}

// 删除用户
async function deleteUser(id) {
    if (!confirm('确定要删除该用户吗？')) {
        return;
    }

    try {
        await del(`/admin/user/${id}`);
        alert('删除成功');
        loadUsers();
    } catch (error) {
        console.error('删除用户失败:', error);
    }
}
