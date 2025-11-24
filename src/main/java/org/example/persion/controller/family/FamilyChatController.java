package org.example.persion.controller.family;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.entity.ChatMessage;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.entity.User;
import org.example.persion.repository.ChatMessageMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.repository.ElderlyMedicalRelationMapper;
import org.example.persion.security.SecurityUtil;
import org.example.persion.vo.MessageVO;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/family/chat")
@RequiredArgsConstructor
public class FamilyChatController {

    private final ElderlyInfoMapper elderlyInfoMapper;
    private final ElderlyFamilyRelationMapper familyRelationMapper;
    private final ElderlyMedicalRelationMapper medicalRelationMapper;
    private final ChatMessageMapper chatMessageMapper;

    /**
     * 获取当前家庭成员关联的老人群组列表
     */
    @GetMapping("/groups")
    public Result<List<GroupVO>> getChatGroups() {
        Long familyUserId = SecurityUtil.getUserId();
        if (familyUserId == null) {
            return Result.error("无法获取当前用户信息");
        }

        // 调用Mapper获取该家庭成员关联的老人列表
        List<ElderlyInfo> elderlyList = elderlyInfoMapper.selectElderlyListByFamilyUserId(familyUserId);

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
                                    user.getRole()))
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
                            user.getRole()))
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
            @RequestParam(defaultValue = "50") Integer size) {

        Page<ChatMessage> page = new Page<>(current, size);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getGroupId, groupId);
        wrapper.eq(ChatMessage::getDeleted, 0); // 只查询未删除的消息
        wrapper.orderByDesc(ChatMessage::getCreateTime); // 先按时间倒序获取最新消息

        Page<ChatMessage> chatMessagePage = chatMessageMapper.selectPage(page, wrapper);
        List<ChatMessage> records = chatMessagePage.getRecords();
        Collections.reverse(records); // 前端展示按时间正序

        Page<MessageVO> voPage = new Page<>(chatMessagePage.getCurrent(), chatMessagePage.getSize(),
                chatMessagePage.getTotal());
        List<MessageVO> vos = records.stream().map(msg -> {
            MessageVO vo = new MessageVO();
            vo.setId(msg.getId()); // 设置消息ID，用于删除功能
            vo.setGroupId(msg.getGroupId());
            vo.setSenderId(msg.getSenderId());
            vo.setSenderName(msg.getSenderName());
            vo.setSenderRole(msg.getSenderRole());
            vo.setContent(msg.getContent());
            vo.setMessageType(msg.getMessageType());
            vo.setAudioUrl(msg.getAudioUrl()); // 设置音频URL
            vo.setImageUrl(msg.getImageUrl()); // 设置图片URL
            vo.setFileName(msg.getFileName()); // 设置文件名
            vo.setFileUrl(msg.getFileUrl()); // 设置文件URL
            vo.setDuration(msg.getDuration()); // 设置音频时长
            // 正确格式化时间
            if (msg.getCreateTime() != null) {
                vo.setTime(msg.getCreateTime().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } else {
                vo.setTime(
                        java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            vo.setMe(SecurityUtil.getUserId() != null && SecurityUtil.getUserId().equals(msg.getSenderId()));
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(vos);
        return Result.success(voPage);
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
