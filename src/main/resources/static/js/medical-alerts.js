// 全局变量，存储Stomp客户端
let stompClient = null;

// DOM元素缓存，提高性能
const domCache = {
    container: null,
    welcomeText: null,
    
    init() {
        this.container = document.getElementById('alert-list-container');
        this.welcomeText = document.getElementById('welcomeText');
    }
};

// 数据缓存，减少重复请求
const dataCache = {
    alerts: null,
    lastFetchTime: 0,
    cacheTimeout: 30000, // 30秒缓存
    
    isValid() {
        return this.alerts && (Date.now() - this.lastFetchTime) < this.cacheTimeout;
    },
    
    set(data) {
        this.alerts = data;
        this.lastFetchTime = Date.now();
    },
    
    get() {
        return this.isValid() ? this.alerts : null;
    },
    
    clear() {
        this.alerts = null;
        this.lastFetchTime = 0;
    }
};

/**
 * 加载告警列表（优化版，支持缓存）
 */
async function loadAlerts(forceRefresh = false) {
    const container = domCache.container;
    if (!container) return;
    
    // 检查缓存
    if (!forceRefresh) {
        const cachedData = dataCache.get();
        if (cachedData) {
            console.log('使用缓存数据');
            renderAlertList(cachedData);
            return;
        }
    }
    
    // 显示骨架屏加载状态
    container.innerHTML = `
        <div class="skeleton-container">
            <div class="skeleton-card"></div>
            <div class="skeleton-card"></div>
            <div class="skeleton-card"></div>
        </div>
    `;
    
    try {
        const response = await request('/medical/alerts?size=20', { // 增加每页数量
            method: 'GET'
        });
        
        if (response.data && response.data.records) {
            // 缓存数据
            dataCache.set(response.data.records);
            renderAlertList(response.data.records);
        } else {
            container.innerHTML = '<p class="empty-tip">暂无告警记录</p>';
        }
    } catch (error) {
        console.error('加载告警列表失败:', error);
        container.innerHTML = `
            <div class="error-tip">
                <p>加载失败，请稍后重试</p>
                <button onclick="loadAlerts(true)" class="retry-btn">重新加载</button>
            </div>
        `;
    }
}

/**
 * 渲染告警列表
 */
function renderAlertList(alerts) {
    const container = domCache.container;
    if (!container) return;
    
    container.innerHTML = '';
    
    if (alerts.length > 0) {
        // 使用文档片段提高性能
        const fragment = document.createDocumentFragment();
        
        // 分批渲染，避免阻塞UI
        const batchSize = 5;
        let currentBatch = 0;
        
        function renderBatch() {
            const start = currentBatch * batchSize;
            const end = Math.min(start + batchSize, alerts.length);
            
            for (let i = start; i < end; i++) {
                const alertElement = createAlertElement(alerts[i]);
                alertElement.style.animationDelay = `${i * 30}ms`; // 减少延迟
                fragment.appendChild(alertElement);
            }
            
            if (currentBatch === 0) {
                // 第一批立即显示
                container.appendChild(fragment.cloneNode(true));
            }
            
            currentBatch++;
            
            if (end < alerts.length) {
                // 继续渲染下一批
                requestAnimationFrame(renderBatch);
            } else if (currentBatch > 1) {
                // 最后一批
                container.appendChild(fragment);
            }
        }
        
        renderBatch();
    } else {
        container.innerHTML = '<p class="empty-tip">暂无告警记录</p>';
    }
}

/**
 * 创建告警元素（优化版）
 * @param {object} alert - 告警数据对象
 * @returns {HTMLElement} 告警卡片元素
 */
function createAlertElement(alert) {
    const alertCard = document.createElement('div');
    alertCard.className = 'alert-card';
    alertCard.id = `alert-card-${alert.id}`;

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

    return alertCard;
}

/**
 * 渲染单个告警卡片（用于实时更新）
 * @param {object} alert - 告警数据对象
 * @param {boolean} prepend - 是否在列表顶部插入
 */
