package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.dto.MedicalAiChatRequest;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.entity.FamilyServiceRecord;
import org.example.persion.entity.HealthData;
import org.example.persion.enums.ServiceProgressStatus;
import org.example.persion.repository.AlertRecordMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.FamilyServiceRecordMapper;
import org.example.persion.repository.HealthDataMapper;
import org.example.persion.service.MedicalAiAssistantService;
import org.example.persion.vo.AlertRecordVO;
import org.example.persion.vo.ElderlyInfoVO;
import org.example.persion.vo.MedicalAiAnswerVO;
import org.example.persion.vo.MedicalAiPatientVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 医护 AI 助手：先规划需要查询的系统工具，再基于工具事实生成回答。
 *
 * <p>第一版故意保持“事实优先”：不依赖外部模型也能完整运行；不做诊断、处方和用药调整。
 * 会话只保存当前老人上下文，不保存完整医疗回答，降低无必要的敏感信息驻留。</p>
 */
@Service
@RequiredArgsConstructor
public class MedicalAiAssistantServiceImpl implements MedicalAiAssistantService {

    private static final Duration SESSION_TTL = Duration.ofHours(2);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final ElderlyInfoMapper elderlyInfoMapper;
    private final HealthDataMapper healthDataMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final FamilyServiceRecordMapper familyServiceRecordMapper;

    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();

    @Override
    public List<MedicalAiPatientVO> listAssignedPatients(Long medicalUserId) {
        requireMedicalUser(medicalUserId);
        return assignedPatients(medicalUserId).stream()
                .sorted(Comparator.comparing(ElderlyInfo::getName, Comparator.nullsLast(String::compareTo)))
                .map(elderly -> {
                    ElderlyInfoVO detail = elderlyInfoMapper.selectElderlyWithRoom(elderly.getId());
                    return new MedicalAiPatientVO(
                            elderly.getId(),
                            elderly.getName(),
                            detail == null ? null : detail.getRoomNumber()
                    );
                })
                .toList();
    }

    @Override
    public MedicalAiAnswerVO chat(Long medicalUserId, MedicalAiChatRequest request) {
        requireMedicalUser(medicalUserId);
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BusinessException(400, "问题不能为空");
        }

        cleanupExpiredSessions();
        String sessionId = normalizeSessionId(request.getSessionId());
        String sessionKey = sessionKey(medicalUserId, sessionId);
        SessionContext context = sessions.computeIfAbsent(sessionKey, ignored -> new SessionContext());
        context.touch();

        String question = request.getMessage().trim();
        List<ElderlyInfo> assigned = assignedPatients(medicalUserId);
        ElderlyInfo target = resolveTargetElderly(request.getElderlyId(), question, assigned, context);

        MedicalAiAnswerVO result = new MedicalAiAnswerVO();
        result.setSessionId(sessionId);
        result.setModelEnhanced(false);
        result.setSafetyNote("仅基于当前系统记录辅助查询，不替代医护判断；不提供诊断、处方或用药调整建议。");

        if (target != null) {
            context.currentElderlyId = target.getId();
            context.currentElderlyName = target.getName();
            result.setElderlyId(target.getId());
            result.setElderlyName(target.getName());
        }

        if (asksForMedicalDecision(question)) {
            result.setAnswer(buildSafetyRedirect(target));
            result.getTools().add(new MedicalAiAnswerVO.ToolTrace(
                    "medical_safety_guard", "blocked", "拦截诊断/处方/用药调整类请求"
            ));
            result.setSuggestions(suggestionsFor(EnumSet.noneOf(Intent.class), target));
            return result;
        }

        if (target == null) {
            result.setAnswer(buildNeedPatientAnswer(assigned));
            result.getTools().add(new MedicalAiAnswerVO.ToolTrace(
                    "patient_scope", "needs_context", "未解析到当前医护负责的老人"
            ));
            result.setSources(List.of("当前医护负责老人列表"));
            result.setSuggestions(assigned.stream()
                    .limit(3)
                    .map(item -> "查看" + item.getName() + "的近期情况")
                    .toList());
            return result;
        }

