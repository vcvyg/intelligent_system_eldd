# 医护 AI Tool 架构演进

当前 Agent 已具备权限校验、事实查询、Tool Trace 和安全边界。

下一阶段采用 Tool Registry 方式扩展：

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

当前提交仅引入扩展接口和 Registry，不迁移已有查询逻辑，避免一次重构影响稳定功能。
