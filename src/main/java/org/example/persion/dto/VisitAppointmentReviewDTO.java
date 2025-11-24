package org.example.persion.dto;

import lombok.Data;
import org.example.persion.enums.VisitAppointmentStatus;

/**
 * 管理端审批探访预约请求
 */
@Data
public class VisitAppointmentReviewDTO {

    /**
     * 审批后的状态：APPROVED / REJECTED
     */
    private VisitAppointmentStatus status;

    /**
     * 审批备注
     */
    private String remark;
}

