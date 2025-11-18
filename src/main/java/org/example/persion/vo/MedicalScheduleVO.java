package org.example.persion.vo;

import com.fasterxml.jackson.annotation.JsonGetter;
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

    /**
     * 获取班次类型，如果未设置则根据开始时间自动判断
     * 早班: 06:00-12:00
     * 中班: 12:00-18:00
     * 晚班: 18:00-22:00
     * 夜班: 22:00-06:00
     */
    @JsonGetter("shiftType")
    public String getShiftType() {
        if (shiftType != null && !shiftType.trim().isEmpty()) {
            return shiftType;
        }

        if (startTime == null) {
            return "未知";
        }

        int hour = startTime.getHour();

        if (hour >= 6 && hour < 12) {
            return "早班";
        } else if (hour >= 12 && hour < 18) {
            return "中班";
        } else if (hour >= 18 && hour < 22) {
            return "晚班";
        } else {
            return "夜班";
        }
    }

    /**
     * 设置班次类型
     */
    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }
}
