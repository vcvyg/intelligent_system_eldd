// 子女端定位脚本

let currentElderlyId = null;
let elderlyList = [];
let map = null; // 高德地图实例
let locationMarker = null; // 位置标记
let geofenceCircles = []; // 电子围栏圆形覆盖物
let isSelectingLocation = false; // 是否正在选择位置
let geocoder = null; // 地理编码服务

document.addEventListener('DOMContentLoaded', async () => {
    const userInfo = checkLogin();
    if (!userInfo || userInfo.role !== 'FAMILY') {
        alert('权限不足或登录已过期');
        logout();
        return;
    }
    document.getElementById('welcomeText').textContent = `欢迎，${userInfo.username}！`;

    // 先加载地图API，然后初始化地图
    await loadMapAPI();
    
    await loadElderlyList();
});

/**
 * 从后端加载高德地图API
 */
async function loadMapAPI() {
    try {
        // 从后端获取API Key
        const result = await get('/config/amap-key');
        if (result.code !== 200 || !result.data || !result.data.apiKey) {
            throw new Error('无法获取地图API Key配置');
        }
        
        const apiKey = result.data.apiKey;
        if (apiKey === 'YOUR_AMAP_KEY' || !apiKey) {
            document.getElementById('mapPlaceholder').innerHTML = `
                <p style="font-size: 18px; margin-bottom: 10px;">⚠️ 地图API Key未配置</p>
                <p style="font-size: 14px;">请在 application.properties 中配置 amap.api.key</p>
                <p style="font-size: 12px; color: var(--color-text-light); margin-top: 10px;">
                    申请地址: <a href="https://console.amap.com/dev/key/app" target="_blank" style="color: var(--color-primary);">https://console.amap.com/dev/key/app</a>
                </p>
            `;
            return;
        }
        
        // 动态加载高德地图API
        return new Promise((resolve, reject) => {
            // 检查是否已经加载
            if (typeof AMap !== 'undefined') {
                initMap();
                resolve();
                return;
            }
            
            // 创建script标签加载地图API
            const script = document.createElement('script');
            script.type = 'text/javascript';
            script.src = `https://webapi.amap.com/maps?v=2.0&key=${apiKey}&plugin=AMap.Geocoder,AMap.PlaceSearch`;
            script.onload = () => {
                console.log('高德地图API加载成功');
                initMap();
                resolve();
            };
            script.onerror = () => {
                console.error('高德地图API加载失败');
                document.getElementById('mapPlaceholder').innerHTML = `
                    <p style="font-size: 18px; margin-bottom: 10px;">⚠️ 地图API加载失败</p>
                    <p style="font-size: 14px;">请检查网络连接或API Key是否正确</p>
                `;
                reject(new Error('地图API加载失败'));
            };
            document.head.appendChild(script);
        });
    } catch (error) {
        console.error('加载地图API失败:', error);
        document.getElementById('mapPlaceholder').innerHTML = `
            <p style="font-size: 18px; margin-bottom: 10px;">⚠️ 地图加载失败</p>
            <p style="font-size: 14px;">${error.message || '未知错误'}</p>
        `;
    }
}

/**
 * 初始化高德地图
 */
