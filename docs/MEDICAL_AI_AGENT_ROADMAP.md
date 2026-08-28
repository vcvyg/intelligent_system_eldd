# 医护 AI Agent：最终落地架构

## 项目定位

这不是一个脱离业务系统的聊天 Demo，而是一套嵌入 Spring Boot 养老业务流程的 Java AI Application。AI 负责理解查询、规划只读 Tool、解释业务事实与生成可审计操作提案；权限、安全、写操作确认、事件可靠投递和最终业务状态仍由 Java 服务端控制。

```text
Medical User Request
        ↓
Patient Scope + Permission Check
        ↓
Medical Safety Guard
        ↓
Hybrid MedicalAiPlanner
  ├─ Deterministic Rule Baseline
  └─ Optional Semantic Model Planner
        ↓
Read-only Tool Policy Gate
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

写操作与上面的自动只读 Planner 分离：

```text
Operational Action Request
        ↓
Permission + Current-State Check
        ↓
Short-lived Action Proposal
        ↓
Human Explicit Confirmation
        ↓
Permission + State Recheck
        ↓
One-time Write Execution + Audit Log
```

## 1. Hybrid Planner + Resilient Executor

已注册只读 Tool：

- `room_lookup`
- `patient_profile`
- `health_recent`
- `alerts_recent`
- `care_schedule`
- `recommendation_preview`

Planner 采用 Hybrid 策略：

- deterministic rule planner 始终保留为稳定基线；
- `medical.ai.planner-enabled=true` 时可启用 OpenAI-compatible semantic planner；
- 模型只能从中央只读 Tool allowlist 选择，最多补充 4 个 Tool；
- 规则已经命中的 Tool 不允许被模型删除或替换；
- 模型返回未知/写 Tool、非法 JSON、超时或异常时直接回退规则规划；
- 已解析到的老人姓名在发送给可选 semantic planner 前替换成“该老人”；
- 模型 Planner 和答案润色是两个独立开关，默认都关闭。

Executor 独立于 Service：

- 保持 Plan 顺序执行；
- 单 Tool 异常隔离；
- 支持 partial result；
- Tool Trace 记录状态和 `elapsedMs`；
- 全部 Tool 失败时返回明确 fallback，不编造业务/医疗事实。

## 2. Grounding、权限与医疗安全

- 每轮重新校验当前医护与目标老人负责关系；
- Redis Session 只保存当前老人 ID，TTL 2 小时，Redis 故障时降级 JVM 最小缓存；
- 诊断、处方、停药/换药、剂量调整与治疗方案在 Planner 前拦截；
- 可选模型只允许重写已经由 Tool 查询到的事实；
- Grounding Gate 拒绝模型新增数字事实或医疗决策措辞；
- 模型失败/越界自动回退 deterministic answer；
- `traceId`、Plan、Tool status、Tool latency、总耗时和 sources 可追踪；
- privacy-safe 日志不记录用户问题、老人姓名、健康正文或最终回答。

## 3. 多领域事件驱动主动关怀

真实接入三个稳定业务写入点：

```text
HealthData saved ─────────────→ HEALTH_RECORDED
Alert created ────────────────→ ALERT_RAISED
Pending/Processing service ───→ SERVICE_SCHEDULED
                                      ↓
                               CareSignalEvent
```

事件只包含：

- 老人 ID；
- signal type；
- 业务 reference ID；
- occurredAt。

不复制健康测量值、告警正文、聊天内容或模型文本。

## 4. Transactional Outbox + Retry

领域事件不再依赖“事务提交后尽力写一条 Trigger”。现在采用 SQL Server Transactional Outbox：

```text
Business Transaction
    ├─ write business row
    └─ care_signal_outbox / PENDING
              ↓ commit together
Scheduled Outbox Worker
              ↓ claim
         PROCESSING
          ↙       ↘
   PROCESSED      RETRY
                    ↓ bounded exponential backoff
               DEAD_LETTER (5 failures)
