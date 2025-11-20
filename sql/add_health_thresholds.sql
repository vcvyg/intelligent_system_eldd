-- 为老人信息表 (elderly_info) 添加健康数据阈值字段 (SQL Server 语法)
-- 最终修正版：仅包含心率、血压、体温
ALTER TABLE elderly_info ADD heart_rate_high INT NULL;
ALTER TABLE elderly_info ADD heart_rate_low INT NULL;
ALTER TABLE elderly_info ADD systolic_pressure_high INT NULL;
ALTER TABLE elderly_info ADD systolic_pressure_low INT NULL;
ALTER TABLE elderly_info ADD diastolic_pressure_high INT NULL;
ALTER TABLE elderly_info ADD diastolic_pressure_low INT NULL;
ALTER TABLE elderly_info ADD temperature_high DECIMAL(5,2) NULL;
ALTER TABLE elderly_info ADD temperature_low DECIMAL(5,2) NULL;
GO

-- 添加字段注释 (SQL Server 语法)
-- 请注意: 如果您的 schema 不是 'dbo', 请替换成您的 schema 名
EXEC sp_addextendedproperty 'MS_Description', '100', 'SCHEMA', 'dbo', 'TABLE', 'elderly_info', 'COLUMN', 'heart_rate_high';
EXEC sp_addextendedproperty 'MS_Description', '60', 'SCHEMA', 'dbo', 'TABLE', 'elderly_info', 'COLUMN', 'heart_rate_low';
EXEC sp_addextendedproperty 'MS_Description', '140', 'SCHEMA', 'dbo', 'TABLE', 'elderly_info', 'COLUMN', 'systolic_pressure_high';
EXEC sp_addextendedproperty 'MS_Description', '90', 'SCHEMA', 'dbo', 'TABLE', 'elderly_info', 'COLUMN', 'systolic_pressure_low';
EXEC sp_addextendedproperty 'MS_Description', '90', 'SCHEMA', 'dbo', 'TABLE', 'elderly_info', 'COLUMN', 'diastolic_pressure_high';
EXEC sp_addextendedproperty 'MS_Description', '60', 'SCHEMA', 'dbo', 'TABLE', 'elderly_info', 'COLUMN', 'diastolic_pressure_low';
EXEC sp_addextendedproperty 'MS_Description', '37.3', 'SCHEMA', 'dbo', 'TABLE', 'elderly_info', 'COLUMN', 'temperature_high';
EXEC sp_addextendedproperty 'MS_Description', '36.0', 'SCHEMA', 'dbo', 'TABLE', 'elderly_info', 'COLUMN', 'temperature_low';
GO

