package org.example.persion.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.persion.entity.GeofenceAlert;

/**
 * 围栏警报记录数据访问接口
 */
@Mapper
public interface GeofenceAlertMapper extends BaseMapper<GeofenceAlert> {
}

