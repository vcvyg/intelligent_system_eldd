-- 优化老人信息查询性能的索引

-- 1. 为搜索功能创建复合索引
-- 支持按姓名、身份证号、紧急联系人搜索
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_elderly_search')
BEGIN
    CREATE INDEX idx_elderly_search ON elderly_info(name, id_card, emergency_contact) 
    WHERE deleted = 0;
    PRINT '✅ 已创建老人信息搜索索引';
END

-- 2. 为分页查询优化创建时间索引
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_elderly_create_time')
BEGIN
    CREATE INDEX idx_elderly_create_time ON elderly_info(create_time DESC, deleted) 
    WHERE deleted = 0;
    PRINT '✅ 已创建老人信息创建时间索引';
END

-- 3. 优化房间关联查询的索引（如果不存在）
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_elderly_room_id')
BEGIN
    CREATE INDEX idx_elderly_room_id ON elderly_info(room_id) 
    WHERE deleted = 0 AND room_id IS NOT NULL;
    PRINT '✅ 已创建老人房间关联索引';
END

-- 4. 为姓名字段单独创建索引（支持模糊搜索）
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_elderly_name')
BEGIN
    CREATE INDEX idx_elderly_name ON elderly_info(name) 
    WHERE deleted = 0;
    PRINT '✅ 已创建老人姓名索引';
END

-- 5. 为身份证号创建唯一索引（如果不存在）
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_elderly_id_card_unique')
BEGIN
    CREATE UNIQUE INDEX idx_elderly_id_card_unique ON elderly_info(id_card) 
    WHERE deleted = 0 AND id_card IS NOT NULL AND id_card != '';
    PRINT '✅ 已创建身份证号唯一索引';
END

-- 6. 为紧急联系人创建索引
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_elderly_emergency_contact')
BEGIN
    CREATE INDEX idx_elderly_emergency_contact ON elderly_info(emergency_contact) 
    WHERE deleted = 0 AND emergency_contact IS NOT NULL;
    PRINT '✅ 已创建紧急联系人索引';
END

-- 7. 创建覆盖索引，包含常用查询字段
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_elderly_list_covering')
BEGIN
    CREATE INDEX idx_elderly_list_covering ON elderly_info(id, name, age, gender, room_id, emergency_contact, emergency_phone, create_time) 
    WHERE deleted = 0;
    PRINT '✅ 已创建老人列表覆盖索引';
END

PRINT '🎉 老人信息查询性能优化完成！';
GO