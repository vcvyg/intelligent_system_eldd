package org.example.persion.controller.medical;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.persion.common.Result;
import org.example.persion.entity.ChatMessage;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.entity.User;
import org.example.persion.repository.ChatMessageMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.repository.ElderlyMedicalRelationMapper;
import org.example.persion.repository.UserMapper;
import org.example.persion.security.SecurityUtil;
import org.example.persion.vo.MessageVO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/medical/chat")
@RequiredArgsConstructor
public class MedicalChatController {

    private final ElderlyInfoMapper elderlyInfoMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ElderlyFamilyRelationMapper familyRelationMapper;
    private final ElderlyMedicalRelationMapper medicalRelationMapper;
    private final UserMapper userMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String UNREAD_COUNT_KEY_PREFIX = "unread:count:";

    /**
     * 获取当前医护人员负责的老人群组列表
     */
    @GetMapping("/groups")
    public Result<List<GroupVO>> getChatGroups() {
        Long medicalUserId = SecurityUtil.getUserId();
        if (medicalUserId == null) {
            return Result.error("无法获取当前用户信息");
        }
        List<ElderlyInfo> elderlyList = elderlyInfoMapper.selectElderlyListByMedicalUserId(medicalUserId);
        List<GroupVO> groupVOs = elderlyList.stream()
                .map(elderly -> {
                    // 创建更具体的组名
                    String groupName = elderly.getName() + "的沟通群";
                    GroupVO groupVO = new GroupVO(elderly.getId(), groupName);
                    
                    // 获取群组成员
                    List<User> familyMembers = familyRelationMapper.selectUsersByElderlyId(elderly.getId());
                    List<User> medicalMembers = medicalRelationMapper.selectUsersByElderlyId(elderly.getId());
                    
                    List<GroupMemberVO> members = Stream.concat(familyMembers.stream(), medicalMembers.stream())
                            .distinct()
                            .map(user -> new GroupMemberVO(
                                    user.getId(),
                                    user.getUsername(),
                                    user.getRealName(),
                                    user.getRole()
                            ))
                            .collect(Collectors.toList());
                    
                    groupVO.setMembers(members);
                    return groupVO;
                })
                .collect(Collectors.toList());
        return Result.success(groupVOs);
    }

    /**
     * 获取指定群组的详细信息
     */
    @GetMapping("/group/{groupId}/info")
    public Result<GroupDetailVO> getGroupInfo(@PathVariable Long groupId) {
        try {
            // 获取老人信息
            ElderlyInfo elderly = elderlyInfoMapper.selectById(groupId);
            if (elderly == null) {
                return Result.error("群组不存在");
            }
            
            // 获取群组成员
            List<User> familyMembers = familyRelationMapper.selectUsersByElderlyId(groupId);
            List<User> medicalMembers = medicalRelationMapper.selectUsersByElderlyId(groupId);
            
            List<GroupMemberVO> members = Stream.concat(familyMembers.stream(), medicalMembers.stream())
                    .distinct()
                    .map(user -> new GroupMemberVO(
                            user.getId(),
                            user.getUsername(),
                            user.getRealName(),
                            user.getRole()
                    ))
                    .collect(Collectors.toList());
            
            GroupDetailVO groupDetail = new GroupDetailVO();
            groupDetail.setGroupId(groupId);
            groupDetail.setGroupName(elderly.getName() + "的沟通群");
            groupDetail.setElderlyName(elderly.getName());
            groupDetail.setMembers(members);
            
            return Result.success(groupDetail);
        } catch (Exception e) {
            return Result.error("获取群组信息失败");
        }
    }

