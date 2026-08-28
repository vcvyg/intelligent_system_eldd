# 医护 AI 助手

## 1. 定位

医护 AI 助手不是通用聊天机器人，而是嵌入医护工作台的 **Tool-grounded Java AI Agent**。

它优先从现有 Spring Boot 业务系统读取事实，而不是让模型凭上下文自由生成答案。完整链路：

```text
自然语言问题
   ↓
老人上下文解析 + 医护负责关系校验
   ↓
Medical Safety Guard
   ↓
Hybrid Planner
   ├─ deterministic rules（稳定基线）
   └─ optional semantic model（补充未覆盖表达）
   ↓
Read-only Tool Policy Gate
   ↓
Resilient Executor
   ↓
业务 Tool / SQL Server
   ↓
Grounded Answer + Sources + Trace
   ↓
Optional LLM Rewriter + Grounding Gate
```

典型问题：

- `王阿姨住哪个房间？`
- `王阿姨最近 7 天心率和血压怎么样？`
- `她最近有未处理告警吗？`
- `那她近期有什么照护安排？`
- `把她最近的健康、告警和安排一起汇总一下。`

## 2. Hybrid Planner

确定性规则 Planner 始终保留，不依赖模型也能完整运行。

开启：

```properties
medical.ai.planner-enabled=true
```

后可使用 OpenAI-compatible semantic planner 补充规则未覆盖的表达，但模型受到以下限制：

1. 只能从本地只读 Tool allowlist 中选工具；
2. 最多补充 4 个 Tool；
3. 规则已命中的 Tool 不允许被模型删除或替换；
4. 返回未知 Tool、写 Tool、坏 JSON、超时或异常时直接回退 deterministic plan；
5. 已解析到的老人姓名会先替换成“该老人”再进入可选语义规划调用；
6. Planner 只输出结构化 Tool 计划，不负责产生医疗事实。

因此模型承担“理解表达”，Java Policy Gate 决定“允许做什么”。

## 3. 当前只读 Tool

| Tool | 数据来源 | 作用 |
| --- | --- | --- |
| `room_lookup` | 老人档案 / 房间 | 查询房间信息 |
| `patient_profile` | 老人档案 | 查询必要档案字段 |
| `health_recent` | `health_data` | 汇总近 7 天健康记录 |
| `alerts_recent` | 告警记录 | 查询近期和未闭环告警 |
| `care_schedule` | 巡查 / 服务记录 | 汇总近期照护安排 |
| `recommendation_preview` | 推荐中心 | 查询可解释 Top 3，只预览不自动投放 |

`patient_access` 和 `medical_safety_guard` 属于 Tool 执行之前的本地策略边界，不交给模型决定。

## 4. Resilient Executor

`MedicalAiExecutor` 与 Planner 分离：

- 按 Plan 顺序执行；
- 单 Tool 异常不会让整轮请求失败；
- 失败 Tool 写入 `failed` Trace，剩余 Tool 继续；
- 支持 partial result；
- 全部 Tool 失败时返回明确 fallback，不生成未经查询的事实；
- 每个 Tool 记录 `elapsedMs`；
- sources 与 answer 一起返回。

## 5. 会话与权限

会话上下文由 Redis 保存：

```text
medical-ai:session:{medicalUserId}:{sessionId}
```

- TTL 2 小时；
- 只保存当前老人 ID；
- 不保存完整问题、回答或健康正文；
- Redis 临时不可用时退化到 JVM 最小缓存；
- Reset 同时清理 Redis / fallback；
- 每一轮仍重新校验医护-老人负责关系，旧 Session 不能绕过权限变化。

## 6. 医疗 Safety + Grounding

### 本地 Safety Guard

诊断、处方、停药/换药、剂量调整和治疗方案请求在 Planner 前拦截。

### 可选答案润色

```properties
medical.ai.enabled=true
medical.ai.base-url=http://127.0.0.1:8000/v1
medical.ai.model=your-model
medical.ai.api-key=${MEDICAL_AI_API_KEY:}
medical.ai.timeout-ms=5000
```

模型只允许重写 Tool 已经查询出的事实。返回文本还要经过本地 Grounding Gate：

- 新出现的数字必须存在于原始事实答案；
- 禁止诊断、处方、用药调整与治疗方案措辞；
- 输出长度必须在合理范围；
- 任一校验失败直接丢弃模型文本并回退 deterministic answer。

