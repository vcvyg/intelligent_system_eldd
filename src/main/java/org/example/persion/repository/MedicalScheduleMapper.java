package org.example.persion.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.persion.entity.MedicalSchedule;
import org.example.persion.vo.MedicalScheduleVO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 医护排班Mapper
 */
@Mapper
public interface MedicalScheduleMapper extends BaseMapper<MedicalSchedule> {

    /**
     * 查询某个医护人员的排班列表(带用户信息)
     */
    @Select("SELECT ms.*, u.username, u.real_name, u.phone " +
            "FROM medical_schedule ms " +
            "LEFT JOIN sys_user u ON ms.medical_user_id = u.id " +
            "WHERE ms.medical_user_id = #{medicalUserId} " +
            "AND ms.deleted = 0 " +
            "ORDER BY ms.schedule_date DESC, ms.start_time")
    List<Map<String, Object>> selectScheduleListWithUserInfo(Long medicalUserId);

    /**
     * 查询指定日期范围的排班列表(带用户信息)
     */
    @Select("SELECT ms.*, u.username, u.real_name, u.phone " +
            "FROM medical_schedule ms " +
            "LEFT JOIN sys_user u ON ms.medical_user_id = u.id " +
            "WHERE ms.schedule_date BETWEEN #{startDate} AND #{endDate} " +
            "AND ms.deleted = 0 " +
            "ORDER BY ms.schedule_date, ms.start_time")
    List<Map<String, Object>> selectScheduleListByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * 查询所有医护人员的排班列表(带用户信息)
     */
    @Select("SELECT ms.*, u.username, u.real_name, u.phone " +
            "FROM medical_schedule ms " +
            "LEFT JOIN sys_user u ON ms.medical_user_id = u.id " +
            "WHERE ms.deleted = 0 " +
            "ORDER BY ms.schedule_date DESC, ms.start_time")
    List<Map<String, Object>> selectAllSchedulesWithUserInfo();

    List<MedicalScheduleVO> selectAllSchedulesWithDetails();

    List<MedicalScheduleVO> selectSchedulesByMedicalUser(Long medicalUserId);

    List<MedicalScheduleVO> selectSchedulesByDateRange(LocalDate startDate, LocalDate endDate);

    List<MedicalScheduleVO> selectSchedulesByMedicalUserAndDate(Long medicalUserId, LocalDate date);
}
