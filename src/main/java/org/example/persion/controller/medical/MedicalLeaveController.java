package org.example.persion.controller.medical;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.entity.LeaveRequest;
import org.example.persion.security.SecurityUtil;
import org.example.persion.service.LeaveRequestService;
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
        Long userId = SecurityUtil.getUserId();
        if (userId == null) {
            throw new BusinessException("无法获取当前用户信息");
        }

        leaveRequest.setMedicalUserId(userId);
        leaveRequestService.submitLeaveRequest(leaveRequest);
        return Result.success();
    }

    /**
     * 获取我的请假申请列表
     */
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> getMyLeaveRequests() {
        Long userId = SecurityUtil.getUserId();
        if (userId == null) {
            throw new BusinessException("无法获取当前用户信息");
        }

        List<Map<String, Object>> list = leaveRequestService.getMyLeaveRequests(userId);
        return Result.success(list);
    }

    /**
     * 撤销请假申请
     */
    @DeleteMapping("/{id}")
    public Result<Void> cancelLeaveRequest(@PathVariable Long id) {
        Long userId = SecurityUtil.getUserId();
        if (userId == null) {
            throw new BusinessException("无法获取当前用户信息");
        }

        leaveRequestService.cancelLeaveRequest(id, userId);
        return Result.success();
    }
}