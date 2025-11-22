-- ========================================
-- 修复聊天消息表时间字段问题
-- 移除数据库默认时间，改为应用程序控制
-- ========================================

USE elderly_care;
GO

PRINT '开始修复聊天消息表时间字段...';

-- 检查表是否存在
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'chat_message')
BEGIN
    PRINT '找到chat_message表，开始修复时间字段...';
    
    -- 移除create_time字段的默认约束
    DECLARE @constraint_name NVARCHAR(128);
    SELECT @constraint_name = dc.name
    FROM sys.default_constraints dc
    INNER JOIN sys.columns c ON dc.parent_column_id = c.column_id
    INNER JOIN sys.tables t ON dc.parent_object_id = t.object_id
    WHERE t.name = 'chat_message' AND c.name = 'create_time';
    
    IF @constraint_name IS NOT NULL
    BEGIN
        EXEC('ALTER TABLE chat_message DROP CONSTRAINT ' + @constraint_name);
        PRINT '✅ 移除create_time默认约束: ' + @constraint_name;
    END
    
    -- 移除update_time字段的默认约束
    SET @constraint_name = NULL;
    SELECT @constraint_name = dc.name
    FROM sys.default_constraints dc
    INNER JOIN sys.columns c ON dc.parent_column_id = c.column_id
    INNER JOIN sys.tables t ON dc.parent_object_id = t.object_id
    WHERE t.name = 'chat_message' AND c.name = 'update_time';
    
    IF @constraint_name IS NOT NULL
    BEGIN
        EXEC('ALTER TABLE chat_message DROP CONSTRAINT ' + @constraint_name);
        PRINT '✅ 移除update_time默认约束: ' + @constraint_name;
    END
    
    -- 修改字段为允许NULL（应用程序会设置值）
    ALTER TABLE chat_message ALTER COLUMN create_time DATETIME2 NULL;
    ALTER TABLE chat_message ALTER COLUMN update_time DATETIME2 NULL;
    
    PRINT '✅ 时间字段修改完成，现在由应用程序控制时间设置';
    
    -- 显示当前时间对比
    PRINT '';
    PRINT '时间对比检查:';
    PRINT '数据库服务器时间: ' + CONVERT(NVARCHAR, GETDATE(), 120);
    PRINT '应用程序应该使用本地时间而不是数据库时间';
    
END
ELSE
BEGIN
    PRINT '❌ 未找到chat_message表';
END

PRINT '';
PRINT '========================================';
PRINT '✅ 聊天消息表时间字段修复完成!';
PRINT '========================================';
PRINT '修复内容:';
PRINT '1. 移除create_time和update_time的数据库默认值';
PRINT '2. 改为由MyBatis-Plus自动填充处理器设置时间';
PRINT '3. 确保使用应用服务器的本地时间';
PRINT '========================================';
GO