package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.dto.PaymentActionDTO;
import org.example.persion.dto.VisitAppointmentRequestDTO;
import org.example.persion.entity.ElderlyFamilyRelation;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.entity.FamilyPaymentRecord;
import org.example.persion.entity.FamilyServiceRecord;
import org.example.persion.entity.VisitAppointment;
import org.example.persion.enums.PaymentStatus;
import org.example.persion.enums.ServiceProgressStatus;
import org.example.persion.enums.VisitAppointmentStatus;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.FamilyPaymentRecordMapper;
import org.example.persion.repository.FamilyServiceRecordMapper;
import org.example.persion.repository.VisitAppointmentMapper;
import org.example.persion.security.SecurityUtil;
import org.example.persion.service.FamilyServicesService;
import org.example.persion.vo.FamilyPaymentRecordVO;
import org.example.persion.vo.FamilyServiceRecordVO;
import org.example.persion.vo.VisitAppointmentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FamilyServicesServiceImpl implements FamilyServicesService {

    private final FamilyServiceRecordMapper serviceRecordMapper;
    private final VisitAppointmentMapper visitAppointmentMapper;
    private final FamilyPaymentRecordMapper paymentRecordMapper;
    private final ElderlyFamilyRelationMapper elderlyFamilyRelationMapper;
    private final ElderlyInfoMapper elderlyInfoMapper;

    @Override
    public List<FamilyServiceRecordVO> getServiceProgress(Long elderlyId) {
        if (elderlyId == null) {
            throw new BusinessException("老人ID不能为空");
        }
        ensureElderlyBoundToCurrentUser(elderlyId);
        ElderlyInfo elderlyInfo = elderlyInfoMapper.selectById(elderlyId);
        List<FamilyServiceRecord> records = serviceRecordMapper.selectList(
                new LambdaQueryWrapper<FamilyServiceRecord>()
                        .eq(FamilyServiceRecord::getElderlyId, elderlyId)
                        .orderByDesc(FamilyServiceRecord::getServiceDate)
                        .orderByDesc(FamilyServiceRecord::getServiceTime)
        );
        return records.stream()
                .map(record -> toServiceVO(record, elderlyInfo))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VisitAppointmentVO createAppointment(VisitAppointmentRequestDTO request) {
        validateAppointmentParams(request);
        ensureElderlyBoundToCurrentUser(request.getElderlyId());

        VisitAppointment appointment = new VisitAppointment();
        appointment.setFamilyUserId(getCurrentUserId());
        appointment.setElderlyId(request.getElderlyId());
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setPurpose(request.getPurpose());
        appointment.setNote(request.getNote());
        appointment.setStatus(VisitAppointmentStatus.PENDING);

        visitAppointmentMapper.insert(appointment);
        ElderlyInfo elderlyInfo = elderlyInfoMapper.selectById(request.getElderlyId());
        return toAppointmentVO(appointment, elderlyInfo);
    }

    @Override
    public List<VisitAppointmentVO> listAppointments() {
        Long userId = getCurrentUserId();
        List<VisitAppointment> appointments = visitAppointmentMapper.selectList(
                new LambdaQueryWrapper<VisitAppointment>()
                        .eq(VisitAppointment::getFamilyUserId, userId)
                        .orderByDesc(VisitAppointment::getAppointmentDate)
                        .orderByDesc(VisitAppointment::getAppointmentTime)
        );
        Map<Long, ElderlyInfo> elderlyInfoMap = loadElderlyInfo(appointments.stream()
                .map(VisitAppointment::getElderlyId)
                .collect(Collectors.toSet()));
        return appointments.stream()
                .map(item -> toAppointmentVO(item, elderlyInfoMap.get(item.getElderlyId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAppointment(Long appointmentId) {
        VisitAppointment appointment = visitAppointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new BusinessException("预约记录不存在");
        }
        if (!appointment.getFamilyUserId().equals(getCurrentUserId())) {
            throw new BusinessException("不能取消他人的预约记录");
        }
        if (appointment.getStatus() == VisitAppointmentStatus.CANCELLED) {
            return;
        }
        appointment.setStatus(VisitAppointmentStatus.CANCELLED);
        appointment.setReviewRemark("家属取消预约");
        visitAppointmentMapper.updateById(appointment);
    }

    @Override
    public List<FamilyPaymentRecordVO> listPendingPayments() {
        Long userId = getCurrentUserId();
        List<FamilyPaymentRecord> records = paymentRecordMapper.selectList(
                new LambdaQueryWrapper<FamilyPaymentRecord>()
                        .eq(FamilyPaymentRecord::getFamilyUserId, userId)
                        .eq(FamilyPaymentRecord::getStatus, PaymentStatus.PENDING)
                        .orderByAsc(FamilyPaymentRecord::getDueDate)
                        .orderByDesc(FamilyPaymentRecord::getCreateTime)
        );
        return enrichPaymentRecords(records);
    }

    @Override
    public List<FamilyPaymentRecordVO> listPaymentHistory() {
        Long userId = getCurrentUserId();
        List<FamilyPaymentRecord> records = paymentRecordMapper.selectList(
                new LambdaQueryWrapper<FamilyPaymentRecord>()
                        .eq(FamilyPaymentRecord::getFamilyUserId, userId)
                        .ne(FamilyPaymentRecord::getStatus, PaymentStatus.PENDING)
                        .orderByDesc(FamilyPaymentRecord::getPayTime)
                        .orderByDesc(FamilyPaymentRecord::getCreateTime)
        );
        return enrichPaymentRecords(records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FamilyPaymentRecordVO pay(Long recordId, PaymentActionDTO request) {
        FamilyPaymentRecord record = paymentRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("支付记录不存在");
        }
        if (!record.getFamilyUserId().equals(getCurrentUserId())) {
            throw new BusinessException("无法支付他人的账单");
        }
        if (record.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("该账单无需支付");
        }
        record.setStatus(PaymentStatus.PAID);
        record.setPayMethod(request != null && request.getPayMethod() != null ? request.getPayMethod() : "WECHAT");
        record.setPayTime(LocalDateTime.now());
        paymentRecordMapper.updateById(record);
        ElderlyInfo elderlyInfo = elderlyInfoMapper.selectById(record.getElderlyId());
        return toPaymentVO(record, elderlyInfo);
    }

    private Long getCurrentUserId() {
        Long userId = SecurityUtil.getUserId();
        if (userId == null) {
            throw new BusinessException("未登录或登录已失效");
        }
        return userId;
    }

    private void ensureElderlyBoundToCurrentUser(Long elderlyId) {
        Long userId = getCurrentUserId();
        Long count = elderlyFamilyRelationMapper.selectCount(
                new LambdaQueryWrapper<ElderlyFamilyRelation>()
                        .eq(ElderlyFamilyRelation::getFamilyUserId, userId)
                        .eq(ElderlyFamilyRelation::getElderlyId, elderlyId)
        );
        if (count == null || count == 0) {
            throw new BusinessException("您无权访问该老人的信息");
        }
    }

    private void validateAppointmentParams(VisitAppointmentRequestDTO request) {
        if (request.getElderlyId() == null) {
            throw new BusinessException("请选择要探访的老人");
        }
        if (request.getAppointmentDate() == null) {
            throw new BusinessException("请选择预约日期");
        }
        if (request.getAppointmentTime() == null) {
            throw new BusinessException("请选择预约时间");
        }
        if (request.getPurpose() == null || request.getPurpose().isBlank()) {
            throw new BusinessException("请填写探访目的");
        }
    }

    private Map<Long, ElderlyInfo> loadElderlyInfo(java.util.Set<Long> elderlyIds) {
        if (elderlyIds == null || elderlyIds.isEmpty()) {
            return Map.of();
        }
        return elderlyInfoMapper.selectBatchIds(elderlyIds).stream()
                .collect(Collectors.toMap(ElderlyInfo::getId, Function.identity()));
    }

    private FamilyServiceRecordVO toServiceVO(FamilyServiceRecord record, ElderlyInfo elderlyInfo) {
        FamilyServiceRecordVO vo = new FamilyServiceRecordVO();
        vo.setId(record.getId());
        vo.setElderlyId(record.getElderlyId());
        vo.setElderlyName(elderlyInfo != null ? elderlyInfo.getName() : "-");
        vo.setServiceType(record.getServiceType());
        vo.setServiceDate(record.getServiceDate());
        vo.setServiceTime(record.getServiceTime());
        vo.setMedicalStaff(record.getMedicalStaff());
        vo.setStatus(record.getStatus() == null ? ServiceProgressStatus.COMPLETED : record.getStatus());
        vo.setDescription(record.getDescription());
        return vo;
    }

    private VisitAppointmentVO toAppointmentVO(VisitAppointment appointment, ElderlyInfo elderlyInfo) {
        VisitAppointmentVO vo = new VisitAppointmentVO();
        vo.setId(appointment.getId());
        vo.setElderlyId(appointment.getElderlyId());
        vo.setFamilyUserId(appointment.getFamilyUserId());
        vo.setElderlyName(elderlyInfo != null ? elderlyInfo.getName() : "-");
        vo.setAppointmentDate(appointment.getAppointmentDate());
        vo.setAppointmentTime(appointment.getAppointmentTime());
        vo.setPurpose(appointment.getPurpose());
        vo.setNote(appointment.getNote());
        vo.setStatus(appointment.getStatus());
        vo.setReviewRemark(appointment.getReviewRemark());
        return vo;
    }

    private List<FamilyPaymentRecordVO> enrichPaymentRecords(List<FamilyPaymentRecord> records) {
        Map<Long, ElderlyInfo> elderlyInfoMap = loadElderlyInfo(records.stream()
                .map(FamilyPaymentRecord::getElderlyId)
                .collect(Collectors.toSet()));
        return records.stream()
                .map(record -> toPaymentVO(record, elderlyInfoMap.get(record.getElderlyId())))
                .collect(Collectors.toList());
    }

    private FamilyPaymentRecordVO toPaymentVO(FamilyPaymentRecord record, ElderlyInfo elderlyInfo) {
        FamilyPaymentRecordVO vo = new FamilyPaymentRecordVO();
        vo.setId(record.getId());
        vo.setElderlyId(record.getElderlyId());
        vo.setElderlyName(elderlyInfo != null ? elderlyInfo.getName() : "-");
        vo.setItemName(record.getItemName());
        vo.setAmount(record.getAmount());
        vo.setStatus(record.getStatus());
        vo.setPayMethod(record.getPayMethod());
        vo.setPayTime(record.getPayTime());
        vo.setDueDate(record.getDueDate());
        vo.setRemark(record.getRemark());
        return vo;
    }
}

