-- 添加房间管理功能
-- 1. 创建房间表
-- 2. 为elderly_info表添加room_id字段
-- 3. 为medical_schedule表添加room_id字段

USE elderly_care;
GO

PRINT '========================================';
PRINT '开始添加房间管理功能...';
PRINT '========================================';
PRINT '';

-- ============================================================
-- 1. 创建房间表
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'room')
BEGIN
    CREATE TABLE room (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        room_number NVARCHAR(20) NOT NULL UNIQUE, -- 房间号(如: 101, 201, A-301)
        floor INT, -- 楼层
        room_type NVARCHAR(20), -- 房间类型: 单人间/双人间/三人间/VIP房
        bed_count INT DEFAULT 1, -- 床位数
        occupied_beds INT DEFAULT 0, -- 已占用床位数
        status NVARCHAR(20) DEFAULT N'可用', -- 状态: 可用/已满/维护中
        facilities NVARCHAR(MAX), -- 设施说明(如: 独立卫浴、空调、电视等)
        remark NVARCHAR(MAX), -- 备注
        create_time DATETIME2 DEFAULT GETDATE(),
        update_time DATETIME2 DEFAULT GETDATE(),
        deleted INT DEFAULT 0
    );

    -- 创建索引
    CREATE INDEX idx_room_number ON room(room_number);
    CREATE INDEX idx_room_status ON room(status);
    CREATE INDEX idx_room_floor ON room(floor);

    PRINT '✅ 房间表创建成功';
END
ELSE
BEGIN
    PRINT '⚠️ 房间表已存在';
END
GO

-- ============================================================
-- 2. 为elderly_info表添加room_id字段
-- ============================================================
IF NOT EXISTS (
    SELECT * FROM sys.columns
    WHERE object_id = OBJECT_ID('elderly_info')
    AND name = 'room_id'
)
BEGIN
    ALTER TABLE elderly_info
    ADD room_id BIGINT NULL,
        FOREIGN KEY (room_id) REFERENCES room(id);

    PRINT '✅ elderly_info表添加room_id字段成功';
END
ELSE
BEGIN
    PRINT '⚠️ elderly_info表已有room_id字段';
END
GO

-- 为room_id创建索引
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_elderly_room_id')
BEGIN
    CREATE INDEX idx_elderly_room_id ON elderly_info(room_id);
    PRINT '✅ elderly_info.room_id索引创建成功';
END
GO

-- ============================================================
-- 3. 为medical_schedule表添加room_id字段
-- ============================================================
IF NOT EXISTS (
    SELECT * FROM sys.columns
    WHERE object_id = OBJECT_ID('medical_schedule')
    AND name = 'room_id'
)
BEGIN
    ALTER TABLE medical_schedule
    ADD room_id BIGINT NULL,
        FOREIGN KEY (room_id) REFERENCES room(id);

    PRINT '✅ medical_schedule表添加room_id字段成功';
END
ELSE
BEGIN
    PRINT '⚠️ medical_schedule表已有room_id字段';
END
GO

-- 为room_id创建索引
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_schedule_room_id')
BEGIN
    CREATE INDEX idx_schedule_room_id ON medical_schedule(room_id);
    PRINT '✅ medical_schedule.room_id索引创建成功';
END
GO

-- ============================================================
-- 4. 插入测试房间数据
-- ============================================================
PRINT '';
PRINT '正在插入测试房间数据...';