function initMap() {
    // 检查高德地图API是否加载
    if (typeof AMap === 'undefined') {
        console.error('高德地图API未加载，请检查API Key配置');
        document.getElementById('mapPlaceholder').innerHTML = `
            <p style="font-size: 18px; margin-bottom: 10px;">⚠️ 地图加载失败</p>
            <p style="font-size: 14px;">请配置高德地图API Key</p>
        `;
        return;
    }

    try {
        // 创建地图实例（默认显示北京）
        map = new AMap.Map('map', {
            zoom: 13,
            center: [116.397128, 39.916527],
            viewMode: '3D'
        });

        // 初始化地理编码服务
        geocoder = new AMap.Geocoder({
            city: '全国'
        });

        // 隐藏占位符
        document.getElementById('mapPlaceholder').style.display = 'none';

        // 地图点击事件
        map.on('click', (e) => {
            if (isSelectingLocation) {
                const lng = e.lnglat.getLng();
                const lat = e.lnglat.getLat();
                document.getElementById('geofenceLng').value = lng.toFixed(6);
                document.getElementById('geofenceLat').value = lat.toFixed(6);
                isSelectingLocation = false;
                
                // 显示提示
                const tip = new AMap.Marker({
                    position: [lng, lat],
                    content: '<div style="background: #4CAF50; color: white; padding: 5px 10px; border-radius: 4px; font-size: 12px;">已选择位置</div>',
                    offset: new AMap.Pixel(0, -10)
                });
                map.add(tip);
                
                // 3秒后移除提示
                setTimeout(() => {
                    map.remove(tip);
                }, 3000);
                
                alert('位置已选择，经纬度已自动填充');
            }
        });

        console.log('地图初始化成功');
    } catch (error) {
        console.error('地图初始化失败:', error);
        document.getElementById('mapPlaceholder').innerHTML = `
            <p style="font-size: 18px; margin-bottom: 10px;">⚠️ 地图初始化失败</p>
            <p style="font-size: 14px;">${error.message || '未知错误'}</p>
        `;
    }
}

/**
 * 加载老人列表
 */
async function loadElderlyList() {
    try {
        const result = await get('/family/relation/my-elderly');
        if (result.code === 200 && result.data) {
            elderlyList = result.data;
            const select = document.getElementById('elderlySelect');
            select.innerHTML = '<option value="">请选择...</option>' +
                elderlyList.map(elderly => {
                    const id = elderly.elderly_id || elderly.id;
                    const name = elderly.name || '-';
                    return `<option value="${id}">${name}</option>`;
                }).join('');
        } else {
            console.error('加载老人列表失败:', result.message);
        }
    } catch (error) {
        console.error('加载老人列表失败:', error);
    }
}

/**
 * 加载位置数据
 */
