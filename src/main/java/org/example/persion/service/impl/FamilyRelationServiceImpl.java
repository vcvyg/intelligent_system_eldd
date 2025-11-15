package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.persion.entity.ElderlyFamilyRelation;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.service.FamilyRelationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 子女端 - 关联管理Service实现
 */
@Service
@RequiredArgsConstructor
public class FamilyRelationServiceImpl implements FamilyRelationService {

    private final ElderlyFamilyRelationMapper familyRelationMapper;
    private final ElderlyInfoMapper elderlyInfoMapper;

    @Override
    public List<Map<String, Object>> getElderlyListByFamilyUser() {
        // TODO: 从JWT token中获取当前子女用户ID
        // 暂时使用固定ID进行演示,实际项目中需要从SecurityContext获取
        Long currentFamilyUserId = 1L;
        return familyRelationMapper.selectElderlyListByFamilyUser(currentFamilyUserId);
    }

    @Override
    public boolean bindElderly(Long elderlyId, String relationType, Integer isPrimaryContact) {
        // TODO: 从JWT token中获取当前子女用户ID
        Long currentFamilyUserId = 1L;

        // 检查是否已存在关联
        LambdaQueryWrapper<ElderlyFamilyRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElderlyFamilyRelation::getElderlyId, elderlyId)
                .eq(ElderlyFamilyRelation::getFamilyUserId, currentFamilyUserId);
        Long count = familyRelationMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("您已经关联过该老人了");
        }

        // 如果设置为主要联系人,先取消该老人其他的主要联系人
        if (isPrimaryContact != null && isPrimaryContact == 1) {
            ElderlyFamilyRelation updateRelation = new ElderlyFamilyRelation();
            updateRelation.setIsPrimaryContact(0);
            LambdaQueryWrapper<ElderlyFamilyRelation> updateWrapper = new LambdaQueryWrapper<>();
            updateWrapper.eq(ElderlyFamilyRelation::getElderlyId, elderlyId)
                    .eq(ElderlyFamilyRelation::getIsPrimaryContact, 1);
            familyRelationMapper.update(updateRelation, updateWrapper);
        }

        // 创建新关联
        ElderlyFamilyRelation relation = new ElderlyFamilyRelation();
        relation.setElderlyId(elderlyId);
        relation.setFamilyUserId(currentFamilyUserId);
        relation.setRelationType(relationType);
        relation.setIsPrimaryContact(isPrimaryContact != null ? isPrimaryContact : 0);
        return familyRelationMapper.insert(relation) > 0;
    }

    @Override
    public boolean unbindElderly(Long elderlyId) {
        // TODO: 从JWT token中获取当前子女用户ID
        Long currentFamilyUserId = 1L;

        LambdaQueryWrapper<ElderlyFamilyRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElderlyFamilyRelation::getElderlyId, elderlyId)
                .eq(ElderlyFamilyRelation::getFamilyUserId, currentFamilyUserId);
        return familyRelationMapper.delete(wrapper) > 0;
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
