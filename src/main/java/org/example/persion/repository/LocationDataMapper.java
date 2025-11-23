package org.example.persion.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.persion.entity.LocationData;

/**
 * 定位数据访问接口
 */
@Mapper
public interface LocationDataMapper extends BaseMapper<LocationData> {

    /**
     * 获取指定老人的最新定位数据
     *
     * @param elderlyId 老人ID
     * @return 最新的定位数据
     */
    @Select("SELECT TOP 1 * FROM location_data " +
            "WHERE elderly_id = #{elderlyId} AND deleted = 0 " +
            "ORDER BY location_time DESC")
    LocationData selectLatestByElderlyId(@Param("elderlyId") Long elderlyId);
}

