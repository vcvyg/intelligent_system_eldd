package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.persion.entity.LeaveRequest;
import org.example.persion.enums.LeaveRequestStatus;
import org.example.persion.repository.LeaveRequestMapper;
import org.example.persion.service.LeaveRequestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class LeaveRequestServiceImpl extends ServiceImpl<LeaveRequestMapper, LeaveRequest>
        implements LeaveRequestService {

    @Override
    @Transactional
    public void submitLeaveRequest(LeaveRequest leaveRequest) {
        leaveRequest.setStatus(LeaveRequestStatus.PENDING);
        save(leaveRequest);
    }

    @Override
    public List<Map<String, Object>> getMyLeaveRequests(Long userId) {
        return baseMapper.selectMaps(new QueryWrapper<LeaveRequest>()
                .eq("medical_user_id", userId)
                .orderByDesc("create_time"));
    }

    @Override
    @Transactional
    public void cancelLeaveRequest(Long id, Long userId) {
        remove(new QueryWrapper<LeaveRequest>()
                .eq("id", id)
                .eq("medical_user_id", userId));
    }

    @Override
    public List<Map<String, Object>> getAllLeaveRequests() {
        return baseMapper.getAllLeaveRequestsWithUser();
    }

    @Override
    public List<Map<String, Object>> getLeaveRequestsByStatus(String status) {
        return baseMapper.getLeaveRequestsByStatusWithUser(status);
    }

    @Override
    @Transactional
    public void reviewLeaveRequest(Long id, String status, Long reviewerId, String remark) {
        LeaveRequest leaveRequest = getById(id);
        if (leaveRequest != null) {
            leaveRequest.setStatus(LeaveRequestStatus.valueOf(status.toUpperCase()));
            leaveRequest.setApproverId(reviewerId);
            leaveRequest.setApproverRemark(remark);
            updateById(leaveRequest);
        }
    }
}
