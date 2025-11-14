package org.example.persion.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.persion.entity.DeviceInfo;
import org.example.persion.vo.DeviceInfoVO;

import java.util.List;

/**
 * 设备信息Mapper接口
 */
@Mapper
public interface DeviceInfoMapper extends BaseMapper<DeviceInfo> {

    /**
     * 查询设备列表(包含老人姓名)
     */
    @Select("SELECT d.*, e.name as elderly_name " +
            "FROM device_info d " +
            "LEFT JOIN elderly_info e ON d.elderly_id = e.id " +
            "WHERE d.deleted = 0 " +
            "ORDER BY d.create_time DESC")
    List<DeviceInfoVO> selectDeviceListWithElderlyName();

    /**
     * 根据设备编号查询
     */
    @Select("SELECT d.*, e.name as elderly_name " +
            "FROM device_info d " +
            "LEFT JOIN elderly_info e ON d.elderly_id = e.id " +
            "WHERE d.device_code = #{deviceCode} AND d.deleted = 0")
    DeviceInfoVO selectByDeviceCode(String deviceCode);

    /**
     * 根据老人ID查询设备列表
     */
    @Select("SELECT d.*, e.name as elderly_name " +
            "FROM device_info d " +
            "LEFT JOIN elderly_info e ON d.elderly_id = e.id " +
            "WHERE d.elderly_id = #{elderlyId} AND d.deleted = 0")
    List<DeviceInfoVO> selectByElderlyId(Long elderlyId);
}
