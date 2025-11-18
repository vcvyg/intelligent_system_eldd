package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.persion.enums.LeaveRequestStatus;
import org.example.persion.enums.LeaveType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("leave_request")
public class LeaveRequest extends BaseEntity {

    private Long medicalUserId; // 申请人ID

    @TableField("reviewer_id")
    private Long approverId; // 审批人ID (数据库列名: reviewer_id)

    private LeaveType leaveType; // 请假类型

    private LocalDate startDate; // 开始日期

    private LocalDate endDate; // 结束日期

    private Integer days; // 天数

    private String reason; // 申请原因

    private LeaveRequestStatus status; // 状态

    @TableField("review_time")
    private LocalDateTime approveTime; // 审批时间 (数据库列名: review_time)

    @TableField("review_remark")
    private String approverRemark; // 审批备注 (数据库列名: review_remark)
}
