package org.example.persion.vo;

import lombok.Data;
import org.example.persion.enums.ServiceProgressStatus;

import java.time.LocalDateTime;

@Data
public class ServiceStatusHistoryVO {
    private ServiceProgressStatus fromStatus;
    private ServiceProgressStatus toStatus;
    private String changedByName;
    private Long changedBy;
    private LocalDateTime changeTime;
    private String remark;
}

