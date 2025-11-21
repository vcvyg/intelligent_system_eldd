package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.entity.ElderlyMedicalRelation;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.ElderlyMedicalRelationMapper;
import org.example.persion.security.SecurityUtil;
import org.example.persion.service.MedicalRelationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 医护端 - 关联管理Service实现
 */
@Service
@RequiredArgsConstructor
public class MedicalRelationServiceImpl implements MedicalRelationService {

    private final ElderlyMedicalRelationMapper medicalRelationMapper;
    private final ElderlyInfoMapper elderlyInfoMapper;

    @Override
    public List<Map<String, Object>> getElderlyListByMedicalUser() {
        Long currentMedicalUserId = SecurityUtil.getUserId();
        if (currentMedicalUserId == null) {
            throw new RuntimeException("无法获取当前用户信息");
        }
        return medicalRelationMapper.selectElderlyListByMedicalUser(currentMedicalUserId);
    }

    @Override
    public boolean bindElderly(Long elderlyId, Integer isPrimaryDoctor) {
        Long currentMedicalUserId = SecurityUtil.getUserId();
        if (currentMedicalUserId == null) {
            throw new RuntimeException("无法获取当前用户信息");
        }

        // 检查是否已存在关联
        LambdaQueryWrapper<ElderlyMedicalRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElderlyMedicalRelation::getElderlyId, elderlyId)
                .eq(ElderlyMedicalRelation::getMedicalUserId, currentMedicalUserId);
        Long count = medicalRelationMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("您已经负责该老人了");
        }

        // 如果设置为主治医生,先取消该老人其他的主治医生
        if (isPrimaryDoctor != null && isPrimaryDoctor == 1) {
            ElderlyMedicalRelation updateRelation = new ElderlyMedicalRelation();
            updateRelation.setIsPrimaryDoctor(0);
            LambdaQueryWrapper<ElderlyMedicalRelation> updateWrapper = new LambdaQueryWrapper<>();
            updateWrapper.eq(ElderlyMedicalRelation::getElderlyId, elderlyId)
                    .eq(ElderlyMedicalRelation::getIsPrimaryDoctor, 1);
            medicalRelationMapper.update(updateRelation, updateWrapper);
        }

        // 创建新关联
        ElderlyMedicalRelation relation = new ElderlyMedicalRelation();
        relation.setElderlyId(elderlyId);
        relation.setMedicalUserId(currentMedicalUserId);
        relation.setIsPrimaryDoctor(isPrimaryDoctor != null ? isPrimaryDoctor : 0);
        relation.setAssignDate(LocalDateTime.now());
        return medicalRelationMapper.insert(relation) > 0;
    }

    @Override
    public boolean unbindElderly(Long elderlyId) {
        Long currentMedicalUserId = SecurityUtil.getUserId();
        if (currentMedicalUserId == null) {
            throw new RuntimeException("无法获取当前用户信息");
        }

        LambdaQueryWrapper<ElderlyMedicalRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElderlyMedicalRelation::getElderlyId, elderlyId)
                .eq(ElderlyMedicalRelation::getMedicalUserId, currentMedicalUserId);
        return medicalRelationMapper.delete(wrapper) > 0;
    }

    @Override
    public List<Map<String, Object>> searchElderlyForBinding(String keyword) {
        // 根据姓名或身份证号搜索老人
        LambdaQueryWrapper<ElderlyInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(ElderlyInfo::getName, keyword)
                .or()
                .like(ElderlyInfo::getIdCard, keyword));
        return elderlyInfoMapper.selectMaps(wrapper);
    }
}
