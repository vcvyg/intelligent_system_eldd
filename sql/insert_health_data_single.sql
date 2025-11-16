-- 为单个老人插入健康数据测试脚本
-- 自动获取第一个老人ID并插入多条健康数据

-- 查看现有老人
SELECT id, name, age, gender FROM elderly_info;

DECLARE @elderlyId BIGINT;

-- 获取第一个老人的ID
SELECT TOP 1 @elderlyId = id FROM elderly_info ORDER BY id;

-- 检查是否有老人数据
IF @elderlyId IS NULL
BEGIN
    PRINT '错误: 数据库中没有老人信息,请先添加老人!';
    RETURN;
END

PRINT '========================================';
PRINT '将为老人ID: ' + CAST(@elderlyId AS VARCHAR(10)) + ' 插入健康数据';
PRINT '========================================';

-- 今日数据 (6条,模拟一天不同时间段的测量)
-- 早上6点
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, measure_time, create_time, update_time)
VALUES (@elderlyId, 68, 115, 75, 36.3, 5.1, 0, DATEADD(HOUR, -12, GETDATE()), GETDATE(), GETDATE());

-- 早上9点 (早餐后)
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, measure_time, create_time, update_time)
VALUES (@elderlyId, 72, 118, 78, 36.5, 6.8, 1200, DATEADD(HOUR, -9, GETDATE()), GETDATE(), GETDATE());

-- 中午12点 (午餐前)
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, measure_time, create_time, update_time)
VALUES (@elderlyId, 75, 120, 80, 36.6, 5.5, 3500, DATEADD(HOUR, -6, GETDATE()), GETDATE(), GETDATE());

-- 下午3点 (午餐后)
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, measure_time, create_time, update_time)
VALUES (@elderlyId, 78, 122, 82, 36.7, 7.2, 5200, DATEADD(HOUR, -3, GETDATE()), GETDATE(), GETDATE());

-- 下午6点 (晚餐前)
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, measure_time, create_time, update_time)
VALUES (@elderlyId, 76, 119, 79, 36.6, 5.8, 6800, DATEADD(HOUR, -1, GETDATE()), GETDATE(), GETDATE());

-- 晚上9点 (晚餐后)
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, measure_time, create_time, update_time)
VALUES (@elderlyId, 70, 116, 76, 36.5, 6.5, 7500, GETDATE(), GETDATE(), GETDATE());

PRINT '今日数据插入成功! (6条)';

-- 昨天的数据 (4条)
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, sleep_duration, measure_time, create_time, update_time)
VALUES
(@elderlyId, 69, 117, 77, 36.4, 5.2, 2000, NULL, DATEADD(DAY, -1, DATEADD(HOUR, -15, GETDATE())), DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE())),
(@elderlyId, 73, 120, 80, 36.6, 6.9, 4500, NULL, DATEADD(DAY, -1, DATEADD(HOUR, -9, GETDATE())), DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE())),
(@elderlyId, 77, 123, 81, 36.7, 7.1, 6200, NULL, DATEADD(DAY, -1, DATEADD(HOUR, -3, GETDATE())), DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE())),
(@elderlyId, 71, 118, 78, 36.5, 5.9, 7800, 420, DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE()));

PRINT '昨天数据插入成功! (4条)';

-- 前天的数据 (3条)
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, sleep_duration, measure_time, create_time, update_time)
VALUES
(@elderlyId, 70, 116, 76, 36.4, 5.3, 3200, NULL, DATEADD(DAY, -2, DATEADD(HOUR, -10, GETDATE())), DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -2, GETDATE())),
(@elderlyId, 74, 121, 79, 36.6, 6.7, 5800, NULL, DATEADD(DAY, -2, DATEADD(HOUR, -4, GETDATE())), DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -2, GETDATE())),
(@elderlyId, 72, 119, 77, 36.5, 6.2, 7100, 450, DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -2, GETDATE()));

PRINT '前天数据插入成功! (3条)';

-- 3天前的数据 (3条)
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, sleep_duration, measure_time, create_time, update_time)
VALUES
(@elderlyId, 68, 115, 75, 36.3, 5.0, 2800, NULL, DATEADD(DAY, -3, DATEADD(HOUR, -10, GETDATE())), DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, -3, GETDATE())),
(@elderlyId, 73, 120, 80, 36.6, 6.5, 5500, NULL, DATEADD(DAY, -3, DATEADD(HOUR, -4, GETDATE())), DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, -3, GETDATE())),
(@elderlyId, 71, 117, 78, 36.5, 6.0, 6900, 410, DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, -3, GETDATE()));

PRINT '3天前数据插入成功! (3条)';

