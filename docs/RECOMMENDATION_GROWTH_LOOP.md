# CareLink 推荐 / 投放 / 反馈策略闭环

CareLink 的推荐中心不只是“给老人展示几条关怀内容”。当前工程把推荐能力拆成业务信号、候选排序、人工复核、站内投放、用户反馈与效果分析六个阶段，并把其中的查询和分析能力暴露给医护 Agent。

```text
Health / Alert / Service write
        ↓
CareSignalEvent
        ↓ same transaction
Transactional Outbox
        ↓ retry / dead letter / lease recovery
Recommendation Trigger / PENDING_REVIEW
        ↓ human approve / reject
Candidate Ranking
  ├─ base score
  ├─ health / alert / service signals
  ├─ user feedback preference
  └─ category diversity
        ↓
Human-confirmed IN_APP Delivery
        ↓
Family Feed
        ↓
CLICK / USEFUL / NOT_INTERESTED
        ↓
Next-round ranking changes
        ↓
Performance Snapshot
  ├─ delivery count
  ├─ CTR
  ├─ useful rate
  ├─ negative rate
  └─ category performance
        ↓
Agent recommendation_performance Tool
```

## 1. 推荐策略与人群上下文

推荐仍然以养老关怀场景为业务边界，不伪造营销数据。每次候选排序读取当前老人已有的业务信号：

- 近 7 天健康记录是否稀疏；
- 是否存在未闭环告警；
- 是否存在待执行 / 执行中的生活服务；
- 家属对历史内容的 `USEFUL / NOT_INTERESTED / CLICK` 反馈；
- 同类别重复负反馈与类别多样性。

这些信号决定候选内容的加权、降权、隐藏和 Top 3 类别打散。

## 2. 投放边界

Agent 不能直接投放。事件触发后先进入 `PENDING_REVIEW`，管理员审批后才能通过后台执行站内投放；同一老人、家属和内容按天去重。

这让“模型理解 / Agent 分析”与“真实业务写操作”分开：

- `recommendation_preview`：只读当前 Top 3 与推荐原因；
- `recommendation_performance`：只读聚合效果与策略观察；
- 审批、投放仍由显式 B 端操作完成。

## 3. 效果分析

`RecommendationService.performance(...)` 对 7~90 天窗口内的投放流水做聚合，不读取健康正文、聊天内容或自由文本，仅统计：

- `deliveryCount`：投放数；
- `clickThroughRate`：存在 `clickedAt` / `CLICKED` 的比例；
- `usefulRate`：当前状态为 `USEFUL` 的比例；
- `negativeRate`：当前状态为 `NOT_INTERESTED` 的比例；
- 各内容类别的投放、点击、正向反馈和负反馈表现。

同时生成确定性策略观察，例如样本不足、整体点击偏低、负反馈偏高、某类别正向反馈较好或负反馈较高。当前实现只做分析，不让模型自动改策略。

管理员接口：

```http
GET /api/admin/recommendations/performance/{elderlyId}?familyUserId={optional}&days=30
```

## 4. Agent Tool

新增只读 Tool：

```text
recommendation_performance
```

典型问法：

- “王阿姨最近推荐投放效果怎么样？”
- “哪类关怀内容反馈更好？”
- “最近不感兴趣反馈是不是偏高？”
- “看一下最近投放表现和策略优化方向。”

Planner 会把“推荐什么”和“推荐效果”拆成两个不同目标：

```text
推荐什么 → recommendation_preview
效果 / 点击 / 反馈 / 策略 → recommendation_performance
```

一个问题也可以同时规划两个 Tool，例如：

> “现在适合推什么，同时看一下最近投放效果和家属反馈。”

此时 Agent 会先读取候选推荐，再读取聚合效果，但仍不会执行投放。

## 5. 与增长 / 营销系统的工程共性

CareLink 仍是养老领域项目，但推荐模块具备可迁移的工程抽象：

- 业务信号驱动候选召回 / 排序；
- 可解释策略原因；
- 审批与投放状态机；
- 频控 / 幂等；
- 用户反馈回流；
- 聚合效果分析；
- Agent 对策略和效果做只读 Tool 调用；
- Human-in-the-loop 控制真实写操作。

当前没有宣称实现广告竞价、真实 Push、复杂人群包、在线学习或 A/B 实验平台；这些属于后续可扩展方向。
