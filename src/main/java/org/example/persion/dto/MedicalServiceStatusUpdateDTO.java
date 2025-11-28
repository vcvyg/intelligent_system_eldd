package org.example.persion.dto;

import lombok.Data;
import org.example.persion.enums.ServiceProgressStatus;

@Data
public class MedicalServiceStatusUpdateDTO {
    private ServiceProgressStatus status;
    private String remark;
}

