package org.example.persion.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.entity.ChatMessage;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.repository.ChatMessageMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.security.SecurityUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RedisTemplate<String, String> redisTemplate;
    private final ElderlyInfoMapper elderlyInfoMapper;
    private final ChatMessageMapper chatMessageMapper;

    private static final String UNREAD_COUNT_KEY_PREFIX = "unread:count:";

    /**
     * 获取当前用户所有群组的未读消息数
     */
    @GetMapping("/unread-counts")
    public Result<Map<Long, Integer>> getUnreadCounts() {
        Long userId = SecurityUtil.getUserId();
        if (userId == null) {
            return Result.error("用户未登录");
        }

        // 获取用户所属的所有群组ID
        List<ElderlyInfo> medicalGroups = elderlyInfoMapper.selectElderlyListByMedicalUserId(userId);
        List<ElderlyInfo> familyGroups = elderlyInfoMapper.selectElderlyListByFamilyUserId(userId);

        List<Long> groupIds = Stream.concat(medicalGroups.stream(), familyGroups.stream())
                .map(ElderlyInfo::getId)
                .distinct()
                .collect(Collectors.toList());

        // 构建Redis keys
        List<String> redisKeys = groupIds.stream()
                .map(groupId -> UNREAD_COUNT_KEY_PREFIX + userId + ":" + groupId)
                .collect(Collectors.toList());

        // 从Redis批量获取
        List<String> counts = redisTemplate.opsForValue().multiGet(redisKeys);

        // 构建返回结果 Map<GroupId, UnreadCount>
        Map<Long, Integer> unreadCountsMap = groupIds.stream()
                .collect(Collectors.toMap(
                        groupId -> groupId,
                        groupId -> {
                            int index = groupIds.indexOf(groupId);
                            if (counts != null && index < counts.size() && counts.get(index) != null) {
                                try {
                                    return Integer.parseInt(counts.get(index));
                                } catch (NumberFormatException e) {
                                    return 0;
                                }
                            }
                            return 0;
                        }
                ));

        return Result.success(unreadCountsMap);
    }

    /**
     * 将指定群组的消息标记为已读（清除未读数）
     */
    @PostMapping("/groups/{groupId}/read")
    public Result<Void> markAsRead(@PathVariable Long groupId) {
        Long userId = SecurityUtil.getUserId();
        if (userId == null) {
            return Result.error("用户未登录");
        }

        String unreadKey = UNREAD_COUNT_KEY_PREFIX + userId + ":" + groupId;
        redisTemplate.delete(unreadKey);

        return Result.success();
    }

    /**
     * 根据消息信息查找消息ID
     */
    @PostMapping("/message/find")
    public Result<ChatMessage> findMessage(@RequestBody FindMessageDTO dto) {
        try {
            LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ChatMessage::getGroupId, dto.getGroupId())
                   .eq(ChatMessage::getSenderId, dto.getSenderId())
                   .eq(ChatMessage::getContent, dto.getContent())
                   .eq(ChatMessage::getMessageType, dto.getMessageType())
                   .eq(ChatMessage::getDeleted, 0)
                   .orderByDesc(ChatMessage::getCreateTime)
                   .last("LIMIT 1");
            
            ChatMessage message = chatMessageMapper.selectOne(wrapper);
            return Result.success(message);
        } catch (Exception e) {
            return Result.error("查找消息失败: " + e.getMessage());
        }
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/message/{messageId}")
    public Result<Void> deleteMessage(@PathVariable Long messageId) {
        try {
            System.out.println("=== 删除消息请求开始 ===");
            System.out.println("消息ID: " + messageId);
            
            Long currentUserId = SecurityUtil.getUserId();
            System.out.println("当前用户ID: " + currentUserId);
            
            if (currentUserId == null) {
                System.out.println("用户未登录，删除失败");
                return Result.error("用户未登录");
            }
            
            // 检查消息是否存在且属于当前用户
            ChatMessage message = chatMessageMapper.selectById(messageId);
            System.out.println("查询到的消息: " + message);
            
            if (message == null) {
                System.out.println("消息不存在，删除失败");
                return Result.error("消息不存在");
            }
            
            System.out.println("消息发送者ID: " + message.getSenderId());
            System.out.println("当前用户ID: " + currentUserId);
            
            if (!message.getSenderId().equals(currentUserId)) {
                System.out.println("权限不足，删除失败");
                return Result.error("只能删除自己发送的消息");
            }
            
            // 软删除消息 - 使用MyBatis-Plus的逻辑删除
            System.out.println("开始软删除消息...");
            int updateResult = chatMessageMapper.deleteById(messageId);
            System.out.println("逻辑删除结果: " + updateResult);
            
            if (updateResult > 0) {
                System.out.println("消息删除成功！messageId=" + messageId);
                return Result.success();
            } else {
                System.out.println("消息删除失败！updateResult=" + updateResult);
                return Result.error("删除失败");
            }
            
        } catch (Exception e) {
            System.err.println("删除消息异常: " + e.getMessage());
            e.printStackTrace();
            return Result.error("删除消息失败: " + e.getMessage());
        }
    }

    @Data
    public static class FindMessageDTO {
        private Long groupId;
        private Long senderId;
        private String content;
        private String messageType;
        private String time;
    }
}
