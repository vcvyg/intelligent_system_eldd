package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.persion.enums.ServiceProgressStatus;

import java.time.LocalDateTime;

/**
 * 服务状态变更历史
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("family_service_status_history")
public class FamilyServiceStatusHistory extends BaseEntity {

    private Long serviceRecordId;

    private ServiceProgressStatus oldStatus;

    private ServiceProgressStatus newStatus;

    private Long changedBy;

    private String changedByName;

    private String remark;

    private LocalDateTime changeTime;
}

