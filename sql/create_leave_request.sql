-- 创建请假/调休申请表
USE elderly_care;
GO

-- 创建请假调休申请表
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'leave_request')
BEGIN
    CREATE TABLE leave_request (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        medical_user_id BIGINT NOT NULL, -- 申请人ID(医护人员)
        leave_type NVARCHAR(20) NOT NULL, -- 请假类型: 事假/病假/年假/调休
        start_date DATE NOT NULL, -- 开始日期
        end_date DATE NOT NULL, -- 结束日期
        days DECIMAL(4,1) NOT NULL, -- 请假天数
        reason NVARCHAR(MAX) NOT NULL, -- 请假原因
        status NVARCHAR(20) DEFAULT N'待审批', -- 状态: 待审批/已同意/已拒绝
        reviewer_id BIGINT, -- 审批人ID
        review_time DATETIME2, -- 审批时间
        review_remark NVARCHAR(MAX), -- 审批备注
        create_time DATETIME2 DEFAULT GETDATE(),
        update_time DATETIME2 DEFAULT GETDATE(),
        deleted INT DEFAULT 0,
        FOREIGN KEY (medical_user_id) REFERENCES sys_user(id)
    );
    PRINT '✅ 请假调休申请表创建成功';
END
ELSE
BEGIN
    PRINT '⚠️ 请假调休申请表已存在';
END
GO

-- 创建索引
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_leave_medical_user_id')
    CREATE INDEX idx_leave_medical_user_id ON leave_request(medical_user_id);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_leave_status')
    CREATE INDEX idx_leave_status ON leave_request(status);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_leave_date_range')
    CREATE INDEX idx_leave_date_range ON leave_request(start_date, end_date);

PRINT '✅ 索引创建完成';
GO

PRINT '';
PRINT '========================================';
PRINT '✅ 请假调休申请表创建完成!';
PRINT '========================================';
PRINT '表结构:';
PRINT '- id: 主键';
PRINT '- medical_user_id: 申请人(医护人员)ID';
PRINT '- leave_type: 请假类型(事假/病假/年假/调休)';
PRINT '- start_date: 开始日期';
PRINT '- end_date: 结束日期';
PRINT '- days: 请假天数';
PRINT '- reason: 请假原因';
PRINT '- status: 审批状态(待审批/已同意/已拒绝)';
PRINT '- reviewer_id: 审批人ID';
PRINT '- review_time: 审批时间';
PRINT '- review_remark: 审批备注';
PRINT '========================================';
GO
