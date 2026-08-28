# 主动关怀推荐中心

## 目标

推荐中心把系统从“用户主动查询”扩展为“业务事件触发关怀复核”。当前版本坚持 **事件可靠、推荐可解释、投放有人确认**：事件不会直接给家属发内容，候选排序来自系统事实，管理员完成复核后才允许事件驱动投放。

```text
Health / Alert / Service business write
        ↓
CareSignalEvent（最小化事件）
        ↓ same transaction
care_signal_outbox / PENDING
        ↓ async worker + retry
recommendation_trigger / PENDING_REVIEW
        ↓
Admin APPROVE / REJECT
        ↓ approved only
规则评分 + 反馈偏好 + 类别打散 Top 3
        ↓
人工确认 IN_APP 投放
        ↓
C 端家属推荐流
        ↓
USEFUL / NOT_INTERESTED / CLICK
        ↓
下一轮排序变化
```

## 1. 已接入领域事件

当前真实接入三个业务写入点：

- `ALERT_RAISED`：`AlertService.createAlert` 成功创建告警；
- `HEALTH_RECORDED`：医护保存健康记录；
- `SERVICE_SCHEDULED`：服务记录进入 `PENDING / PROCESSING`。

`CareSignalEvent` 只携带：

- `elderlyId`
- `signalType`
- `referenceId`
- `occurredAt`

不复制告警正文、健康测量值、服务描述、聊天内容或模型文本。

## 2. Transactional Outbox

领域事件由 `RecommendationSignalListener` 在业务事务提交前写入 `care_signal_outbox`，因此业务写入和事件记录要么一起提交，要么一起回滚。

Outbox 生命周期：

```text
PENDING
  ↓ worker claim
PROCESSING
  ├─ success → PROCESSED
  └─ failure → RETRY
                  ↓ bounded exponential backoff
             DEAD_LETTER after 5 attempts
```

可靠性策略：

- `event_key = signalType:elderlyId:referenceId`；
- DB 唯一索引 + Java 幂等检查；
- worker 使用状态条件更新抢占任务；
- 小批量顺序消费；
- 有界指数退避；
- 失败只记录异常类型，不持久化异常正文；
- 单体项目没有为了“事件驱动”标签额外引入 Kafka。

配置：

```properties
medical.ai.care-outbox.enabled=true
medical.ai.care-outbox.batch-size=25
medical.ai.care-outbox.fixed-delay-ms=10000
```

部署新版本前执行最新 `sql/add_recommendation_center.sql`。

## 3. Human Review 状态机

`recommendation_trigger` 不再从 `PENDING_REVIEW` 直接跳到投放：

```text
PENDING_REVIEW
   ├─ APPROVED → human delivery → DELIVERED
   └─ REJECTED
```

复核审计字段：

- `reviewer_id`
- `reviewed_at`
- `decision_reason`
- `delivered_at`

当某老人存在 `PENDING_REVIEW` 且没有任何 `APPROVED` 事件时，事件驱动投放接口拒绝直接发送。只有真正创建了新的 Delivery，已批准 Trigger 才进入 `DELIVERED`。

## 4. 当前评分信号

- 基础内容优先级 `base_score`；
- 近 7 天健康记录少于 3 条：提升 `HEALTH_CHECK`；
- 存在未闭环告警：提升 `SAFETY`，并适度提升 `HEALTH_CHECK`；
- 存在待执行 / 执行中服务：提升 `CARE_SERVICE`；
- `USEFUL`：提升同内容与同类别；
- `NOT_INTERESTED`：隐藏当前内容并降低同类别；同类别累计两次负反馈后退出当前推荐；
- 同一 delivery 的重复反馈使用更新语义，不会无限叠加权重；
- 排序后优先保证类别多样性，再补足 Top 3。

## 5. B 端 API

复核队列：

```http
GET /api/admin/recommendations/triggers?elderlyId={optional}
```

批准：

```http
POST /api/admin/recommendations/triggers/{triggerId}/approve?reason={optional}
```

拒绝：

```http
POST /api/admin/recommendations/triggers/{triggerId}/reject?reason={optional}
```

推荐预览：

```http
GET /api/admin/recommendations/preview/{elderlyId}?familyUserId={optional}
```

人工确认站内投放：

```http
POST /api/admin/recommendations/deliver/{elderlyId}
```

同一老人、家属、内容在同一天只创建一次投放记录。

## 6. C 端 API

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

## 7. 数据表

执行：

```text
sql/add_recommendation_center.sql
```

表：

- `recommendation_content`：通用关怀内容池；
- `care_signal_outbox`：领域事件可靠投递；
- `recommendation_trigger`：人工复核状态机与审计；
- `recommendation_delivery`：面向家属的投放流水；
- `recommendation_feedback`：点击 / 有用 / 不感兴趣反馈。

脚本包含对已有 `recommendation_trigger` 安装的增量列升级，不要求删除旧表重建。

## 8. 安全边界

- 家属只能读取和反馈自己关联老人的推荐；
- 内容池只包含健康记录提醒、安全提醒、生活服务、作息与家庭陪伴等通用内容；
- 推荐原因来自确定性业务信号和反馈，不由模型编造；
- Event / Outbox / Trigger 不复制医疗正文；
- 当前投放通道为站内 `IN_APP`，不声称接入真实 Push；
- AI Agent 的 `recommendation_preview` 永远是只读 Tool；
- 模型 Planner 的 Tool allowlist 不包含投放或审批写操作。

## 9. 前端闭环

管理员推荐工作台支持：

1. 选择老人；
2. 查看 `PENDING_REVIEW / APPROVED` 事件；
3. 对待复核事件执行批准 / 拒绝；
4. 预览可解释 Top 3；
5. 人工确认投放；
6. 已批准事件随真实 Delivery 进入 `DELIVERED`。

家属端继续完成推荐曝光与反馈，形成：

**业务事件 → Reliable Outbox → 人工复核 → 可解释排序 → 人工投放 → 家属反馈 → 下一轮排序变化**。
