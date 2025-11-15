package org.example.persion.controller.medical;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.entity.LeaveRequest;
import org.example.persion.service.LeaveRequestService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 医护端 - 请假/调休申请Controller
 */
@RestController
@RequestMapping("/api/medical/leave")
@RequiredArgsConstructor
public class MedicalLeaveController {

    private final LeaveRequestService leaveRequestService;

    /**
     * 提交请假申请
     */
    @PostMapping("/submit")
    public Result<Void> submitLeaveRequest(@RequestBody LeaveRequest leaveRequest) {
        // 获取当前登录用户ID
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        leaveRequest.setMedicalUserId(userId);
        leaveRequestService.submitLeaveRequest(leaveRequest);
        return Result.success();
    }

    /**
     * 获取我的请假申请列表
     */
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> getMyLeaveRequests() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        List<Map<String, Object>> list = leaveRequestService.getMyLeaveRequests(userId);
        return Result.success(list);
    }

    /**
     * 撤销请假申请
     */
    @DeleteMapping("/{id}")
    public Result<Void> cancelLeaveRequest(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        leaveRequestService.cancelLeaveRequest(id, userId);
        return Result.success();
    }
}
