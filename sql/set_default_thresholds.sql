-- 补充：为已存在的老人设置健康阈值数据
-- 前提：已执行 add_health_thresholds.sql 添加了字段

-- 方案1：为所有老人设置相同的标准阈值
UPDATE elderly_info SET
    heart_rate_high = 100,
    heart_rate_low = 60,
    systolic_pressure_high = 140,
    systolic_pressure_low = 90,
    diastolic_pressure_high = 80,
    diastolic_pressure_low = 60,
    temperature_high = 37.3,
    temperature_low = 36.0
WHERE deleted = 0;  -- 只更新未删除的记录

-- 查看更新结果
SELECT id, name, age,
       heart_rate_high, heart_rate_low,
       systolic_pressure_high, systolic_pressure_low,
       temperature_high, temperature_low
FROM elderly_info
WHERE deleted = 0;

GO

-- 方案2：如果你想为不同年龄段设置不同阈值
-- 例如：70岁以上的老人放宽心率标准
UPDATE elderly_info SET
    heart_rate_high = 110,  -- 放宽到110
    heart_rate_low = 50     -- 放宽到50
WHERE age >= 70 AND deleted = 0;

GO
