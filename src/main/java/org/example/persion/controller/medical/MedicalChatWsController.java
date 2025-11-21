package org.example.persion.controller.medical;

import lombok.RequiredArgsConstructor;
import org.example.persion.entity.ChatMessage;
import org.example.persion.entity.User;
import org.example.persion.repository.ChatMessageMapper;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.repository.ElderlyMedicalRelationMapper;
import org.example.persion.repository.UserMapper;
import org.example.persion.vo.MessageVO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

@Controller
@RequiredArgsConstructor
public class MedicalChatWsController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ElderlyFamilyRelationMapper familyRelationMapper;
    private final ElderlyMedicalRelationMapper medicalRelationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;

    private static final String UNREAD_COUNT_KEY_PREFIX = "unread:count:";

    @MessageMapping("/chat/group/{groupId}")
    public void handleGroupMessage(@DestinationVariable Long groupId, String content, SimpMessageHeaderAccessor headerAccessor) {
        // 从WebSocket会话中获取用户信息
        Long senderId = (Long) headerAccessor.getSessionAttributes().get("userId");
        String senderUsername = (String) headerAccessor.getSessionAttributes().get("username");
        String senderRole = (String) headerAccessor.getSessionAttributes().get("role");
        
        if (senderId == null) {
            System.out.println("No user ID found in WebSocket session");
            return; // Not authenticated
        }
        
        User sender = userMapper.selectById(senderId);
        if (sender == null) {
            System.out.println("User not found in database: " + senderId);
            return; // User not found
        }
        
        System.out.println("Processing message from user: " + senderId + " (" + senderUsername + ") to group: " + groupId);

        // 1. Create ChatMessage entity for DB
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setGroupId(groupId);
        chatMessage.setSenderId(sender.getId());
        chatMessage.setSenderName(sender.getRealName() != null ? sender.getRealName() : sender.getUsername());
        chatMessage.setSenderRole(sender.getRole());
        chatMessage.setMessageType("text");
        chatMessage.setContent(content);

        // 2. Save to SQL Server (MyBatis-Plus will auto-fill create_time)
        chatMessageMapper.insert(chatMessage);

        // 3. Get all members of the group
        List<User> familyMembers = familyRelationMapper.selectUsersByElderlyId(groupId);
        List<User> medicalMembers = medicalRelationMapper.selectUsersByElderlyId(groupId);

        // 4. Broadcast message to all group members (including sender)
        List<User> allMembers = Stream.concat(familyMembers.stream(), medicalMembers.stream())
                .distinct()
                .toList();
        
        System.out.println("Broadcasting message to " + allMembers.size() + " members");
        
        for (User member : allMembers) {
            // Increment unread count in Redis for everyone except the sender
            if (!member.getId().equals(sender.getId())) {
                String unreadKey = UNREAD_COUNT_KEY_PREFIX + member.getId() + ":" + groupId;
                redisTemplate.opsForValue().increment(unreadKey);
            }

            // Create a personalized VO for each recipient
            MessageVO personalMessageVO = createMessageVO(chatMessage, member.getId());
            
            System.out.println("Sending message to user: " + member.getUsername() + " (ID: " + member.getId() + ")");
            
            // Send message to the user's personal queue
            messagingTemplate.convertAndSendToUser(
                member.getUsername(),
                "/queue/group-messages",
                personalMessageVO
            );
        }
        
        // Also broadcast to the general group topic for immediate display
        MessageVO generalMessageVO = createMessageVO(chatMessage, senderId);
        messagingTemplate.convertAndSend("/topic/group/" + groupId, generalMessageVO);
        System.out.println("Message broadcasted to topic: /topic/group/" + groupId);
    }

    private MessageVO createMessageVO(ChatMessage chatMessage, Long currentUserId) {
        MessageVO vo = new MessageVO();
        vo.setGroupId(chatMessage.getGroupId());
        vo.setSenderId(chatMessage.getSenderId());
        vo.setSenderName(chatMessage.getSenderName());
        vo.setSenderRole(chatMessage.getSenderRole());
        vo.setContent(chatMessage.getContent());
        vo.setMessageType(chatMessage.getMessageType());
        // Format time from entity's createTime, which is now a LocalDateTime
        if (chatMessage.getCreateTime() != null) {
            vo.setTime(chatMessage.getCreateTime().format(DateTimeFormatter.ISO_DATE_TIME));
        }
        if (currentUserId != null) {
            vo.setMe(currentUserId.equals(chatMessage.getSenderId()));
        }
        return vo;
    }
}