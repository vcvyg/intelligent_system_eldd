package org.example.persion.controller.medical;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.example.persion.common.Result;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.example.persion.repository.UserMapper;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.entity.User;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/medical/chat")
@RequiredArgsConstructor
public class MedicalChatController {
    // Redis存储消息
    private static final String CHAT_KEY_PREFIX = "medical:chat:messages:"; // medical:chat:messages:{userId}

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ElderlyFamilyRelationMapper elderlyFamilyRelationMapper;

    // 获取子女列表
    @GetMapping("/users")
    public Result<List<UserVO>> getUsers() {
        List<UserVO> list = new ArrayList<>();

        // 从数据库查询所有FAMILY角色的用户
        List<User> familyUsers = userMapper.selectFamilyUsers();

        for (User user : familyUsers) {
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setName(user.getRealName() != null ? user.getRealName() : user.getUsername());

            // 查询该子女关联的老人信息
            List<Map<String, Object>> elderlyList = elderlyFamilyRelationMapper.selectElderlyListByFamilyUser(user.getId());
            if (elderlyList != null && !elderlyList.isEmpty()) {
                // 获取第一个关联的老人姓名
                Object elderlyName = elderlyList.get(0).get("name");
                vo.setElderlyName(elderlyName != null ? elderlyName.toString() : null);
            }

            list.add(vo);
        }

        return Result.success(list);
    }

    // 获取与某子女的消息
    @GetMapping("/messages")
    public Result<List<MessageVO>> getMessages(@RequestParam Long userId) {
        String redisKey = CHAT_KEY_PREFIX + userId;
        List<Object> rawList = redisTemplate.opsForList().range(redisKey, 0, -1);
        List<MessageVO> vos = new ArrayList<>();
        if (rawList != null) {
            for (Object obj : rawList) {
                if (obj instanceof ChatMessage) {
                    ChatMessage m = (ChatMessage) obj;
                    MessageVO vo = new MessageVO();
                    vo.setContent(m.getContent());
                    vo.setTime(m.getTime());
                    vo.setMe(m.isMe());
                    vo.setType(m.getType());
                    vo.setFileName(m.getFileName());
                    vos.add(vo);
                }
            }
        }
        return Result.success(vos);
    }

    // 发送消息
    @PostMapping("/send")
    public Result<Void> send(@RequestBody SendDTO dto) {
        // 这里假设医护端发送的消息 me=true
        ChatMessage msg = new ChatMessage();
        msg.setContent(dto.getContent());
        msg.setTime(LocalDateTime.now().toString());
        msg.setMe(true);
        msg.setType(dto.getType() == null ? "text" : dto.getType());
        msg.setFileName(dto.getFileName());
        String redisKey = CHAT_KEY_PREFIX + dto.getToUserId();
        redisTemplate.opsForList().rightPush(redisKey, msg);
        // 可选：限制每个会话最多保存100条消息
        redisTemplate.opsForList().trim(redisKey, -100, -1);
        return Result.success();
    }

    @Data
    public static class UserVO {
        private Long id;
        private String name;
        private String elderlyName;
    }

    @Data
    public static class MessageVO {
        private String content;
        private String time;
        private boolean me;
        private String type; // text/image/audio
        private String fileName;
    }

    @Data
    public static class SendDTO {
        private Long toUserId;
        private String content;
        private String type; // text/image/audio
        private String fileName;
    }

    @Data
    public static class ChatMessage {
        private String content;
        private String time;
        private boolean me;
        private String type; // text/image/audio
        private String fileName;
    }
}
