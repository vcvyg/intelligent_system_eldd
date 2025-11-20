-- 为所有老人设置默认健康阈值
-- 注意：请先在 elderly_info 表中添加阈值字段（执行 add_health_thresholds.sql）

-- 为所有现有老人设置标准健康阈值
UPDATE elderly_info SET
    heart_rate_high = 100,       -- 心率上限 100 bpm
    heart_rate_low = 60,          -- 心率下限 60 bpm
    systolic_pressure_high = 140, -- 收缩压上限 140 mmHg
    systolic_pressure_low = 90,   -- 收缩压下限 90 mmHg
    diastolic_pressure_high = 90, -- 舒张压上限 90 mmHg
    diastolic_pressure_low = 60,  -- 舒张压下限 60 mmHg
    temperature_high = 37.3,      -- 体温上限 37.3°C
    temperature_low = 36.0        -- 体温下限 36.0°C
WHERE heart_rate_high IS NULL;   -- 只更新尚未设置阈值的老人

-- 查询已设置阈值的老人信息
SELECT
    id,
    name,
    age,
    heart_rate_high,
    heart_rate_low,
    systolic_pressure_high,
    systolic_pressure_low,
    diastolic_pressure_high,
    diastolic_pressure_low,
    temperature_high,
    temperature_low
FROM elderly_info
WHERE deleted = 0
ORDER BY id;

GO
