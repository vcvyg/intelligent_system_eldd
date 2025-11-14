package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.dto.AlertCreateDTO;
import org.example.persion.dto.AlertHandleDTO;
import org.example.persion.entity.AlertRecord;
import org.example.persion.repository.AlertRecordMapper;
import org.example.persion.service.AlertService;
import org.example.persion.vo.AlertRecordVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 预警管理服务实现类
 */
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertRecordMapper alertRecordMapper;

    @Override
    public Page<AlertRecordVO> getAlertList(int current, int size, String alertType, String alertLevel, String status) {
        Page<AlertRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();

        // 预警类型筛选
        if (alertType != null && !alertType.isEmpty()) {
            wrapper.eq(AlertRecord::getAlertType, alertType);
        }

        // 预警等级筛选
        if (alertLevel != null && !alertLevel.isEmpty()) {
            wrapper.eq(AlertRecord::getAlertLevel, alertLevel);
        }

        // 状态筛选
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AlertRecord::getStatus, status);
        }

        wrapper.orderByDesc(AlertRecord::getAlertTime);

        Page<AlertRecord> alertPage = alertRecordMapper.selectPage(page, wrapper);

        // 转换为VO
        Page<AlertRecordVO> voPage = new Page<>();
        voPage.setCurrent(alertPage.getCurrent());
        voPage.setSize(alertPage.getSize());
        voPage.setTotal(alertPage.getTotal());

        List<AlertRecordVO> voList = alertPage.getRecords().stream().map(alert -> {
            AlertRecordVO vo = new AlertRecordVO();
            BeanUtils.copyProperties(alert, vo);
            return vo;
        }).toList();

        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public AlertRecordVO getAlertById(Long id) {
        AlertRecord alert = alertRecordMapper.selectById(id);
        if (alert == null) {
            throw new BusinessException("预警记录不存在");
        }

        AlertRecordVO vo = new AlertRecordVO();
        BeanUtils.copyProperties(alert, vo);
        return vo;
    }

    @Override
    public List<AlertRecordVO> getAlertsByElderlyId(Long elderlyId) {
        return alertRecordMapper.selectByElderlyId(elderlyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAlert(AlertCreateDTO dto) {
        AlertRecord alert = new AlertRecord();
        BeanUtils.copyProperties(dto, alert);

        // 如果未设置预警时间,使用当前时间
        if (alert.getAlertTime() == null) {
            alert.setAlertTime(LocalDateTime.now());
        }

        // 默认状态为待处理
        alert.setStatus("待处理");

        alertRecordMapper.insert(alert);
        return alert.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMedical(Long alertId, Long medicalId) {
        AlertRecord alert = alertRecordMapper.selectById(alertId);
        if (alert == null) {
            throw new BusinessException("预警记录不存在");
        }

        alert.setAssignedMedicalId(medicalId);
        alert.setStatus("处理中");
        alertRecordMapper.updateById(alert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleAlert(AlertHandleDTO dto) {
        AlertRecord alert = alertRecordMapper.selectById(dto.getAlertId());
        if (alert == null) {
            throw new BusinessException("预警记录不存在");
        }

        if (dto.getAssignedMedicalId() != null) {
            alert.setAssignedMedicalId(dto.getAssignedMedicalId());
        }

        alert.setHandleResult(dto.getHandleResult());
        alert.setHandleTime(LocalDateTime.now());
        alert.setStatus("已处理");

        alertRecordMapper.updateById(alert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ignoreAlert(Long alertId) {
        AlertRecord alert = alertRecordMapper.selectById(alertId);
        if (alert == null) {
            throw new BusinessException("预警记录不存在");
        }

        alert.setStatus("已忽略");
        alert.setHandleTime(LocalDateTime.now());
        alertRecordMapper.updateById(alert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlert(Long id) {
        AlertRecord alert = alertRecordMapper.selectById(id);
        if (alert == null) {
            throw new BusinessException("预警记录不存在");
        }

        alert.setDeleted(1);
        alertRecordMapper.updateById(alert);
    }

    @Override
    public Map<String, Object> getAlertStatistics() {
        List<AlertRecord> alerts = alertRecordMapper.selectList(null);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("total", alerts.size());
        statistics.put("pending", alerts.stream().filter(a -> "待处理".equals(a.getStatus())).count());
        statistics.put("processing", alerts.stream().filter(a -> "处理中".equals(a.getStatus())).count());
        statistics.put("handled", alerts.stream().filter(a -> "已处理".equals(a.getStatus())).count());

        // 按预警等级统计
        Map<String, Long> levelCount = new HashMap<>();
        levelCount.put("低", alerts.stream().filter(a -> "低".equals(a.getAlertLevel())).count());
        levelCount.put("中", alerts.stream().filter(a -> "中".equals(a.getAlertLevel())).count());
        levelCount.put("高", alerts.stream().filter(a -> "高".equals(a.getAlertLevel())).count());
        levelCount.put("紧急", alerts.stream().filter(a -> "紧急".equals(a.getAlertLevel())).count());

        statistics.put("levelCount", levelCount);

        // 按预警类型统计
        Map<String, Long> typeCount = new HashMap<>();
        typeCount.put("心率异常", alerts.stream().filter(a -> "心率异常".equals(a.getAlertType())).count());
        typeCount.put("血压异常", alerts.stream().filter(a -> "血压异常".equals(a.getAlertType())).count());
        typeCount.put("血糖异常", alerts.stream().filter(a -> "血糖异常".equals(a.getAlertType())).count());
        typeCount.put("体温异常", alerts.stream().filter(a -> "体温异常".equals(a.getAlertType())).count());
        typeCount.put("跌倒", alerts.stream().filter(a -> "跌倒".equals(a.getAlertType())).count());
        typeCount.put("离家", alerts.stream().filter(a -> "离家".equals(a.getAlertType())).count());

        statistics.put("typeCount", typeCount);

        return statistics;
    }

    @Override
    public List<AlertRecordVO> getTodayAlerts() {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AlertRecord::getAlertTime, startOfDay, endOfDay);
        wrapper.orderByDesc(AlertRecord::getAlertTime);

        List<AlertRecord> alerts = alertRecordMapper.selectList(wrapper);
        return alerts.stream().map(alert -> {
            AlertRecordVO vo = new AlertRecordVO();
            BeanUtils.copyProperties(alert, vo);
            return vo;
        }).toList();
    }
}
