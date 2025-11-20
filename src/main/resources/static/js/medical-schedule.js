// 医护端 - 我的排班查看
document.addEventListener('DOMContentLoaded', function() {
    const userInfo = checkLogin();
    if (!userInfo || userInfo.role !== 'MEDICAL') {
        alert('权限不足');
        logout();
        return;
    }

    document.getElementById('welcomeText').textContent = `欢迎, ${userInfo.username}!`;

    // 全局变量
    let currentWeekStart = getWeekStart(new Date());

    // 初始化
    updateWeekDisplay();
    loadWeekSchedules();

    // 绑定事件
    window.previousWeek = () => changeWeek(-7);
    window.nextWeek = () => changeWeek(7);
    window.goToCurrentWeek = () => {
        currentWeekStart = getWeekStart(new Date());
        updateWeekDisplay();
        loadWeekSchedules();
    };
    window.printSchedule = () => window.print();

    function changeWeek(days) {
        currentWeekStart.setDate(currentWeekStart.getDate() + days);
        updateWeekDisplay();
        loadWeekSchedules();
    }

    function getWeekStart(date) {
        const d = new Date(date);
        const day = d.getDay();
        const diff = d.getDate() - day + (day === 0 ? -6 : 1); // 周一为起始
        return new Date(d.setDate(diff));
    }

    function formatDate(date, format = 'yyyy-MM-dd') {
        const d = new Date(date);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        if (format === 'M月d日') return `${d.getMonth() + 1}月${d.getDate()}日`;
        return `${year}-${month}-${day}`;
    }

    function getWeekNumber(date) {
        const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
        const dayNum = d.getUTCDay() || 7;
        d.setUTCDate(d.getUTCDate() + 4 - dayNum);
        const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
        return Math.ceil((((d - yearStart) / 86400000) + 1) / 7);
    }

    function updateWeekDisplay() {
        const weekEnd = new Date(currentWeekStart);
        weekEnd.setDate(weekEnd.getDate() + 6);
        document.getElementById('weekInfo').textContent =
            `第${getWeekNumber(currentWeekStart)}周 (${formatDate(currentWeekStart, 'M月d日')} - ${formatDate(weekEnd, 'M月d日')})`;
    }

    async function loadWeekSchedules() {
        const startDate = formatDate(currentWeekStart);
        const endDate = new Date(currentWeekStart);
        endDate.setDate(endDate.getDate() + 6);

        try {
            const result = await get(`/medical/schedule/my/range?startDate=${startDate}&endDate=${formatDate(endDate)}`);
            if (result.code === 200 && result.data) {
                renderWeekView(result.data);
                updateWeekStats(result.data);
            }
        } catch (error) {
            console.error('加载排班数据失败:', error);
        }
    }

    function renderWeekView(schedules) {
        const grid = document.getElementById('weekScheduleGrid');
        const today = formatDate(new Date());
        const timeSlots = ['07:00', '09:00', '11:00', '13:00', '15:00', '17:00', '19:00', '21:00'];

        let html = '<div class="grid-header time-col">时间</div>';
        for (let i = 0; i < 7; i++) {
            const date = new Date(currentWeekStart);
            date.setDate(date.getDate() + i);
            html += `<div class="grid-header ${formatDate(date) === today ? 'today' : ''}">
                        <div class="date">${formatDate(date, 'M月d日')}</div>
                        <div class="weekday">${['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]}</div>
                     </div>`;
        }

        timeSlots.forEach(slot => {
            const endHour = (parseInt(slot.split(':')[0]) + 2).toString().padStart(2, '0');
            html += `<div class="time-cell">${slot}-${endHour}:00</div>`;

            for (let i = 0; i < 7; i++) {
                const date = new Date(currentWeekStart);
                date.setDate(date.getDate() + i);
                const dateStr = formatDate(date);

                const schedule = schedules.find(s => s.scheduleDate === dateStr && s.startTime.startsWith(slot.substring(0, 2)));

                if (schedule) {
                    html += `<div class="schedule-cell">
                                <div class="schedule-card">
                                    <div class="schedule-card-time">${schedule.startTime.substring(0,5)}-${schedule.endTime.substring(0,5)}</div>
                                    <div class="schedule-card-room">${schedule.roomNumber || '-'}</div>
                                </div>
                             </div>`;
                } else {
                    html += '<div class="schedule-cell empty"></div>';
                }
            }
        });
        grid.innerHTML = html;
    }

    function updateWeekStats(schedules) {
        document.getElementById('weekTotalSchedules').textContent = schedules.length;
        const totalHours = schedules.reduce((acc, s) => {
            const start = new Date(`1970-01-01T${s.startTime}`);
            const end = new Date(`1970-01-01T${s.endTime}`);
            return acc + (end - start) / (1000 * 60 * 60);
        }, 0);
        document.getElementById('weekTotalHours').textContent = totalHours.toFixed(1);
        document.getElementById('weekAvgPerDay').textContent = (totalHours / 7).toFixed(1);
    }
});
