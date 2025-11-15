package org.example.persion.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.service.LeaveRequestService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端 - 请假/调休审批Controller
 */
@RestController
@RequestMapping("/api/admin/leave")
@RequiredArgsConstructor
public class AdminLeaveController {

    private final LeaveRequestService leaveRequestService;

    /**
     * 获取所有请假申请
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getAllLeaveRequests() {
        List<Map<String, Object>> list = leaveRequestService.getAllLeaveRequests();
        return Result.success(list);
    }

    /**
     * 根据状态获取请假申请
     */
    @GetMapping("/list/{status}")
    public Result<List<Map<String, Object>>> getLeaveRequestsByStatus(@PathVariable String status) {
        List<Map<String, Object>> list = leaveRequestService.getLeaveRequestsByStatus(status);
        return Result.success(list);
    }

    /**
     * 审批请假申请
     */
    @PutMapping("/review/{id}")
    public Result<Void> reviewLeaveRequest(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String remark) {

        // 获取当前审批人ID
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long reviewerId = Long.parseLong(authentication.getName());

        leaveRequestService.reviewLeaveRequest(id, status, reviewerId, remark);
        return Result.success();
    }
}
