package org.example.persion.controller.medical;

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

        Page<ChatMessage> page = new Page<>(current, size);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getGroupId, groupId);
        wrapper.orderByAsc(ChatMessage::getCreateTime);

        Page<ChatMessage> chatMessagePage = chatMessageMapper.selectPage(page, wrapper);

        Page<MessageVO> voPage = new Page<>(chatMessagePage.getCurrent(), chatMessagePage.getSize(), chatMessagePage.getTotal());
        List<MessageVO> vos = chatMessagePage.getRecords().stream().map(msg -> {
            MessageVO vo = new MessageVO();
            vo.setGroupId(msg.getGroupId());
            vo.setSenderId(msg.getSenderId());
            vo.setSenderName(msg.getSenderName());
            vo.setSenderRole(msg.getSenderRole());
            vo.setContent(msg.getContent());
            vo.setMessageType(msg.getMessageType());
            vo.setTime(msg.getCreateTime().toString());
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
