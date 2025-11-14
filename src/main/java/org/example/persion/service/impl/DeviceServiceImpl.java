package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.dto.DeviceCreateDTO;
import org.example.persion.dto.DeviceUpdateDTO;
import org.example.persion.entity.DeviceInfo;
import org.example.persion.repository.DeviceInfoMapper;
import org.example.persion.service.DeviceService;
import org.example.persion.vo.DeviceInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备管理服务实现类
 */
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceInfoMapper deviceInfoMapper;

    @Override
    public Page<DeviceInfoVO> getDeviceList(int current, int size, String keyword, String deviceType, String status) {
        // 这里简化处理,实际应该使用自定义SQL进行关联查询分页
        Page<DeviceInfo> page = new Page<>(current, size);
        LambdaQueryWrapper<DeviceInfo> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索:设备编号、设备名称
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(DeviceInfo::getDeviceCode, keyword)
                    .or().like(DeviceInfo::getDeviceName, keyword));
        }

        // 设备类型筛选
        if (deviceType != null && !deviceType.isEmpty()) {
            wrapper.eq(DeviceInfo::getDeviceType, deviceType);
        }

        // 状态筛选
        if (status != null && !status.isEmpty()) {
            wrapper.eq(DeviceInfo::getStatus, status);
        }

        wrapper.orderByDesc(DeviceInfo::getCreateTime);

        Page<DeviceInfo> devicePage = deviceInfoMapper.selectPage(page, wrapper);

        // 转换为VO(这里简化处理,实际应该关联查询老人姓名)
        Page<DeviceInfoVO> voPage = new Page<>();
        voPage.setCurrent(devicePage.getCurrent());
        voPage.setSize(devicePage.getSize());
        voPage.setTotal(devicePage.getTotal());

        List<DeviceInfoVO> voList = devicePage.getRecords().stream().map(device -> {
            DeviceInfoVO vo = new DeviceInfoVO();
            BeanUtils.copyProperties(device, vo);
            // TODO: 这里可以查询老人姓名
            return vo;
        }).toList();

        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public DeviceInfoVO getDeviceById(Long id) {
        DeviceInfo device = deviceInfoMapper.selectById(id);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }

        DeviceInfoVO vo = new DeviceInfoVO();
        BeanUtils.copyProperties(device, vo);
        return vo;
    }

    @Override
    public DeviceInfoVO getDeviceByCode(String deviceCode) {
        return deviceInfoMapper.selectByDeviceCode(deviceCode);
    }

    @Override
    public List<DeviceInfoVO> getDevicesByElderlyId(Long elderlyId) {
        return deviceInfoMapper.selectByElderlyId(elderlyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDevice(DeviceCreateDTO dto) {
        // 检查设备编号是否已存在
        LambdaQueryWrapper<DeviceInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceInfo::getDeviceCode, dto.getDeviceCode());
        if (deviceInfoMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("设备编号已存在");
        }

        DeviceInfo device = new DeviceInfo();
        BeanUtils.copyProperties(dto, device);
        device.setLastSyncTime(LocalDateTime.now());

        deviceInfoMapper.insert(device);
        return device.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDevice(DeviceUpdateDTO dto) {
        DeviceInfo device = deviceInfoMapper.selectById(dto.getId());
        if (device == null) {
            throw new BusinessException("设备不存在");
        }

        BeanUtils.copyProperties(dto, device);
        deviceInfoMapper.updateById(device);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDevice(Long id) {
        DeviceInfo device = deviceInfoMapper.selectById(id);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }

        device.setDeleted(1);
        deviceInfoMapper.updateById(device);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindDeviceToElderly(Long deviceId, Long elderlyId) {
        DeviceInfo device = deviceInfoMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }

        device.setElderlyId(elderlyId);
        deviceInfoMapper.updateById(device);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindDevice(Long deviceId) {
        DeviceInfo device = deviceInfoMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }

        device.setElderlyId(null);
        deviceInfoMapper.updateById(device);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDeviceStatus(Long deviceId, String status) {
        DeviceInfo device = deviceInfoMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }

        device.setStatus(status);
        device.setLastSyncTime(LocalDateTime.now());
        deviceInfoMapper.updateById(device);
    }

    @Override
    public Object getDeviceStatistics() {
        List<DeviceInfo> devices = deviceInfoMapper.selectList(null);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("total", devices.size());
        statistics.put("online", devices.stream().filter(d -> "在线".equals(d.getStatus())).count());
        statistics.put("offline", devices.stream().filter(d -> "离线".equals(d.getStatus())).count());
        statistics.put("fault", devices.stream().filter(d -> "故障".equals(d.getStatus())).count());

        // 按类型统计
        Map<String, Long> typeCount = new HashMap<>();
        typeCount.put("心率监测器", devices.stream().filter(d -> "心率监测器".equals(d.getDeviceType())).count());
        typeCount.put("血压计", devices.stream().filter(d -> "血压计".equals(d.getDeviceType())).count());
        typeCount.put("血糖仪", devices.stream().filter(d -> "血糖仪".equals(d.getDeviceType())).count());
        typeCount.put("定位设备", devices.stream().filter(d -> "定位设备".equals(d.getDeviceType())).count());
        typeCount.put("体温计", devices.stream().filter(d -> "体温计".equals(d.getDeviceType())).count());

        statistics.put("typeCount", typeCount);

        return statistics;
    }
}
