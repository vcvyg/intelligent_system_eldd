// 医护排班管理 - 周视图
// 全局变量
let currentWeekStart = null; // 当前周的开始日期
let scheduleData = {}; // 排班数据: {date: {timeSlot: [schedules]}}
let medicalUsers = []; // 医护人员列表
let userColorMap = {}; // 用户ID到颜色的映射

// 时间段定义(每2小时一个时间段，覆盖全天)
const TIME_SLOTS = [
    '07:00', '09:00', '11:00', '13:00', '15:00', '17:00', '19:00', '21:00', '23:00'
];

// 星期映射
const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', async function() {
    // 检查登录状态
    const userInfo = checkLogin();
    if (!userInfo || userInfo.role !== 'ADMIN') {
        alert('权限不足');
        logout();
        return;
    }

    // 显示欢迎信息
    document.getElementById('welcomeText').textContent = `欢迎,${userInfo.username}!`;

    // 先加载医护人员列表(等待完成)
    await loadMedicalUsers();

    // 初始化当前周并加载数据
    goToCurrentWeek();
});

// 获取当前周的开始日期(周一)
function getWeekStart(date) {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1); // 调整到周一
    return new Date(d.setDate(diff));
}

// 获取周数
function getWeekNumber(date) {
    const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    const dayNum = d.getUTCDay() || 7;
    d.setUTCDate(d.getUTCDate() + 4 - dayNum);
    const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
    return Math.ceil((((d - yearStart) / 86400000) + 1) / 7);
}

// 格式化日期
function formatDate(date, format = 'yyyy-MM-dd') {
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');

    if (format === 'MM-dd') return `${month}-${day}`;
    if (format === 'M月d日') return `${parseInt(month)}月${parseInt(day)}日`;
    return `${year}-${month}-${day}`;
}

// 跳转到当前周
function goToCurrentWeek() {
    currentWeekStart = getWeekStart(new Date());
    updateWeekDisplay();
    loadWeekSchedules();
}

// 上一周
function previousWeek() {
    const newDate = new Date(currentWeekStart);
    newDate.setDate(newDate.getDate() - 7);
    currentWeekStart = newDate;
    updateWeekDisplay();
    loadWeekSchedules();
}

// 下一周
function nextWeek() {
    const newDate = new Date(currentWeekStart);
    newDate.setDate(newDate.getDate() + 7);
    currentWeekStart = newDate;
    updateWeekDisplay();
    loadWeekSchedules();
}

// 更新周显示
function updateWeekDisplay() {
    const weekNum = getWeekNumber(currentWeekStart);
    const weekEnd = new Date(currentWeekStart);
    weekEnd.setDate(weekEnd.getDate() + 6);

    const startStr = formatDate(currentWeekStart, 'M月d日');
    const endStr = formatDate(weekEnd, 'M月d日');

    document.getElementById('weekInfo').textContent = `第${weekNum}周 (${startStr} - ${endStr})`;
}

// 加载医护人员列表
async function loadMedicalUsers() {
    try {
        // 使用正确的API路径(common.js已经加了/api前缀)
        const result = await get('/admin/user/list?role=MEDICAL&size=1000');
        if (result.code === 200 && result.data && result.data.records) {
            medicalUsers = result.data.records;

            // 为每个医护人员分配颜色
            medicalUsers.forEach((user, index) => {
                userColorMap[user.id] = (index % 8) + 1; // 8种颜色循环
            });

            // 更新下拉列表
            const select = document.getElementById('medicalUserId');
            select.innerHTML = '<option value="">请选择医护人员</option>' +
                medicalUsers.map(user =>
                    `<option value="${user.id}">${user.realName || user.username}</option>`
                ).join('');
        }
    } catch (error) {
        console.error('加载医护人员列表失败:', error);
    }
}

// 加载本周排班数据
async function loadWeekSchedules() {
    try {
        const weekEnd = new Date(currentWeekStart);
        weekEnd.setDate(weekEnd.getDate() + 6);

        const startDate = formatDate(currentWeekStart);
        const endDate = formatDate(weekEnd);

        const result = await get(`/admin/schedule/range?startDate=${startDate}&endDate=${endDate}`);

        if (result.code === 200 && result.data) {
            // 组织数据结构 - 保存完整的排班对象以便编辑
            scheduleData = {};
            const allSchedules = {};  // 用于存储所有排班对象

            result.data.forEach(schedule => {
                // 保存完整对象
                allSchedules[schedule.id] = schedule;

                const date = schedule.schedule_date;
                const timeSlot = schedule.start_time.substring(0, 5); // 取HH:mm

                if (!scheduleData[date]) {
                    scheduleData[date] = {};
                }
                if (!scheduleData[date][timeSlot]) {
                    scheduleData[date][timeSlot] = [];
                }
                scheduleData[date][timeSlot].push(schedule);
            });

            // 保存到全局变量供编辑使用
            window.allSchedules = allSchedules;

            // 渲染周视图
            renderWeekView();

            // 更新统计
            updateWeekStats(result.data);
        }
    } catch (error) {
        console.error('加载排班数据失败:', error);
    }
}

