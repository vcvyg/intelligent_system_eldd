package org.example.persion.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.persion.entity.AlertRecord;
import org.example.persion.vo.AlertRecordVO;

import java.util.List;

/**
 * 预警记录Mapper接口
 */
@Mapper
public interface AlertRecordMapper extends BaseMapper<AlertRecord> {

    /**
     * 查询预警列表(包含老人姓名、设备名称、医护人员姓名)
     */
    @Select("SELECT a.*, e.name as elderly_name, d.device_name, u.real_name as medical_name " +
            "FROM alert_record a " +
            "LEFT JOIN elderly_info e ON a.elderly_id = e.id " +
            "LEFT JOIN device_info d ON a.device_id = d.id " +
            "LEFT JOIN sys_user u ON a.assigned_medical_id = u.id " +
            "WHERE a.deleted = 0 " +
            "ORDER BY a.alert_time DESC")
    List<AlertRecordVO> selectAlertListWithDetails();

    /**
     * 根据老人ID查询预警列表
     */
    @Select("SELECT a.*, e.name as elderly_name, d.device_name, u.real_name as medical_name " +
            "FROM alert_record a " +
            "LEFT JOIN elderly_info e ON a.elderly_id = e.id " +
            "LEFT JOIN device_info d ON a.device_id = d.id " +
            "LEFT JOIN sys_user u ON a.assigned_medical_id = u.id " +
            "WHERE a.elderly_id = #{elderlyId} AND a.deleted = 0 " +
            "ORDER BY a.alert_time DESC")
    List<AlertRecordVO> selectByElderlyId(Long elderlyId);

    /**
     * 查询医护人员待处理的告警
     */
    @Select("SELECT a.*, e.name as elderly_name, d.device_name, u.real_name as medical_name " +
            "FROM alert_record a " +
            "LEFT JOIN elderly_info e ON a.elderly_id = e.id " +
            "LEFT JOIN device_info d ON a.device_id = d.id " +
            "LEFT JOIN sys_user u ON a.assigned_medical_id = u.id " +
            "WHERE a.assigned_medical_id = #{medicalUserId} AND a.status = '待处理' AND a.deleted = 0 " +
            "ORDER BY a.alert_time DESC")
    List<AlertRecordVO> findPendingAlertsByMedicalUser(Long medicalUserId);
}
