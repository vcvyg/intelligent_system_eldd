package org.example.persion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.persion.ai.agent.MedicalAiExecutionResult;
import org.example.persion.ai.agent.MedicalAiExecutor;
import org.example.persion.ai.agent.MedicalAiPlan;
import org.example.persion.ai.agent.MedicalAiPlanner;
import org.example.persion.ai.agent.MedicalAiToolExecution;
import org.example.persion.ai.tool.MedicalAiToolContext;
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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 医护 AI 助手：负责权限、会话、Planner 调度与结果编排。
 *
 * <p>Planner 决定只读业务 Tool，Executor 负责容错执行；核心 Service 不直接访问健康、告警、
 * 服务或档案业务表。不依赖外部模型也能完整运行；不做诊断、处方和用药调整。</p>
 */
@Service
@RequiredArgsConstructor
public class MedicalAiAssistantServiceImpl implements MedicalAiAssistantService {

    private static final Duration SESSION_TTL = Duration.ofHours(2);

    private final ElderlyInfoMapper elderlyInfoMapper;
    private final MedicalAiPlanner medicalAiPlanner;
    private final MedicalAiExecutor medicalAiExecutor;

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
        result.setTraceId(UUID.randomUUID().toString());
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
                    "medical_safety_guard", "blocked", "拦截诊断/处方/用药调整类请求", 0L
            ));
            result.setPlanReason("医疗安全规则优先于 Planner，未执行任何业务 Tool");
            result.setSuggestions(suggestionsFor(List.of(), target));
            return result;
        }

        if (target == null) {
            result.setAnswer(buildNeedPatientAnswer(assigned));
            result.getTools().add(new MedicalAiAnswerVO.ToolTrace(
                    "patient_scope", "needs_context", "未解析到当前医护负责的老人", 0L
            ));
            result.setPlanReason("缺少可授权的老人上下文，Planner 暂不执行业务 Tool");
            result.setSources(List.of("当前医护负责老人列表"));
            result.setSuggestions(assigned.stream()
                    .limit(3)
                    .map(item -> "查看" + item.getName() + "的近期情况")
                    .toList());
            return result;
        }

        assertAssigned(target.getId(), assigned);
        result.getTools().add(new MedicalAiAnswerVO.ToolTrace(
                "patient_access", "ok", "已校验当前医护与" + safeName(target) + "的负责关系", 0L
        ));

        MedicalAiPlan plan = medicalAiPlanner.plan(question);
        result.setPlan(plan.toolNames());
        result.setPlanReason(plan.reason());

        if (plan.toolNames().isEmpty()) {
            result.setAnswer(buildCapabilityAnswer(target));
            result.setSuggestions(suggestionsFor(plan.toolNames(), target));
            return result;
        }

        MedicalAiExecutionResult execution = medicalAiExecutor.execute(
                plan,
                new MedicalAiToolContext(target.getId(), safeName(target), question)
        );
        result.setAnswer(execution.answer());
        result.setSources(execution.sources());
        for (MedicalAiToolExecution tool : execution.executions()) {
            result.getTools().add(new MedicalAiAnswerVO.ToolTrace(
                    tool.toolName(), tool.status(), tool.summary(), tool.elapsedMs()
            ));
        }
        if (execution.partial()) {
            result.setPlanReason(plan.reason() + "；部分 Tool 执行失败，已降级返回成功查询到的事实");
        }
        result.setSuggestions(suggestionsFor(plan.toolNames(), target));
        return result;
    }

    @Override
    public void resetSession(Long medicalUserId, String sessionId) {
        requireMedicalUser(medicalUserId);
        if (sessionId != null && !sessionId.isBlank()) {
            sessions.remove(sessionKey(medicalUserId, sessionId.trim()));
        }
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

    private List<String> suggestionsFor(List<String> plannedTools, ElderlyInfo target) {
        if (target == null) return List.of();
        String name = safeName(target);
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();

        if (!plannedTools.contains("health_recent")) suggestions.add(name + "最近7天健康指标怎么样？");
        if (!plannedTools.contains("alerts_recent")) suggestions.add(name + "最近有未处理告警吗？");
        if (!plannedTools.contains("care_schedule")) suggestions.add(name + "近期有什么照护安排？");
        if (!plannedTools.contains("recommendation_preview")) suggestions.add("现在适合给" + name + "推荐什么关怀内容？");
        if (!plannedTools.contains("room_lookup")) suggestions.add(name + "住哪个房间？");
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

    private static final class SessionContext {
        private Long currentElderlyId;
        private String currentElderlyName;
        private LocalDateTime lastAccess = LocalDateTime.now();

        private void touch() {
            lastAccess = LocalDateTime.now();
        }
    }
}
