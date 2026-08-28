package org.example.persion.ai.tool;

import lombok.RequiredArgsConstructor;
import org.example.persion.entity.HealthData;
import org.example.persion.repository.HealthDataMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * 近 7 天健康数据查询 Tool。
 */
@Component
@RequiredArgsConstructor
public class HealthQueryTool implements MedicalAiTool {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final HealthDataMapper healthDataMapper;

    @Override
    public String name() {
        return "health_recent";
    }

    @Override
    public boolean supports(String question) {
        if (question == null) {
            return false;
        }
        String q = question.toLowerCase();
        return containsAny(q, "健康", "心率", "血压", "血糖", "体温", "睡眠", "步数", "指标", "身体", "health");
    }

    @Override
    public MedicalAiToolResult execute(MedicalAiToolContext context) {
        LocalDateTime now = LocalDateTime.now();
        List<HealthData> records = healthDataMapper.findByDateTimeRange(
                now.minusDays(7), now, context.elderlyId()
        );
        records = records == null ? List.of() : records;

        if (records.isEmpty()) {
            return new MedicalAiToolResult(
                    "近7天健康记录",
                    "系统没有查到可用的健康测量记录。",
                    "empty",
                    "近7天无记录",
                    List.of("health_data / 近7天健康记录")
            );
        }

        HealthData latest = records.stream()
                .filter(item -> item.getMeasureTime() != null)
                .max(Comparator.comparing(HealthData::getMeasureTime))
                .orElse(records.get(records.size() - 1));

        List<String> latestFacts = new ArrayList<>();
        if (latest.getHeartRate() != null) latestFacts.add("心率 " + number(latest.getHeartRate()) + " bpm");
        if (latest.getBloodPressureHigh() != null && latest.getBloodPressureLow() != null) {
            latestFacts.add("血压 " + number(latest.getBloodPressureHigh()) + "/" + number(latest.getBloodPressureLow()) + " mmHg");
        }
        if (latest.getTemperature() != null) latestFacts.add("体温 " + number(latest.getTemperature()) + "℃");
        if (latest.getBloodSugar() != null) latestFacts.add("血糖 " + number(latest.getBloodSugar()));
        if (latest.getSleepDuration() != null) latestFacts.add("睡眠 " + latest.getSleepDuration() + " 分钟");
        if (latest.getSteps() != null) latestFacts.add("步数 " + latest.getSteps());

        StringBuilder body = new StringBuilder();
        body.append("共查到 ").append(records.size()).append(" 条记录。最新一条");
        if (latest.getMeasureTime() != null) {
            body.append("（").append(TIME_FORMAT.format(latest.getMeasureTime())).append("）");
        }
        body.append("：")
                .append(latestFacts.isEmpty() ? "有记录但主要指标为空" : String.join("，", latestFacts))
                .append("。");

        average(records, HealthData::getHeartRate)
                .ifPresent(avg -> body.append(" 近7天已记录心率均值约 ").append(avg).append(" bpm。"));
        body.append(" 以上仅是系统记录汇总，不据此自动下诊断结论。");

        return new MedicalAiToolResult(
                "近7天健康记录",
                body.toString(),
                "ok",
                "读取近7天 " + records.size() + " 条健康测量",
                List.of("health_data / 近7天健康记录")
        );
    }

    private String number(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private Optional<String> average(List<HealthData> records,
                                     Function<HealthData, BigDecimal> extractor) {
        List<BigDecimal> values = records.stream().map(extractor).filter(Objects::nonNull).toList();
        if (values.isEmpty()) return Optional.empty();
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return Optional.of(sum.divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString());
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}
