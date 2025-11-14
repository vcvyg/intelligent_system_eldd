-- ========================================
-- 智慧养老系统 - 新增功能模块数据库脚本
-- 新增: 设备管理、预警管理、操作日志
-- ========================================

USE elderly_care;
GO

-- 1. 创建设备信息表
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'device_info')
BEGIN
    CREATE TABLE device_info (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        device_code NVARCHAR(50) NOT NULL UNIQUE, -- 设备编号
        device_name NVARCHAR(100) NOT NULL, -- 设备名称
        device_type NVARCHAR(50) NOT NULL, -- 设备类型: 心率监测器/血压计/血糖仪/定位设备/体温计
        manufacturer NVARCHAR(100), -- 生产厂商
        model NVARCHAR(50), -- 设备型号
        elderly_id BIGINT, -- 绑定的老人ID
        status NVARCHAR(20) DEFAULT N'在线', -- 设备状态: 在线/离线/故障
        last_sync_time DATETIME2, -- 最后同步时间
        purchase_date DATE, -- 购买日期
        warranty_expire_date DATE, -- 保修到期日期
        remark NVARCHAR(MAX), -- 备注
        create_time DATETIME2 DEFAULT GETDATE(),
        update_time DATETIME2 DEFAULT GETDATE(),
        deleted INT DEFAULT 0,
        FOREIGN KEY (elderly_id) REFERENCES elderly_info(id)
    );
    PRINT '✅ 设备信息表创建成功';
END
ELSE
BEGIN
    PRINT '⚠️ 设备信息表已存在';
END
GO

-- 2. 创建预警记录表
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'alert_record')
BEGIN
    CREATE TABLE alert_record (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        elderly_id BIGINT NOT NULL, -- 老人ID
        alert_type NVARCHAR(50) NOT NULL, -- 预警类型: 心率异常/血压异常/血糖异常/体温异常/跌倒/离家
        alert_level NVARCHAR(20) NOT NULL, -- 预警等级: 低/中/高/紧急
        alert_content NVARCHAR(MAX) NOT NULL, -- 预警内容详情
        alert_value NVARCHAR(100), -- 异常数值
        alert_time DATETIME2 DEFAULT GETDATE(), -- 预警时间
        device_id BIGINT, -- 触发设备ID
        status NVARCHAR(20) DEFAULT N'待处理', -- 处理状态: 待处理/处理中/已处理/已忽略
        assigned_medical_id BIGINT, -- 分配的医护人员ID
        handle_time DATETIME2, -- 处理时间
        handle_result NVARCHAR(MAX), -- 处理结果
        create_time DATETIME2 DEFAULT GETDATE(),
        update_time DATETIME2 DEFAULT GETDATE(),
        deleted INT DEFAULT 0,
        FOREIGN KEY (elderly_id) REFERENCES elderly_info(id),
        FOREIGN KEY (device_id) REFERENCES device_info(id),
        FOREIGN KEY (assigned_medical_id) REFERENCES sys_user(id)
    );
    PRINT '✅ 预警记录表创建成功';
END
ELSE
BEGIN
    PRINT '⚠️ 预警记录表已存在';
END
GO

-- 3. 创建操作日志表
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'operation_log')
BEGIN
    CREATE TABLE operation_log (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT, -- 操作用户ID
        username NVARCHAR(50), -- 操作用户名
        operation_type NVARCHAR(50) NOT NULL, -- 操作类型: 新增/修改/删除/查询/登录/登出
        operation_module NVARCHAR(50) NOT NULL, -- 操作模块: 用户管理/老人管理/设备管理/预警管理
        operation_desc NVARCHAR(MAX), -- 操作描述
        request_method NVARCHAR(10), -- 请求方法: GET/POST/PUT/DELETE
        request_url NVARCHAR(500), -- 请求URL
        request_param NVARCHAR(MAX), -- 请求参数
        response_result NVARCHAR(MAX), -- 响应结果
        ip_address NVARCHAR(50), -- IP地址
        user_agent NVARCHAR(MAX), -- 用户代理
        execute_time BIGINT, -- 执行时长(毫秒)
        status NVARCHAR(20) DEFAULT N'成功', -- 状态: 成功/失败
        error_msg NVARCHAR(MAX), -- 错误信息
        create_time DATETIME2 DEFAULT GETDATE(),
        FOREIGN KEY (user_id) REFERENCES sys_user(id)
    );
    PRINT '✅ 操作日志表创建成功';
END
ELSE
BEGIN
    PRINT '⚠️ 操作日志表已存在';
END
GO

-- 4. 创建索引
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_device_code')
    CREATE INDEX idx_device_code ON device_info(device_code);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_device_elderly_id')
    CREATE INDEX idx_device_elderly_id ON device_info(elderly_id);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_alert_elderly_id')
    CREATE INDEX idx_alert_elderly_id ON alert_record(elderly_id);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_alert_time')
    CREATE INDEX idx_alert_time ON alert_record(alert_time);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_alert_status')
    CREATE INDEX idx_alert_status ON alert_record(status);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_log_user_id')
    CREATE INDEX idx_log_user_id ON operation_log(user_id);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_log_create_time')
    CREATE INDEX idx_log_create_time ON operation_log(create_time);

PRINT '✅ 索引创建完成';
GO

PRINT '';
PRINT '========================================';
PRINT '✅ 新增模块数据库表创建完成!';
PRINT '========================================';
PRINT '新增表: ';
PRINT '1. device_info - 设备信息表';
PRINT '2. alert_record - 预警记录表';
PRINT '3. operation_log - 操作日志表';
PRINT '';
PRINT '建议执行以下插入示例数据:';
PRINT '========================================';
GO

-- 5. 插入示例数据
-- 示例设备数据
IF NOT EXISTS (SELECT * FROM device_info WHERE device_code = 'DEV001')
BEGIN
    INSERT INTO device_info (device_code, device_name, device_type, manufacturer, model, elderly_id, status, last_sync_time, purchase_date, warranty_expire_date)
    VALUES
    (N'DEV001', N'心率监测器-01', N'心率监测器', N'华为健康', N'HW-HR-100', NULL, N'在线', GETDATE(), '2024-01-15', '2026-01-15'),
    (N'DEV002', N'血压计-01', N'血压计', N'欧姆龙', N'OM-BP-200', NULL, N'在线', GETDATE(), '2024-02-20', '2026-02-20'),
    (N'DEV003', N'血糖仪-01', N'血糖仪', N'罗氏', N'RC-BG-300', NULL, N'离线', GETDATE(), '2024-03-10', '2026-03-10'),
    (N'DEV004', N'智能手环-01', N'定位设备', N'小米', N'MI-Band-8', NULL, N'在线', GETDATE(), '2024-04-05', '2025-04-05');

    PRINT '✅ 示例设备数据插入完成';
END
ELSE
BEGIN
    PRINT '⚠️ 示例设备数据已存在';
END
GO

-- 示例预警数据 (需要先有老人数据)
-- INSERT INTO alert_record (elderly_id, alert_type, alert_level, alert_content, alert_value, device_id)
-- VALUES
-- (1, '心率异常', '高', '检测到心率过高,请及时关注', '120 bpm', 1),
-- (1, '血压异常', '中', '收缩压偏高,建议休息', '150/90 mmHg', 2);

PRINT '';
PRINT '========================================';
PRINT '✅ 所有操作完成!';
PRINT '========================================';
GO
