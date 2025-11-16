-- 插入测试健康数据
-- 注意: 需要根据实际的elderly_id进行调整

-- 查看现有老人ID
-- SELECT id, name FROM elderly_info;

-- 为老人插入今日健康数据 (假设elderly_id为1-3)
-- 今日早上的数据
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, measure_time, create_time, update_time)
VALUES
(1, 72, 120, 80, 36.5, 5.2, 3500, DATEADD(HOUR, -2, GETDATE()), GETDATE(), GETDATE()),
(2, 68, 115, 75, 36.4, 4.8, 4200, DATEADD(HOUR, -2, GETDATE()), GETDATE(), GETDATE()),
(3, 75, 125, 82, 36.6, 5.5, 2800, DATEADD(HOUR, -2, GETDATE()), GETDATE(), GETDATE());

-- 今日中午的数据
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, measure_time, create_time, update_time)
VALUES
(1, 78, 122, 81, 36.7, 6.8, 5200, DATEADD(HOUR, -1, GETDATE()), GETDATE(), GETDATE()),
(2, 71, 118, 77, 36.5, 7.2, 6100, DATEADD(HOUR, -1, GETDATE()), GETDATE(), GETDATE()),
(3, 80, 128, 85, 36.8, 6.5, 4500, DATEADD(HOUR, -1, GETDATE()), GETDATE(), GETDATE());

-- 昨天的数据
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, sleep_duration, measure_time, create_time, update_time)
VALUES
(1, 70, 118, 78, 36.4, 5.0, 6800, 420, DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE())),
(2, 69, 116, 76, 36.3, 4.9, 7200, 450, DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE())),
(3, 73, 123, 80, 36.5, 5.3, 5500, 390, DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE()));

-- 前天的数据
INSERT INTO health_data (elderly_id, heart_rate, blood_pressure_high, blood_pressure_low, temperature, blood_sugar, steps, sleep_duration, measure_time, create_time, update_time)
VALUES
(1, 71, 119, 79, 36.6, 5.1, 7100, 410, DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -2, GETDATE())),
(2, 67, 114, 74, 36.4, 4.7, 7500, 460, DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -2, GETDATE())),
(3, 76, 126, 83, 36.7, 5.6, 5200, 380, DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -2, GETDATE()));

-- 查看插入结果
SELECT
    hd.id,
    ei.name AS elderly_name,
    hd.heart_rate,
    hd.blood_pressure_high,
    hd.blood_pressure_low,
    hd.temperature,
    hd.blood_sugar,
    hd.steps,
    hd.measure_time,
    hd.create_time
FROM health_data hd
LEFT JOIN elderly_info ei ON hd.elderly_id = ei.id
ORDER BY hd.create_time DESC;