```

实现特性：

- `event_key` 唯一约束 + Service 幂等检查；
- worker 条件更新抢占，降低多实例重复消费；
- 最大 5 次重试；
- 有界指数退避；
- 只记录异常类型，不写异常业务正文；
- 不引入没有实际吞吐需求的 Kafka，保持单体部署可运行。

数据库升级脚本：`sql/add_recommendation_center.sql`。

## 5. Recommendation Human-in-the-loop

事件到达后只进入人工复核队列，不自动触达家属：

```text
Outbox PROCESSED
        ↓
recommendation_trigger / PENDING_REVIEW
        ↓
Admin Review
   ├─ APPROVED ──→ Top K preview ──→ Human delivery ──→ DELIVERED
   └─ REJECTED
```

Trigger 记录：

- `reviewerId`
- `reviewedAt`
- `decisionReason`
- `deliveredAt`

当存在事件驱动的 `PENDING_REVIEW` 且没有任何已批准事件时，投放接口会拒绝直接投放。只有真实创建新 Delivery 后，已批准 Trigger 才进入 `DELIVERED`。

推荐排序继续使用健康记录稀疏、未闭环告警、待执行服务和家属反馈；`USEFUL / NOT_INTERESTED / CLICK` 使用更新语义，避免重复反馈无限叠权。

## 6. Controlled Agent Write Action

只读 Tool 仍是自动 Planner 的唯一可执行 Tool。写操作采用完全独立的确认链路，首个落地动作是“开始处理告警”：

1. 医护发起 action proposal；
2. Java 后端校验告警状态及医护-老人负责关系；
3. 创建 10 分钟有效的一次性 proposal token；
4. proposal 阶段不修改任何业务状态；
5. 用户再次显式确认；
6. 后端重新校验权限、老人 ID、告警当前状态；
7. one-time token 消费成功后才调用 `AlertService.processAlert`；
8. 记录 privacy-safe propose / confirm audit log。

API：

```http
POST /api/medical/ai-assistant/actions/alerts/{alertId}/proposals
POST /api/medical/ai-assistant/actions/proposals/{proposalId}/confirm
DELETE /api/medical/ai-assistant/actions/proposals/{proposalId}
```

这让 Agent 具备“可行动”能力，同时避免模型直接获得无确认的数据库写权限。

## 7. Agent Evaluation + CI Artifact

冻结数据集：

`src/test/resources/medical-ai-evaluation-cases.json`

CI 会真实执行并生成：

- Planner exact-match；
- micro precision；
- micro recall；
- micro F1；
- 每条 case 的 expected / actual Tool。

输出：

- `target/medical-ai-evaluation-report.json`
- `target/medical-ai-evaluation-report.md`
- GitHub Actions artifact：`medical-ai-evaluation-report`

同时核心回归覆盖：

- Hybrid Planner policy gate；
- Executor partial result；
- Redis Session fallback；
- Safety Guard；
- 权限越权；
- Grounding Gate；
- Outbox 幂等；
- Health / Service / Alert domain signal；
- Recommendation review state machine；
- 家属反馈排序；
- Human-confirmed write action。

## 8. 收尾边界

当前版本已经形成完整的 Java AI Application 主链路。后续不再为了技术名词继续堆 RAG、向量库、MCP、Kafka 或更多 Agent 框架。

只有出现真实需求时再演进：

- 自然语言覆盖率不足：扩展 semantic planner / structured output；
- 跨服务、高吞吐事件：Outbox 下游迁移 MQ；
- 需要更多写操作：复用 proposal-confirm-audit 框架逐个加入白名单；
- 需要线上 SLO：将现有 trace/latency 接入 Micrometer dashboard。

当前代码优先保证：**业务事实可追溯、AI 行为受策略约束、写操作有人确认、事件不会静默丢失、模型失败可降级、测试结果可复现。**
