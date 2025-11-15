package org.example.persion.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.persion.entity.LeaveRequest;

import java.util.List;
import java.util.Map;

/**
 * 请假/调休申请Mapper
 */
@Mapper
public interface LeaveRequestMapper extends BaseMapper<LeaveRequest> {

    /**
     * 获取请假申请列表(带用户信息)
     */
    @Select("SELECT lr.*, u.username, u.real_name " +
            "FROM leave_request lr " +
            "LEFT JOIN sys_user u ON lr.medical_user_id = u.id " +
            "WHERE lr.deleted = 0 " +
            "ORDER BY lr.create_time DESC")
    List<Map<String, Object>> selectLeaveRequestsWithUser();

    /**
     * 根据状态获取请假申请列表
     */
    @Select("SELECT lr.*, u.username, u.real_name " +
            "FROM leave_request lr " +
            "LEFT JOIN sys_user u ON lr.medical_user_id = u.id " +
            "WHERE lr.deleted = 0 AND lr.status = #{status} " +
            "ORDER BY lr.create_time DESC")
    List<Map<String, Object>> selectLeaveRequestsByStatus(String status);

    /**
     * 根据医护人员ID获取请假申请列表
     */
    @Select("SELECT lr.*, u.username, u.real_name " +
            "FROM leave_request lr " +
            "LEFT JOIN sys_user u ON lr.medical_user_id = u.id " +
            "WHERE lr.deleted = 0 AND lr.medical_user_id = #{medicalUserId} " +
            "ORDER BY lr.create_time DESC")
    List<Map<String, Object>> selectLeaveRequestsByMedicalUser(Long medicalUserId);
}
