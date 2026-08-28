package org.example.persion.ai.tool;

import lombok.RequiredArgsConstructor;
import org.example.persion.repository.AlertRecordMapper;
import org.example.persion.vo.AlertRecordVO;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 最近告警查询 Tool。
 */
@Component
@RequiredArgsConstructor
public class AlertQueryTool implements MedicalAiTool {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final AlertRecordMapper alertRecordMapper;

    @Override
    public String name() {
        return "alerts_recent";
    }

    @Override
    public boolean supports(String question) {
        if (question == null) return false;
        String q = question.toLowerCase();
        return containsAny(q, "告警", "预警", "报警", "异常提醒", "alarm", "alert");
    }

    @Override
    public MedicalAiToolResult execute(MedicalAiToolContext context) {
        List<AlertRecordVO> alerts = alertRecordMapper.selectByElderlyId(context.elderlyId());
        alerts = alerts == null ? List.of() : alerts;
        List<AlertRecordVO> recent = alerts.stream().limit(5).toList();
        long openCount = alerts.stream().filter(this::isOpenAlert).count();

        if (recent.isEmpty()) {
            return new MedicalAiToolResult(
                    "告警",
                    "当前没有查到告警记录。",
                    "empty",
                    "无告警记录",
                    List.of("alert_record / 告警记录")
            );
        }

        String detail = recent.stream().map(item -> {
            String time = item.getAlertTime() == null ? "时间未知" : TIME_FORMAT.format(item.getAlertTime());
            String status = emptyAs(item.getStatus(), "状态未知");
            return time + " " + emptyAs(item.getAlertType(), "告警") + "（" + status + "）"
                    + (item.getAlertContent() == null ? "" : "：" + item.getAlertContent());
        }).collect(Collectors.joining("；"));

        return new MedicalAiToolResult(
                "告警",
                "共查到 " + alerts.size() + " 条，当前未闭环/待处理约 " + openCount + " 条。最近记录：" + detail + "。",
                "ok",
                "读取告警 " + alerts.size() + " 条，未闭环约 " + openCount + " 条",
                List.of("alert_record / 告警记录")
        );
    }

    private boolean isOpenAlert(AlertRecordVO alert) {
        String status = alert.getStatus();
        if (status == null) return true;
        return !(status.contains("已处理")
                || status.contains("已关闭")
                || status.contains("已忽略")
                || status.equalsIgnoreCase("CLOSED"));
    }

    private String emptyAs(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}
