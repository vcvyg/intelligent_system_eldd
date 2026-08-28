package org.example.persion.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 助手可查询的老人简要信息，仅返回工作台需要的最小字段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalAiPatientVO {
    private Long id;
    private String name;
    private String roomNumber;
}
