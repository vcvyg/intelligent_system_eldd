package org.example.persion.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 患者健康详情VO，包含老人基本信息和其健康数据列表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientHealthDetailsVO {

    /**
     * 老人基本信息
     */
    private ElderlyInfoVO elderlyInfo;

    /**
     * 最近的健康数据记录
     */
    private List<HealthDataVO> healthDataRecords;
}
