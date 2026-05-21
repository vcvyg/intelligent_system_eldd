package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.dto.MedicalPaymentRecordRequestDTO;
import org.example.persion.dto.MedicalServiceRecordRequestDTO;
import org.example.persion.dto.MedicalServiceStatusUpdateDTO;
import org.example.persion.entity.ElderlyFamilyRelation;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.entity.FamilyPaymentRecord;
import org.example.persion.entity.FamilyServiceRecord;
import org.example.persion.entity.FamilyServiceStatusHistory;
import org.example.persion.entity.User;
import org.example.persion.enums.PaymentStatus;
import org.example.persion.enums.ServiceProgressStatus;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.FamilyPaymentRecordMapper;
import org.example.persion.repository.FamilyServiceRecordMapper;
import org.example.persion.repository.FamilyServiceStatusHistoryMapper;
import org.example.persion.repository.UserMapper;
import org.example.persion.security.SecurityUtil;
import org.example.persion.service.MedicalFamilyServicesService;
import org.example.persion.vo.FamilyContactVO;
import org.example.persion.vo.FamilyPaymentRecordVO;
import org.example.persion.vo.FamilyServiceRecordVO;
import org.example.persion.vo.MedicalFamilyServiceSummaryVO;
import org.example.persion.vo.ServiceStatusHistoryVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalFamilyServicesServiceImpl implements MedicalFamilyServicesService {

    private final FamilyServiceRecordMapper familyServiceRecordMapper;
    private final FamilyPaymentRecordMapper familyPaymentRecordMapper;
    private final FamilyServiceStatusHistoryMapper serviceStatusHistoryMapper;
    private final ElderlyInfoMapper elderlyInfoMapper;
    private final ElderlyFamilyRelationMapper elderlyFamilyRelationMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FamilyServiceRecordVO createServiceRecord(MedicalServiceRecordRequestDTO request) {
        validateServiceRecord(request);
        ElderlyInfo elderlyInfo = elderlyInfoMapper.selectById(request.getElderlyId());
        if (elderlyInfo == null) {
            throw new BusinessException("未找到指定老人信息");
        }
        User medicalUser = getCurrentUser();

        FamilyServiceRecord record = new FamilyServiceRecord();
        record.setElderlyId(request.getElderlyId());
        record.setServiceType(request.getServiceType());
        record.setServiceDate(Optional.ofNullable(request.getServiceDate()).orElse(LocalDate.now()));
        record.setServiceTime(Optional.ofNullable(request.getServiceTime()).orElse(LocalTime.now()));
        record.setMedicalStaff(medicalUser.getRealName() != null ? medicalUser.getRealName() : medicalUser.getUsername());
        record.setStatus(request.getStatus() != null ? request.getStatus() : ServiceProgressStatus.COMPLETED);
        record.setDescription(request.getDescription());

        familyServiceRecordMapper.insert(record);
        recordStatusHistory(record.getId(), null, record.getStatus(), "创建记录", medicalUser);
        return toServiceRecordVO(record, elderlyInfo, getStatusTimeline(record.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FamilyServiceRecordVO updateServiceRecordStatus(Long recordId, MedicalServiceStatusUpdateDTO request) {
        if (recordId == null) {
            throw new BusinessException("缺少服务记录ID");
        }
        if (request == null || request.getStatus() == null) {
            throw new BusinessException("请选择要更新的状态");
        }
        FamilyServiceRecord record = familyServiceRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("服务记录不存在");
        }
        ServiceProgressStatus oldStatus = record.getStatus();
        if (Objects.equals(oldStatus, request.getStatus())) {
            throw new BusinessException("状态未发生变化");
        }
        record.setStatus(request.getStatus());
        familyServiceRecordMapper.updateById(record);

        User operator = getCurrentUser();
        recordStatusHistory(recordId, oldStatus, request.getStatus(), request.getRemark(), operator);

        ElderlyInfo elderlyInfo = elderlyInfoMapper.selectById(record.getElderlyId());
        return toServiceRecordVO(record, elderlyInfo, getStatusTimeline(recordId));
    }

    @Override
    public List<FamilyServiceRecordVO> listServiceRecords(Long elderlyId) {
        if (elderlyId == null) {
            throw new BusinessException("请选择老人");
        }
        ElderlyInfo elderlyInfo = elderlyInfoMapper.selectById(elderlyId);
        if (elderlyInfo == null) {
            throw new BusinessException("未找到指定老人信息");
        }
        List<FamilyServiceRecord> records = familyServiceRecordMapper.selectList(
                new LambdaQueryWrapper<FamilyServiceRecord>()
                        .eq(FamilyServiceRecord::getElderlyId, elderlyId)
                        .orderByDesc(FamilyServiceRecord::getServiceDate)
                        .orderByDesc(FamilyServiceRecord::getServiceTime)
        );
        Map<Long, List<ServiceStatusHistoryVO>> historyMap = loadStatusTimeline(records);
        return records.stream()
                .map(record -> toServiceRecordVO(record, elderlyInfo, historyMap.getOrDefault(record.getId(), List.of())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FamilyPaymentRecordVO createPaymentRecord(MedicalPaymentRecordRequestDTO request) {
        validatePaymentRecord(request);
        ElderlyInfo elderlyInfo = elderlyInfoMapper.selectById(request.getElderlyId());
        if (elderlyInfo == null) {
            throw new BusinessException("未找到指定老人信息");
        }
        Long familyUserId = resolveFamilyUserId(request.getElderlyId(), request.getFamilyUserId());

        FamilyPaymentRecord record = new FamilyPaymentRecord();
        record.setElderlyId(request.getElderlyId());
        record.setFamilyUserId(familyUserId);
        record.setItemName(request.getItemName());
        record.setAmount(request.getAmount());
        record.setStatus(PaymentStatus.PENDING);
        record.setDueDate(Optional.ofNullable(request.getDueDate()).orElse(LocalDate.now().plusDays(7)));
        record.setRemark(request.getRemark());
        record.setCreateTime(LocalDateTime.now());

        familyPaymentRecordMapper.insert(record);
        return toPaymentRecordVO(record, elderlyInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FamilyPaymentRecordVO cancelPaymentRecord(Long recordId) {
        if (recordId == null) {
            throw new BusinessException("缺少缴费记录ID");
        }
        FamilyPaymentRecord record = familyPaymentRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("缴费记录不存在");
        }
        if (record.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("仅待支付缴费通知可撤销");
        }

        record.setStatus(PaymentStatus.CANCELLED);
        familyPaymentRecordMapper.updateById(record);

        ElderlyInfo elderlyInfo = elderlyInfoMapper.selectById(record.getElderlyId());
        return toPaymentRecordVO(record, elderlyInfo);
    }

    @Override
    public List<FamilyPaymentRecordVO> listPaymentRecords(Long elderlyId) {
        if (elderlyId == null) {
            throw new BusinessException("请选择老人");
        }
        ElderlyInfo elderlyInfo = elderlyInfoMapper.selectById(elderlyId);
        if (elderlyInfo == null) {
            throw new BusinessException("未找到指定老人信息");
        }
        List<FamilyPaymentRecord> records = familyPaymentRecordMapper.selectList(
                new LambdaQueryWrapper<FamilyPaymentRecord>()
                        .eq(FamilyPaymentRecord::getElderlyId, elderlyId)
                        .orderByDesc(FamilyPaymentRecord::getCreateTime)
        );
        return records.stream()
                .map(record -> toPaymentRecordVO(record, elderlyInfo))
                .collect(Collectors.toList());
    }

    @Override
    public List<FamilyContactVO> listFamilyContacts(Long elderlyId) {
        if (elderlyId == null) {
            throw new BusinessException("请选择老人");
        }
        List<Map<String, Object>> rawList = elderlyFamilyRelationMapper.selectFamilyListWithUserInfo(elderlyId);
        return rawList.stream()
                .map(item -> {
                    FamilyContactVO vo = new FamilyContactVO();
                    Object userId = item.get("family_user_id");
                    vo.setUserId(userId == null ? null : Long.parseLong(userId.toString()));
                    vo.setUsername(item.getOrDefault("username", "-").toString());
                    Object realName = item.get("real_name");
                    vo.setRealName(realName == null ? vo.getUsername() : realName.toString());
                    vo.setPhone(item.getOrDefault("phone", "-").toString());
                    vo.setRelationType(item.getOrDefault("relation_type", "家属").toString());
                    Object primary = item.get("is_primary_contact");
                    vo.setPrimaryContact(primary != null && Integer.parseInt(primary.toString()) == 1);
                    return vo;
                })
                .sorted(Comparator.comparing((FamilyContactVO vo) -> !Boolean.TRUE.equals(vo.getPrimaryContact())))
                .collect(Collectors.toList());
    }

    @Override
    public MedicalFamilyServiceSummaryVO getSummary() {
        LocalDate today = LocalDate.now();
        Long todayServiceCount = familyServiceRecordMapper.selectCount(
                new LambdaQueryWrapper<FamilyServiceRecord>()
                        .eq(FamilyServiceRecord::getServiceDate, today)
        );

        List<FamilyPaymentRecord> pendingPayments = familyPaymentRecordMapper.selectList(
                new LambdaQueryWrapper<FamilyPaymentRecord>()
                        .eq(FamilyPaymentRecord::getStatus, PaymentStatus.PENDING)
        );
        BigDecimal pendingPaymentAmount = pendingPayments.stream()
                .map(FamilyPaymentRecord::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        MedicalFamilyServiceSummaryVO summaryVO = new MedicalFamilyServiceSummaryVO();
        summaryVO.setTodayServiceCount(todayServiceCount);
        summaryVO.setPendingPaymentCount((long) pendingPayments.size());
        summaryVO.setPendingPaymentAmount(pendingPaymentAmount);
        return summaryVO;
    }

    private void validateServiceRecord(MedicalServiceRecordRequestDTO request) {
        if (request == null || request.getElderlyId() == null) {
            throw new BusinessException("请选择需要填写服务记录的老人");
        }
        if (request.getServiceType() == null || request.getServiceType().isBlank()) {
            throw new BusinessException("请填写服务类型");
        }
    }

    private void validatePaymentRecord(MedicalPaymentRecordRequestDTO request) {
        if (request == null || request.getElderlyId() == null) {
            throw new BusinessException("请选择需要生成账单的老人");
        }
        if (request.getItemName() == null || request.getItemName().isBlank()) {
            throw new BusinessException("请填写项目名称");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("请填写正确的金额");
        }
    }

    private Long resolveFamilyUserId(Long elderlyId, Long providedFamilyUserId) {
        if (providedFamilyUserId != null) {
            ElderlyFamilyRelation relation = elderlyFamilyRelationMapper.selectOne(
                    new LambdaQueryWrapper<ElderlyFamilyRelation>()
                            .eq(ElderlyFamilyRelation::getElderlyId, elderlyId)
                            .eq(ElderlyFamilyRelation::getFamilyUserId, providedFamilyUserId)
                            .eq(ElderlyFamilyRelation::getDeleted, 0)
            );
            if (relation == null) {
                throw new BusinessException("未找到该家属与老人的绑定关系");
            }
            return providedFamilyUserId;
        }

        List<ElderlyFamilyRelation> relations = elderlyFamilyRelationMapper.selectList(
                new LambdaQueryWrapper<ElderlyFamilyRelation>()
                        .eq(ElderlyFamilyRelation::getElderlyId, elderlyId)
                        .eq(ElderlyFamilyRelation::getDeleted, 0)
        );
        return relations.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.<ElderlyFamilyRelation>comparingInt(relation ->
                                relation.getIsPrimaryContact() != null ? -relation.getIsPrimaryContact() : 0)
                        .thenComparing(ElderlyFamilyRelation::getId))
                .map(ElderlyFamilyRelation::getFamilyUserId)
                .findFirst()
                .orElseThrow(() -> new BusinessException("该老人尚未绑定任何家属，无法创建缴费记录"));
    }

    private User getCurrentUser() {
        Long userId = SecurityUtil.getUserId();
        if (userId == null) {
            throw new BusinessException("登录已失效，请重新登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("未找到当前登录用户");
        }
        return user;
    }

    private void recordStatusHistory(Long recordId, ServiceProgressStatus oldStatus,
                                     ServiceProgressStatus newStatus, String remark, User operator) {
        FamilyServiceStatusHistory history = new FamilyServiceStatusHistory();
        history.setServiceRecordId(recordId);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        if (operator == null) {
            operator = getCurrentUser();
        }
        history.setChangedBy(operator.getId());
        history.setChangedByName(operator.getRealName() != null ? operator.getRealName() : operator.getUsername());
        history.setRemark(remark);
        history.setChangeTime(LocalDateTime.now());
        serviceStatusHistoryMapper.insert(history);
    }

    private Map<Long, List<ServiceStatusHistoryVO>> loadStatusTimeline(List<FamilyServiceRecord> records) {
        if (records == null || records.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = records.stream()
                .map(FamilyServiceRecord::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<FamilyServiceStatusHistory> histories = serviceStatusHistoryMapper.selectList(
                new LambdaQueryWrapper<FamilyServiceStatusHistory>()
                        .in(FamilyServiceStatusHistory::getServiceRecordId, ids)
                        .orderByAsc(FamilyServiceStatusHistory::getChangeTime)
                        .orderByAsc(FamilyServiceStatusHistory::getId)
        );
        return histories.stream()
                .collect(Collectors.groupingBy(FamilyServiceStatusHistory::getServiceRecordId,
                        Collectors.mapping(this::toHistoryVO, Collectors.toList())));
    }

    private List<ServiceStatusHistoryVO> getStatusTimeline(Long recordId) {
        if (recordId == null) {
            return List.of();
        }
        List<FamilyServiceStatusHistory> histories = serviceStatusHistoryMapper.selectList(
                new LambdaQueryWrapper<FamilyServiceStatusHistory>()
                        .eq(FamilyServiceStatusHistory::getServiceRecordId, recordId)
                        .orderByAsc(FamilyServiceStatusHistory::getChangeTime)
                        .orderByAsc(FamilyServiceStatusHistory::getId)
        );
        return histories.stream()
                .map(this::toHistoryVO)
                .collect(Collectors.toList());
    }

    private ServiceStatusHistoryVO toHistoryVO(FamilyServiceStatusHistory history) {
        ServiceStatusHistoryVO vo = new ServiceStatusHistoryVO();
        vo.setFromStatus(history.getOldStatus());
        vo.setToStatus(history.getNewStatus());
        vo.setChangedBy(history.getChangedBy());
        vo.setChangedByName(history.getChangedByName());
        vo.setRemark(history.getRemark());
        vo.setChangeTime(history.getChangeTime());
        return vo;
    }

    private FamilyServiceRecordVO toServiceRecordVO(FamilyServiceRecord record, ElderlyInfo elderlyInfo,
                                                    List<ServiceStatusHistoryVO> timeline) {
        FamilyServiceRecordVO vo = new FamilyServiceRecordVO();
        Objects.requireNonNull(record, "record不能为空");
        BeanUtils.copyProperties(record, vo);
        vo.setElderlyName(elderlyInfo != null ? elderlyInfo.getName() : "-");
        vo.setStatusTimeline(timeline);
        return vo;
    }

    private FamilyPaymentRecordVO toPaymentRecordVO(FamilyPaymentRecord record, ElderlyInfo elderlyInfo) {
        FamilyPaymentRecordVO vo = new FamilyPaymentRecordVO();
        Objects.requireNonNull(record, "record不能为空");
        BeanUtils.copyProperties(record, vo);
        vo.setElderlyName(elderlyInfo != null ? elderlyInfo.getName() : "-");
        return vo;
    }
}

