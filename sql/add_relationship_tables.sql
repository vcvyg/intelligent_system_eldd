-- ========================================
-- 智慧养老系统 - 关联关系表
-- 新增: 老人-子女关系、老人-医护关系、医护排班、预警通知记录
-- ========================================

USE elderly_care;
GO

-- 1. 创建老人-子女关系表
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'elderly_family_relation')
BEGIN
    CREATE TABLE elderly_family_relation (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        elderly_id BIGINT NOT NULL, -- 老人ID
        family_user_id BIGINT NOT NULL, -- 子女用户ID
        relation_type NVARCHAR(20) NOT NULL, -- 关系类型: 子女/儿媳/女婿/孙子/孙女等
        is_primary_contact INT DEFAULT 0, -- 是否主要联系人 0-否 1-是
        create_time DATETIME2 DEFAULT GETDATE(),
        update_time DATETIME2 DEFAULT GETDATE(),
        deleted INT DEFAULT 0,
        FOREIGN KEY (elderly_id) REFERENCES elderly_info(id),
        FOREIGN KEY (family_user_id) REFERENCES sys_user(id)
    );
    PRINT '✅ 老人-子女关系表创建成功';
END
ELSE
BEGIN
    PRINT '⚠️ 老人-子女关系表已存在';
END
GO

-- 2. 创建老人-医护人员关系表
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'elderly_medical_relation')
BEGIN
    CREATE TABLE elderly_medical_relation (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        elderly_id BIGINT NOT NULL, -- 老人ID
        medical_user_id BIGINT NOT NULL, -- 医护人员ID
        is_primary_doctor INT DEFAULT 0, -- 是否主治医生 0-否 1-是
        assign_date DATETIME2 DEFAULT GETDATE(), -- 分配日期
        remark NVARCHAR(MAX), -- 备注
        create_time DATETIME2 DEFAULT GETDATE(),
        update_time DATETIME2 DEFAULT GETDATE(),
        deleted INT DEFAULT 0,
        FOREIGN KEY (elderly_id) REFERENCES elderly_info(id),
        FOREIGN KEY (medical_user_id) REFERENCES sys_user(id)
    );
    PRINT '✅ 老人-医护人员关系表创建成功';
END
ELSE
BEGIN
    PRINT '⚠️ 老人-医护人员关系表已存在';
END
GO

-- 3. 创建医护人员排班表
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'medical_schedule')
BEGIN
    CREATE TABLE medical_schedule (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        medical_user_id BIGINT NOT NULL, -- 医护人员ID
        schedule_date DATE NOT NULL, -- 排班日期
        shift_type NVARCHAR(20) NOT NULL, -- 班次类型: 早班/中班/晚班/夜班
        start_time TIME NOT NULL, -- 开始时间
        end_time TIME NOT NULL, -- 结束时间
        status NVARCHAR(20) DEFAULT N'正常', -- 状态: 正常/请假/调班
        remark NVARCHAR(MAX), -- 备注
        create_time DATETIME2 DEFAULT GETDATE(),
        update_time DATETIME2 DEFAULT GETDATE(),
        deleted INT DEFAULT 0,
        FOREIGN KEY (medical_user_id) REFERENCES sys_user(id)
    );
    PRINT '✅ 医护人员排班表创建成功';
END
ELSE
BEGIN
    PRINT '⚠️ 医护人员排班表已存在';
END
GO

-- 4. 创建预警通知记录表
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'alert_notification')
BEGIN
    CREATE TABLE alert_notification (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        alert_id BIGINT NOT NULL, -- 预警ID
        receiver_user_id BIGINT NOT NULL, -- 接收者用户ID
        notification_type NVARCHAR(20) NOT NULL, -- 通知类型: 短信/邮件/系统通知
        receiver_contact NVARCHAR(100) NOT NULL, -- 接收者联系方式(手机号或邮箱)
        notification_content NVARCHAR(MAX) NOT NULL, -- 通知内容
        send_status NVARCHAR(20) DEFAULT N'待发送', -- 发送状态: 待发送/发送中/发送成功/发送失败
        send_time DATETIME2, -- 发送时间
        error_msg NVARCHAR(MAX), -- 错误信息
        create_time DATETIME2 DEFAULT GETDATE(),
        FOREIGN KEY (alert_id) REFERENCES alert_record(id),
        FOREIGN KEY (receiver_user_id) REFERENCES sys_user(id)
    );
    PRINT '✅ 预警通知记录表创建成功';
