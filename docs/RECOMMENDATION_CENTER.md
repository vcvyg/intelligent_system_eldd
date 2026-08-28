# 主动关怀推荐中心

## 目标

推荐中心把系统从“用户主动查询”扩展为“业务事件触发关怀复核”。当前版本坚持可解释和人工确认：事件只进入复核队列，候选排序来自系统事实，真正投放仍由 B 端人员确认，不生成诊断、处方或用药建议。

```text
告警等业务事件
        ↓
CareSignalEvent（最小化事件）
        ↓
recommendation_trigger
        ↓
B 端人工复核队列
        ↓
规则评分 + 反馈偏好
        ↓
类别打散 Top 3
        ↓
人工确认投放
        ↓
C 端家属推荐流
        ↓
有用 / 不感兴趣 / 点击
        ↓
下一轮排序变化
```

## 事件驱动触发

当前已接入 `ALERT_RAISED`：任何通过 `AlertService.createAlert` 创建的告警在业务事务中发布 `CareSignalEvent`，事务提交后由 `RecommendationSignalListener` 写入复核队列。

设计边界：

- Event 只携带 `elderlyId`、`signalType`、`referenceId` 和时间，不复制告警正文或健康测量值。
- `recommendation_trigger` 使用业务引用做幂等，避免同一个告警重复生成复核任务。
- Trigger 状态先进入 `PENDING_REVIEW`。
- 管理端读取待复核事件后决定是否预览并投放；只有真正创建新投放时，当前待复核 Trigger 才变为 `DELIVERED`。
- 监听器失败不回滚已经提交的告警事务，也不会自动降级成“直接给家属发送”。

## 当前评分信号

- 基础内容优先级 `base_score`。
- 近 7 天健康记录少于 3 条：提升 `HEALTH_CHECK`。
- 存在未闭环告警：提升 `SAFETY`，同时适度提升 `HEALTH_CHECK`。
- 存在待执行/执行中生活服务：提升 `CARE_SERVICE`。
- `USEFUL`：提升同类别和同内容分数。
- `NOT_INTERESTED`：隐藏同内容并降低同类别；同类别累计两次负反馈后，该类别退出当前推荐。
- 同一 delivery 的重复反馈采用更新语义，不因重复点击无限累加权重。
- 排序后优先保证类别多样性，再补足 Top 3。

## B 端 API

读取待人工复核事件：

```http
GET /api/admin/recommendations/triggers?elderlyId={optional}
```

管理员预览：

```http
GET /api/admin/recommendations/preview/{elderlyId}?familyUserId={optional}
```

人工确认站内投放：

```http
POST /api/admin/recommendations/deliver/{elderlyId}
```

同一老人、家属、内容在同一天只创建一次投放记录。

## C 端 API

家属读取已投放推荐：

```http
GET /api/family/recommendations/{elderlyId}
```

反馈：

```http
POST /api/family/recommendations/feedback
Content-Type: application/json

{
  "elderlyId": 11,
  "deliveryId": 101,
  "feedbackType": "USEFUL"
}
```

支持：`USEFUL`、`NOT_INTERESTED`、`CLICK`。

## 数据表

执行：

```text
sql/add_recommendation_center.sql
```

新增：

- `recommendation_content`：脱敏内容池和基础优先级。
- `recommendation_trigger`：业务事件驱动的人工复核队列。
- `recommendation_delivery`：面向家属的真实投放流水。
- `recommendation_feedback`：点击、有用、不感兴趣反馈。

## 安全边界

- 家属只能读取和反馈自己关联老人的推荐。
- 内容池只包含健康记录提醒、安全提醒、生活服务、作息与家庭陪伴等通用关怀内容。
- 推荐原因来自确定性的系统事实和反馈信号，可直接展示，不由模型编造。
- Event/Trigger 不复制医疗正文，避免为了推荐链路额外扩散敏感信息。
- 当前投放通道为站内模拟 `IN_APP`，不声称已连接真实 Push 平台。
- AI Agent 的 `recommendation_preview` 仍是只读 Tool，不拥有自动投放权限。

## 当前接入状态

1. 告警创建已接入事件驱动关怀 Trigger。
2. 管理员 B 端可看到待复核事件、预览 Top 3 并人工投放。
3. 家属 C 端已接入推荐流及“有用 / 不感兴趣 / 点击”反馈。
4. 反馈真实影响后续排序，且同 delivery 重复反馈不会无限累加信号。
5. 医护 AI 助手已注册 `recommendation_preview` Tool，可回答“现在适合给她推荐什么”，但不会自动投放。
6. 后续可继续把 `HEALTH_RECORDED`、`SERVICE_SCHEDULED` 等稳定业务写入点接入同一事件接口，而不需要改推荐核心。