生产医疗数据只应连接经过批准的内部/合规模型服务。

## 7. Trace 与可观测性

每轮返回：

- `traceId`
- `plan`
- `planReason`
- Tool status
- Tool `elapsedMs`
- End-to-end `elapsedMs`
- sources
- `modelEnhanced`

privacy-safe 日志只记录 Trace、Plan、Tool 状态和耗时，不记录问题正文、老人姓名、健康事实或最终回答。

## 8. 受控写操作：Human-in-the-loop

自动 Planner 的白名单仍然只有只读 Tool。需要修改业务状态时走独立的 proposal-confirm 链路。

当前落地动作：**开始处理告警**。

### 第一步：生成提案

```http
POST /api/medical/ai-assistant/actions/alerts/{alertId}/proposals
```

后端会：

- 校验当前医护身份；
- 校验告警存在且仍处于可开始处理状态；
- 校验该老人属于当前医护负责范围；
- 创建 10 分钟有效的一次性 proposal；
- 不修改告警状态。

返回示例字段：

```json
{
  "proposalId": "...",
  "actionType": "START_ALERT_PROCESSING",
  "targetId": 33,
  "confirmationRequired": true,
  "expiresAt": "..."
}
```

### 第二步：显式确认

```http
POST /api/medical/ai-assistant/actions/proposals/{proposalId}/confirm
```

确认时再次校验：

- proposal 所属用户；
- proposal 是否已过期 / 已使用；
- 当前医护-老人负责关系；
- 告警目标是否仍然一致；
- 告警当前状态是否仍允许执行。

全部通过后才调用 `AlertService.processAlert`。proposal 使用后立即失效，避免重复提交。

取消：

```http
DELETE /api/medical/ai-assistant/actions/proposals/{proposalId}
```

这一设计让 Agent 具备真实行动能力，但模型本身没有无确认数据库写权限。

## 9. 主动关怀事件链路

医护 Agent 与主动关怀推荐共用同一套业务事实，但职责不同：

- Agent 的 `recommendation_preview` 只读；
- 健康记录、告警、待执行服务会发布最小化 `CareSignalEvent`；
- Transactional Outbox 负责可靠投递；
- Recommendation Trigger 进入人工复核；
- 管理员批准后才能在事件驱动场景执行站内投放。

详见：`docs/RECOMMENDATION_CENTER.md`。

## 10. Agent Evaluation

冻结数据集：

```text
src/test/resources/medical-ai-evaluation-cases.json
```

CI 真实执行 Planner 并生成：

- exact-match；
- micro precision；
- micro recall；
- micro F1；
- 每个 case 的 expected / actual Tools。

报告文件：

```text
target/medical-ai-evaluation-report.json
target/medical-ai-evaluation-report.md
```

GitHub Actions 会上传 `medical-ai-evaluation-report` artifact，而不是把评测数字手写进文档。

## 11. API

查询当前医护负责老人：

```http
GET /api/medical/ai-assistant/patients
```

对话：

```http
POST /api/medical/ai-assistant/chat
Content-Type: application/json

{
  "sessionId": "optional-session-id",
  "elderlyId": 11,
  "message": "她最近心率和告警怎么样？"
}
```

重置会话：

```http
DELETE /api/medical/ai-assistant/sessions/{sessionId}
```

## 12. 前端演示

```text
medical-dashboard.html
  → AI 助手
  → medical-ai-assistant.html
```

页面展示当前老人上下文、多轮问答、Agent Plan、Tool Trace、sources、短 Trace ID、执行耗时、模型增强状态、安全提示和推荐追问。

原有 `medical-chat.html` 始终表示真实医护 ↔ 家属沟通，AI 不伪装成人工医护。

## 13. CI 验证

主分支使用 `.github/workflows/core-ci.yml`，覆盖：

- tracked secret / runtime artifact guard；
- Maven 编译；
- deterministic + semantic Hybrid Planner；
- Tool allowlist policy gate；
- multi-tool routing；
- Executor partial result；
- Redis session fallback；
- 越权访问拦截；
- Safety Guard；
- Grounding Gate；
- Transactional Outbox 幂等；
- Health / Alert / Service domain events；
- Recommendation review state machine；
- Human-confirmed write action；
- 推荐反馈排序；
- 交互页面 JavaScript syntax；
- Agent evaluation artifact。
