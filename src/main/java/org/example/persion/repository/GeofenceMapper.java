package org.example.persion.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.persion.entity.Geofence;

/**
 * 电子围栏数据访问接口
 */
@Mapper
public interface GeofenceMapper extends BaseMapper<Geofence> {
}

