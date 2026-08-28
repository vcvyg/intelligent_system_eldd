# 医护 AI 助手

## 1. 这是什么

医护 AI 助手不是一个脱离业务系统的通用聊天机器人，而是一个面向医护工作台的 **Tool-grounded Query Agent**。

它的目标是：医护人员用自然语言提问，系统先确定当前老人和查询范围，再选择一个或多个只读业务工具，从现有数据库中读取事实，最后组合成回答并推荐下一步可能想问的问题。

典型问题：

- `王阿姨住哪个房间？`
- `王阿姨最近 7 天心率和血压怎么样？`
- `她最近有未处理告警吗？`
- `那她近期有什么照护安排？`
- `把她最近的健康、告警和安排一起汇总一下。`

## 2. 为什么按 Agent 而不是普通问答实现

一轮问答可能同时需要多个业务能力。例如：

```text
王阿姨住哪，最近心率和告警怎么样？
        ↓
解析当前老人 + 医护负责关系校验
        ↓
Plan: room_lookup → health_recent → alerts_recent
        ↓
执行 Tool 并收集事实
        ↓
汇总工具事实
        ↓
返回答案 + 数据来源 + Tool Trace + 推荐追问
```

关键区别在于：回答不是直接由模型凭上下文生成，而是由系统工具产生事实。模型层（如果显式开启）只允许对已经得到的事实做语言整理。

每轮回答还会返回：

- `traceId`：本轮执行标识，便于演示和后续日志/指标关联；
- `plan`：本轮计划涉及的业务 Tool；
- `planReason`：Planner 选择这些 Tool 的原因，或说明为什么没有执行 Tool；
- `tools`：实际执行轨迹和状态；
- `elapsedMs`：本轮后端总耗时；
- `sources`：答案使用的数据来源。

因此页面可以直接展示“计划 → 执行 → 来源 → 最终回答”，而不是只展示一段无法解释来源的 AI 文本。

## 3. 当前工具

| Tool | 数据来源 | 作用 |
| --- | --- | --- |
| `patient_access` | 医护-老人负责关系 | 每轮重新校验当前医护是否有权查询目标老人 |
| `room_lookup` | 老人档案 / 房间信息 | 查询房号、房型 |
| `patient_profile` | 老人档案 | 查询年龄、性别、已登记病史、过敏史等必要字段 |
| `health_recent` | `health_data` | 汇总近 7 天健康指标与最新记录 |
| `alerts_recent` | 告警记录 | 查询最近告警和未闭环告警数量 |
| `care_schedule` | 近期健康巡查 + 服务记录 | 组合“近期照护安排” |
| `recommendation_preview` | 主动关怀内容池 + 健康/告警/服务信号 | 查询可解释 Top 3 推荐，只预览不自动投放 |
| `medical_safety_guard` | 安全规则 | 拦截诊断、处方、换药、剂量调整等请求 |
| `llm_polish` | 可选模型 | 仅对已查询事实做语言组织，不负责产生业务事实 |

### 关于“护理计划”

当前项目没有独立、正式的护理计划实体，因此第一版不会假装存在该表。

页面中的“近期照护安排”明确由两部分组合：

1. 现有巡诊/健康记录链路中的近期记录；
2. 待执行或执行中的生活服务安排。

如果以后新增正式护理计划表，只需要增加新的只读 Tool，并把它加入规划和建议逻辑即可。

## 4. 会话上下文

会话只记忆当前老人 ID / 姓名，不保存完整医疗回答。

因此可以连续追问：

```text
医护：王阿姨住哪里？
AI：……3-206 房。

医护：那她近期有什么照护安排？
AI：……
```

会话上下文默认 2 小时失效；点击“新建会话”会主动清除上下文。

即使上下文里保存过某个老人，下一轮查询仍会重新执行医护-老人负责关系校验，避免权限关系变化后继续读取旧数据。

## 5. 权限与医疗安全边界

### 权限

- 仅 `MEDICAL` 角色可以访问 `/api/medical/ai-assistant/**`。
- 可查询老人来自当前医护真实负责关系。
- 显式传入未分配老人 ID 会返回 403。
- 会话记忆不能绕过负责关系校验。

### 医疗安全

助手可以：

- 查询和整理现有健康记录；
- 查询告警；
- 查询房间和业务档案；
- 汇总近期服务安排。
- 查询当前适合的主动关怀推荐。

助手不会：

- 自动诊断疾病；
- 开处方；
- 给出停药、换药、加减药建议；
- 调整剂量；
- 根据单次指标自动生成治疗方案。

这类请求由 `medical_safety_guard` 拦截，并提示由医护人员基于系统记录进行专业判断。

## 6. 可选 OpenAI-compatible 模型

默认关闭模型调用。不开模型时，系统仍然可以完整演示规划、工具查询、会话记忆、权限控制和推荐追问。