async function loadLocationData() {
    const select = document.getElementById('elderlySelect');
    currentElderlyId = select.value;

    if (!currentElderlyId) {
        document.getElementById('locationInfo').style.display = 'none';
        document.getElementById('mapPlaceholder').style.display = 'flex';
        document.getElementById('mapPlaceholder').innerHTML = `
            <p style="font-size: 18px; margin-bottom: 10px;">🗺️ 地图区域</p>
            <p style="font-size: 14px;">请选择老人查看位置信息</p>
        `;
        // 清除地图上的标记
        clearMapMarkers();
        return;
    }

    try {
        // 从后端API获取真实位置数据
        const result = await get(`/family/location/${currentElderlyId}`);
        if (result.code === 200 && result.data) {
            const locationData = result.data;
            // 格式化时间
            const formattedData = {
                longitude: locationData.longitude,
                latitude: locationData.latitude,
                address: locationData.address || '地址解析中...',
                updateTime: locationData.updateTime ? new Date(locationData.updateTime).toLocaleString('zh-CN') : new Date().toLocaleString('zh-CN'),
                deviceStatus: locationData.deviceStatus || '未知',
                deviceType: locationData.deviceType || '未知设备',
                lastSync: locationData.lastSync ? new Date(locationData.lastSync).toLocaleString('zh-CN') : new Date().toLocaleString('zh-CN')
            };
            updateLocationDisplay(formattedData);
            updateMapLocation(formattedData);
        } else {
            throw new Error(result.message || '获取位置数据失败');
        }

        // 加载围栏和警报数据
        await loadGeofenceList();
        await loadAlertHistory();
    } catch (error) {
        console.error('加载位置数据失败:', error);
        
        // 显示友好的错误提示
        document.getElementById('locationInfo').style.display = 'none';
        document.getElementById('mapPlaceholder').style.display = 'flex';
        
        const errorMessage = error.message || '获取位置数据失败';
        let displayMessage = '';
        let showRefreshButton = false;
        
        if (errorMessage.includes('暂无定位数据') || errorMessage.includes('未绑定定位设备')) {
            displayMessage = `
                <p style="font-size: 18px; margin-bottom: 10px;">📍 暂无定位数据</p>
                <p style="font-size: 14px; color: var(--color-text-gray); margin-bottom: 20px;">${errorMessage}</p>
                <button id="refreshLocationBtn" class="btn btn-primary" style="padding: 10px 20px; border-radius: 5px; border: none; cursor: pointer; background: var(--color-primary); color: white;">
                    🔄 立即获取定位
                </button>
            `;
            showRefreshButton = true;
        } else if (errorMessage.includes('正在获取位置信息')) {
            displayMessage = `
                <p style="font-size: 18px; margin-bottom: 10px;">⏳ 正在获取位置信息</p>
                <p style="font-size: 14px; color: var(--color-text-gray); margin-bottom: 20px;">${errorMessage}</p>
                <button id="refreshLocationBtn" class="btn btn-primary" style="padding: 10px 20px; border-radius: 5px; border: none; cursor: pointer; background: var(--color-primary); color: white;">
                    🔄 刷新位置
                </button>
            `;
            showRefreshButton = true;
        } else {
            displayMessage = `
                <p style="font-size: 18px; margin-bottom: 10px;">⚠️ 加载位置数据失败</p>
                <p style="font-size: 14px; color: var(--color-text-gray); margin-bottom: 20px;">${errorMessage}</p>
                <button id="refreshLocationBtn" class="btn btn-primary" style="padding: 10px 20px; border-radius: 5px; border: none; cursor: pointer; background: var(--color-primary); color: white;">
                    🔄 重试
                </button>
            `;
            showRefreshButton = true;
        }
        
        document.getElementById('mapPlaceholder').innerHTML = displayMessage;
        
        // 添加刷新按钮事件
        if (showRefreshButton) {
            const refreshBtn = document.getElementById('refreshLocationBtn');
            if (refreshBtn) {
                refreshBtn.onclick = async function() {
                    refreshBtn.disabled = true;
                    refreshBtn.textContent = '⏳ 获取中...';
                    try {
                        // 先调用更新接口
                        await post(`/family/location/update/${currentElderlyId}`, {});
                        // 等待1秒后重新加载
                        setTimeout(() => {
                            loadLocationData();
                        }, 1000);
                    } catch (e) {
                        console.error('更新位置失败:', e);
                        refreshBtn.disabled = false;
                        refreshBtn.textContent = '🔄 重试';
                    }
                };
            }
        }
        
        // 清除地图标记
        clearMapMarkers();
    }
}

/**
 * 更新位置信息显示
 */
function updateLocationDisplay(locationData) {
    document.getElementById('longitude').textContent = locationData.longitude;
    document.getElementById('latitude').textContent = locationData.latitude;
    document.getElementById('address').textContent = locationData.address || '地址解析中...';
    document.getElementById('updateTime').textContent = locationData.updateTime;
    document.getElementById('deviceStatus').textContent = locationData.deviceStatus;
    document.getElementById('deviceType').textContent = locationData.deviceType;
    document.getElementById('lastSync').textContent = locationData.lastSync;
    document.getElementById('locationInfo').style.display = 'grid';
}

/**
 * 更新地图上的位置标记
 */
function updateMapLocation(locationData) {
    if (!map) {
        console.error('地图未初始化');
        return;
    }

    const lng = parseFloat(locationData.longitude);
    const lat = parseFloat(locationData.latitude);

    if (isNaN(lng) || isNaN(lat)) {
        console.error('无效的经纬度:', locationData);
        return;
    }

    // 隐藏占位符
    document.getElementById('mapPlaceholder').style.display = 'none';

    // 清除旧标记
    if (locationMarker) {
        map.remove(locationMarker);
    }

    // 创建新标记
    locationMarker = new AMap.Marker({
        position: [lng, lat],
        title: '老人当前位置',
        icon: new AMap.Icon({
            size: new AMap.Size(40, 50),
            image: 'https://webapi.amap.com/theme/v1.3/markers/n/mid.png',
            imageOffset: new AMap.Pixel(0, 0),
            imageSize: new AMap.Size(40, 50)
        })
    });

    map.add(locationMarker);
    
    // 设置地图中心点和缩放级别
    map.setCenter([lng, lat]);
    map.setZoom(15);

    // 如果地址为空，进行逆地理编码
    if (!locationData.address || locationData.address === '地址解析中...') {
        reverseGeocode(lng, lat);
    }
}

