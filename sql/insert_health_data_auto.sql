-- 自动插入测试健康数据
-- 该脚本会自动查找数据库中的老人ID并插入数据

-- 第一步: 查看现有老人
SELECT id, name, age, gender FROM elderly_info;

-- 第二步: 使用动态SQL为前3个老人插入健康数据
DECLARE @elderly1 BIGINT, @elderly2 BIGINT, @elderly3 BIGINT;

-- 获取前3个老人的ID
SELECT TOP 3 @elderly1 = CASE WHEN ROW_NUMBER() OVER(ORDER BY id) = 1 THEN id ELSE @elderly1 END,
             @elderly2 = CASE WHEN ROW_NUMBER() OVER(ORDER BY id) = 2 THEN id ELSE @elderly2 END,
             @elderly3 = CASE WHEN ROW_NUMBER() OVER(ORDER BY id) = 3 THEN id ELSE @elderly3 END
FROM elderly_info;

-- 显示将要使用的老人ID
PRINT '将为以下老人ID插入健康数据:';
PRINT 'ID1: ' + CAST(@elderly1 AS VARCHAR(10));
PRINT 'ID2: ' + CAST(@elderly2 AS VARCHAR(10));
PRINT 'ID3: ' + CAST(@elderly3 AS VARCHAR(10));

-- 如果老人不足3个,提示错误
IF @elderly1 IS NULL OR @elderly2 IS NULL OR @elderly3 IS NULL
BEGIN
    PRINT '错误: 数据库中老人数量少于3个,请先添加老人信息!';
    RETURN;
END

-- 插入今日早上的数据
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, measure_time, create_time, update_time)
VALUES
(@elderly1, 72, 120, 80, 36.5, 5.2, 3500, DATEADD(HOUR, -2, GETDATE()), GETDATE(), GETDATE()),
(@elderly2, 68, 115, 75, 36.4, 4.8, 4200, DATEADD(HOUR, -2, GETDATE()), GETDATE(), GETDATE()),
(@elderly3, 75, 125, 82, 36.6, 5.5, 2800, DATEADD(HOUR, -2, GETDATE()), GETDATE(), GETDATE());

PRINT '今日早上数据插入成功!';

-- 插入今日中午的数据
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, measure_time, create_time, update_time)
VALUES
(@elderly1, 78, 122, 81, 36.7, 6.8, 5200, DATEADD(HOUR, -1, GETDATE()), GETDATE(), GETDATE()),
(@elderly2, 71, 118, 77, 36.5, 7.2, 6100, DATEADD(HOUR, -1, GETDATE()), GETDATE(), GETDATE()),
(@elderly3, 80, 128, 85, 36.8, 6.5, 4500, DATEADD(HOUR, -1, GETDATE()), GETDATE(), GETDATE());

PRINT '今日中午数据插入成功!';

-- 插入昨天的数据
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, sleep_duration, measure_time, create_time, update_time)
VALUES
(@elderly1, 70, 118, 78, 36.4, 5.0, 6800, 420, DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE())),
(@elderly2, 69, 116, 76, 36.3, 4.9, 7200, 450, DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE())),
(@elderly3, 73, 123, 80, 36.5, 5.3, 5500, 390, DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE()));

PRINT '昨天数据插入成功!';

-- 插入前天的数据
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, sleep_duration, measure_time, create_time, update_time)
VALUES
(@elderly1, 71, 119, 79, 36.6, 5.1, 7100, 410, DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -2, GETDATE())),
(@elderly2, 67, 114, 74, 36.4, 4.7, 7500, 460, DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -2, GETDATE())),
(@elderly3, 76, 126, 83, 36.7, 5.6, 5200, 380, DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -2, GETDATE()));

PRINT '前天数据插入成功!';

PRINT '========================================';
PRINT '所有健康数据插入完成!';
PRINT '今日记录: 6条';
PRINT '昨天记录: 3条';
PRINT '前天记录: 3条';
PRINT '总计: 12条';
PRINT '========================================';

-- 查看插入结果
SELECT
    hd.id,
    ei.name AS elderly_name,
    ei.age,
    ei.gender,
    hd.heart_rate AS 心率,
    hd.blood_pressure_high AS 收缩压,
    hd.blood_pressure_low AS 舒张压,
    hd.temperature AS 体温,
    hd.blood_sugar AS 血糖,
    hd.steps AS 步数,
    hd.sleep_duration AS 睡眠时长,
    hd.measure_time AS 测量时间,
    hd.create_time AS 创建时间
FROM health_data hd
LEFT JOIN elderly_info ei ON hd.elderly_id = ei.id
ORDER BY hd.create_time DESC;