// 渲染周视图
function renderWeekView() {
    const grid = document.getElementById('weekScheduleGrid');
    const today = formatDate(new Date());

    let html = '';

    // 表头行
    html += '<div class="grid-header time-col">时间</div>';
    for (let i = 0; i < 7; i++) {
        const date = new Date(currentWeekStart);
        date.setDate(date.getDate() + i);
        const dateStr = formatDate(date);
        const isToday = dateStr === today;

        html += `
            <div class="grid-header ${isToday ? 'today' : ''}">
                <div class="date">${formatDate(date, 'MM-dd')}</div>
                <div class="weekday">${WEEKDAYS[date.getDay()]}</div>
            </div>
        `;
    }

    // 数据行
    TIME_SLOTS.forEach((timeSlot, index) => {
        // 计算结束时间
        const startHour = parseInt(timeSlot.split(':')[0]);
        const endHour = (startHour + 2) % 24;
        const endTime = `${String(endHour).padStart(2, '0')}:00`;
        const timeRange = `${timeSlot}-${endTime}`;

        // 时间列
        html += `<div class="time-cell">${timeRange}</div>`;

        // 每天的单元格
        for (let i = 0; i < 7; i++) {
            const date = new Date(currentWeekStart);
            date.setDate(date.getDate() + i);
            const dateStr = formatDate(date);

            // 获取该时间段的排班
            const schedules = (scheduleData[dateStr] && scheduleData[dateStr][timeSlot]) || [];

            html += `<div class="schedule-cell ${schedules.length > 0 ? 'has-schedule' : ''}"
                          onclick="openScheduleCell('${dateStr}', '${timeSlot}')">`;

            if (schedules.length > 0) {
                schedules.forEach(schedule => {
                    const colorClass = `color-${userColorMap[schedule.medical_user_id] || 1}`;
                    const name = schedule.real_name || schedule.username || '未知';
                    const time = `${schedule.start_time.substring(0, 5)}-${schedule.end_time.substring(0, 5)}`;

                    html += `
                        <div class="schedule-card ${colorClass}" onclick="event.stopPropagation(); editSchedule(${schedule.id})">
                            <div class="schedule-card-name">${name}</div>
                            <div class="schedule-card-time">${time}</div>
                        </div>
                    `;
                });
            }

            html += '</div>';
        }
    });

    grid.innerHTML = html;
}

// 打开单元格添加排班
function openScheduleCell(date, timeSlot) {
    document.getElementById('modalTitle').textContent = '添加排班';
    document.getElementById('scheduleForm').reset();
    document.getElementById('scheduleId').value = '';

    // 设置日期和时间
    document.getElementById('scheduleDate').value = date;
    document.getElementById('scheduleTimeSlot').value = timeSlot;

    // 显示日期时间
    const dateObj = new Date(date);
    const weekday = WEEKDAYS[dateObj.getDay()];
    document.getElementById('scheduleDateTimeDisplay').textContent =
        `${formatDate(date, 'M月d日')} ${weekday} ${timeSlot}`;

    // 设置默认时间 - 根据点击的时间段自动填充
    const hour = parseInt(timeSlot.split(':')[0]);
    document.getElementById('startTime').value = timeSlot;
    // 结束时间默认为开始时间+2小时
    const endHour = (hour + 2) % 24;
    document.getElementById('endTime').value = `${String(endHour).padStart(2, '0')}:00`;

    // 设置默认状态
    document.getElementById('status').value = '正常';

    // 隐藏删除按钮
    document.getElementById('deleteBtn').style.display = 'none';

    document.getElementById('scheduleModal').style.display = 'flex';
}