-- 检查是否已有房间数据
IF NOT EXISTS (SELECT * FROM room WHERE deleted = 0)
BEGIN
    -- 1楼房间（普通单人间和双人间）
    INSERT INTO room (room_number, floor, room_type, bed_count, occupied_beds, status, facilities)
    VALUES
        (N'101', 1, N'单人间', 1, 0, N'可用', N'独立卫浴、空调、电视、呼叫器'),
        (N'102', 1, N'双人间', 2, 0, N'可用', N'独立卫浴、空调、电视、呼叫器'),
        (N'103', 1, N'双人间', 2, 0, N'可用', N'独立卫浴、空调、电视、呼叫器'),
        (N'104', 1, N'单人间', 1, 0, N'可用', N'独立卫浴、空调、电视、呼叫器'),
        (N'105', 1, N'三人间', 3, 0, N'可用', N'独立卫浴、空调、电视、呼叫器');

    -- 2楼房间（普通房和VIP房）
    INSERT INTO room (room_number, floor, room_type, bed_count, occupied_beds, status, facilities)
    VALUES
        (N'201', 2, N'单人间', 1, 0, N'可用', N'独立卫浴、空调、电视、呼叫器'),
        (N'202', 2, N'双人间', 2, 0, N'可用', N'独立卫浴、空调、电视、呼叫器'),
        (N'203', 2, N'VIP房', 1, 0, N'可用', N'独立卫浴、空调、电视、冰箱、沙发、呼叫器'),
        (N'204', 2, N'VIP房', 1, 0, N'可用', N'独立卫浴、空调、电视、冰箱、沙发、呼叫器'),
        (N'205', 2, N'双人间', 2, 0, N'可用', N'独立卫浴、空调、电视、呼叫器');

    -- 3楼房间（高级VIP房和特护房）
    INSERT INTO room (room_number, floor, room_type, bed_count, occupied_beds, status, facilities)
    VALUES
        (N'301', 3, N'VIP房', 1, 0, N'可用', N'独立卫浴、空调、电视、冰箱、沙发、阳台、呼叫器'),
        (N'302', 3, N'VIP房', 1, 0, N'可用', N'独立卫浴、空调、电视、冰箱、沙发、阳台、呼叫器'),
        (N'303', 3, N'单人间', 1, 0, N'可用', N'独立卫浴、空调、电视、呼叫器'),
        (N'304', 3, N'单人间', 1, 0, N'可用', N'独立卫浴、空调、电视、呼叫器'),
        (N'305', 3, N'双人间', 2, 0, N'可用', N'独立卫浴、空调、电视、呼叫器');

    PRINT '✅ 测试房间数据插入成功（共15个房间）';
END
ELSE
BEGIN
    PRINT '✓ 房间数据已存在，跳过插入';
END
GO

-- ============================================================
-- 5. 验证结果
-- ============================================================
PRINT '';
PRINT '========================================';
PRINT '验证房间管理功能...';
PRINT '========================================';

-- 查询所有房间
PRINT '';
PRINT '所有房间列表:';
SELECT
    id AS '房间ID',
    room_number AS '房间号',
    floor AS '楼层',
    room_type AS '房间类型',
    bed_count AS '总床位',
    occupied_beds AS '已占用',
    status AS '状态',
    facilities AS '设施'
FROM room
WHERE deleted = 0
ORDER BY floor, room_number;

-- 统计信息
PRINT '';
PRINT '房间统计信息:';
SELECT
    COUNT(*) AS '总房间数',
    SUM(bed_count) AS '总床位数',
    SUM(occupied_beds) AS '已占用床位',
    SUM(bed_count) - SUM(occupied_beds) AS '剩余床位'
FROM room
WHERE deleted = 0;

PRINT '';
PRINT '========================================';
PRINT '✅ 房间管理功能添加完成!';
PRINT '========================================';
PRINT '';
PRINT '新增内容:';
PRINT '1. ✅ 创建room表（房间信息表）';
PRINT '2. ✅ elderly_info表添加room_id字段';
PRINT '3. ✅ medical_schedule表添加room_id字段';
PRINT '4. ✅ 插入15个测试房间数据';
PRINT '';
PRINT '下一步需要:';
PRINT '- 创建Room实体类';
PRINT '- 创建房间管理Controller和Service';
PRINT '- 更新老人信息管理页面（添加房间选择）';
PRINT '- 更新医护排班页面（显示房间信息）';
PRINT '========================================';
GO