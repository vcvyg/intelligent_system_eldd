package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.example.persion.entity.LeaveRequest;
import org.example.persion.repository.LeaveRequestMapper;
import org.example.persion.service.LeaveRequestService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 请假/调休申请Service实现
 */
@Service
@RequiredArgsConstructor
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private final LeaveRequestMapper leaveRequestMapper;

    @Override
    public void submitLeaveRequest(LeaveRequest leaveRequest) {
        // 设置默认状态为待审批
        leaveRequest.setStatus("待审批");
        leaveRequestMapper.insert(leaveRequest);
    }

    @Override
    public List<Map<String, Object>> getAllLeaveRequests() {
        return leaveRequestMapper.selectLeaveRequestsWithUser();
    }

    @Override
    public List<Map<String, Object>> getLeaveRequestsByStatus(String status) {
        return leaveRequestMapper.selectLeaveRequestsByStatus(status);
    }

    @Override
    public List<Map<String, Object>> getMyLeaveRequests(Long medicalUserId) {
        return leaveRequestMapper.selectLeaveRequestsByMedicalUser(medicalUserId);
    }

    @Override
    public void reviewLeaveRequest(Long id, String status, Long reviewerId, String remark) {
        LambdaUpdateWrapper<LeaveRequest> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LeaveRequest::getId, id)
                .set(LeaveRequest::getStatus, status)
                .set(LeaveRequest::getReviewerId, reviewerId)
                .set(LeaveRequest::getReviewTime, LocalDateTime.now())
                .set(LeaveRequest::getReviewRemark, remark);

        leaveRequestMapper.update(null, updateWrapper);
    }

    @Override
    public void cancelLeaveRequest(Long id, Long medicalUserId) {
        // 只能撤销自己的待审批申请
        LambdaQueryWrapper<LeaveRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LeaveRequest::getId, id)
                .eq(LeaveRequest::getMedicalUserId, medicalUserId)
                .eq(LeaveRequest::getStatus, "待审批");

        LeaveRequest leaveRequest = leaveRequestMapper.selectOne(queryWrapper);
        if (leaveRequest == null) {
            throw new RuntimeException("只能撤销自己的待审批申请");
        }

        leaveRequestMapper.deleteById(id);
    }
}
