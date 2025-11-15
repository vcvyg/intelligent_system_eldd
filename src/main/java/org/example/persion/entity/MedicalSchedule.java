package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 医护人员排班实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("medical_schedule")
public class MedicalSchedule extends BaseEntity {

    /**
     * 医护人员ID
     */
    private Long medicalUserId;

    /**
     * 排班日期
     */
    private LocalDate scheduleDate;

    /**
     * 班次类型: 早班/中班/晚班/夜班
     */
    private String shiftType;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 状态: 正常/请假/调班
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
