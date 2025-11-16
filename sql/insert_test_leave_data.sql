-- 插入测试请假申请数据
USE elderly_care;
GO

-- 先查询一个医护人员的ID（如果没有则创建一个测试用户）
DECLARE @medical_user_id BIGINT;

-- 查找第一个医护人员
SELECT TOP 1 @medical_user_id = id FROM sys_user WHERE role = 'MEDICAL' AND deleted = 0;

-- 如果没有医护人员，则创建一个测试医护人员
IF @medical_user_id IS NULL
BEGIN
    INSERT INTO sys_user (username, password, real_name, phone, role, create_time, update_time, deleted)
    VALUES (N'test_doctor', N'$2a$10$X5wFvzMbBvJ5K3P8nZQ3qOYqJ8qZ7kVGz1KvYqJ8qZ7kVGz1KvYqJ', N'测试医生', N'13800138000', N'MEDICAL', GETDATE(), GETDATE(), 0);

    SET @medical_user_id = SCOPE_IDENTITY();
    PRINT '✅ 创建测试医护人员，ID: ' + CAST(@medical_user_id AS NVARCHAR);
END
ELSE
BEGIN
    PRINT '✅ 找到医护人员，ID: ' + CAST(@medical_user_id AS NVARCHAR);
END

-- 插入多条测试请假申请数据
-- 1. 待审批的事假申请
INSERT INTO leave_request (medical_user_id, leave_type, start_date, end_date, days, reason, status, create_time, update_time)
VALUES (@medical_user_id, N'事假', '2025-11-18', '2025-11-19', 2, N'家中有事需要处理，需要请假两天。', N'待审批', GETDATE(), GETDATE());

-- 2. 待审批的病假申请
INSERT INTO leave_request (medical_user_id, leave_type, start_date, end_date, days, reason, status, create_time, update_time)
VALUES (@medical_user_id, N'病假', '2025-11-20', '2025-11-22', 3, N'身体不适，需要休息治疗。', N'待审批', GETDATE(), GETDATE());

-- 3. 待审批的年假申请
INSERT INTO leave_request (medical_user_id, leave_type, start_date, end_date, days, reason, status, create_time, update_time)
VALUES (@medical_user_id, N'年假', '2025-12-01', '2025-12-05', 5, N'计划年底休年假，出去旅游放松。', N'待审批', GETDATE(), GETDATE());

-- 4. 待审批的调休申请
INSERT INTO leave_request (medical_user_id, leave_type, start_date, end_date, days, reason, status, create_time, update_time)
VALUES (@medical_user_id, N'调休', '2025-11-25', '2025-11-25', 1, N'之前加班，需要调休一天。', N'待审批', GETDATE(), GETDATE());

-- 5. 已同意的请假申请（用于展示历史记录）
INSERT INTO leave_request (medical_user_id, leave_type, start_date, end_date, days, reason, status, review_time, review_remark, create_time, update_time)
VALUES (@medical_user_id, N'事假', '2025-11-10', '2025-11-11', 2, N'参加朋友婚礼。', N'已同意', GETDATE(), N'同意请假，注意安排好工作交接。', DATEADD(DAY, -3, GETDATE()), GETDATE());

-- 6. 已拒绝的请假申请（用于展示历史记录）
INSERT INTO leave_request (medical_user_id, leave_type, start_date, end_date, days, reason, status, review_time, review_remark, create_time, update_time)
VALUES (@medical_user_id, N'年假', '2025-11-15', '2025-11-20', 6, N'想休息一段时间。', N'已拒绝', GETDATE(), N'该时间段人员紧张，暂时无法批准。', DATEADD(DAY, -2, GETDATE()), GETDATE());

PRINT '';
PRINT '========================================';
PRINT '✅ 测试数据插入完成!';
PRINT '========================================';
PRINT '插入的数据包括:';
PRINT '- 4条待审批申请（事假、病假、年假、调休）';
PRINT '- 1条已同意申请';
PRINT '- 1条已拒绝申请';
PRINT '共计: 6条测试记录';
PRINT '========================================';
GO

-- 查询插入的数据
SELECT
    lr.id,
    u.real_name AS '申请人',
    u.username AS '用户名',
    lr.leave_type AS '请假类型',
    lr.start_date AS '开始日期',
    lr.end_date AS '结束日期',
    lr.days AS '天数',
    lr.reason AS '申请原因',
    lr.status AS '状态',
    lr.review_remark AS '审批意见',
    lr.create_time AS '申请时间'
FROM leave_request lr
JOIN sys_user u ON lr.medical_user_id = u.id
WHERE lr.deleted = 0
ORDER BY lr.create_time DESC;
GO