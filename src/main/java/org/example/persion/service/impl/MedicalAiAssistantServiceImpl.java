package org.example.persion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.persion.ai.tool.MedicalAiTool;
import org.example.persion.ai.tool.MedicalAiToolContext;
import org.example.persion.ai.tool.MedicalAiToolRegistry;
import org.example.persion.ai.tool.MedicalAiToolResult;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.dto.MedicalAiChatRequest;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.service.MedicalAiAssistantService;
import org.example.persion.vo.ElderlyInfoVO;
import org.example.persion.vo.MedicalAiAnswerVO;
import org.example.persion.vo.MedicalAiPatientVO;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
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
 * 医护 AI 助手：负责权限、会话、问题路由与 Tool 编排。
 *
 * <p>所有业务事实查询都由注册 Tool 负责，核心 Service 不直接访问健康、告警、服务或档案业务表。
 * 不依赖外部模型也能完整运行；不做诊断、处方和用药调整。</p>
 */
@Service
@RequiredArgsConstructor
public class MedicalAiAssistantServiceImpl implements MedicalAiAssistantService {

    private static final Duration SESSION_TTL = Duration.ofHours(2);

    private final ElderlyInfoMapper elderlyInfoMapper;
    private final MedicalAiToolRegistry medicalAiToolRegistry;

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
            appendRegisteredTool("room_lookup", target, question, answer, result, sources);
        }
        if (intents.contains(Intent.PROFILE)) {
            appendRegisteredTool("patient_profile", target, question, answer, result, sources);
        }
        if (intents.contains(Intent.HEALTH)) {
            appendRegisteredTool("health_recent", target, question, answer, result, sources);
        }
        if (intents.contains(Intent.ALERT)) {
            appendRegisteredTool("alerts_recent", target, question, answer, result, sources);
        }
        if (intents.contains(Intent.CARE)) {
            appendRegisteredTool("care_schedule", target, question, answer, result, sources);
        }
        if (intents.contains(Intent.RECOMMENDATION)) {
            appendRegisteredTool("recommendation_preview", target, question, answer, result, sources);
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

    private void appendRegisteredTool(String toolName,
                                      ElderlyInfo target,
                                      String question,
                                      StringBuilder answer,
                                      MedicalAiAnswerVO result,
                                      Set<String> sources) {
        MedicalAiTool tool = medicalAiToolRegistry.require(toolName);
        MedicalAiToolResult toolResult = tool.execute(new MedicalAiToolContext(
                target.getId(), safeName(target), question
        ));

        section(answer, toolResult.sectionTitle(), toolResult.body());
        result.getTools().add(new MedicalAiAnswerVO.ToolTrace(
                tool.name(), toolResult.status(), toolResult.summary()
        ));
        sources.addAll(toolResult.sources());
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
        if (containsAny(q, "推荐", "主动关怀", "适合推", "推什么", "关怀内容", "recommend")) intents.add(Intent.RECOMMENDATION);

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
        if (!intents.contains(Intent.RECOMMENDATION)) suggestions.add("现在适合给" + name + "推荐什么关怀内容？");
        if (!intents.contains(Intent.ROOM)) suggestions.add(name + "住哪个房间？");
        suggestions.add("把她最近的健康、告警和安排一起汇总一下");
        return suggestions.stream().limit(4).toList();
    }

    private String buildCapabilityAnswer(ElderlyInfo target) {
        return "我已定位到" + safeName(target) + "。你可以继续问房间、老人档案、近7天健康指标、最近告警、近期健康巡查和待执行服务安排，也可以查询当前适合的主动关怀推荐；例如“她住哪，最近心率和告警怎么样？”或“现在适合给她推荐什么？”。";
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
        return prefix + "我可以查询并整理系统中的健康指标、告警、巡查记录、服务安排和主动关怀推荐，但不能替代专业诊断，也不会给出处方、停药/换药或剂量调整建议。你可以让我先把相关系统记录调出来，供医护人员判断。";
    }

    private boolean asksForMedicalDecision(String question) {
        return containsAny(question,
                "怎么用药", "吃什么药", "开什么药", "停药", "换药", "加药", "减药", "调整剂量", "药量",
                "给个诊断", "帮我诊断", "诊断一下", "是不是得了", "怎么治疗", "治疗方案", "开处方");
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

    private enum Intent {
        ROOM, PROFILE, HEALTH, ALERT, CARE, RECOMMENDATION
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
