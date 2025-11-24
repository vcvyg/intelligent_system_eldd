package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.dto.MedicalPaymentRecordRequestDTO;
import org.example.persion.dto.MedicalServiceRecordRequestDTO;
import org.example.persion.entity.ElderlyFamilyRelation;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.entity.FamilyPaymentRecord;
import org.example.persion.entity.FamilyServiceRecord;
import org.example.persion.entity.User;
import org.example.persion.enums.PaymentStatus;
import org.example.persion.enums.ServiceProgressStatus;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.FamilyPaymentRecordMapper;
import org.example.persion.repository.FamilyServiceRecordMapper;
import org.example.persion.repository.UserMapper;
import org.example.persion.security.SecurityUtil;
import org.example.persion.service.MedicalFamilyServicesService;
import org.example.persion.vo.FamilyContactVO;
import org.example.persion.vo.FamilyPaymentRecordVO;
import org.example.persion.vo.FamilyServiceRecordVO;
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
        return toServiceRecordVO(record, elderlyInfo);
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
        return records.stream()
                .map(record -> toServiceRecordVO(record, elderlyInfo))
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

    private FamilyServiceRecordVO toServiceRecordVO(FamilyServiceRecord record, ElderlyInfo elderlyInfo) {
        FamilyServiceRecordVO vo = new FamilyServiceRecordVO();
        Objects.requireNonNull(record, "record不能为空");
        BeanUtils.copyProperties(record, vo);
        vo.setElderlyName(elderlyInfo != null ? elderlyInfo.getName() : "-");
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

