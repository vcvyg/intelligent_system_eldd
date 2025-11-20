// 全局变量，存储Stomp客户端
let stompClient = null;

/**
 * 加载告警列表
 */
async function loadAlerts() {
    const container = document.getElementById('alert-list-container');
    try {
        const response = await request('/medical/alerts', {
            method: 'GET'
        });
        // 清空现有列表
        container.innerHTML = '';
        if (response.data && response.data.records && response.data.records.length > 0) {
            response.data.records.forEach(alert => {
                renderAlert(alert);
            });
        } else {
            container.innerHTML = '<p class="empty-tip">暂无告警记录</p>';
        }
    } catch (error) {
        console.error('加载告警列表失败:', error);
        container.innerHTML = '<p class="empty-tip">加载失败，请稍后重试</p>';
    }
}

/**
 * 渲染单个告警卡片
 * @param {object} alert - 告警数据对象
 * @param {boolean} prepend - 是否在列表顶部插入 (用于实时更新)
 */
function renderAlert(alert, prepend = false) {
    const container = document.getElementById('alert-list-container');
    const emptyTip = container.querySelector('.empty-tip');
    if (emptyTip) {
        emptyTip.remove();
    }

    const alertCard = document.createElement('div');
    alertCard.className = 'alert-card';
    alertCard.id = `alert-card-${alert.id}`; // 为卡片添加唯一ID

    // 根据告警级别添加不同样式和颜色
    const levelClass = getLevelClass(alert.alertLevel);
    alertCard.classList.add(levelClass);

    // 根据状态添加样式
    if (alert.status === '处理中') {
        alertCard.classList.add('processing');
    } else if (alert.status === '已处理' || alert.status === '已忽略') {
        alertCard.classList.add('handled');
    }

    const formattedTime = new Date(alert.alertTime).toLocaleString();

    // 动态生成按钮
    let actionButtons = '';
    switch (alert.status) {
        case '待处理':
            actionButtons = `
                <button class="btn-handle" onclick="startProcessingAlert(${alert.id})">立即处理</button>
                <button class="btn-ignore" onclick="ignoreAlert(${alert.id})">忽略</button>
            `;
            break;
        case '处理中':
            actionButtons = `<button class="btn-complete" onclick="finishProcessingAlert(${alert.id})">处理完成</button>`;
            break;
        case '已处理':
        case '已忽略':
            // 已处理、已忽略状态不显示操作按钮
            break;
        default:
            // 其他状态也不显示操作按钮
            break;
    }

    alertCard.innerHTML = `
        <div class="alert-header">
            <span class="alert-level level-${levelClass}">${alert.alertLevel}</span>
            <span class="alert-type">${alert.alertType}</span>
            <span class="alert-time">${formattedTime}</span>
        </div>
        <div class="alert-body">
            <div class="alert-info">
                <span class="info-item"><strong>老人:</strong> ${alert.elderlyName || '-'}</span>
                <span class="info-item"><strong>房间:</strong> ${alert.roomName || '-'}</span>
            </div>
            <p class="alert-content">${alert.alertContent}</p>
            ${alert.alertValue ? `<p class="alert-value">测量值: ${alert.alertValue}</p>` : ''}
        </div>
        <div class="alert-footer">
            <span class="alert-status">状态: ${alert.status}</span>
            <div class="alert-actions">
                ${actionButtons}
            </div>
        </div>
    `;

    if (prepend) {
        container.prepend(alertCard);
        playNotificationSound();
    } else {
        container.appendChild(alertCard);
    }
}

/**
 * 获取告警等级对应的CSS类名
 * @param {string} level - 告警等级
 * @returns {string} CSS类名
 */
function getLevelClass(level) {
    const levelMap = {
        '低': 'level-low',
        '中': 'level-medium',
        '高': 'level-high',
        '紧急': 'level-critical'
    };
    return levelMap[level] || 'level-low';
}

/**
 * 连接WebSocket以实时接收告警
 * @param {number} userId - 当前登录用户的ID
 */
function connectWebSocket(userId) {
    const token = localStorage.getItem('authToken');
    // 通过 URL 参数传递 token（SockJS 兼容方案）
    const socket = new SockJS('/ws-chat?token=' + encodeURIComponent(token));
    stompClient = Stomp.over(socket);

    console.log('Token being sent via URL parameter');
    stompClient.connect({}, function (frame) {
        console.log('WebSocket已连接: ' + frame);
        // 订阅通用告警通道，医护端接收所有告警
        stompClient.subscribe('/topic/alerts', function (message) {
            const newAlert = JSON.parse(message.body);
            // 检查页面上是否已有此告警卡片，如果有则更新，没有则添加
            const existingCard = document.getElementById(`alert-card-${newAlert.id}`);
            if (existingCard) {
                existingCard.remove();
            }
            renderAlert(newAlert, true); // 在列表顶部插入新告警
        });
    }, function(error) {
        console.error('WebSocket连接失败:', error);
        // 可以在这里设置5秒后重连
        setTimeout(() => connectWebSocket(userId), 5000);
    });
}

/**
 * 播放提示音
 */
function playNotificationSound() {
    // 使用一段Base64编码的1秒静音WAV文件作为提示音, 避免文件404错误.
    // 您可以替换成自己的提示音文件路径, 例如: '/sounds/alert.mp3'
    const audio = new Audio('data:audio/wav;base64,UklGRigAAABXQVZFZm10IBIAAAABAAEARKwAAIhYAQACABAAAABkYXRhAgAAAAEA');
    audio.play().catch(e => console.log("无法播放提示音:", e));
}


// --- 告警处理交互 ---

/**
 * 开始处理告警
 * @param {number} alertId 
 */
async function startProcessingAlert(alertId) {
    try {
        await request(`/medical/alerts/${alertId}/process`, {
            method: 'PUT'
        });
        // 刷新整个列表以更新状态
        loadAlerts();
    } catch (error) {
        console.error('开始处理告警失败:', error);
    }
}

/**
 * 完成处理告警
 * @param {number} alertId
 */
async function finishProcessingAlert(alertId) {
    const result = prompt("请输入处理结果：");
    if (result === null) { // 用户点击了取消
        return;
    }
    if (!result.trim()) {
        alert("处理结果不能为空！");
        return;
    }

    try {
        await request('/medical/alerts/handle', {
            method: 'PUT',
            body: JSON.stringify({
                alertId: alertId,
                handleResult: result,
                status: '已处理'
            })
        });
        // 刷新整个列表以更新状态
        loadAlerts();
    } catch (error) {
        console.error('完成处理告警失败:', error);
        alert('处理失败，请稍后重试');
    }
}

/**
 * 忽略告警
 * @param {number} alertId
 */
async function ignoreAlert(alertId) {
    if (!confirm('确定要忽略这条告警吗？')) {
        return;
    }

    try {
        await request(`/medical/alerts/${alertId}/ignore`, {
            method: 'PUT'
        });
        alert('告警已忽略');
        // 刷新整个列表以更新状态
        loadAlerts();
    } catch (error) {
        console.error('忽略告警失败:', error);
        alert('操作失败，请稍后重试');
    }
}
