package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.persion.ai.event.CareSignalEvent;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.dto.AlertCreateDTO;
import org.example.persion.dto.AlertHandleDTO;
import org.example.persion.entity.AlertRecord;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.entity.Room;
import org.example.persion.repository.AlertRecordMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.RoomMapper;
import org.example.persion.security.SecurityUtil;
import org.example.persion.service.AlertService;
import org.example.persion.vo.AlertRecordVO;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 预警管理服务实现类
 */
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertRecordMapper alertRecordMapper;
    private final ElderlyInfoMapper elderlyInfoMapper;
    private final RoomMapper roomMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Page<AlertRecordVO> getAlertList(int current, int size, String alertType, String alertLevel, String status, String elderlyName) {
        Page<AlertRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(AlertRecord::getDeleted, 0);

        if (elderlyName != null && !elderlyName.isEmpty()) {
            List<ElderlyInfo> elderlyList = elderlyInfoMapper.selectList(
                    new LambdaQueryWrapper<ElderlyInfo>()
                            .like(ElderlyInfo::getName, elderlyName)
            );
            Set<Long> elderlyIds = elderlyList.stream()
                    .map(ElderlyInfo::getId)
                    .collect(Collectors.toSet());
            if (elderlyIds.isEmpty()) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.in(AlertRecord::getElderlyId, elderlyIds);
            }
        }

        if (alertType != null && !alertType.isEmpty()) {
            wrapper.eq(AlertRecord::getAlertType, alertType);
        }
        if (alertLevel != null && !alertLevel.isEmpty()) {
            wrapper.eq(AlertRecord::getAlertLevel, alertLevel);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AlertRecord::getStatus, status);
        }

        wrapper.orderByDesc(AlertRecord::getAlertTime);
        Page<AlertRecord> alertPage = alertRecordMapper.selectPage(page, wrapper);

        Page<AlertRecordVO> voPage = new Page<>();
        BeanUtils.copyProperties(alertPage, voPage, "records");

        List<AlertRecordVO> voList = new ArrayList<>();
        if (!alertPage.getRecords().isEmpty()) {
            Set<Long> elderlyIds = alertPage.getRecords().stream()
                    .map(AlertRecord::getElderlyId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Map<Long, ElderlyInfo> elderlyMap = new HashMap<>();
            Set<Long> roomIds = new HashSet<>();
            if (!elderlyIds.isEmpty()) {
                List<ElderlyInfo> elderlyList = elderlyInfoMapper.selectBatchIds(elderlyIds);
                elderlyMap = elderlyList.stream()
                        .collect(Collectors.toMap(ElderlyInfo::getId, e -> e));
                roomIds = elderlyList.stream()
                        .map(ElderlyInfo::getRoomId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }

            Map<Long, Room> roomMap = new HashMap<>();
            if (!roomIds.isEmpty()) {
                List<Room> roomList = roomMapper.selectBatchIds(roomIds);
                roomMap = roomList.stream()
                        .collect(Collectors.toMap(Room::getId, r -> r));
            }

            for (AlertRecord alert : alertPage.getRecords()) {
                AlertRecordVO vo = new AlertRecordVO();
                BeanUtils.copyProperties(alert, vo);

                if (alert.getElderlyId() != null) {
                    ElderlyInfo elderlyInfo = elderlyMap.get(alert.getElderlyId());
                    if (elderlyInfo != null) {
                        vo.setElderlyName(elderlyInfo.getName());
                        if (elderlyInfo.getRoomId() != null) {
                            Room room = roomMap.get(elderlyInfo.getRoomId());
                            if (room != null) {
                                vo.setRoomName(room.getRoomNumber());
                            }
                        }
                    }
                }
                voList.add(vo);
            }
        }

        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public List<AlertRecordVO> getAllAlertsWithDetails() {
        return alertRecordMapper.selectAlertListWithDetails();
    }

    @Override
    public List<AlertRecordVO> getMyAlertTasks() {
        Long medicalUserId = SecurityUtil.getUserId();
        if (medicalUserId == null) {
            throw new BusinessException(401, "登录已失效，请重新登录");
        }
        return alertRecordMapper.selectTaskListForMedicalUser(medicalUserId);
    }

    @Override
    public AlertRecordVO getAlertById(Long id) {
        AlertRecord alert = alertRecordMapper.selectById(id);
        if (alert == null) {
            throw new BusinessException("预警记录不存在");
        }

        AlertRecordVO vo = new AlertRecordVO();
        BeanUtils.copyProperties(alert, vo);
        if (alert.getElderlyId() != null) {
            ElderlyInfo elderlyInfo = elderlyInfoMapper.selectById(alert.getElderlyId());
            if (elderlyInfo != null) {
                vo.setElderlyName(elderlyInfo.getName());
                if (elderlyInfo.getRoomId() != null) {
                    Room room = roomMapper.selectById(elderlyInfo.getRoomId());
                    if (room != null) {
                        vo.setRoomName(room.getRoomNumber());
                    }
                }
            }
        }
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

        if (alert.getAlertTime() == null) {
            alert.setAlertTime(LocalDateTime.now());
        }
        alert.setStatus("待处理");

        alertRecordMapper.insert(alert);
        eventPublisher.publishEvent(CareSignalEvent.alertRaised(
                alert.getElderlyId(),
                alert.getId(),
                alert.getAlertTime()
        ));
        return alert.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMedical(Long alertId, Long medicalId) {
        AlertRecord alert = alertRecordMapper.selectById(alertId);
        if (alert == null) {
            throw new BusinessException("预警记录不存在");
        }
        rejectClosedAlert(alert);

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
        rejectClosedAlert(alert);

        if (dto.getAssignedMedicalId() != null) {
            alert.setAssignedMedicalId(dto.getAssignedMedicalId());
        } else if (alert.getAssignedMedicalId() == null) {
            Long operatorId = SecurityUtil.getUserId();
            if (operatorId != null) {
                alert.setAssignedMedicalId(operatorId);
            }
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
        rejectClosedAlert(alert);

        alert.setStatus("已忽略");
        alert.setHandleTime(LocalDateTime.now());
        alertRecordMapper.updateById(alert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processAlert(Long alertId) {
        AlertRecord alert = alertRecordMapper.selectById(alertId);
        if (alert == null) {
            throw new BusinessException("预警记录不存在");
        }
        if (!"待处理".equals(alert.getStatus()) && !"未处理".equals(alert.getStatus())) {
            throw new BusinessException("该告警已在处理中或已处理完成");
        }

        Long operatorId = org.example.persion.security.SecurityUtil.getUserId();
        if (operatorId != null) {
            alert.setAssignedMedicalId(operatorId);
        }

        alert.setStatus("处理中");
        alertRecordMapper.updateById(alert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlert(Long id) {
        AlertRecord alert = alertRecordMapper.selectById(id);
        if (alert == null) {
            throw new BusinessException("预警记录不存在");
        }
        alertRecordMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> getAlertStatistics() {
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRecord::getDeleted, 0);
        List<AlertRecord> alerts = alertRecordMapper.selectList(wrapper);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("total", alerts.size());
        statistics.put("pending", alerts.stream().filter(a -> "待处理".equals(a.getStatus()) || "未处理".equals(a.getStatus())).count());
        statistics.put("processing", alerts.stream().filter(a -> "处理中".equals(a.getStatus())).count());
        statistics.put("handled", alerts.stream().filter(a -> "已处理".equals(a.getStatus())).count());

        Map<String, Long> levelCount = new HashMap<>();
        levelCount.put("低", alerts.stream().filter(a -> "低".equals(a.getAlertLevel())).count());
        levelCount.put("中", alerts.stream().filter(a -> "中".equals(a.getAlertLevel())).count());
        levelCount.put("高", alerts.stream().filter(a -> "高".equals(a.getAlertLevel())).count());
        levelCount.put("紧急", alerts.stream().filter(a -> "紧急".equals(a.getAlertLevel())).count());
        statistics.put("levelCount", levelCount);

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
        wrapper.eq(AlertRecord::getDeleted, 0);
        wrapper.between(AlertRecord::getAlertTime, startOfDay, endOfDay);
        wrapper.orderByDesc(AlertRecord::getAlertTime);

        List<AlertRecord> alerts = alertRecordMapper.selectList(wrapper);
        return alerts.stream().map(alert -> {
            AlertRecordVO vo = new AlertRecordVO();
            BeanUtils.copyProperties(alert, vo);
            return vo;
        }).toList();
    }

    private void rejectClosedAlert(AlertRecord alert) {
        if ("已处理".equals(alert.getStatus()) || "已忽略".equals(alert.getStatus())) {
            throw new BusinessException("该告警任务已关闭，不能重复处理");
        }
    }
}
