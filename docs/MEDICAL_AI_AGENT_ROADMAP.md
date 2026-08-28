# 医护 AI Agent 演进路线

## 当前已落地架构

当前助手采用“事实优先、模型可选”的 Java Agent 架构：

```text
Medical User Request
        ↓
Patient Scope + Permission Check
        ↓
Medical Safety Guard
        ↓
MedicalAiPlanner
        ↓
MedicalAiPlan
        ↓
MedicalAiExecutor
        ↓
MedicalAiToolRegistry
        ↓
Read-only Business Tools
        ↓
Grounded Fact Answer + Sources + Tool Trace
        ↓
Optional LLM Rewriter + Grounding Gate
```

核心原则：

- Planner 只规划业务 Tool，不负责产生医疗事实；
- Executor 按计划执行 Tool，单个 Tool 失败时继续执行剩余步骤并返回部分结果；
- Tool Trace 记录状态和单 Tool 耗时；
- `traceId` 贯穿一轮请求，日志只记录 plan、tool status、耗时等非医疗正文信息；
- 外部模型默认关闭，只允许重写已查询事实，失败或越过 grounding gate 自动回退；
- 会话上下文只保存当前老人 ID，不保存完整问题、回答或医疗事实。

## 已完成：Planner + Tool Registry + Resilient Executor

已注册只读 Tool：

- `room_lookup`
- `patient_profile`
- `health_recent`
- `alerts_recent`
- `care_schedule`
- `recommendation_preview`

执行层已经从 Service 条件分支迁移为独立 `MedicalAiExecutor`：

- 保持 Planner 给出的执行顺序；
- 单 Tool 异常隔离；
- 支持 partial result；
- 不向前端泄露底层异常内容；
- 单 Tool 记录 `elapsedMs`；
- 全部 Tool 失败时返回明确 fallback，不生成未经系统验证的事实。

## 已完成：主动关怀推荐闭环

```text
健康 / 告警 / 服务信号
        ↓
规则召回 + 特征评分
        ↓
类别多样性 Top K
        ↓
B 端人工预览 / 站内投放
        ↓
C 端家属反馈
        ↓
下一轮排序偏好更新
```

当前实现：

- 近 7 天健康记录稀疏、未闭环告警、待执行服务作为排序信号；
- `USEFUL` 提升同内容和同类别权重；
- `NOT_INTERESTED` 隐藏当前内容并降低同类别偏好；
- 同一 delivery 的反馈采用更新语义，避免重复点击无限累加反馈权重；
- 同一老人 / 家属 / 内容按日避免重复投放；
- Agent 的 `recommendation_preview` 只做预览，不绕过人工投放环节。

## 已完成：Agent Evaluation 基线

固定评测数据集：

`src/test/resources/medical-ai-evaluation-cases.json`

CI 中执行：

- Planner Tool exact-match；
- 复合问题多 Tool 顺序；
- Profile / Care 等迁移回归；
- 综合问题展开；
- Executor partial result；
- 越权老人访问拦截；
- 医疗决策请求在 Planner 前拦截；
- 模型 grounding gate；
- 推荐排序与反馈回归。

评测集作为冻结回归集：新增 Planner 语义或 Tool 时必须同步增加样例，避免“加一个关键词、坏掉另一类问题”。

## 已完成：Redis Session + 降级

Agent 会话上下文已从 Service JVM Map 抽离：

- Redis key：`medical-ai:session:{medicalUserId}:{sessionId}`；
- TTL：2 小时；
- Value 仅保存当前老人 ID；
- Redis 临时不可用时退化到 JVM 最小上下文缓存；
- Reset 同时清理 Redis / fallback；
- 每轮仍重新校验当前医护是否继续拥有该老人权限，因此 Session 不能绕过权限变化。

## 已完成：可观测 Trace

当前每轮回答返回：

- `traceId`
- `plan`
- `planReason`
- Tool status
- Tool `elapsedMs`
- End-to-end `elapsedMs`
- sources
- modelEnhanced

同时输出 privacy-safe 执行日志，只记录 Trace、Plan、Tool 状态和耗时，不记录用户问题、医疗事实、老人姓名或回答正文。

## 下一阶段：真正值得继续做的生产化能力

### 1. 事件驱动主动关怀

把当前“请求时读取健康 / 告警 / 服务信号”进一步演进为领域事件：

```text
HealthRecorded / AlertRaised / ServiceScheduled
        ↓
CareEvent
        ↓
Recommendation Trigger
        ↓
Candidate Ranking
        ↓
Human Review / Delivery
```

优先使用现有业务事件，不为了简历额外引入无必要的消息中间件。

### 2. Evaluation Report

在现有 CI 回归基础上增加可机器读取的评测报告：

- Tool selection exact-match / precision / recall；
- safety block rate；
- permission block rate；
- partial-result rate；
- P50 / P95 Tool latency；
- model fallback rate。

### 3. 写 Tool 的人工确认机制

若未来增加“处理告警、创建服务记录”等写 Tool，必须先增加：

- Human-in-the-loop confirmation；
- 幂等键；
- 操作审计；
- 明确可回滚边界；
- 写 Tool 白名单。

### 4. Planner 升级

当前 deterministic Planner 是稳定基线。只有当自然语言覆盖率成为真实问题时，再增加模型 Planner，并保留规则 Planner 作为 fallback / policy gate，而不是为了堆 AI 技术名词直接替换稳定链路。