// 编辑排班
async function editSchedule(id) {
    try {
        // 从缓存中获取排班数据
        const schedule = window.allSchedules && window.allSchedules[id];
        if (!schedule) {
            alert('未找到排班信息');
            return;
        }

        document.getElementById('modalTitle').textContent = '编辑排班';
        document.getElementById('scheduleId').value = schedule.id;
        document.getElementById('medicalUserId').value = schedule.medical_user_id;
        document.getElementById('scheduleDate').value = schedule.schedule_date;
        document.getElementById('startTime').value = schedule.start_time;
        document.getElementById('endTime').value = schedule.end_time;
        document.getElementById('status').value = schedule.status || '正常';
        document.getElementById('remark').value = schedule.remark || '';

        // 显示日期时间
        const dateObj = new Date(schedule.schedule_date);
        const weekday = WEEKDAYS[dateObj.getDay()];
        const timeSlot = schedule.start_time.substring(0, 5);
        document.getElementById('scheduleDateTimeDisplay').textContent =
            `${formatDate(schedule.schedule_date, 'M月d日')} ${weekday} ${timeSlot}`;

        // 显示删除按钮
        document.getElementById('deleteBtn').style.display = 'inline-block';

        document.getElementById('scheduleModal').style.display = 'flex';
    } catch (error) {
        console.error('加载排班详情失败:', error);
        alert('加载排班详情失败');
    }
}

// 关闭模态框
function closeScheduleModal() {
    document.getElementById('scheduleModal').style.display = 'none';
}

// 保存排班
async function saveSchedule() {
    const scheduleId = document.getElementById('scheduleId').value;
    const medicalUserId = document.getElementById('medicalUserId').value;
    const scheduleDate = document.getElementById('scheduleDate').value;
    const startTime = document.getElementById('startTime').value;
    const endTime = document.getElementById('endTime').value;

    // 验证必填项
    if (!medicalUserId) {
        alert('请选择医护人员');
        return;
    }
    if (!scheduleDate) {
        alert('日期不能为空');
        return;
    }
    if (!startTime || !endTime) {
        alert('请设置开始和结束时间');
        return;
    }

    const formData = {
        medicalUserId: parseInt(medicalUserId),
        scheduleDate: scheduleDate,
        startTime: startTime,
        endTime: endTime,
        status: document.getElementById('status').value,
        remark: document.getElementById('remark').value
    };

    try {
        let result;
        if (scheduleId) {
            // 更新 - 需要包含ID
            formData.id = parseInt(scheduleId);
            result = await put('/admin/schedule/update', formData);
        } else {
            // 新增
            result = await post('/admin/schedule/add', formData);
        }

        if (result.code === 200) {
            alert(scheduleId ? '更新成功' : '添加成功');
            closeScheduleModal();
            loadWeekSchedules(); // 重新加载
        } else {
            alert(result.message || '操作失败');
        }
    } catch (error) {
        console.error('保存排班失败:', error);
        alert('保存失败,请重试');
    }
}

// 删除排班
async function deleteSchedule() {
    const scheduleId = document.getElementById('scheduleId').value;
    if (!scheduleId) return;

    if (!confirm('确定要删除这个排班吗?')) {
        return;
    }

    try {
        const result = await del(`/admin/schedule/${scheduleId}`);
        if (result.code === 200) {
            alert('删除成功');
            closeScheduleModal();
            loadWeekSchedules(); // 重新加载
        } else {
            alert(result.message || '删除失败');
        }
    } catch (error) {
        console.error('删除排班失败:', error);
        alert('删除失败,请重试');
    }
}

// 更新周统计
function updateWeekStats(schedules) {
    // 本周排班总数
    document.getElementById('weekTotalSchedules').textContent = schedules.length;

    // 医护人员数(去重)
    const uniqueUsers = new Set(schedules.map(s => s.medical_user_id));
    document.getElementById('weekMedicalStaff').textContent = uniqueUsers.size;

    // 计算总工作时长
    let totalHours = 0;
    schedules.forEach(schedule => {
        const start = schedule.start_time.split(':');
        const end = schedule.end_time.split(':');
        const startMinutes = parseInt(start[0]) * 60 + parseInt(start[1]);
        const endMinutes = parseInt(end[0]) * 60 + parseInt(end[1]);
        totalHours += (endMinutes - startMinutes) / 60;
    });
    document.getElementById('weekTotalHours').textContent = totalHours.toFixed(1);

    // 平均每天
    const avgPerDay = (schedules.length / 7).toFixed(1);
    document.getElementById('weekAvgPerDay').textContent = avgPerDay;
}

// 导出排班
function exportSchedules() {
    const weekEnd = new Date(currentWeekStart);
    weekEnd.setDate(weekEnd.getDate() + 6);
    const startDate = formatDate(currentWeekStart);
    const endDate = formatDate(weekEnd);

    window.location.href = `/admin/schedule/export?startDate=${startDate}&endDate=${endDate}`;
}

// 打印排班
function printSchedule() {
    window.print();
}

// 点击模态框外部关闭
window.onclick = function(event) {
    const modal = document.getElementById('scheduleModal');
    if (event.target === modal) {
        closeScheduleModal();
    }
}
