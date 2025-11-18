package org.example.persion.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.persion.entity.LeaveRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface LeaveRequestMapper extends BaseMapper<LeaveRequest> {

    List<LeaveRequest> findApprovedByUsersAndDateRange(@Param("staffIds") List<Long> staffIds,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 获取所有请假申请（包含申请人信息）
     */
    List<Map<String, Object>> getAllLeaveRequestsWithUser();

    /**
     * 根据状态获取请假申请（包含申请人信息）
     */
    List<Map<String, Object>> getLeaveRequestsByStatusWithUser(@Param("status") String status);
}
