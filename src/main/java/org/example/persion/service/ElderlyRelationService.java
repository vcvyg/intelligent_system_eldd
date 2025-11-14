package org.example.persion.service;

import org.example.persion.entity.ElderlyFamilyRelation;

import java.util.List;
import java.util.Map;

/**
 * 老人-子女关系Service
 */
public interface ElderlyRelationService {

    /**
     * 获取老人的子女关联列表
     */
    List<Map<String, Object>> getFamilyRelations(Long elderlyId);

    /**
     * 添加子女关联
     */
    boolean addFamilyRelation(Long elderlyId, Long familyUserId, String relationType, Integer isPrimaryContact);

    /**
     * 删除子女关联
     */
    boolean removeFamilyRelation(Long relationId);

    /**
     * 获取老人的医护分配列表
     */
    List<Map<String, Object>> getMedicalRelations(Long elderlyId);

    /**
     * 添加医护分配
     */
    boolean addMedicalRelation(Long elderlyId, Long medicalUserId, Integer isPrimaryDoctor);

    /**
     * 删除医护分配
     */
    boolean removeMedicalRelation(Long relationId);

    /**
     * 获取老人绑定的设备列表
     */
    List<Map<String, Object>> getDeviceRelations(Long elderlyId);
}