function renderAlert(alert, prepend = false) {
    const container = domCache.container;
    if (!container) return;
    
    const emptyTip = container.querySelector('.empty-tip, .loading');
    if (emptyTip) {
        emptyTip.remove();
    }

    const alertCard = createAlertElement(alert);

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

/**
 * 局部更新告警卡片状态
 * @param {number} alertId - 告警ID
 * @param {string} newStatus - 新状态
 */
function updateAlertCardStatus(alertId, newStatus) {
    const alertCard = document.getElementById(`alert-card-${alertId}`);
    if (!alertCard) return;
    
    // 更新状态显示
    const statusElement = alertCard.querySelector('.alert-status');
    if (statusElement) {
        statusElement.textContent = `状态: ${newStatus}`;
    }
    
    // 更新卡片样式
    alertCard.classList.remove('processing', 'handled');
    if (newStatus === '处理中') {
        alertCard.classList.add('processing');
    } else if (newStatus === '已处理' || newStatus === '已忽略') {
        alertCard.classList.add('handled');
    }
    
    // 更新操作按钮
    const actionsContainer = alertCard.querySelector('.alert-actions');
    if (actionsContainer) {
        let newButtons = '';
        switch (newStatus) {
            case '处理中':
                newButtons = `<button class="btn-complete" onclick="finishProcessingAlert(${alertId})">处理完成</button>`;
                break;
            case '已处理':
            case '已忽略':
                newButtons = ''; // 已完成状态不显示按钮
                break;
        }
        actionsContainer.innerHTML = newButtons;
    }
}

/**
 * 显示提示消息
 * @param {string} message - 消息内容
 * @param {string} type - 消息类型: 'success', 'error', 'info'
 */
function showToast(message, type = 'success') {
    // 移除现有的toast
    const existingToast = document.querySelector('.toast');
    if (existingToast) {
        existingToast.remove();
    }
    
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    
    // 添加到页面
    document.body.appendChild(toast);
    
    // 显示动画
    setTimeout(() => toast.classList.add('show'), 100);
    
    // 自动隐藏
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}


// --- 告警处理交互 ---

/**
 * 开始处理告警
 * @param {number} alertId 
 */
async function startProcessingAlert(alertId) {
    const alertCard = document.getElementById(`alert-card-${alertId}`);
    if (!alertCard) return;
    
    // 显示处理中状态
    const button = alertCard.querySelector('.btn-handle');
    if (button) {
        button.textContent = '处理中...';
        button.disabled = true;
    }
    
    try {
        await request(`/medical/alerts/${alertId}/process`, {
            method: 'PUT'
        });
        
        // 局部更新UI，不重新加载整个列表
        updateAlertCardStatus(alertId, '处理中');
        showToast('告警已开始处理');
    } catch (error) {
        console.error('开始处理告警失败:', error);
        showToast('处理失败，请稍后重试', 'error');
        
        // 恢复按钮状态
        if (button) {
            button.textContent = '立即处理';
            button.disabled = false;
        }
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
        showToast("处理结果不能为空！", 'error');
        return;
    }

    const alertCard = document.getElementById(`alert-card-${alertId}`);
    const button = alertCard?.querySelector('.btn-complete');
    
    // 显示处理中状态
    if (button) {
        button.textContent = '提交中...';
        button.disabled = true;
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
        
        // 局部更新UI，不重新加载整个列表
        updateAlertCardStatus(alertId, '已处理');
        showToast('告警处理完成');
    } catch (error) {
        console.error('完成处理告警失败:', error);
        showToast('处理失败，请稍后重试', 'error');
        
        // 恢复按钮状态
        if (button) {
            button.textContent = '处理完成';
            button.disabled = false;
        }
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

    const alertCard = document.getElementById(`alert-card-${alertId}`);
    const button = alertCard?.querySelector('.btn-ignore');
    
    // 显示处理中状态
    if (button) {
        button.textContent = '处理中...';
        button.disabled = true;
    }

    try {
        await request(`/medical/alerts/${alertId}/ignore`, {
            method: 'PUT'
        });
        
        // 局部更新UI，不重新加载整个列表
        updateAlertCardStatus(alertId, '已忽略');
        showToast('告警已忽略');
    } catch (error) {
        console.error('忽略告警失败:', error);
        showToast('操作失败，请稍后重试', 'error');
        
        // 恢复按钮状态
        if (button) {
            button.textContent = '忽略';
            button.disabled = false;
        }
    }
}