需要语言润色时，可在受控环境中配置：

```properties
medical.ai.enabled=true
medical.ai.base-url=http://127.0.0.1:8000/v1
medical.ai.model=your-model
medical.ai.api-key=${MEDICAL_AI_API_KEY:}
medical.ai.timeout-ms=5000
```

模型调用只发生在：

- 已经定位到有权限的老人；
- 已经查询到明确数据来源；
- 当前回答没有被医疗安全规则拦截。

### Grounding Gate

模型返回文本不会直接成为最终答案，还必须经过本地 grounding gate：

1. 模型新增的数字必须已经存在于原始 Tool 事实中；
2. 输出中不得出现诊断、处方、停药/换药、剂量调整、治疗方案等决策措辞；
3. 输出长度必须处于合理范围；
4. 任一校验失败时直接丢弃模型结果，回退到确定性的 Tool 事实回答；
5. 模型调用本身有超时边界，调用失败同样自动降级。

因此“LLM 只做润色”不仅依赖 Prompt，也有代码层的输出约束。

**生产环境中不要把真实医疗数据发送给未经批准的外部模型。** 推荐优先接入内部合规模型或在明确的数据治理策略下使用模型服务。

## 7. API

### 查询当前医护负责老人

```http
GET /api/medical/ai-assistant/patients
```

### 对话

```http
POST /api/medical/ai-assistant/chat
Content-Type: application/json

{
  "sessionId": "optional-session-id",
  "elderlyId": 11,
  "message": "她最近心率和告警怎么样？"
}
```

返回包含：

- `traceId`：本轮 Agent Trace 标识；
- `plan`：计划执行的业务 Tool；
- `planReason`：本轮规划原因；
- `answer`：最终回答；
- `tools`：实际执行过的工具和状态；
- `sources`：数据来源；
- `elapsedMs`：本轮后端总耗时；
- `suggestions`：推荐追问；
- `safetyNote`：医疗边界提示；
- `modelEnhanced`：是否使用了可选模型润色。

### 重置会话

```http
DELETE /api/medical/ai-assistant/sessions/{sessionId}
```

## 8. 前端演示

医护登录后进入：

```text
medical-dashboard.html
  → AI 助手
  → medical-ai-assistant.html
```

页面展示：

- 当前负责老人列表；
- 当前会话老人上下文；
- 多轮对话；
- Agent Plan；
- Tool Trace；
- 数据来源；
- 短 Trace ID 与执行耗时；
- 事实工具模式 / 模型润色状态；
- 安全提示；
- 推荐追问。

原有 `medical-chat.html` 继续承担“医护 ↔ 家属”的人工实时沟通，不把 AI 伪装成人工医护。

## 9. 如何扩展一个新 Tool

推荐保持“只读查询 Tool 优先”的扩展顺序：

1. 明确数据权限和真实数据源；
2. 实现 `MedicalAiTool` 并注册为 Spring Bean；
3. 在 `MedicalAiPlanner` 中增加对应语义和 Tool 名称；
4. Planner 自动把 Tool 写入 Plan，Agent 从 Registry 执行并把结果写入 `ToolTrace` 和 `sources`；
5. 增加至少一个成功测试和一个权限/空数据边界测试；
6. 再决定是否允许模型对结果做语言整理。

适合下一步加入的工具：

- 家属联系人查询；
- 老人近期服务历史；
- 医护本人今日排班；
- 告警详情与处理进度；
- 正式护理计划（如果后续新增数据模型）。

如果未来增加“修改服务状态、创建记录、处理告警”等写 Tool，应额外加入人工确认、幂等、操作审计和可回滚边界，不建议直接复用当前只读查询策略。

### 当前执行结构

当前业务逻辑已经按以下结构运行：

```text
Planner / Router
      ↓
Tool Executor / Registry
      ↓
Fact Synthesizer
      ↓
Optional LLM Rewriter + Grounding Gate
```

Planner 只决定只读业务 Tool，权限校验、安全拦截和可选模型润色不会混入业务 Plan；实际执行顺序和状态单独记录在 Tool Trace 中。

## 10. 验证

主分支统一使用：

```text
.github/workflows/core-ci.yml
```

当前 CI 包含：

- tracked secret / 本地运行时文件 guard；
- 全项目 Maven 编译；
- 多工具组合查询；
- 会话上下文追问；
- 会话 reset；
- 无上下文时的权限范围提示；
- 越权老人访问拦截；
- 诊断/用药请求安全拦截；
- 安全拦截时禁止调用外部模型；
- LLM 新增数字事实拦截；
- LLM 医疗决策措辞拦截；
- Agent plan / trace 元数据；
- 文件上传白名单与群组权限；
- 下载路径穿越防护；
- 原有聊天与告警重点回归测试；
- 医护 AI 前端 JavaScript 语法检查。
