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
        try {
            // 从WebSocket会话中获取用户信息
            Long senderId = (Long) headerAccessor.getSessionAttributes().get("userId");
            String senderUsername = (String) headerAccessor.getSessionAttributes().get("username");
            
            if (senderId == null) {
                System.err.println("错误: WebSocket会话中未找到用户ID。消息无法处理。");
                return; // Not authenticated
            }
            
            User sender = userMapper.selectById(senderId);
            if (sender == null) {
                System.err.println("错误: 在数据库中未找到ID为 " + senderId + " 的用户。");
                return; // User not found
            }
            
            System.out.println("正在处理来自用户 " + senderId + " (" + senderUsername + ") 发往群组 " + groupId + " 的消息。");

            // 1. 解析消息内容
            String messageType = "TEXT";
            String messageContent = content;
            String audioUrl = null;
            String imageUrl = null;
            String fileName = null;
            String fileUrl = null;
            Integer duration = null;
            
            if (content.startsWith("{")) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(content);
                    
                    if (jsonNode.has("messageType")) {
                        messageType = jsonNode.get("messageType").asText();
                        
                        if (jsonNode.has("content")) {
                            messageContent = jsonNode.get("content").asText();
                        }
                        
                        if ("VOICE".equals(messageType)) {
                            audioUrl = jsonNode.has("audioUrl") ? jsonNode.get("audioUrl").asText() : null;
                            duration = jsonNode.has("duration") ? jsonNode.get("duration").asInt() : null;
                        } else if ("IMAGE".equals(messageType)) {
                            imageUrl = jsonNode.has("imageUrl") ? jsonNode.get("imageUrl").asText() : null;
                        } else if ("FILE".equals(messageType)) {
                            fileName = jsonNode.has("fileName") ? jsonNode.get("fileName").asText() : null;
                            fileUrl = jsonNode.has("fileUrl") ? jsonNode.get("fileUrl").asText() : null;
                            System.out.println("DEBUG - 解析FILE消息: fileName=" + fileName + ", fileUrl=" + fileUrl);
                            System.out.println("DEBUG - 原始JSON: " + jsonNode.toString());
                        } else if ("delete".equals(messageType)) {
                            // 处理删除消息
                            Long messageId = jsonNode.has("messageId") ? jsonNode.get("messageId").asLong() : null;
                            if (messageId != null) {
                                handleDeleteMessage(groupId, messageId, senderId);
                            }
                            return;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("警告: 消息内容解析为JSON失败，将作为纯文本处理。错误: " + e.getMessage());
                }
            }

            // 2. 创建并填充ChatMessage实体
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setGroupId(groupId);
            chatMessage.setSenderId(sender.getId());
            chatMessage.setSenderName(sender.getRealName() != null ? sender.getRealName() : sender.getUsername());
            chatMessage.setSenderRole(sender.getRole());
            chatMessage.setMessageType(messageType);
            chatMessage.setContent(messageContent);
            
            if ("VOICE".equals(messageType)) {
                chatMessage.setAudioUrl(audioUrl);
                chatMessage.setDuration(duration);
            } else if ("IMAGE".equals(messageType)) {
                chatMessage.setImageUrl(imageUrl);
            } else if ("FILE".equals(messageType)) {
                System.out.println("DEBUG - 保存FILE消息: fileName=" + fileName + ", fileUrl=" + fileUrl);
                chatMessage.setFileName(fileName);
                chatMessage.setFileUrl(fileUrl);
            }

            java.time.LocalDateTime currentTime = java.time.LocalDateTime.now();
            chatMessage.setCreateTime(currentTime);
            chatMessage.setUpdateTime(currentTime);
            chatMessage.setDeleted(0); // 设置默认的删除状态为0 (未删除)
            
            // 3. 保存到数据库
            System.out.println("准备将消息存入数据库: " + chatMessage);
            chatMessageMapper.insert(chatMessage);
            System.out.println("消息成功存入数据库! ID: " + chatMessage.getId());

            // 4. 广播消息
            List<User> familyMembers = familyRelationMapper.selectUsersByElderlyId(groupId);
            List<User> medicalMembers = medicalRelationMapper.selectUsersByElderlyId(groupId);
            List<User> allMembers = Stream.concat(familyMembers.stream(), medicalMembers.stream()).distinct().toList();
            
            System.out.println("准备向 " + allMembers.size() + " 位群组成员广播消息。");
            
            for (User member : allMembers) {
                if (!member.getId().equals(sender.getId())) {
                    String unreadKey = UNREAD_COUNT_KEY_PREFIX + member.getId() + ":" + groupId;
                    redisTemplate.opsForValue().increment(unreadKey);
                }
                MessageVO personalMessageVO = createMessageVO(chatMessage, member.getId());
                messagingTemplate.convertAndSendToUser(member.getUsername(), "/queue/group-messages", personalMessageVO);
            }
            
            MessageVO generalMessageVO = createMessageVO(chatMessage, senderId);
            messagingTemplate.convertAndSend("/topic/group/" + groupId, generalMessageVO);
            System.out.println("消息已成功广播到群组话题: /topic/group/" + groupId);

        } catch (Exception e) {
            // !!! 关键的错误捕获 !!!
            System.err.println("!!!!!! 处理WebSocket消息时发生严重错误 !!!!!!");
            e.printStackTrace();
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
    }

    private MessageVO createMessageVO(ChatMessage chatMessage, Long currentUserId) {
        MessageVO vo = new MessageVO();
        vo.setId(chatMessage.getId());
        vo.setGroupId(chatMessage.getGroupId());
        vo.setSenderId(chatMessage.getSenderId());
        vo.setSenderName(chatMessage.getSenderName());
        vo.setSenderRole(chatMessage.getSenderRole());
        vo.setContent(chatMessage.getContent());
        vo.setMessageType(chatMessage.getMessageType());
        
        // 设置多媒体字段 - 将绝对路径转换为相对路径
        vo.setAudioUrl(convertToRelativePath(chatMessage.getAudioUrl()));
        vo.setImageUrl(convertToRelativePath(chatMessage.getImageUrl()));
        vo.setDuration(chatMessage.getDuration());
        
        // 设置文件字段
        if (chatMessage.getFileName() != null) {
            vo.setFileName(chatMessage.getFileName());
        }
        if (chatMessage.getFileUrl() != null) {
            vo.setFileUrl(convertToRelativePath(chatMessage.getFileUrl()));
        }
        
        // 调试日志
        if ("FILE".equals(chatMessage.getMessageType())) {
            System.out.println("DEBUG - createMessageVO FILE消息: fileName=" + chatMessage.getFileName() + 
                             ", fileUrl=" + chatMessage.getFileUrl() + 
                             ", content=" + chatMessage.getContent());
        }
        
        // Format time from entity's createTime, which is now a LocalDateTime
        if (chatMessage.getCreateTime() != null) {
            // 使用ISO格式，确保时间正确传递
            vo.setTime(chatMessage.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            System.out.println("WebSocket - 消息时间: " + vo.getTime() + " (原始: " + chatMessage.getCreateTime() + ")");
        } else {
            // 如果没有创建时间，使用当前时间
            vo.setTime(java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            System.out.println("WebSocket - 使用当前时间: " + vo.getTime());
        }
        
        if (currentUserId != null) {
            vo.setMe(currentUserId.equals(chatMessage.getSenderId()));
        }
        return vo;
    }
    
    /**
     * 将路径转换为Web访问路径
     */
    private String convertToRelativePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        
        // 如果已经是Web路径，直接返回
        if (path.startsWith("/uploads/")) {
            return path;
        }
        
        try {
            // 如果是绝对路径，转换为相对路径
            if (path.contains("uploads")) {
                int uploadsIndex = path.indexOf("uploads");
                String relativePath = "/" + path.substring(uploadsIndex).replace("\\", "/");
                System.out.println("WebSocket - 转换路径: " + path + " -> " + relativePath);
                return relativePath;
            }
        } catch (Exception e) {
            System.err.println("WebSocket - 路径转换失败: " + e.getMessage());
        }
        
        return path;
    }
    
    /**
     * 处理删除消息
     */
    private void handleDeleteMessage(Long groupId, Long messageId, Long senderId) {
        try {
            // 1. 验证消息是否存在且属于发送者
            ChatMessage existingMessage = chatMessageMapper.selectById(messageId);
            if (existingMessage == null) {
                System.err.println("要删除的消息不存在: messageId=" + messageId);
                return;
            }
            
            if (!existingMessage.getSenderId().equals(senderId)) {
                System.err.println("用户无权删除此消息: messageId=" + messageId + ", senderId=" + senderId);
                return;
            }
            
            // 2. 逻辑删除消息（设置deleted=1）
            existingMessage.setDeleted(1);
            int updatedRows = chatMessageMapper.updateById(existingMessage);
            if (updatedRows > 0) {
                System.out.println("消息已逻辑删除: messageId=" + messageId + ", deleted=1");
            } else {
                System.err.println("逻辑删除消息失败: messageId=" + messageId);
                return;
            }
            
            // 3. 获取群组所有成员
            List<User> familyMembers = familyRelationMapper.selectUsersByElderlyId(groupId);
            List<User> medicalMembers = medicalRelationMapper.selectUsersByElderlyId(groupId);
            List<User> allMembers = Stream.concat(familyMembers.stream(), medicalMembers.stream())
                    .distinct()
                    .toList();
            
            // 4. 创建删除通知消息
            MessageVO deleteNotification = new MessageVO();
            deleteNotification.setGroupId(groupId);
            deleteNotification.setMessageType("delete");
            deleteNotification.setSenderId(senderId);
            deleteNotification.setContent("消息已删除");
            deleteNotification.setTime(java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // 添加消息ID用于前端删除
            deleteNotification.setId(messageId);
            
            // 5. 广播删除通知给所有群组成员
            for (User member : allMembers) {
                messagingTemplate.convertAndSendToUser(
                    member.getUsername(),
                    "/queue/group-messages",
                    deleteNotification
                );
            }
            
            // 6. 也广播到群组主题
            messagingTemplate.convertAndSend("/topic/group/" + groupId, deleteNotification);
            
            System.out.println("删除消息通知已广播: messageId=" + messageId + ", groupId=" + groupId);
            
        } catch (Exception e) {
            System.err.println("处理删除消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}