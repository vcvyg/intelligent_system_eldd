package org.example.persion.service;

import java.util.List;
import java.util.Map;

/**
 * 医护端 - 关联管理Service
 */
public interface MedicalRelationService {

    /**
     * 获取当前医护人员负责的老人列表
     */
    List<Map<String, Object>> getElderlyListByMedicalUser();

    /**
     * 医护人员主动绑定老人
     */
    boolean bindElderly(Long elderlyId, Integer isPrimaryDoctor);

    /**
     * 医护人员主动解绑老人
     */
    boolean unbindElderly(Long elderlyId);

    /**
     * 搜索可绑定的老人(根据姓名或身份证号)
     */
    List<Map<String, Object>> searchElderlyForBinding(String keyword);
}
