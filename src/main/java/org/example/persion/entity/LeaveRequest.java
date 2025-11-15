package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 请假/调休申请实体
 */
@Data
@TableName("leave_request")
public class LeaveRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 申请人ID(医护人员)
     */
    private Long medicalUserId;

    /**
     * 请假类型: 事假/病假/年假/调休
     */
    private String leaveType;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 请假天数
     */
    private BigDecimal days;

    /**
     * 请假原因
     */
    private String reason;

    /**
     * 审批状态: 待审批/已同意/已拒绝
     */
    private String status;

    /**
     * 审批人ID
     */
    private Long reviewerId;

    /**
     * 审批时间
     */
    private LocalDateTime reviewTime;

    /**
     * 审批备注
     */
    private String reviewRemark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
