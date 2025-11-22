-- 为chat_message表添加文件相关字段
-- 执行时间: 2024-11-22

-- 添加文件名字段
ALTER TABLE chat_message ADD file_name VARCHAR(255);

-- 添加文件URL字段  
ALTER TABLE chat_message ADD file_url VARCHAR(500);

-- 验证字段添加（可选，根据数据库类型调整）
-- SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'chat_message';