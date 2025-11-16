-- ========================================
-- 修复 elderly_info 表结构
-- 将 room_id 字段移到正确的位置
-- ========================================

USE elderly_care;
GO

PRINT '========================================';
PRINT '开始修复 elderly_info 表结构...';
PRINT '========================================';

-- 1. 备份现有数据到临时表
IF OBJECT_ID('elderly_info_backup', 'U') IS NOT NULL
    DROP TABLE elderly_info_backup;
GO

SELECT * INTO elderly_info_backup FROM elderly_info;
PRINT '✅ 已备份现有数据';
GO

-- 2. 删除外键约束
DECLARE @constraint_name NVARCHAR(200);

-- 删除 elderly_info 的外键约束
SELECT @constraint_name = name
FROM sys.foreign_keys
WHERE parent_object_id = OBJECT_ID('elderly_info')
AND referenced_object_id = OBJECT_ID('room');

IF @constraint_name IS NOT NULL
BEGIN
    EXEC('ALTER TABLE elderly_info DROP CONSTRAINT ' + @constraint_name);
    PRINT '✅ 已删除 room_id 外键约束';
END

SELECT @constraint_name = name
FROM sys.foreign_keys
WHERE parent_object_id = OBJECT_ID('elderly_info')
AND referenced_object_id = OBJECT_ID('sys_user');

IF @constraint_name IS NOT NULL
BEGIN
    EXEC('ALTER TABLE elderly_info DROP CONSTRAINT ' + @constraint_name);
    PRINT '✅ 已删除 user_id 外键约束';
END
GO

-- 3. 删除依赖 elderly_info 的外键约束
DECLARE @fk_name NVARCHAR(200);
DECLARE fk_cursor CURSOR FOR
SELECT name FROM sys.foreign_keys
WHERE referenced_object_id = OBJECT_ID('elderly_info');

OPEN fk_cursor;
FETCH NEXT FROM fk_cursor INTO @fk_name;

WHILE @@FETCH_STATUS = 0
BEGIN
    DECLARE @table_name NVARCHAR(200);
    SELECT @table_name = OBJECT_NAME(parent_object_id)
    FROM sys.foreign_keys
    WHERE name = @fk_name;

    EXEC('ALTER TABLE ' + @table_name + ' DROP CONSTRAINT ' + @fk_name);
    PRINT '✅ 已删除外键约束: ' + @fk_name;

    FETCH NEXT FROM fk_cursor INTO @fk_name;
END

CLOSE fk_cursor;
DEALLOCATE fk_cursor;
GO

-- 4. 删除旧表
DROP TABLE elderly_info;
PRINT '✅ 已删除旧表';
GO

-- 5. 创建新表（正确的列顺序）
CREATE TABLE elderly_info (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT,
    name NVARCHAR(50) NOT NULL,
    age INT,
    gender NVARCHAR(10),
    birthday DATE,
    id_card NVARCHAR(18),
    address NVARCHAR(255),
    room_id BIGINT NULL,  -- room_id 放在这里，而不是最后
    emergency_contact NVARCHAR(50),
    emergency_phone NVARCHAR(20),
    medical_history NVARCHAR(MAX),
    allergy_history NVARCHAR(MAX),
    create_time DATETIME2 DEFAULT GETDATE(),
    update_time DATETIME2 DEFAULT GETDATE(),
    deleted INT DEFAULT 0,
    CONSTRAINT FK_elderly_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT FK_elderly_room FOREIGN KEY (room_id) REFERENCES room(id)
);
PRINT '✅ 已创建新表结构';
GO

-- 6. 恢复数据
SET IDENTITY_INSERT elderly_info ON;

INSERT INTO elderly_info (
    id, user_id, name, age, gender, birthday, id_card, address,
    room_id, emergency_contact, emergency_phone, medical_history,
    allergy_history, create_time, update_time, deleted
)
SELECT
    id, user_id, name, age, gender, birthday, id_card, address,
    room_id, emergency_contact, emergency_phone, medical_history,
    allergy_history, create_time, update_time, deleted
FROM elderly_info_backup;

SET IDENTITY_INSERT elderly_info OFF;
PRINT '✅ 已恢复数据';
GO

-- 7. 重新创建索引
CREATE INDEX idx_elderly_user_id ON elderly_info(user_id);
CREATE INDEX idx_elderly_room_id ON elderly_info(room_id);
CREATE INDEX idx_elderly_id_card ON elderly_info(id_card);
PRINT '✅ 已重建索引';
GO

-- 8. 重新创建其他表的外键约束
-- health_data 表
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'health_data')
BEGIN
    ALTER TABLE health_data
    ADD CONSTRAINT FK_health_elderly FOREIGN KEY (elderly_id) REFERENCES elderly_info(id);
    PRINT '✅ 已重建 health_data 外键约束';
END
GO

-- elderly_family_relation 表
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'elderly_family_relation')
BEGIN
    ALTER TABLE elderly_family_relation
    ADD CONSTRAINT FK_family_elderly FOREIGN KEY (elderly_id) REFERENCES elderly_info(id);
    PRINT '✅ 已重建 elderly_family_relation 外键约束';
END
GO

-- elderly_medical_relation 表
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'elderly_medical_relation')
BEGIN
    ALTER TABLE elderly_medical_relation
    ADD CONSTRAINT FK_medical_elderly FOREIGN KEY (elderly_id) REFERENCES elderly_info(id);
    PRINT '✅ 已重建 elderly_medical_relation 外键约束';
END
GO

-- device_info 表
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'device_info')
BEGIN
    IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('device_info') AND name = 'elderly_id')
    BEGIN
        ALTER TABLE device_info
        ADD CONSTRAINT FK_device_elderly FOREIGN KEY (elderly_id) REFERENCES elderly_info(id);
        PRINT '✅ 已重建 device_info 外键约束';
    END
END
GO

-- 9. 验证数据
SELECT COUNT(*) AS record_count FROM elderly_info;
PRINT '✅ 数据恢复完成';
GO

-- 10. 查看表结构
EXEC sp_help 'elderly_info';
GO

PRINT '';
PRINT '========================================';
PRINT '✅ elderly_info 表结构修复完成！';
PRINT '========================================';