END
ELSE
BEGIN
    PRINT '⚠️ 预警通知记录表已存在';
END
GO

-- 5. 创建医护任务表
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'medical_task')
BEGIN
    CREATE TABLE medical_task (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        elderly_id BIGINT NOT NULL, -- 老人ID
        medical_user_id BIGINT NOT NULL, -- 负责医护人员ID
        task_type NVARCHAR(50) NOT NULL, -- 任务类型: 定期检查/用药提醒/康复训练/心理疏导等
        task_title NVARCHAR(200) NOT NULL, -- 任务标题
        task_content NVARCHAR(MAX), -- 任务内容
        priority NVARCHAR(20) DEFAULT N'普通', -- 优先级: 低/普通/高/紧急
        scheduled_time DATETIME2 NOT NULL, -- 计划执行时间
        status NVARCHAR(20) DEFAULT N'待处理', -- 状态: 待处理/进行中/已完成/已取消
        complete_time DATETIME2, -- 完成时间
        complete_note NVARCHAR(MAX), -- 完成备注
        create_time DATETIME2 DEFAULT GETDATE(),
        update_time DATETIME2 DEFAULT GETDATE(),
        deleted INT DEFAULT 0,
        FOREIGN KEY (elderly_id) REFERENCES elderly_info(id),
        FOREIGN KEY (medical_user_id) REFERENCES sys_user(id)
    );
    PRINT '✅ 医护任务表创建成功';
END
ELSE
BEGIN
    PRINT '⚠️ 医护任务表已存在';
END
GO

-- 6. 创建索引
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_elderly_family_elderly_id')
    CREATE INDEX idx_elderly_family_elderly_id ON elderly_family_relation(elderly_id);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_elderly_family_family_user_id')
    CREATE INDEX idx_elderly_family_family_user_id ON elderly_family_relation(family_user_id);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_elderly_medical_elderly_id')
    CREATE INDEX idx_elderly_medical_elderly_id ON elderly_medical_relation(elderly_id);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_elderly_medical_medical_user_id')
    CREATE INDEX idx_elderly_medical_medical_user_id ON elderly_medical_relation(medical_user_id);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_medical_schedule_user_id')
    CREATE INDEX idx_medical_schedule_user_id ON medical_schedule(medical_user_id);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_medical_schedule_date')
    CREATE INDEX idx_medical_schedule_date ON medical_schedule(schedule_date);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_alert_notification_alert_id')
    CREATE INDEX idx_alert_notification_alert_id ON alert_notification(alert_id);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_medical_task_elderly_id')
    CREATE INDEX idx_medical_task_elderly_id ON medical_task(elderly_id);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_medical_task_medical_user_id')
    CREATE INDEX idx_medical_task_medical_user_id ON medical_task(medical_user_id);

PRINT '✅ 索引创建完成';
GO

-- 7. 插入示例数据 (假设已有用户和老人数据)
-- 示例: 老人-子女关系
-- INSERT INTO elderly_family_relation (elderly_id, family_user_id, relation_type, is_primary_contact)
-- VALUES (1, 2, N'子女', 1);

-- 示例: 老人-医护关系
-- INSERT INTO elderly_medical_relation (elderly_id, medical_user_id, is_primary_doctor)
-- VALUES (1, 3, 1);

-- 示例: 医护排班
-- INSERT INTO medical_schedule (medical_user_id, schedule_date, shift_type, start_time, end_time)
-- VALUES (3, '2025-01-15', N'早班', '08:00:00', '16:00:00');

PRINT '';
PRINT '========================================';
PRINT '✅ 关联关系表创建完成!';
PRINT '========================================';
PRINT '新增表: ';
PRINT '1. elderly_family_relation - 老人子女关系表';
PRINT '2. elderly_medical_relation - 老人医护关系表';
PRINT '3. medical_schedule - 医护排班表';
PRINT '4. alert_notification - 预警通知记录表';
PRINT '5. medical_task - 医护任务表';
PRINT '';
PRINT '下一步: 开发对应的后端接口和前端页面';
PRINT '========================================';
GO
