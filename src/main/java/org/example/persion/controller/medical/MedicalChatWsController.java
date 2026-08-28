package org.example.persion.controller.medical;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.persion.entity.ChatMessage;
import org.example.persion.entity.User;
import org.example.persion.repository.ChatMessageMapper;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.repository.ElderlyMedicalRelationMapper;
import org.example.persion.repository.UserMapper;
import org.example.persion.service.ChatGroupAccessService;
import org.example.persion.vo.MessageVO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Canonical WebSocket write path for family/medical chat messages.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MedicalChatWsController {

    private static final String UNREAD_COUNT_KEY_PREFIX = "unread:count:";
    private static final int MAX_TEXT_LENGTH = 4000;
    private static final Set<String> SUPPORTED_TYPES = Set.of("TEXT", "VOICE", "IMAGE", "FILE", "delete");

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ElderlyFamilyRelationMapper familyRelationMapper;
    private final ElderlyMedicalRelationMapper medicalRelationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final ChatGroupAccessService chatGroupAccessService;

    @MessageMapping("/chat/group/{groupId}")
    public void handleGroupMessage(@DestinationVariable Long groupId,
                                   String content,
                                   SimpMessageHeaderAccessor headerAccessor) {
        try {
            Long senderId = sessionUserId(headerAccessor);
            if (senderId == null || !chatGroupAccessService.canAccess(senderId, groupId)) {
                log.warn("Rejected unauthorized WebSocket group write, groupId={}", groupId);
                return;
            }

            User sender = userMapper.selectById(senderId);
            if (sender == null) {
                log.warn("Rejected WebSocket message for missing userId={}", senderId);
                return;
            }

            ParsedMessage parsed = parse(content);
            if (parsed == null || !SUPPORTED_TYPES.contains(parsed.type)) {
                log.warn("Rejected unsupported WebSocket message type");
                return;
            }

            if ("delete".equals(parsed.type)) {
                if (parsed.messageId != null) {
                    handleDeleteMessage(groupId, parsed.messageId, senderId);
                }
                return;
            }

            if ("TEXT".equals(parsed.type)) {
                if (parsed.content == null || parsed.content.isBlank() || parsed.content.length() > MAX_TEXT_LENGTH) {
                    log.warn("Rejected empty or oversized chat text, groupId={}", groupId);
                    return;
                }
            } else if (!validAttachmentPath(parsed.attachmentUrl())) {
                log.warn("Rejected non-upload attachment path, groupId={}, type={}", groupId, parsed.type);
                return;
            }

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setGroupId(groupId);
            chatMessage.setSenderId(sender.getId());
            chatMessage.setSenderName(sender.getRealName() != null ? sender.getRealName() : sender.getUsername());
            chatMessage.setSenderRole(sender.getRole());
            chatMessage.setMessageType(parsed.type);
            chatMessage.setContent(parsed.content);

            switch (parsed.type) {
                case "VOICE" -> {
                    chatMessage.setAudioUrl(parsed.audioUrl);
                    chatMessage.setDuration(parsed.duration);
                }
                case "IMAGE" -> chatMessage.setImageUrl(parsed.imageUrl);
                case "FILE" -> {
                    chatMessage.setFileName(safeFileName(parsed.fileName));
                    chatMessage.setFileUrl(parsed.fileUrl);
                }
                default -> {
                    // TEXT has no attachment fields.
                }
            }

            LocalDateTime now = LocalDateTime.now();
            chatMessage.setCreateTime(now);
            chatMessage.setUpdateTime(now);
            chatMessage.setDeleted(0);
            chatMessageMapper.insert(chatMessage);

            broadcast(chatMessage, senderId, groupId);
        } catch (Exception exception) {
            // Do not log message content or attachment paths: chat data may contain
            // personal/medical information.
            log.error("WebSocket chat processing failed for groupId={}: {}",
                    groupId, exception.getClass().getSimpleName());
        }
    }

    private ParsedMessage parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        if (!raw.trim().startsWith("{")) {
            ParsedMessage text = new ParsedMessage();
            text.type = "TEXT";
            text.content = raw;
            return text;
        }

        try {
            JsonNode node = objectMapper.readTree(raw);
            ParsedMessage parsed = new ParsedMessage();
            parsed.type = node.has("messageType") ? node.get("messageType").asText("TEXT") : "TEXT";
            parsed.content = node.has("content") ? node.get("content").asText("") : "";
            parsed.audioUrl = text(node, "audioUrl");
            parsed.imageUrl = text(node, "imageUrl");
            parsed.fileUrl = text(node, "fileUrl");
            parsed.fileName = text(node, "fileName");
            parsed.duration = node.has("duration") ? node.get("duration").asInt() : null;
            parsed.messageId = node.has("messageId") ? node.get("messageId").asLong() : null;
            return parsed;
        } catch (Exception exception) {
            // Invalid JSON is treated as plain text for backwards compatibility.
            ParsedMessage text = new ParsedMessage();
            text.type = "TEXT";
            text.content = raw;
            return text;
        }
    }

    private String text(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private void broadcast(ChatMessage chatMessage, Long senderId, Long groupId) {
        List<User> familyMembers = familyRelationMapper.selectUsersByElderlyId(groupId);
        List<User> medicalMembers = medicalRelationMapper.selectUsersByElderlyId(groupId);
        List<User> allMembers = Stream.concat(familyMembers.stream(), medicalMembers.stream())
                .distinct()
                .toList();

        for (User member : allMembers) {
            if (!member.getId().equals(senderId)) {
                String unreadKey = UNREAD_COUNT_KEY_PREFIX + member.getId() + ":" + groupId;
                redisTemplate.opsForValue().increment(unreadKey);
            }
            if (member.getUsername() != null) {
                messagingTemplate.convertAndSendToUser(
                        member.getUsername(),
                        "/queue/group-messages",
                        createMessageVO(chatMessage, member.getId())
                );
            }
        }

        messagingTemplate.convertAndSend(
                "/topic/group/" + groupId,
                createMessageVO(chatMessage, senderId)
        );
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
        vo.setAudioUrl(toWebUploadPath(chatMessage.getAudioUrl()));
        vo.setImageUrl(toWebUploadPath(chatMessage.getImageUrl()));
        vo.setDuration(chatMessage.getDuration());
        vo.setFileName(chatMessage.getFileName());
        vo.setFileUrl(toWebUploadPath(chatMessage.getFileUrl()));
        vo.setTime((chatMessage.getCreateTime() == null ? LocalDateTime.now() : chatMessage.getCreateTime())
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        vo.setMe(currentUserId != null && currentUserId.equals(chatMessage.getSenderId()));
        return vo;
    }

    private void handleDeleteMessage(Long groupId, Long messageId, Long senderId) {
        ChatMessage existing = chatMessageMapper.selectById(messageId);
        if (existing == null
                || !groupId.equals(existing.getGroupId())
                || !senderId.equals(existing.getSenderId())
                || Integer.valueOf(1).equals(existing.getDeleted())) {
            log.warn("Rejected invalid chat delete request, groupId={}, messageId={}", groupId, messageId);
            return;
        }

        existing.setDeleted(1);
        existing.setUpdateTime(LocalDateTime.now());
        if (chatMessageMapper.updateById(existing) <= 0) {
            return;
        }

        MessageVO notification = new MessageVO();
        notification.setId(messageId);
        notification.setGroupId(groupId);
        notification.setMessageType("delete");
        notification.setSenderId(senderId);
        notification.setContent("消息已删除");
        notification.setTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        List<User> allMembers = Stream.concat(
                        familyRelationMapper.selectUsersByElderlyId(groupId).stream(),
                        medicalRelationMapper.selectUsersByElderlyId(groupId).stream())
                .distinct()
                .toList();
        for (User member : allMembers) {
            if (member.getUsername() != null) {
                messagingTemplate.convertAndSendToUser(member.getUsername(), "/queue/group-messages", notification);
            }
        }
        messagingTemplate.convertAndSend("/topic/group/" + groupId, notification);
    }

    private Long sessionUserId(SimpMessageHeaderAccessor accessor) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        if (attributes == null) {
            return null;
        }
        Object userId = attributes.get("userId");
        return userId instanceof Long ? (Long) userId : null;
    }

    private boolean validAttachmentPath(String path) {
        return path != null
                && path.startsWith("/uploads/")
                && !path.contains("..")
                && !path.contains("\\")
                && path.length() <= 500;
    }

    private String safeFileName(String filename) {
        if (filename == null || filename.isBlank()) {
            return "附件";
        }
        String normalized = filename.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String name = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        name = name.replaceAll("[\\r\\n\\t]", "_");
        return name.length() > 255 ? name.substring(0, 255) : name;
    }

    private String toWebUploadPath(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        if (validAttachmentPath(path)) {
            return path;
        }

        // Backwards compatibility for historical records that stored an absolute
        // path. Only expose the suffix rooted at uploads, never the original path.
        String normalized = path.replace('\\', '/');
        int index = normalized.indexOf("uploads/");
        if (index >= 0) {
            String webPath = "/" + normalized.substring(index);
            return validAttachmentPath(webPath) ? webPath : null;
        }
        return null;
    }

    private static final class ParsedMessage {
        private String type;
        private String content;
        private String audioUrl;
        private String imageUrl;
        private String fileUrl;
        private String fileName;
        private Integer duration;
        private Long messageId;

        private String attachmentUrl() {
            return switch (type) {
                case "VOICE" -> audioUrl;
                case "IMAGE" -> imageUrl;
                case "FILE" -> fileUrl;
                default -> null;
            };
        }
    }
}
