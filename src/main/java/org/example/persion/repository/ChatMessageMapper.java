package org.example.persion.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.persion.entity.ChatMessage;

/**
 * 聊天消息数据访问接口
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
