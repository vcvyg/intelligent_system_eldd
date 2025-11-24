package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.dto.VisitAppointmentReviewDTO;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.entity.User;
import org.example.persion.entity.VisitAppointment;
import org.example.persion.enums.VisitAppointmentStatus;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.UserMapper;
import org.example.persion.repository.VisitAppointmentMapper;
import org.example.persion.service.AdminFamilyServicesService;
import org.example.persion.vo.VisitAppointmentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminFamilyServicesServiceImpl implements AdminFamilyServicesService {

    private final VisitAppointmentMapper visitAppointmentMapper;
    private final ElderlyInfoMapper elderlyInfoMapper;
    private final UserMapper userMapper;

    @Override
    public List<VisitAppointmentVO> listAppointments(VisitAppointmentStatus status) {
        LambdaQueryWrapper<VisitAppointment> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            queryWrapper.eq(VisitAppointment::getStatus, status);
        }
        queryWrapper.orderByDesc(VisitAppointment::getAppointmentDate)
                .orderByDesc(VisitAppointment::getAppointmentTime);

        List<VisitAppointment> records = visitAppointmentMapper.selectList(queryWrapper);
        Map<Long, ElderlyInfo> elderlyInfoMap = loadElderlyInfo(records);
        Map<Long, User> userMap = loadFamilyUsers(records);

        return records.stream()
                .map(record -> toVisitAppointmentVO(record, elderlyInfoMap.get(record.getElderlyId()),
                        userMap.get(record.getFamilyUserId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VisitAppointmentVO reviewAppointment(Long appointmentId, VisitAppointmentReviewDTO request) {
        if (request == null || request.getStatus() == null) {
            throw new BusinessException("请选择审批结果");
        }
        VisitAppointment appointment = visitAppointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new BusinessException("预约记录不存在");
        }
        if (appointment.getStatus() == VisitAppointmentStatus.CANCELLED) {
            throw new BusinessException("该预约已被家属取消");
        }
        if (appointment.getStatus() == VisitAppointmentStatus.APPROVED ||
                appointment.getStatus() == VisitAppointmentStatus.REJECTED) {
            throw new BusinessException("该预约已审批");
        }
        if (request.getStatus() != VisitAppointmentStatus.APPROVED &&
                request.getStatus() != VisitAppointmentStatus.REJECTED) {
            throw new BusinessException("审批状态仅支持同意或拒绝");
        }

        appointment.setStatus(request.getStatus());
        appointment.setReviewRemark(request.getRemark());
        visitAppointmentMapper.updateById(appointment);

        ElderlyInfo elderlyInfo = elderlyInfoMapper.selectById(appointment.getElderlyId());
        User familyUser = userMapper.selectById(appointment.getFamilyUserId());
        return toVisitAppointmentVO(appointment, elderlyInfo, familyUser);
    }

    private Map<Long, ElderlyInfo> loadElderlyInfo(List<VisitAppointment> appointments) {
        return appointments.stream()
                .map(VisitAppointment::getElderlyId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        elderlyInfoMapper::selectById,
                        (left, right) -> left
                ));
    }

    private Map<Long, User> loadFamilyUsers(List<VisitAppointment> appointments) {
        return appointments.stream()
                .map(VisitAppointment::getFamilyUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        userMapper::selectById,
                        (left, right) -> left
                ));
    }

    private VisitAppointmentVO toVisitAppointmentVO(VisitAppointment appointment,
                                                    ElderlyInfo elderlyInfo,
                                                    User familyUser) {
        VisitAppointmentVO vo = new VisitAppointmentVO();
        vo.setId(appointment.getId());
        vo.setElderlyId(appointment.getElderlyId());
        vo.setFamilyUserId(appointment.getFamilyUserId());
        vo.setFamilyUsername(familyUser != null ?
                (familyUser.getRealName() != null ? familyUser.getRealName() : familyUser.getUsername()) : "-");
        vo.setElderlyName(elderlyInfo != null ? elderlyInfo.getName() : "-");
        vo.setAppointmentDate(appointment.getAppointmentDate());
        vo.setAppointmentTime(appointment.getAppointmentTime());
        vo.setPurpose(appointment.getPurpose());
        vo.setNote(appointment.getNote());
        vo.setStatus(appointment.getStatus());
        vo.setReviewRemark(appointment.getReviewRemark());
        return vo;
    }
}