/**
 * 逆地理编码（经纬度转地址）
 */
function reverseGeocode(lng, lat) {
    if (!geocoder) {
        console.error('地理编码服务未初始化');
        return;
    }

    geocoder.getAddress([lng, lat], (status, result) => {
        if (status === 'complete' && result.info === 'OK') {
            const address = result.regeocode.formattedAddress;
            document.getElementById('address').textContent = address;
        } else {
            console.error('逆地理编码失败:', result);
            document.getElementById('address').textContent = '地址解析失败';
        }
    });
}

/**
 * 清除地图上的所有标记
 */
function clearMapMarkers() {
    if (locationMarker) {
        map.remove(locationMarker);
        locationMarker = null;
    }
    clearGeofenceCircles();
}

/**
 * 刷新位置
 */
async function refreshLocation() {
    if (!currentElderlyId) {
        alert('请先选择老人');
        return;
    }
    
    const refreshBtn = document.querySelector('button[onclick="refreshLocation()"]');
    const originalText = refreshBtn.textContent;
    
    try {
        // 更新按钮状态
        refreshBtn.disabled = true;
        refreshBtn.textContent = '⏳ 刷新中...';
        
        // 先尝试触发位置更新
        const updateResult = await post(`/family/location/update/${currentElderlyId}`, {});
        if (updateResult.code === 200) {
            // 等待1秒后重新加载数据
            setTimeout(() => {
                loadLocationData();
            }, 1000);
        } else {
            // 即使更新失败，也尝试重新加载现有数据
            console.warn('位置更新失败，但仍尝试加载现有数据:', updateResult.message);
            loadLocationData();
        }
    } catch (error) {
        console.error('刷新位置失败:', error);
        // 即使出错，也尝试重新加载现有数据
        loadLocationData();
    } finally {
        // 恢复按钮状态
        setTimeout(() => {
            refreshBtn.disabled = false;
            refreshBtn.textContent = originalText;
        }, 2000);
    }
}

/**
 * 加载电子围栏列表
 */
async function loadGeofenceList() {
    if (!currentElderlyId) {
        return;
    }

    try {
        // 从后端API获取真实围栏数据
        const result = await get(`/family/location/geofence/${currentElderlyId}`);
        if (result.code === 200 && result.data) {
            const geofences = result.data;
            displayGeofenceList(geofences);
            displayGeofenceOnMap(geofences);
        } else {
            // 如果没有围栏数据，显示空列表
            displayGeofenceList([]);
            displayGeofenceOnMap([]);
        }
    } catch (error) {
        console.error('加载围栏列表失败:', error);
    }
}

/**
 * 显示围栏列表
 */
function displayGeofenceList(geofences) {
    const geofenceList = document.getElementById('geofenceList');
    
    if (!geofences || geofences.length === 0) {
        geofenceList.innerHTML = '<div style="text-align: center; padding: 40px; color: var(--color-text-gray);">暂无电子围栏设置</div>';
        return;
    }

    geofenceList.innerHTML = geofences.map(geofence => `
        <div class="geofence-item">
            <div class="info">
                <div class="name">${geofence.name}</div>
                <div class="radius">半径: ${geofence.radius}米 | 状态: ${geofence.status}</div>
            </div>
            <div class="action-btns">
                <button class="btn-secondary btn-sm" onclick="editGeofence(${geofence.id})">编辑</button>
                <button class="btn-delete btn-sm" onclick="deleteGeofence(${geofence.id})">删除</button>
            </div>
        </div>
    `).join('');
}

/**
 * 在地图上显示电子围栏
 */
