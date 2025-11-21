package org.example.persion.controller;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.entity.ElderlyInfo;
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
}
