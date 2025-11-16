-- 快速授权脚本 - 为多个组员批量授权
-- 使用方法: 修改下面的用户名列表，然后执行
USE elderly_care;
GO

PRINT '========================================';
PRINT '批量授权脚本';
PRINT '========================================';
PRINT '';

-- ============================================================
-- 配置区域：在这里添加所有需要授权的组员登录名
-- ============================================================
DECLARE @users TABLE (username NVARCHAR(128));

-- 添加你的组员登录名（修改为实际的登录名）
INSERT INTO @users VALUES (N'LiMei');
INSERT INTO @users VALUES (N'taohy');

-- 继续添加更多组员...

-- ============================================================
-- 批量授权处理
-- ============================================================
DECLARE @username NVARCHAR(128);
DECLARE @sql NVARCHAR(MAX);
DECLARE @count INT = 0;

DECLARE user_cursor CURSOR FOR
    SELECT username FROM @users;

OPEN user_cursor;
FETCH NEXT FROM user_cursor INTO @username;

WHILE @@FETCH_STATUS = 0
BEGIN
    PRINT '------------------------------------';
    PRINT '处理用户: ' + @username;
    PRINT '------------------------------------';

    -- 创建数据库用户（如果不存在）
    IF NOT EXISTS (SELECT * FROM sys.database_principals WHERE name = @username)
    BEGIN
        BEGIN TRY
            SET @sql = N'CREATE USER [' + @username + N'] FOR LOGIN [' + @username + N'];';
            EXEC sp_executesql @sql;
            PRINT '✅ 创建数据库用户: ' + @username;
        END TRY
        BEGIN CATCH
            PRINT '❌ 创建用户失败: ' + ERROR_MESSAGE();
            PRINT '   请确保 SQL Server 登录名 [' + @username + '] 已存在';
        END CATCH
    END
    ELSE
    BEGIN
        PRINT '✓ 用户已存在';
    END

    -- 添加到数据读取角色
    BEGIN TRY
        EXEC sp_addrolemember 'db_datareader', @username;
        PRINT '✅ 已添加到 db_datareader 角色';
    END TRY
    BEGIN CATCH
        IF ERROR_NUMBER() = 15023 -- 用户已在角色中
            PRINT '✓ 已在 db_datareader 角色中';
        ELSE
            PRINT '❌ 添加角色失败: ' + ERROR_MESSAGE();
    END CATCH

    -- 添加到数据写入角色
    BEGIN TRY
        EXEC sp_addrolemember 'db_datawriter', @username;
        PRINT '✅ 已添加到 db_datawriter 角色';
    END TRY
    BEGIN CATCH
        IF ERROR_NUMBER() = 15023 -- 用户已在角色中
            PRINT '✓ 已在 db_datawriter 角色中';
        ELSE
            PRINT '❌ 添加角色失败: ' + ERROR_MESSAGE();
    END CATCH

    SET @count = @count + 1;
    PRINT '';

    FETCH NEXT FROM user_cursor INTO @username;
END

CLOSE user_cursor;
DEALLOCATE user_cursor;

PRINT '========================================';
PRINT '✅ 批量授权完成!';
PRINT '========================================';
PRINT '处理用户数: ' + CAST(@count AS NVARCHAR);
PRINT '';
PRINT '授予的权限:';
PRINT '- db_datareader: 读取所有表（包括未来新建的表）';
PRINT '- db_datawriter: 修改所有表数据（INSERT/UPDATE/DELETE）';
PRINT '';
PRINT '现在所有组员都可以访问 leave_request 等所有表了！';
PRINT '========================================';
GO

-- 查看所有用户的权限配置
PRINT '';
PRINT '当前数据库所有用户及其角色:';
PRINT '------------------------------------';
SELECT DISTINCT
    u.name AS '用户名',
    r.name AS '角色'
FROM sys.database_principals u
LEFT JOIN sys.database_role_members rm ON u.principal_id = rm.member_principal_id
LEFT JOIN sys.database_principals r ON rm.role_principal_id = r.principal_id
WHERE u.type IN ('S', 'U') -- SQL用户和Windows用户
    AND u.name NOT IN ('dbo', 'guest', 'INFORMATION_SCHEMA', 'sys')
    AND u.name NOT LIKE 'NT %'
ORDER BY u.name, r.name;
GO