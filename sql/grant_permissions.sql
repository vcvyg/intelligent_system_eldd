-- 为组员授予数据库权限脚本
-- 适用于智慧养老系统 elderly_care 数据库
USE elderly_care;
GO

PRINT '========================================';
PRINT '开始为组员配置数据库权限...';
PRINT '========================================';
PRINT '';

-- ============================================================
-- 第一步: 创建/更新数据库用户（如果需要）
-- ============================================================
-- 注意：请根据实际情况修改登录名和用户名

-- 示例：如果组员使用的是SQL Server登录名 'team_member1'
DECLARE @username NVARCHAR(128) = N'team_member1'; -- 修改为实际的登录名

-- 检查用户是否存在
IF NOT EXISTS (SELECT * FROM sys.database_principals WHERE name = @username)
BEGIN
    DECLARE @sql NVARCHAR(MAX);
    SET @sql = N'CREATE USER [' + @username + N'] FOR LOGIN [' + @username + N'];';
    EXEC sp_executesql @sql;
    PRINT '✅ 创建数据库用户: ' + @username;
END
ELSE
BEGIN
    PRINT '✓ 用户已存在: ' + @username;
END
GO

-- ============================================================
-- 第二步: 授予所有现有表的读写权限
-- ============================================================
PRINT '';
PRINT '正在授予所有表的读写权限...';

DECLARE @username NVARCHAR(128) = N'team_member1'; -- 修改为实际的用户名
DECLARE @sql NVARCHAR(MAX) = '';

-- 为所有用户表授予SELECT, INSERT, UPDATE, DELETE权限
SELECT @sql = @sql +
    'GRANT SELECT, INSERT, UPDATE, DELETE ON ' +
    QUOTENAME(SCHEMA_NAME(schema_id)) + '.' + QUOTENAME(name) +
    ' TO [' + @username + '];' + CHAR(13) + CHAR(10)
FROM sys.tables
WHERE is_ms_shipped = 0; -- 只包括用户创建的表

-- 执行授权语句
IF LEN(@sql) > 0
BEGIN
    EXEC sp_executesql @sql;
    PRINT '✅ 已授予所有表的读写权限';
END
ELSE
BEGIN
    PRINT '⚠️ 没有找到需要授权的表';
END
GO

-- ============================================================
-- 第三步: 授予数据库级别的权限（推荐方案）
-- ============================================================
PRINT '';
PRINT '正在授予数据库级别权限...';

DECLARE @username NVARCHAR(128) = N'team_member1'; -- 修改为实际的用户名

-- 方案A: 添加到 db_datareader 和 db_datawriter 角色（推荐）
-- 这样可以自动访问所有表，包括未来新建的表
EXEC sp_addrolemember 'db_datareader', @username;
EXEC sp_addrolemember 'db_datawriter', @username;
PRINT '✅ 已添加到 db_datareader 和 db_datawriter 角色';
PRINT '   → 可以读取所有表（包括未来新建的表）';
PRINT '   → 可以写入所有表（INSERT/UPDATE/DELETE）';
GO

-- ============================================================
-- 第四步: 授予存储过程执行权限（如果有）
-- ============================================================
PRINT '';
PRINT '正在授予存储过程执行权限...';

DECLARE @username NVARCHAR(128) = N'team_member1'; -- 修改为实际的用户名
DECLARE @sql NVARCHAR(MAX) = '';

SELECT @sql = @sql +
    'GRANT EXECUTE ON ' +
    QUOTENAME(SCHEMA_NAME(schema_id)) + '.' + QUOTENAME(name) +
    ' TO [' + @username + '];' + CHAR(13) + CHAR(10)
FROM sys.procedures
WHERE is_ms_shipped = 0;

IF LEN(@sql) > 0
BEGIN
    EXEC sp_executesql @sql;
    PRINT '✅ 已授予所有存储过程的执行权限';
END
ELSE
BEGIN
    PRINT '✓ 没有需要授权的存储过程';
END
GO

-- ============================================================
-- 验证权限配置
-- ============================================================
PRINT '';
PRINT '========================================';
PRINT '验证权限配置...';
PRINT '========================================';

DECLARE @username NVARCHAR(128) = N'team_member1'; -- 修改为实际的用户名

-- 查看用户的角色成员身份
PRINT '';
PRINT '用户角色成员身份:';
SELECT
    u.name AS '用户名',
    r.name AS '角色名'
FROM sys.database_role_members rm
JOIN sys.database_principals u ON rm.member_principal_id = u.principal_id
JOIN sys.database_principals r ON rm.role_principal_id = r.principal_id
WHERE u.name = @username;

-- 查看用户的表权限
PRINT '';
PRINT '用户对表的权限:';
SELECT
    OBJECT_NAME(major_id) AS '表名',
    permission_name AS '权限类型'
FROM sys.database_permissions
WHERE grantee_principal_id = USER_ID(@username)
    AND class = 1; -- 对象权限

PRINT '';
PRINT '========================================';
PRINT '✅ 权限配置完成!';
PRINT '========================================';
PRINT '';
PRINT '说明:';
PRINT '1. db_datareader 角色: 可以读取所有表（包括未来新建的表）';
PRINT '2. db_datawriter 角色: 可以修改所有表数据（INSERT/UPDATE/DELETE）';
PRINT '3. 不包括 ALTER/DROP 等DDL权限，确保数据库结构安全';
PRINT '4. 组员现在可以正常访问所有表，包括新建的 leave_request 表';
PRINT '';
PRINT '如果有多个组员，请修改脚本中的 @username 变量并重新运行';
PRINT '========================================';
GO
