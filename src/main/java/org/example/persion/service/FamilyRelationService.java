package org.example.persion.service;

import java.util.List;
import java.util.Map;

/**
 * 子女端 - 关联管理Service
 */
public interface FamilyRelationService {

    /**
     * 获取当前子女用户关联的老人列表
     */
    List<Map<String, Object>> getElderlyListByFamilyUser();

    /**
     * 子女主动绑定老人
     */
    boolean bindElderly(Long elderlyId, String relationType, Integer isPrimaryContact);

    /**
     * 子女主动解绑老人
     */
    boolean unbindElderly(Long elderlyId);

    /**
     * 搜索可绑定的老人(根据姓名或身份证号)
     */
    List<Map<String, Object>> searchElderlyForBinding(String keyword);
}
