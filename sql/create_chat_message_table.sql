-- ========================================
-- 智慧养老系统 - 聊天消息表
-- 新增: 聊天消息存储，支持文本、图片、语音消息
-- ========================================

USE elderly_care;
GO

-- 创建聊天消息表
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'chat_message')
BEGIN
    CREATE TABLE chat_message (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        group_id BIGINT NOT NULL, -- 群组ID（对应老人ID）
        sender_id BIGINT NOT NULL, -- 发送者用户ID
        sender_name NVARCHAR(50) NOT NULL, -- 发送者姓名（冗余，方便查询）
        sender_role NVARCHAR(20) NOT NULL, -- 发送者角色（冗余，方便查询）
        message_type NVARCHAR(20) DEFAULT 'text', -- 消息类型: text, image, audio
        content NVARCHAR(MAX) NOT NULL, -- 消息内容或文件描述
        audio_url NVARCHAR(500), -- 音频文件URL（当messageType为audio时使用）
        image_url NVARCHAR(500), -- 图片文件URL（当messageType为image时使用）
        duration INT, -- 音频时长（秒，当messageType为audio时使用）
        create_time DATETIME2 NULL, -- 由应用程序设置，避免时区问题
        update_time DATETIME2 NULL, -- 由应用程序设置，避免时区问题
        deleted INT DEFAULT 0,
        FOREIGN KEY (group_id) REFERENCES elderly_info(id),
        FOREIGN KEY (sender_id) REFERENCES sys_user(id)
    );
    PRINT '✅ 聊天消息表创建成功';
END
ELSE
BEGIN
    PRINT '⚠️ 聊天消息表已存在';
END
GO

-- 创建索引
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_chat_message_group_id')
    CREATE INDEX idx_chat_message_group_id ON chat_message(group_id);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_chat_message_sender_id')
    CREATE INDEX idx_chat_message_sender_id ON chat_message(sender_id);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_chat_message_create_time')
    CREATE INDEX idx_chat_message_create_time ON chat_message(create_time);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_chat_message_type')
    CREATE INDEX idx_chat_message_type ON chat_message(message_type);

PRINT '✅ 聊天消息表索引创建完成';
GO

PRINT '';
PRINT '========================================';
PRINT '✅ 聊天消息表创建完成!';
PRINT '========================================';
PRINT '表名: chat_message';
PRINT '功能: 存储聊天消息，支持文本、图片、语音消息';
PRINT '';
PRINT '字段说明:';
PRINT '- group_id: 群组ID（对应老人ID）';
PRINT '- sender_id: 发送者用户ID';
PRINT '- message_type: 消息类型（text/image/audio）';
PRINT '- content: 消息内容';
PRINT '- audio_url: 语音文件URL';
PRINT '- image_url: 图片文件URL';
PRINT '- duration: 语音时长（秒）';
PRINT '========================================';
GO