    /**
     * 获取指定群组的聊天记录（分页）
     */
    @GetMapping("/group/{groupId}/messages")
    public Result<Page<MessageVO>> getGroupMessages(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {

        try {
            System.out.println("MedicalChatController - 获取群组消息请求: groupId=" + groupId + ", current=" + current + ", size=" + size);
            
            // 检查当前用户
            Long currentUserId = SecurityUtil.getUserId();
            System.out.println("MedicalChatController - 当前用户ID: " + currentUserId);
            
            if (currentUserId == null) {
                System.err.println("MedicalChatController - 用户未登录或认证失败");
                return Result.error("用户未登录");
            }

            Page<ChatMessage> page = new Page<>(current, size);
            LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ChatMessage::getGroupId, groupId);
            wrapper.eq(ChatMessage::getDeleted, 0); // 只查询未删除的消息
            wrapper.orderByDesc(ChatMessage::getCreateTime); // 先取最新消息

            System.out.println("MedicalChatController - 执行数据库查询...");
            Page<ChatMessage> chatMessagePage = chatMessageMapper.selectPage(page, wrapper);
            System.out.println("MedicalChatController - 查询结果: 总数=" + chatMessagePage.getTotal() + ", 当前页记录数=" + chatMessagePage.getRecords().size());
            List<ChatMessage> records = chatMessagePage.getRecords();
            Collections.reverse(records); // 还原为时间正序，便于前端展示

            Page<MessageVO> voPage = new Page<>(chatMessagePage.getCurrent(), chatMessagePage.getSize(), chatMessagePage.getTotal());
            List<MessageVO> vos = records.stream().map(msg -> {
                MessageVO vo = new MessageVO();
                vo.setId(msg.getId()); // 设置消息ID - 这是关键修复！
                vo.setGroupId(msg.getGroupId());
                vo.setSenderId(msg.getSenderId());
                vo.setSenderName(msg.getSenderName());
                vo.setSenderRole(msg.getSenderRole());
                vo.setContent(msg.getContent());
                vo.setMessageType(msg.getMessageType());
                
                // 设置多媒体字段
                vo.setAudioUrl(convertToRelativePath(msg.getAudioUrl()));
                vo.setImageUrl(convertToRelativePath(msg.getImageUrl()));
                vo.setDuration(msg.getDuration());
                
                // 设置文件字段 - 这是关键修复！
                vo.setFileName(msg.getFileName());
                vo.setFileUrl(convertToRelativePath(msg.getFileUrl()));
                
                // 正确格式化时间
                if (msg.getCreateTime() != null) {
                    vo.setTime(msg.getCreateTime().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                } else {
                    vo.setTime(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                }
                vo.setMe(SecurityUtil.getUserId() != null && SecurityUtil.getUserId().equals(msg.getSenderId()));
                return vo;
            }).collect(Collectors.toList());

            voPage.setRecords(vos);
            System.out.println("MedicalChatController - 返回成功响应，消息数量: " + vos.size());
            return Result.success(voPage);
            
        } catch (Exception e) {
            System.err.println("MedicalChatController - 获取群组消息失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("获取消息失败: " + e.getMessage());
        }
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
                System.out.println("MedicalChatController - 转换路径: " + path + " -> " + relativePath);
                return relativePath;
            }
        } catch (Exception e) {
            System.err.println("MedicalChatController - 路径转换失败: " + e.getMessage());
        }
        
        return path;
    }

    /**
     * HTTP API端点：发送消息（WebSocket降级方案）
     */
    @PostMapping("/group/{groupId}/send")
    public Result<MessageVO> sendMessage(@PathVariable Long groupId, @RequestBody String content) {
        try {
            Long senderId = SecurityUtil.getUserId();
            if (senderId == null) {
                return Result.error("用户未登录");
            }
            
            User sender = userMapper.selectById(senderId);
            if (sender == null) {
                return Result.error("用户不存在");
            }
            
            // 解析消息内容
            String messageType = "TEXT";
            String messageContent = content;
            String audioUrl = null;
            String imageUrl = null;
            String fileName = null;
            String fileUrl = null;
            Integer duration = null;
            
            if (content != null && content.startsWith("{")) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jsonNode = mapper.readTree(content);
                    
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
                        }
                    }
                } catch (Exception e) {
                    System.err.println("警告: 消息内容解析为JSON失败，将作为纯文本处理。错误: " + e.getMessage());
                }
            }
            
            // 创建并填充ChatMessage实体
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
                chatMessage.setFileName(fileName);
                chatMessage.setFileUrl(fileUrl);
            }
            
            LocalDateTime currentTime = LocalDateTime.now();
            chatMessage.setCreateTime(currentTime);
            chatMessage.setUpdateTime(currentTime);
            chatMessage.setDeleted(0);
            
            // 保存到数据库
            chatMessageMapper.insert(chatMessage);
            
            // 尝试通过WebSocket广播消息（如果WebSocket可用）
            try {
                List<User> familyMembers = familyRelationMapper.selectUsersByElderlyId(groupId);
                List<User> medicalMembers = medicalRelationMapper.selectUsersByElderlyId(groupId);
                List<User> allMembers = Stream.concat(familyMembers.stream(), medicalMembers.stream())
                        .distinct()
                        .collect(Collectors.toList());
                
                for (User member : allMembers) {
                    if (!member.getId().equals(sender.getId())) {
                        String unreadKey = UNREAD_COUNT_KEY_PREFIX + member.getId() + ":" + groupId;
                        redisTemplate.opsForValue().increment(unreadKey);
                    }
                    MessageVO personalMessageVO = createMessageVO(chatMessage, member.getId());
                    String username = member.getUsername();
                    if (username != null) {
                        messagingTemplate.convertAndSendToUser(username, "/queue/group-messages", personalMessageVO);
                    }
                }
                
                MessageVO generalMessageVO = createMessageVO(chatMessage, senderId);
                if (generalMessageVO != null) {
                    messagingTemplate.convertAndSend("/topic/group/" + groupId, generalMessageVO);
                }
            } catch (Exception e) {
                // WebSocket广播失败不影响消息保存
                System.err.println("WebSocket广播失败（消息已保存到数据库）: " + e.getMessage());
            }
            
            // 返回消息VO
            MessageVO messageVO = createMessageVO(chatMessage, senderId);
            return Result.success(messageVO);
            
        } catch (Exception e) {
            System.err.println("发送消息失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("发送消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建MessageVO
     */
    private MessageVO createMessageVO(ChatMessage chatMessage, Long currentUserId) {
        MessageVO vo = new MessageVO();
        vo.setId(chatMessage.getId());
        vo.setGroupId(chatMessage.getGroupId());
        vo.setSenderId(chatMessage.getSenderId());
        vo.setSenderName(chatMessage.getSenderName());
        vo.setSenderRole(chatMessage.getSenderRole());
        vo.setContent(chatMessage.getContent());
        vo.setMessageType(chatMessage.getMessageType());
        
        vo.setAudioUrl(convertToRelativePath(chatMessage.getAudioUrl()));
        vo.setImageUrl(convertToRelativePath(chatMessage.getImageUrl()));
        vo.setDuration(chatMessage.getDuration());
        vo.setFileName(chatMessage.getFileName());
        vo.setFileUrl(convertToRelativePath(chatMessage.getFileUrl()));
        
        if (chatMessage.getCreateTime() != null) {
            vo.setTime(chatMessage.getCreateTime().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } else {
            vo.setTime(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        
        if (currentUserId != null) {
            vo.setMe(currentUserId.equals(chatMessage.getSenderId()));
        }
        return vo;
    }

    @Data
    public static class GroupVO {
        private Long groupId;
        private String groupName;
        private List<GroupMemberVO> members;

        public GroupVO(Long groupId, String groupName) {
            this.groupId = groupId;
            this.groupName = groupName;
        }
    }
    
    @Data
    public static class GroupMemberVO {
        private Long userId;
        private String username;
        private String realName;
        private String role;
        
        public GroupMemberVO(Long userId, String username, String realName, String role) {
            this.userId = userId;
            this.username = username;
            this.realName = realName;
            this.role = role;
        }
    }
    
    @Data
    public static class GroupDetailVO {
        private Long groupId;
        private String groupName;
        private String elderlyName;
        private List<GroupMemberVO> members;
    }
}
