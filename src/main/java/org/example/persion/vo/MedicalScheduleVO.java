package org.example.persion.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class MedicalScheduleVO {

    private Long id;

    private Long medicalUserId;
    private String medicalUserName; // 医护人员姓名

    private LocalDate scheduleDate;
    private String shiftType;
    private LocalTime startTime;
    private LocalTime endTime;

    private String status;
    private String remark;

    private Long roomId;
    private String roomNumber; // 房间号
}
