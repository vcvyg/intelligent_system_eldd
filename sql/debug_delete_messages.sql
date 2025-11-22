-- 调试删除消息功能的SQL脚本
USE elderly_care;

-- 1. 查看所有聊天消息
SELECT 
    id,
    group_id,
    sender_id,
    sender_name,
    message_type,
    content,
    create_time,
    deleted
FROM chat_message 
ORDER BY create_time DESC
LIMIT 20;

PRINT '=== 最近20条消息 ===';

-- 2. 查看被标记为删除的消息
SELECT 
    id,
    group_id,
    sender_id,
    sender_name,
    message_type,
    content,
    create_time,
    deleted
FROM chat_message 
WHERE deleted = 1
ORDER BY create_time DESC;

PRINT '=== 被标记为删除的消息 ===';

-- 3. 统计消息数量
SELECT 
    COUNT(*) as total_messages,
    COUNT(CASE WHEN deleted = 0 THEN 1 END) as active_messages,
    COUNT(CASE WHEN deleted = 1 THEN 1 END) as deleted_messages
FROM chat_message;

PRINT '=== 消息统计 ===';

-- 4. 按群组统计消息
SELECT 
    group_id,
    COUNT(*) as total_messages,
    COUNT(CASE WHEN deleted = 0 THEN 1 END) as active_messages,
    COUNT(CASE WHEN deleted = 1 THEN 1 END) as deleted_messages
FROM chat_message 
GROUP BY group_id
ORDER BY group_id;

PRINT '=== 按群组统计 ===';

-- 5. 手动删除测试消息（如果需要）
-- DELETE FROM chat_message WHERE id = 1; -- 取消注释来删除特定消息

PRINT '=== 调试完成 ===';