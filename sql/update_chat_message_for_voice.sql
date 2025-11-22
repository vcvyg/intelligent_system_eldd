-- ========================================
-- 更新聊天消息表以支持语音和图片功能
-- 添加: audio_url, image_url, duration 字段
-- ========================================

USE elderly_care;
GO

-- 检查并添加 audio_url 字段
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('chat_message') AND name = 'audio_url')
BEGIN
    ALTER TABLE chat_message ADD audio_url NVARCHAR(500) NULL;
    PRINT '✅ 添加 audio_url 字段成功';
END
ELSE
BEGIN
    PRINT '⚠️ audio_url 字段已存在';
END
GO

-- 检查并添加 image_url 字段
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('chat_message') AND name = 'image_url')
BEGIN
    ALTER TABLE chat_message ADD image_url NVARCHAR(500) NULL;
    PRINT '✅ 添加 image_url 字段成功';
END
ELSE
BEGIN
    PRINT '⚠️ image_url 字段已存在';
END
GO

-- 检查并添加 duration 字段
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('chat_message') AND name = 'duration')
BEGIN
    ALTER TABLE chat_message ADD duration INT NULL;
    PRINT '✅ 添加 duration 字段成功';
END
ELSE
BEGIN
    PRINT '⚠️ duration 字段已存在';
END
GO

-- 创建额外的索引以优化查询
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_chat_message_sender_id' AND object_id = OBJECT_ID('chat_message'))
BEGIN
    CREATE INDEX idx_chat_message_sender_id ON chat_message(sender_id);
    PRINT '✅ 创建 sender_id 索引成功';
END
ELSE
BEGIN
    PRINT '⚠️ sender_id 索引已存在';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_chat_message_create_time' AND object_id = OBJECT_ID('chat_message'))
BEGIN
    CREATE INDEX idx_chat_message_create_time ON chat_message(create_time);
    PRINT '✅ 创建 create_time 索引成功';
END
ELSE
BEGIN
    PRINT '⚠️ create_time 索引已存在';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_chat_message_type' AND object_id = OBJECT_ID('chat_message'))
BEGIN
    CREATE INDEX idx_chat_message_type ON chat_message(message_type);
    PRINT '✅ 创建 message_type 索引成功';
END
ELSE
BEGIN
    PRINT '⚠️ message_type 索引已存在';
END
GO

PRINT '';
PRINT '========================================';
PRINT '✅ 聊天消息表更新完成!';
PRINT '========================================';
PRINT '新增字段:';
PRINT '- audio_url: 音频文件URL (NVARCHAR(500))';
PRINT '- image_url: 图片文件URL (NVARCHAR(500))';
PRINT '- duration: 音频时长秒数 (INT)';
PRINT '';
PRINT '新增索引:';
PRINT '- idx_chat_message_sender_id';
PRINT '- idx_chat_message_create_time';
PRINT '- idx_chat_message_type';
PRINT '';
PRINT '现在可以支持语音和图片消息了！';
PRINT '========================================';
GO