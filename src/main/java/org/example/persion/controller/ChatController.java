package org.example.persion.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.persion.common.Result;
import org.example.persion.entity.ChatMessage;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.repository.ChatMessageMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.security.SecurityUtil;
import org.example.persion.service.ChatGroupAccessService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private static final String UNREAD_COUNT_KEY_PREFIX = "unread:count:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ElderlyInfoMapper elderlyInfoMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatGroupAccessService chatGroupAccessService;

    @GetMapping("/unread-counts")
    public Result<Map<Long, Integer>> getUnreadCounts() {
        Long userId = SecurityUtil.getUserId();
        if (userId == null) {
            return Result.error(401, "用户未登录");
        }

        List<ElderlyInfo> medicalGroups = elderlyInfoMapper.selectElderlyListByMedicalUserId(userId);
        List<ElderlyInfo> familyGroups = elderlyInfoMapper.selectElderlyListByFamilyUserId(userId);
        List<Long> groupIds = Stream.concat(medicalGroups.stream(), familyGroups.stream())
                .map(ElderlyInfo::getId)
                .distinct()
                .toList();

        List<String> redisKeys = groupIds.stream()
                .map(groupId -> UNREAD_COUNT_KEY_PREFIX + userId + ":" + groupId)
                .toList();
        List<String> counts = redisKeys.isEmpty() ? List.of() : redisTemplate.opsForValue().multiGet(redisKeys);

        Map<Long, Integer> unreadCounts = groupIds.stream()
                .collect(Collectors.toMap(
                        groupId -> groupId,
                        groupId -> parseCount(counts, groupIds.indexOf(groupId))
                ));
        return Result.success(unreadCounts);
    }

    @PostMapping("/groups/{groupId}/read")
    public Result<Void> markAsRead(@PathVariable Long groupId) {
        Long userId = SecurityUtil.getUserId();
        if (userId == null) {
            return Result.error(401, "用户未登录");
        }
        if (!chatGroupAccessService.canAccess(userId, groupId)) {
            return Result.error(403, "无权访问该聊天群组");
        }

        redisTemplate.delete(UNREAD_COUNT_KEY_PREFIX + userId + ":" + groupId);
        return Result.success();
    }

    @PostMapping("/message/find")
    public Result<ChatMessage> findMessage(@RequestBody FindMessageDTO dto) {
        Long userId = SecurityUtil.getUserId();
        if (userId == null) {
            return Result.error(401, "用户未登录");
        }
        if (dto == null || dto.getGroupId() == null) {
            return Result.error(400, "群组ID不能为空");
        }
        if (!chatGroupAccessService.canAccess(userId, dto.getGroupId())) {
            return Result.error(403, "无权访问该聊天群组");
        }

        try {
            LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ChatMessage::getGroupId, dto.getGroupId())
                    .eq(dto.getSenderId() != null, ChatMessage::getSenderId, dto.getSenderId())
                    .eq(dto.getContent() != null, ChatMessage::getContent, dto.getContent())
                    .eq(dto.getMessageType() != null, ChatMessage::getMessageType, dto.getMessageType())
                    .eq(ChatMessage::getDeleted, 0)
                    .orderByDesc(ChatMessage::getCreateTime)
                    .last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY");

            return Result.success(chatMessageMapper.selectOne(wrapper));
        } catch (Exception exception) {
            log.error("Failed to locate chat message in groupId={}", dto.getGroupId(), exception);
            return Result.error("查找消息失败");
        }
    }

    @DeleteMapping("/message/{messageId}")
    public Result<Void> deleteMessage(@PathVariable Long messageId) {
        Long userId = SecurityUtil.getUserId();
        if (userId == null) {
            return Result.error(401, "用户未登录");
        }

        try {
            ChatMessage message = chatMessageMapper.selectById(messageId);
            if (message == null || Integer.valueOf(1).equals(message.getDeleted())) {
                return Result.error(404, "消息不存在");
            }
            if (!chatGroupAccessService.canAccess(userId, message.getGroupId())) {
                return Result.error(403, "无权访问该聊天群组");
            }
            if (!userId.equals(message.getSenderId())) {
                return Result.error(403, "只能删除自己发送的消息");
            }

            return chatMessageMapper.deleteById(messageId) > 0
                    ? Result.success()
                    : Result.error("删除失败");
        } catch (Exception exception) {
            log.error("Failed to delete chat messageId={}", messageId, exception);
            return Result.error("删除消息失败");
        }
    }

    private int parseCount(List<String> counts, int index) {
        if (counts == null || index < 0 || index >= counts.size() || counts.get(index) == null) {
            return 0;
        }
        try {
            return Integer.parseInt(counts.get(index));
        } catch (NumberFormatException exception) {
            return 0;
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