function displayGeofenceOnMap(geofences) {
    if (!map) {
        return;
    }

    // 清除旧的围栏
    clearGeofenceCircles();

    if (!geofences || geofences.length === 0) {
        return;
    }

    // 在地图上绘制围栏
    geofences.forEach(geofence => {
        if (geofence.status === '启用') {
            const circle = new AMap.Circle({
                center: [geofence.longitude, geofence.latitude],
                radius: geofence.radius,
                strokeColor: '#FF6B6B',
                strokeOpacity: 0.8,
                strokeWeight: 2,
                fillColor: '#FF6B6B',
                fillOpacity: 0.2
            });

            map.add(circle);
            geofenceCircles.push(circle);

            // 添加围栏名称标签
            const label = new AMap.Marker({
                position: [geofence.longitude, geofence.latitude],
                content: `<div style="background: rgba(255, 107, 107, 0.8); color: white; padding: 4px 8px; border-radius: 4px; font-size: 12px; white-space: nowrap;">${geofence.name}</div>`,
                offset: new AMap.Pixel(0, 0)
            });
            map.add(label);
            geofenceCircles.push(label);
        }
    });
}

/**
 * 清除地图上的围栏
 */
function clearGeofenceCircles() {
    if (map && geofenceCircles.length > 0) {
        geofenceCircles.forEach(circle => {
            map.remove(circle);
        });
        geofenceCircles = [];
    }
}

/**
 * 加载警报历史
 */
async function loadAlertHistory() {
    if (!currentElderlyId) {
        return;
    }

    try {
        // 从后端API获取真实警报数据
        const result = await get(`/family/location/alerts/${currentElderlyId}`);
        if (result.code === 200 && result.data) {
            // 格式化时间
            const formattedAlerts = result.data.map(alert => ({
                time: alert.time ? new Date(alert.time).toLocaleString('zh-CN') : '-',
                content: alert.content || '-'
            }));
            displayAlertHistory(formattedAlerts);
        } else {
            // 如果没有警报数据，显示空列表
            displayAlertHistory([]);
        }
    } catch (error) {
        console.error('加载警报历史失败:', error);
    }
}

/**
 * 显示警报历史
 */
function displayAlertHistory(alerts) {
    const alertList = document.getElementById('alertHistoryList');
    
    if (!alerts || alerts.length === 0) {
        alertList.innerHTML = '<div style="text-align: center; padding: 40px; color: var(--color-text-gray);">暂无警报记录</div>';
        return;
    }

    alertList.innerHTML = alerts.map(alert => `
        <div class="alert-item">
            <div class="time">${alert.time}</div>
            <div class="content">${alert.content}</div>
        </div>
    `).join('');
}

/**
 * 显示添加围栏模态框
 */
function showAddGeofenceModal() {
    if (!currentElderlyId) {
        alert('请先选择老人');
        return;
    }
    const modal = document.getElementById('addGeofenceModal');
    // 重置为添加模式
    modal.removeAttribute('data-editing-id');
    const modalTitle = modal.querySelector('.modal-header h2');
    if (modalTitle) {
        modalTitle.textContent = '添加电子围栏';
    }
    const saveBtn = modal.querySelector('.modal-footer .btn-primary');
    if (saveBtn) {
        saveBtn.textContent = '保存';
    }
    modal.style.display = 'block';
    isSelectingLocation = false;
}

/**
 * 关闭添加围栏模态框
 */
function closeAddGeofenceModal() {
    const modal = document.getElementById('addGeofenceModal');
    modal.style.display = 'none';
    isSelectingLocation = false;
    // 清空表单和编辑ID
    document.getElementById('geofenceName').value = '';
    document.getElementById('geofenceLat').value = '';
    document.getElementById('geofenceLng').value = '';
    document.getElementById('geofenceRadius').value = '';
    modal.removeAttribute('data-editing-id');
    // 更新按钮文本
    const saveBtn = modal.querySelector('.modal-footer .btn-primary');
    if (saveBtn) {
        saveBtn.textContent = '保存';
    }
}

/**
 * 在地图上选择位置
 */
function selectLocationOnMap() {
    if (!map) {
        alert('地图未初始化，请稍后再试');
        return;
    }
    isSelectingLocation = true;
    alert('请在地图上点击选择围栏中心位置');
}

/**
 * 保存围栏
 */
