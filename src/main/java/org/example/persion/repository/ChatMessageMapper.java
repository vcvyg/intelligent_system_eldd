package org.example.persion.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.example.persion.entity.ChatMessage;

/**
 * 聊天消息数据访问接口
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    
    /**
     * 物理删除消息（真正从数据库中删除）
     */
    @Delete("DELETE FROM chat_message WHERE id = #{id}")
    int deleteMessagePhysically(Long id);
}
