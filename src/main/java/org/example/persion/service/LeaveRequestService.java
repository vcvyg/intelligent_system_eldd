package org.example.persion.service;

import org.example.persion.entity.LeaveRequest;

import java.util.List;
import java.util.Map;

/**
 * 请假/调休申请Service
 */
public interface LeaveRequestService {

    /**
     * 提交请假申请
     */
    void submitLeaveRequest(LeaveRequest leaveRequest);

    /**
     * 获取所有请假申请(管理端)
     */
    List<Map<String, Object>> getAllLeaveRequests();

    /**
     * 根据状态获取请假申请
     */
    List<Map<String, Object>> getLeaveRequestsByStatus(String status);

    /**
     * 获取当前医护人员的请假申请
     */
    List<Map<String, Object>> getMyLeaveRequests(Long medicalUserId);

    /**
     * 审批请假申请
     */
    void reviewLeaveRequest(Long id, String status, Long reviewerId, String remark);

    /**
     * 撤销请假申请
     */
    void cancelLeaveRequest(Long id, Long medicalUserId);
}
