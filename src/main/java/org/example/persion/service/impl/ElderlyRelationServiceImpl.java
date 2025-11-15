package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.persion.entity.DeviceInfo;
import org.example.persion.entity.ElderlyFamilyRelation;
import org.example.persion.entity.ElderlyMedicalRelation;
import org.example.persion.repository.DeviceInfoMapper;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.repository.ElderlyMedicalRelationMapper;
import org.example.persion.service.ElderlyRelationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 老人关联关系Service实现
 */
@Service
@RequiredArgsConstructor
public class ElderlyRelationServiceImpl implements ElderlyRelationService {

    private final ElderlyFamilyRelationMapper familyRelationMapper;
    private final ElderlyMedicalRelationMapper medicalRelationMapper;
    private final DeviceInfoMapper deviceInfoMapper;

    @Override
    public List<Map<String, Object>> getFamilyRelations(Long elderlyId) {
        return familyRelationMapper.selectFamilyListWithUserInfo(elderlyId);
    }

    @Override
    public boolean addFamilyRelation(Long elderlyId, Long familyUserId, String relationType, Integer isPrimaryContact) {
        // 检查是否已存在关联
        LambdaQueryWrapper<ElderlyFamilyRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElderlyFamilyRelation::getElderlyId, elderlyId)
                .eq(ElderlyFamilyRelation::getFamilyUserId, familyUserId);
        Long count = familyRelationMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("该子女已经关联过了");
        }

        // 如果设置为主要联系人,先取消其他的主要联系人
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
        relation.setFamilyUserId(familyUserId);
        relation.setRelationType(relationType);
        relation.setIsPrimaryContact(isPrimaryContact != null ? isPrimaryContact : 0);
        return familyRelationMapper.insert(relation) > 0;
    }

    @Override
    public boolean removeFamilyRelation(Long relationId) {
        return familyRelationMapper.deleteById(relationId) > 0;
    }

    @Override
    public List<Map<String, Object>> getMedicalRelations(Long elderlyId) {
        return medicalRelationMapper.selectMedicalListWithUserInfo(elderlyId);
    }

    @Override
    public boolean addMedicalRelation(Long elderlyId, Long medicalUserId, Integer isPrimaryDoctor) {
        // 检查是否已存在关联
        LambdaQueryWrapper<ElderlyMedicalRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElderlyMedicalRelation::getElderlyId, elderlyId)
                .eq(ElderlyMedicalRelation::getMedicalUserId, medicalUserId);
        Long count = medicalRelationMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("该医护人员已经分配过了");
        }

        // 如果设置为主治医生,先取消其他的主治医生
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
        relation.setMedicalUserId(medicalUserId);
        relation.setIsPrimaryDoctor(isPrimaryDoctor != null ? isPrimaryDoctor : 0);
        relation.setAssignDate(LocalDateTime.now());
        return medicalRelationMapper.insert(relation) > 0;
    }

    @Override
    public boolean removeMedicalRelation(Long relationId) {
        return medicalRelationMapper.deleteById(relationId) > 0;
    }

    @Override
    public List<Map<String, Object>> getDeviceRelations(Long elderlyId) {
        // 查询绑定到该老人的设备
        LambdaQueryWrapper<DeviceInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceInfo::getElderlyId, elderlyId);
        return deviceInfoMapper.selectMaps(wrapper);
    }

    @Override
    public boolean bindDevice(Long elderlyId, Long deviceId) {
        // 检查设备是否已被其他老人绑定
        DeviceInfo device = deviceInfoMapper.selectById(deviceId);
        if (device == null) {
            throw new RuntimeException("设备不存在");
        }
        if (device.getElderlyId() != null && !device.getElderlyId().equals(elderlyId)) {
            throw new RuntimeException("该设备已被其他老人绑定");
        }

        // 更新设备绑定
        DeviceInfo updateDevice = new DeviceInfo();
        updateDevice.setId(deviceId);
        updateDevice.setElderlyId(elderlyId);
        return deviceInfoMapper.updateById(updateDevice) > 0;
    }

    @Override
    public boolean unbindDevice(Long elderlyId, Long deviceId) {
        // 检查设备是否属于该老人
        DeviceInfo device = deviceInfoMapper.selectById(deviceId);
        if (device == null) {
            throw new RuntimeException("设备不存在");
        }
        if (device.getElderlyId() == null || !device.getElderlyId().equals(elderlyId)) {
            throw new RuntimeException("该设备未绑定到此老人");
        }

        // 解绑设备
        DeviceInfo updateDevice = new DeviceInfo();
        updateDevice.setId(deviceId);
        updateDevice.setElderlyId(null);
        return deviceInfoMapper.updateById(updateDevice) > 0;
    }
}
