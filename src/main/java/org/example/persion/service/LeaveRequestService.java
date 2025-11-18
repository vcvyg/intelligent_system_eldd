package org.example.persion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.persion.entity.LeaveRequest;

import java.util.List;
import java.util.Map;

public interface LeaveRequestService extends IService<LeaveRequest> {

    void submitLeaveRequest(LeaveRequest leaveRequest);

    List<Map<String, Object>> getMyLeaveRequests(Long userId);

    void cancelLeaveRequest(Long id, Long userId);

    List<Map<String, Object>> getAllLeaveRequests();

    List<Map<String, Object>> getLeaveRequestsByStatus(String status);

    void reviewLeaveRequest(Long id, String status, Long reviewerId, String remark);
}
