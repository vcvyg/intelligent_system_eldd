package org.example.persion.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.persion.entity.HealthData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 健康数据数据访问接口
 */
@Mapper
public interface HealthDataMapper extends BaseMapper<HealthData> {

    /**
     * 查询指定时间范围内的日均健康数据
     * @param startDateTime 开始时间
     * @param endDateTime 结束时间 (不包含)
     * @param elderlyId 老人ID (可选)
     * @return 每日平均数据列表
     */
    @Select("<script>" +
            "SELECT " +
            "  CAST(measure_time AS DATE) AS measure_date, " +
            "  AVG(heart_rate) AS avg_heart_rate, " +
            "  AVG(blood_pressure_high) AS avg_blood_pressure_high, " +
            "  AVG(blood_pressure_low) AS avg_blood_pressure_low, " +
            "  AVG(blood_sugar) AS avg_blood_sugar, " +
            "  SUM(steps) AS total_steps " +
            "FROM health_data " +
            "WHERE measure_time &gt;= #{startDateTime} AND measure_time &lt; #{endDateTime} " +
            "<if test='elderlyId != null'>" +
            "  AND elderly_id = #{elderlyId} " +
            "</if>" +
            "GROUP BY CAST(measure_time AS DATE) " +
            "ORDER BY measure_date" +
            "</script>")
    List<Map<String, Object>> findDailyAverageByDateRange(@Param("startDateTime") LocalDateTime startDateTime,
                                                          @Param("endDateTime") LocalDateTime endDateTime,
                                                          @Param("elderlyId") Long elderlyId);

    /**
     * 查询指定日期时间范围内的所有健康数据记录
     * @param startDateTime 开始时间
     * @param endDateTime 结束时间
     * @param elderlyId 老人ID (可选)
     * @return 健康数据记录列表
     */
    @Select("<script>" +
            "SELECT * FROM health_data " +
            "WHERE measure_time &gt;= #{startDateTime} AND measure_time &lt;= #{endDateTime} " +
            "<if test='elderlyId != null'>" +
            "  AND elderly_id = #{elderlyId} " +
            "</if>" +
            "ORDER BY measure_time ASC" +
            "</script>")
    List<HealthData> findByDateTimeRange(@Param("startDateTime") LocalDateTime startDateTime,
                                         @Param("endDateTime") LocalDateTime endDateTime,
                                         @Param("elderlyId") Long elderlyId);
}