async function saveGeofence() {
    const name = document.getElementById('geofenceName').value.trim();
    const lat = document.getElementById('geofenceLat').value;
    const lng = document.getElementById('geofenceLng').value;
    const radius = document.getElementById('geofenceRadius').value;

    if (!name || !lat || !lng || !radius) {
        alert('请填写完整信息');
        return;
    }

    const latNum = parseFloat(lat);
    const lngNum = parseFloat(lng);
    const radiusNum = parseInt(radius);

    if (isNaN(latNum) || isNaN(lngNum) || isNaN(radiusNum)) {
        alert('请输入有效的数值');
        return;
    }

    if (latNum < -90 || latNum > 90) {
        alert('纬度范围应在 -90 到 90 之间');
        return;
    }

    if (lngNum < -180 || lngNum > 180) {
        alert('经度范围应在 -180 到 180 之间');
        return;
    }

    if (radiusNum < 10 || radiusNum > 10000) {
        alert('围栏半径应在 10 到 10000 米之间');
        return;
    }

    try {
        const modal = document.getElementById('addGeofenceModal');
        const editingId = modal.getAttribute('data-editing-id');
        
        let result;
        if (editingId) {
            // 更新围栏
            result = await put(`/family/location/geofence/${editingId}`, {
                name: name,
                latitude: latNum,
                longitude: lngNum,
                radius: radiusNum,
                status: '启用'
            });
        } else {
            // 创建新围栏
            result = await post('/family/location/geofence', {
                elderlyId: currentElderlyId,
                name: name,
                latitude: latNum,
                longitude: lngNum,
                radius: radiusNum
            });
        }
        
        if (result.code === 200) {
            alert(editingId ? '围栏更新成功' : '围栏保存成功');
            closeAddGeofenceModal();
            await loadGeofenceList();
        } else {
            alert('保存失败：' + (result.message || '未知错误'));
        }
    } catch (error) {
        console.error('保存围栏失败:', error);
        alert('保存失败，请稍后重试');
    }
}

/**
 * 编辑围栏
 */
async function editGeofence(id) {
    if (!currentElderlyId) {
        alert('请先选择老人');
        return;
    }
    
    try {
        // 从围栏列表中查找要编辑的围栏
        const result = await get(`/family/location/geofence/${currentElderlyId}`);
        if (result.code === 200 && result.data) {
            const geofence = result.data.find(g => g.id === id);
            if (!geofence) {
                alert('未找到要编辑的围栏');
                return;
            }
            
            // 填充表单
            document.getElementById('geofenceName').value = geofence.name || '';
            document.getElementById('geofenceLat').value = geofence.latitude || '';
            document.getElementById('geofenceLng').value = geofence.longitude || '';
            document.getElementById('geofenceRadius').value = geofence.radius || '';
            
            // 保存编辑的围栏ID
            const modal = document.getElementById('addGeofenceModal');
            modal.setAttribute('data-editing-id', id);
            
            // 更新模态框标题和按钮文本
            const modalTitle = modal.querySelector('.modal-header h2');
            if (modalTitle) {
                modalTitle.textContent = '编辑电子围栏';
            }
            const saveBtn = modal.querySelector('.modal-footer .btn-primary');
            if (saveBtn) {
                saveBtn.textContent = '更新';
            }
            
            // 显示模态框
            modal.style.display = 'block';
            isSelectingLocation = false;
        } else {
            alert('获取围栏信息失败');
        }
    } catch (error) {
        console.error('编辑围栏失败:', error);
        alert('编辑围栏失败，请稍后重试');
    }
}

/**
 * 删除围栏
 */
async function deleteGeofence(id) {
    if (!confirm('确定要删除这个围栏吗？')) {
        return;
    }

    try {
        // 调用真实的删除接口
        const result = await del(`/family/location/geofence/${id}`);
        if (result.code === 200) {
            alert('围栏删除成功');
            await loadGeofenceList();
        } else {
            alert('删除失败：' + (result.message || '未知错误'));
        }
    } catch (error) {
        console.error('删除围栏失败:', error);
        alert('删除失败，请稍后重试');
    }
}

// 点击模态框外部关闭
window.onclick = function(event) {
    const modal = document.getElementById('addGeofenceModal');
    if (event.target === modal) {
        closeAddGeofenceModal();
    }
}