        // 每轮再次校验作用域，避免会话上下文绕过权限。
        assertAssigned(target.getId(), assigned);
        result.getTools().add(new MedicalAiAnswerVO.ToolTrace(
                "patient_access", "ok", "已校验当前医护与" + safeName(target) + "的负责关系"
        ));

        EnumSet<Intent> intents = route(question);
        if (intents.isEmpty()) {
            result.setAnswer(buildCapabilityAnswer(target));
            result.setSuggestions(suggestionsFor(intents, target));
            return result;
        }

        StringBuilder answer = new StringBuilder();
        Set<String> sources = new LinkedHashSet<>();

        if (intents.contains(Intent.ROOM)) {
            appendRoom(target, answer, result, sources);
        }
        if (intents.contains(Intent.PROFILE)) {
            appendProfile(target, question, answer, result, sources);
        }
        if (intents.contains(Intent.HEALTH)) {
            appendHealth(target, answer, result, sources);
        }
        if (intents.contains(Intent.ALERT)) {
            appendAlerts(target, answer, result, sources);
        }
        if (intents.contains(Intent.CARE)) {
            appendCarePlan(target, answer, result, sources);
        }

        result.setAnswer(answer.toString().trim());
        result.setSources(new ArrayList<>(sources));
        result.setSuggestions(suggestionsFor(intents, target));
        return result;
    }

    @Override
    public void resetSession(Long medicalUserId, String sessionId) {
        requireMedicalUser(medicalUserId);
        if (sessionId != null && !sessionId.isBlank()) {
            sessions.remove(sessionKey(medicalUserId, sessionId.trim()));
        }
    }

    private void appendRoom(ElderlyInfo target,
                            StringBuilder answer,
                            MedicalAiAnswerVO result,
                            Set<String> sources) {
        ElderlyInfoVO detail = elderlyInfoMapper.selectElderlyWithRoom(target.getId());
        String room = detail == null || detail.getRoomNumber() == null || detail.getRoomNumber().isBlank()
                ? "系统暂未登记房间"
                : detail.getRoomNumber() + (detail.getRoomType() == null ? "" : "（" + detail.getRoomType() + "）");
        section(answer, "房间信息", safeName(target) + "目前为：" + room + "。");
        result.getTools().add(new MedicalAiAnswerVO.ToolTrace("room_lookup", "ok", room));
        sources.add("老人档案 / 房间信息");
    }

    private void appendProfile(ElderlyInfo target,
                               String question,
                               StringBuilder answer,
                               MedicalAiAnswerVO result,
                               Set<String> sources) {
        List<String> facts = new ArrayList<>();
        if (target.getAge() != null) facts.add(target.getAge() + "岁");
        if (target.getGender() != null && !target.getGender().isBlank()) facts.add(target.getGender());

        if (containsAny(question, "病史", "既往史", "基础病", "病情")) {
            facts.add("系统病史：" + emptyAs(target.getMedicalHistory(), "暂无登记"));
        }
        if (containsAny(question, "过敏", "过敏史")) {
            facts.add("过敏史：" + emptyAs(target.getAllergyHistory(), "暂无登记"));
        }
        if (facts.isEmpty()) {
            facts.add("已定位到当前负责老人档案");
        }

        section(answer, "档案信息", String.join("；", facts) + "。");
        result.getTools().add(new MedicalAiAnswerVO.ToolTrace(
                "patient_profile", "ok", "读取必要的老人档案字段"
        ));
        sources.add("老人档案");
    }

    private void appendHealth(ElderlyInfo target,
                              StringBuilder answer,
                              MedicalAiAnswerVO result,
                              Set<String> sources) {
        LocalDateTime now = LocalDateTime.now();
        List<HealthData> records = healthDataMapper.findByDateTimeRange(now.minusDays(7), now, target.getId());
        records = records == null ? List.of() : records;

        if (records.isEmpty()) {
            section(answer, "近7天健康记录", "系统没有查到可用的健康测量记录。");
            result.getTools().add(new MedicalAiAnswerVO.ToolTrace("health_recent", "empty", "近7天无记录"));
            sources.add("health_data / 近7天健康记录");
            return;
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

        StringBuilder healthText = new StringBuilder();
        healthText.append("共查到 ").append(records.size()).append(" 条记录。最新一条");
        if (latest.getMeasureTime() != null) {
            healthText.append("（").append(TIME_FORMAT.format(latest.getMeasureTime())).append("）");
        }
        healthText.append("：").append(latestFacts.isEmpty() ? "有记录但主要指标为空" : String.join("，", latestFacts)).append("。");

        average(records, HealthData::getHeartRate).ifPresent(avg -> healthText.append(" 近7天已记录心率均值约 ").append(avg).append(" bpm。"));
        healthText.append(" 以上仅是系统记录汇总，不据此自动下诊断结论。");

        section(answer, "近7天健康记录", healthText.toString());
        result.getTools().add(new MedicalAiAnswerVO.ToolTrace(
                "health_recent", "ok", "读取近7天 " + records.size() + " 条健康测量"
        ));
        sources.add("health_data / 近7天健康记录");
    }

    private void appendAlerts(ElderlyInfo target,
                              StringBuilder answer,
                              MedicalAiAnswerVO result,
                              Set<String> sources) {
        List<AlertRecordVO> alerts = alertRecordMapper.selectByElderlyId(target.getId());
        alerts = alerts == null ? List.of() : alerts;
        List<AlertRecordVO> recent = alerts.stream().limit(5).toList();
        long openCount = alerts.stream().filter(this::isOpenAlert).count();

        if (recent.isEmpty()) {
            section(answer, "告警", "当前没有查到告警记录。");
            result.getTools().add(new MedicalAiAnswerVO.ToolTrace("alerts_recent", "empty", "无告警记录"));
        } else {
            String detail = recent.stream().map(item -> {
                String time = item.getAlertTime() == null ? "时间未知" : TIME_FORMAT.format(item.getAlertTime());
                String status = emptyAs(item.getStatus(), "状态未知");
                return time + " " + emptyAs(item.getAlertType(), "告警") + "（" + status + "）"
                        + (item.getAlertContent() == null ? "" : "：" + item.getAlertContent());
            }).collect(Collectors.joining("；"));
            section(answer, "告警", "共查到 " + alerts.size() + " 条，当前未闭环/待处理约 " + openCount + " 条。最近记录：" + detail + "。");
            result.getTools().add(new MedicalAiAnswerVO.ToolTrace(
                    "alerts_recent", "ok", "读取告警 " + alerts.size() + " 条，未闭环约 " + openCount + " 条"
            ));
        }
        sources.add("alert_record / 告警记录");
    }

    private void appendCarePlan(ElderlyInfo target,
                                StringBuilder answer,
                                MedicalAiAnswerVO result,
                                Set<String> sources) {
        LocalDateTime now = LocalDateTime.now();
        List<HealthData> recentRoundLike = healthDataMapper.findByDateTimeRange(now.minusDays(3), now, target.getId());
        recentRoundLike = recentRoundLike == null ? List.of() : recentRoundLike;

        List<FamilyServiceRecord> services = familyServiceRecordMapper.selectList(
                new LambdaQueryWrapper<FamilyServiceRecord>()
                        .eq(FamilyServiceRecord::getElderlyId, target.getId())
                        .ge(FamilyServiceRecord::getServiceDate, LocalDate.now().minusDays(1))
                        .in(FamilyServiceRecord::getStatus, ServiceProgressStatus.PENDING, ServiceProgressStatus.PROCESSING)
                        .orderByAsc(FamilyServiceRecord::getServiceDate)
                        .orderByAsc(FamilyServiceRecord::getServiceTime)
        );
        services = services == null ? List.of() : services;

        StringBuilder text = new StringBuilder("项目当前没有独立的“护理计划”表，因此这里按“近期健康巡查记录 + 待执行服务安排”汇总。 ");
        if (recentRoundLike.isEmpty()) {
            text.append("近3天没有健康巡查/测量记录。 ");
        } else {
            HealthData latest = recentRoundLike.stream()
                    .filter(item -> item.getMeasureTime() != null)
                    .max(Comparator.comparing(HealthData::getMeasureTime))
                    .orElse(recentRoundLike.get(recentRoundLike.size() - 1));
            text.append("近3天有 ").append(recentRoundLike.size()).append(" 条健康巡查/测量记录");
            if (latest.getMeasureTime() != null) text.append("，最近一次为 ").append(TIME_FORMAT.format(latest.getMeasureTime()));
            text.append("。 ");
        }

        if (services.isEmpty()) {
            text.append("目前没有查到待执行或执行中的生活服务安排。");
        } else {
            String serviceText = services.stream().limit(5).map(item -> {
                String when = item.getServiceDate() == null ? "日期待定" : item.getServiceDate().toString();
                if (item.getServiceTime() != null) when += " " + item.getServiceTime();
                return when + " " + emptyAs(item.getServiceType(), "服务") + "（" + item.getStatus() + "）"
                        + (item.getDescription() == null ? "" : "：" + item.getDescription());
            }).collect(Collectors.joining("；"));
            text.append("待执行/执行中安排：").append(serviceText).append("。");
        }

        section(answer, "近期照护安排", text.toString());
        result.getTools().add(new MedicalAiAnswerVO.ToolTrace(
                "care_schedule", "ok", "组合近期健康巡查与待执行服务安排"
        ));
        sources.add("health_data / 近3天健康巡查记录");
        sources.add("family_service_record / 待执行服务安排");
    }

    private ElderlyInfo resolveTargetElderly(Long requestedId,
                                             String question,
                                             List<ElderlyInfo> assigned,
                                             SessionContext context) {
        if (requestedId != null) {
            assertAssigned(requestedId, assigned);
            return assigned.stream().filter(item -> requestedId.equals(item.getId())).findFirst().orElseThrow();
        }

        for (ElderlyInfo item : assigned) {
            if (item.getName() != null && !item.getName().isBlank() && question.contains(item.getName())) {
                return item;
            }
        }

        if (context.currentElderlyId != null) {
            return assigned.stream()
                    .filter(item -> context.currentElderlyId.equals(item.getId()))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private EnumSet<Intent> route(String question) {
        String q = question.toLowerCase(Locale.ROOT);
        EnumSet<Intent> intents = EnumSet.noneOf(Intent.class);

        if (containsAny(q, "房间", "房号", "几号房", "住哪", "住在哪里", "room")) intents.add(Intent.ROOM);
        if (containsAny(q, "档案", "年龄", "性别", "病史", "既往史", "基础病", "过敏", "病情", "profile")) intents.add(Intent.PROFILE);
        if (containsAny(q, "健康", "心率", "血压", "血糖", "体温", "睡眠", "步数", "指标", "身体", "health")) intents.add(Intent.HEALTH);
        if (containsAny(q, "告警", "预警", "报警", "异常提醒", "alarm", "alert")) intents.add(Intent.ALERT);
        if (containsAny(q, "护理计划", "照护计划", "护理安排", "照护安排", "近期安排", "服务安排", "巡查", "巡诊", "care", "plan")) intents.add(Intent.CARE);

        if (containsAny(q, "最近怎么样", "近期情况", "整体情况", "概况", "综合看一下")) {
            intents.add(Intent.HEALTH);
            intents.add(Intent.ALERT);
            intents.add(Intent.CARE);
        }
        return intents;
    }

    private List<String> suggestionsFor(Set<Intent> intents, ElderlyInfo target) {
        if (target == null) return List.of();
        String name = safeName(target);
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();

        if (!intents.contains(Intent.HEALTH)) suggestions.add(name + "最近7天健康指标怎么样？");
        if (!intents.contains(Intent.ALERT)) suggestions.add(name + "最近有未处理告警吗？");
        if (!intents.contains(Intent.CARE)) suggestions.add(name + "近期有什么照护安排？");
        if (!intents.contains(Intent.ROOM)) suggestions.add(name + "住哪个房间？");
        suggestions.add("把她最近的健康、告警和安排一起汇总一下");
        return suggestions.stream().limit(4).toList();
    }

    private String buildCapabilityAnswer(ElderlyInfo target) {
        return "我已定位到" + safeName(target) + "。你可以继续问房间、老人档案、近7天健康指标、最近告警、近期健康巡查和待执行服务安排；也可以一次问多个，例如“她住哪，最近心率和告警怎么样？”。";
    }

    private String buildNeedPatientAnswer(List<ElderlyInfo> assigned) {
        if (assigned.isEmpty()) {
            return "当前账号没有分配到可查询的老人，因此 AI 助手不会绕过权限读取其他老人数据。";
        }
        String names = assigned.stream().map(this::safeName).limit(5).collect(Collectors.joining("、"));
        return "请先选择或在问题中提到一位你负责的老人。当前可查询：" + names + "。选择后可以继续用“她/他”追问。";
    }

    private String buildSafetyRedirect(ElderlyInfo target) {
        String prefix = target == null ? "" : "关于" + safeName(target) + "，";
        return prefix + "我可以查询并整理系统中的健康指标、告警、巡查记录和服务安排，但不能替代专业诊断，也不会给出处方、停药/换药或剂量调整建议。你可以让我先把相关系统记录调出来，供医护人员判断。";
    }

    private boolean asksForMedicalDecision(String question) {
        return containsAny(question,
                "怎么用药", "吃什么药", "开什么药", "停药", "换药", "加药", "减药", "调整剂量", "药量",
                "给个诊断", "帮我诊断", "诊断一下", "是不是得了", "怎么治疗", "治疗方案", "开处方");
    }

    private boolean isOpenAlert(AlertRecordVO alert) {
        String status = alert.getStatus();
        if (status == null) return true;
        return !(status.contains("已处理") || status.contains("已关闭") || status.contains("已忽略") || status.equalsIgnoreCase("CLOSED"));
    }

    private void assertAssigned(Long elderlyId, List<ElderlyInfo> assigned) {
        boolean allowed = assigned.stream().anyMatch(item -> elderlyId.equals(item.getId()));
        if (!allowed) {
            throw new BusinessException(403, "无权访问该老人信息：AI 助手只允许查询当前医护负责的老人");
        }
    }

    private List<ElderlyInfo> assignedPatients(Long medicalUserId) {
        List<ElderlyInfo> assigned = elderlyInfoMapper.selectElderlyListByMedicalUserId(medicalUserId);
        return assigned == null ? List.of() : assigned;
    }

    private void requireMedicalUser(Long medicalUserId) {
        if (medicalUserId == null) {
            throw new BusinessException(401, "未获取到当前登录用户");
        }
    }

    private String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return UUID.randomUUID().toString();
        String normalized = sessionId.trim();
        if (normalized.length() > 80) throw new BusinessException(400, "会话ID过长");
        return normalized;
    }

    private String sessionKey(Long userId, String sessionId) {
        return userId + ":" + sessionId;
    }

    private void cleanupExpiredSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minus(SESSION_TTL);
        sessions.entrySet().removeIf(entry -> entry.getValue().lastAccess.isBefore(cutoff));
    }

    private void section(StringBuilder answer, String title, String body) {
        if (!answer.isEmpty()) answer.append("\n\n");
        answer.append("【").append(title).append("】").append(body);
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) return false;
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private String safeName(ElderlyInfo elderly) {
        return elderly.getName() == null || elderly.getName().isBlank() ? "该老人" : elderly.getName();
    }

    private String emptyAs(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String number(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private java.util.Optional<String> average(List<HealthData> records,
                                               java.util.function.Function<HealthData, BigDecimal> extractor) {
        List<BigDecimal> values = records.stream().map(extractor).filter(java.util.Objects::nonNull).toList();
        if (values.isEmpty()) return java.util.Optional.empty();
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return java.util.Optional.of(sum.divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString());
    }

    private enum Intent {
        ROOM, PROFILE, HEALTH, ALERT, CARE
    }

    private static final class SessionContext {
        private Long currentElderlyId;
        private String currentElderlyName;
        private LocalDateTime lastAccess = LocalDateTime.now();

        private void touch() {
            lastAccess = LocalDateTime.now();
        }
    }
}
