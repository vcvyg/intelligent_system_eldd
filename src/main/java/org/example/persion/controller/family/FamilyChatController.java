package org.example.persion.controller.family;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.controller.medical.MedicalChatController.GroupVO;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.security.SecurityUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/family/chat")
@RequiredArgsConstructor
public class FamilyChatController {

    private final ElderlyInfoMapper elderlyInfoMapper;

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
                .map(elderly -> new GroupVO(elderly.getId(), elderly.getName()))
                .collect(Collectors.toList());

        return Result.success(groupVOs);
    }
}
