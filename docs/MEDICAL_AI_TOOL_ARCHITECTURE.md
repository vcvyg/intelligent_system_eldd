# 医护 AI Tool 架构演进

当前 Agent 已具备权限校验、Planner、事实查询、Tool Trace 和安全边界。

当前采用 Planner + Tool Registry 方式扩展：

```text
用户问题
   |
   v
Planner / Router
   |
   v
MedicalAiToolRegistry
   |
   +-- HealthQueryTool
   +-- AlertQueryTool
   +-- RoomQueryTool
   +-- CareScheduleTool
   +-- RecommendationTool
   |
   v
Tool Executor
   |
   v
Fact Synthesizer
   |
   v
Answer + Trace + Sources
```

设计原则：

1. 新增业务能力通过新增 Tool 接入，不修改 Agent 主流程。
2. Tool 只负责受权限约束的数据获取，不负责自然语言生成。
3. 所有 Tool 执行结果进入 Trace，支持调试和 Agent Evaluation。
4. 写操作 Tool 需要额外增加人工确认、幂等和审计。

Planner 输出的 `plan` 只包含待执行的业务 Tool；权限校验、安全拦截和可选模型润色保留在 `tools` Trace 中，不伪装成业务查询计划。Service 按计划顺序从 Registry 取出 Tool 执行，再统一合并事实、来源和推荐追问。
