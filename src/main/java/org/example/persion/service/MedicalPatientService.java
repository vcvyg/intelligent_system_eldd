package org.example.persion.service;

import org.example.persion.vo.ElderlyInfoVO;
import org.example.persion.vo.PatientHealthDetailsVO;

import java.util.List;

/**
 * 医护端-患者管理服务接口
 */
public interface MedicalPatientService {

    /**
     * 获取所有老人列表
     * @return 所有老人信息VO列表
     */
    List<ElderlyInfoVO> getAllPatients();

    /**
     * 获取当前医护人员负责的老人列表
     * @param medicalStaffId 当前登录的医护人员ID
     * @return 老人信息VO列表
     */
    List<ElderlyInfoVO> getMyPatients(Long medicalStaffId);

    /**
     * 获取单个老人的详细健康信息
     * @param elderlyId 老人ID
     * @return 患者健康详情VO
     */
    PatientHealthDetailsVO getPatientHealthDetails(Long elderlyId);
}
