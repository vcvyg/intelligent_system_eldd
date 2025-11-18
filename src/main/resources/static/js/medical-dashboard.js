document.addEventListener('DOMContentLoaded', function() {
    // 1. 检查登录状态
    const userInfo = checkLogin();
    if (!userInfo) {
        // checkLogin函数内部会处理跳转，这里只是为了确保后续代码不执行
        return;
    }

    // 2. 检查用户角色
    if (userInfo.role !== 'MEDICAL') {
        alert('您没有权限访问此页面。');
        logout(); // 角色不匹配，登出
        return;
    }

    // 3. 设置欢迎信息
    const welcomeText = document.getElementById('welcomeText');
    if (welcomeText) {
        welcomeText.textContent = `欢迎，${userInfo.username} (医护人员)`;
    }

    // 4. 调用测试接口
    async function testMedicalEndpoint() {
        try {
            const response = await get('/medical/test');
            if (response && response.code === 200) {
                console.log('医护端接口测试成功:', response.data);
                // 可以在页面上显示一个成功的提示
                const overview = document.querySelector('section h2');
                if(overview) {
                    const successMsg = document.createElement('p');
                    successMsg.textContent = '与服务器连接成功！';
                    successMsg.style.color = 'green';
                    overview.insertAdjacentElement('afterend', successMsg);
                }
            } else {
                console.error('医护端接口测试失败:', response);
            }
        } catch (error) {
            console.error('调用医护端接口时出错:', error);
        }
    }

    // 执行测试
    testMedicalEndpoint();

    // 后续可以在这里加载仪表盘的其他数据
    // e.g., loadDashboardData();
});