-- 4天前的数据 (3条)
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, sleep_duration, measure_time, create_time, update_time)
VALUES
(@elderlyId, 69, 116, 76, 36.4, 5.2, 3100, NULL, DATEADD(DAY, -4, DATEADD(HOUR, -10, GETDATE())), DATEADD(DAY, -4, GETDATE()), DATEADD(DAY, -4, GETDATE())),
(@elderlyId, 75, 122, 81, 36.7, 6.8, 5300, NULL, DATEADD(DAY, -4, DATEADD(HOUR, -4, GETDATE())), DATEADD(DAY, -4, GETDATE()), DATEADD(DAY, -4, GETDATE())),
(@elderlyId, 72, 119, 79, 36.6, 6.3, 6700, 430, DATEADD(DAY, -4, GETDATE()), DATEADD(DAY, -4, GETDATE()), DATEADD(DAY, -4, GETDATE()));

PRINT '4天前数据插入成功! (3条)';

-- 5天前的数据 (3条)
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, sleep_duration, measure_time, create_time, update_time)
VALUES
(@elderlyId, 67, 114, 74, 36.3, 4.9, 2900, NULL, DATEADD(DAY, -5, DATEADD(HOUR, -10, GETDATE())), DATEADD(DAY, -5, GETDATE()), DATEADD(DAY, -5, GETDATE())),
(@elderlyId, 74, 121, 80, 36.6, 6.6, 5600, NULL, DATEADD(DAY, -5, DATEADD(HOUR, -4, GETDATE())), DATEADD(DAY, -5, GETDATE()), DATEADD(DAY, -5, GETDATE())),
(@elderlyId, 71, 118, 78, 36.5, 6.1, 7200, 440, DATEADD(DAY, -5, GETDATE()), DATEADD(DAY, -5, GETDATE()), DATEADD(DAY, -5, GETDATE()));

PRINT '5天前数据插入成功! (3条)';

-- 6天前的数据 (3条)
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, sleep_duration, measure_time, create_time, update_time)
VALUES
(@elderlyId, 70, 117, 77, 36.4, 5.1, 3000, NULL, DATEADD(DAY, -6, DATEADD(HOUR, -10, GETDATE())), DATEADD(DAY, -6, GETDATE()), DATEADD(DAY, -6, GETDATE())),
(@elderlyId, 76, 123, 82, 36.7, 6.9, 5400, NULL, DATEADD(DAY, -6, DATEADD(HOUR, -4, GETDATE())), DATEADD(DAY, -6, GETDATE()), DATEADD(DAY, -6, GETDATE())),
(@elderlyId, 73, 120, 79, 36.6, 6.4, 6800, 460, DATEADD(DAY, -6, GETDATE()), DATEADD(DAY, -6, GETDATE()), DATEADD(DAY, -6, GETDATE()));

PRINT '6天前数据插入成功! (3条)';

PRINT '';
PRINT '========================================';
PRINT '所有健康数据插入完成!';
PRINT '========================================';
PRINT '今天:   6条 (全天多次测量)';
PRINT '昨天:   4条';
PRINT '前天:   3条';
PRINT '3天前: 3条';
PRINT '4天前: 3条';
PRINT '5天前: 3条';
PRINT '6天前: 3条';
PRINT '----------------------------------------';
PRINT '总计:  25条健康记录';
PRINT '========================================';

-- 查看插入结果
SELECT
    hd.id,
    ei.name AS 老人姓名,
    ei.age AS 年龄,
    ei.gender AS 性别,
    hd.heart_rate AS 心率,
    CAST(hd.blood_pressure_high AS VARCHAR) + '/' + CAST(hd.blood_pressure_low AS VARCHAR) AS 血压,
    hd.temperature AS 体温,
    hd.blood_sugar AS 血糖,
    hd.steps AS 步数,
    hd.sleep_duration AS 睡眠时长分钟,
    CONVERT(VARCHAR, hd.measure_time, 120) AS 测量时间,
    DATEDIFF(DAY, hd.create_time, GETDATE()) AS 天数差
FROM health_data hd
LEFT JOIN elderly_info ei ON hd.elderly_id = ei.id
ORDER BY hd.measure_time DESC;

-- 统计信息
SELECT
    '健康数据统计' AS 类型,
    COUNT(*) AS 总记录数,
    COUNT(CASE WHEN CAST(create_time AS DATE) = CAST(GETDATE() AS DATE) THEN 1 END) AS 今日记录,
    MIN(measure_time) AS 最早记录时间,
    MAX(measure_time) AS 最新记录时间
FROM health_data
WHERE elderly_id = @elderlyId;
