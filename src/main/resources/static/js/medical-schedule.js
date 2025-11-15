// 医护端 - 我的排班查看
// 全局变量
let currentWeekStart = null; // 当前周的开始日期
let scheduleData = {}; // 排班数据: {date: {timeSlot: [schedules]}}

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
    if (!userInfo || userInfo.role !== 'MEDICAL') {
        alert('权限不足');
        logout();
        return;
    }

    // 显示欢迎信息
    document.getElementById('welcomeText').textContent = `欢迎,${userInfo.username}!`;

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

// 加载本周排班数据
async function loadWeekSchedules() {
    try {
        const weekEnd = new Date(currentWeekStart);
        weekEnd.setDate(weekEnd.getDate() + 6);

        const startDate = formatDate(currentWeekStart);
        const endDate = formatDate(weekEnd);

        const result = await get(`/medical/schedule/my/range?startDate=${startDate}&endDate=${endDate}`);

        if (result.code === 200 && result.data) {
            // 组织数据结构
            scheduleData = {};

            result.data.forEach(schedule => {
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

            if (schedules.length > 0) {
                // 有排班 - 显示排班信息
                const schedule = schedules[0]; // 一个时间段只会有一个排班
                const time = `${schedule.start_time.substring(0, 5)}-${schedule.end_time.substring(0, 5)}`;
                const status = schedule.status || '正常';

                html += `
                    <div class="schedule-cell">
                        <div class="schedule-card">
                            <div class="schedule-card-time">${time}</div>
                            <div class="schedule-card-status">${status}</div>
                        </div>
                    </div>
                `;
            } else {
                // 无排班 - 显示空白
                html += `<div class="schedule-cell empty"></div>`;
            }
        }
    });

    grid.innerHTML = html;
}

// 更新周统计
function updateWeekStats(schedules) {
    // 本周排班总数
    document.getElementById('weekTotalSchedules').textContent = schedules.length;

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

    // 平均每天工作时长
    const avgPerDay = (totalHours / 7).toFixed(1);
    document.getElementById('weekAvgPerDay').textContent = avgPerDay;
}

// 打印排班
function printSchedule() {
    window.print();
}
