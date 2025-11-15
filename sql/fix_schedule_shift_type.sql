-- 修复医护排班表shift_type字段允许为NULL
USE elderly_care;
GO

-- 修改shift_type字段允许NULL
ALTER TABLE medical_schedule
ALTER COLUMN shift_type NVARCHAR(20) NULL;
GO

PRINT '✅ medical_schedule表的shift_type字段已修改为允许NULL';
GO
