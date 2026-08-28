# 医护 AI Agent 后续演进路线

## 当前架构

当前助手采用事实优先模式：

```
用户问题
   ↓
意图识别
   ↓
权限校验
   ↓
业务查询工具
   ↓
事实汇总
   ↓
可选模型润色
```

## 已完成：Planner + Tool Registry

当前 Agent 已将问题规划与 Tool 执行拆开，避免业务能力增加后 Service 内部条件分支持续增长。

当前抽象：

```java
interface MedicalTool {
    String name();
    boolean supports(Intent intent);
    ToolResult execute(ToolContext context);
}
```

已注册 Tool：

- `room_lookup`
- `patient_profile`
- `health_recent`
- `alerts_recent`
- `care_schedule`
- `recommendation_preview`

## 已完成：主动关怀推荐闭环

将被动问答扩展为主动服务：

```
健康事件
  ↓
用户画像
  ↓
推荐策略
  ↓
Push/站内提醒
  ↓
用户反馈
  ↓
更新偏好
```

第一版已采用：

- 规则召回
- 特征评分
- 反馈加权
- 按日投放去重

## 第三阶段：Agent Evaluation

通过固定测试集评估：

- Tool 选择准确率
- 越权访问拦截率
- 安全拒答正确率
- 回答事实一致性
- 平均响应耗时

测试案例见 `MEDICAL_AI_EVALUATION_CASES.json`。

## 第四阶段：生产化能力

- Redis Session 替换 JVM 内存上下文
- Tool 执行审计日志
- Prompt / Tool 版本管理
- 指标监控
- 人工确认后的写操作 Tool
