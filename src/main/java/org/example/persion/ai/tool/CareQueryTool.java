package org.example.persion.ai.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.persion.entity.FamilyServiceRecord;
import org.example.persion.entity.HealthData;
import org.example.persion.enums.ServiceProgressStatus;
import org.example.persion.repository.FamilyServiceRecordMapper;
import org.example.persion.repository.HealthDataMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 近期照护安排查询 Tool。
 *
 * <p>当前系统没有独立护理计划实体，因此按现有业务事实组合近 3 天健康巡查记录与待执行服务安排。</p>
 */
@Component
@RequiredArgsConstructor
public class CareQueryTool implements MedicalAiTool {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final HealthDataMapper healthDataMapper;
    private final FamilyServiceRecordMapper familyServiceRecordMapper;

    @Override
    public String name() {
        return "care_schedule";
    }

    @Override
    public boolean supports(String question) {
        if (question == null) return false;
        String q = question.toLowerCase();
        return containsAny(q,
                "护理计划", "照护计划", "护理安排", "照护安排", "近期安排",
                "服务安排", "巡查", "巡诊", "care", "plan");
    }

    @Override
    public MedicalAiToolResult execute(MedicalAiToolContext context) {
        LocalDateTime now = LocalDateTime.now();
        List<HealthData> recentRoundLike = healthDataMapper.findByDateTimeRange(
                now.minusDays(3), now, context.elderlyId()
        );
        recentRoundLike = recentRoundLike == null ? List.of() : recentRoundLike;

        List<FamilyServiceRecord> services = familyServiceRecordMapper.selectList(
                new LambdaQueryWrapper<FamilyServiceRecord>()
                        .eq(FamilyServiceRecord::getElderlyId, context.elderlyId())
                        .ge(FamilyServiceRecord::getServiceDate, LocalDate.now().minusDays(1))
                        .in(FamilyServiceRecord::getStatus, ServiceProgressStatus.PENDING, ServiceProgressStatus.PROCESSING)
                        .orderByAsc(FamilyServiceRecord::getServiceDate)
                        .orderByAsc(FamilyServiceRecord::getServiceTime)
        );
        services = services == null ? List.of() : services;

        StringBuilder body = new StringBuilder(
                "项目当前没有独立的“护理计划”表，因此这里按“近期健康巡查记录 + 待执行服务安排”汇总。 "
        );

        if (recentRoundLike.isEmpty()) {
            body.append("近3天没有健康巡查/测量记录。 ");
        } else {
            HealthData latest = recentRoundLike.stream()
                    .filter(item -> item.getMeasureTime() != null)
                    .max(Comparator.comparing(HealthData::getMeasureTime))
                    .orElse(recentRoundLike.get(recentRoundLike.size() - 1));
            body.append("近3天有 ").append(recentRoundLike.size()).append(" 条健康巡查/测量记录");
            if (latest.getMeasureTime() != null) {
                body.append("，最近一次为 ").append(TIME_FORMAT.format(latest.getMeasureTime()));
            }
            body.append("。 ");
        }

        if (services.isEmpty()) {
            body.append("目前没有查到待执行或执行中的生活服务安排。");
        } else {
            String serviceText = services.stream().limit(5).map(item -> {
                String when = item.getServiceDate() == null ? "日期待定" : item.getServiceDate().toString();
                if (item.getServiceTime() != null) when += " " + item.getServiceTime();
                return when + " " + emptyAs(item.getServiceType(), "服务") + "（" + item.getStatus() + "）"
                        + (item.getDescription() == null ? "" : "：" + item.getDescription());
            }).collect(Collectors.joining("；"));
            body.append("待执行/执行中安排：").append(serviceText).append("。");
        }

        return new MedicalAiToolResult(
                "近期照护安排",
                body.toString(),
                "ok",
                "组合近期健康巡查与待执行服务安排",
                List.of(
                        "health_data / 近3天健康巡查记录",
                        "family_service_record / 待执行服务安排"
                )
        );
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
