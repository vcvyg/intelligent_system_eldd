# 主动关怀推荐中心

## 目标

推荐中心把系统从“用户主动查询”扩展为“系统根据业务事实主动关怀”。第一版保持小而精：使用现有健康记录、告警、服务安排和家属反馈做可解释排序，不生成诊断、处方或用药建议。

```text
健康/告警/服务事件
        ↓
候选内容池
        ↓
规则评分 + 反馈偏好
        ↓
类别打散 Top 3
        ↓
B 端投放
        ↓
C 端家属推荐流
        ↓
有用 / 不感兴趣 / 点击
        ↓
下一轮排序变化
```

## 当前评分信号

- 基础内容优先级 `base_score`。
- 近 7 天健康记录少于 3 条：提升 `HEALTH_CHECK`。
- 存在未闭环告警：提升 `SAFETY`，同时适度提升 `HEALTH_CHECK`。
- 存在待执行/执行中生活服务：提升 `CARE_SERVICE`。
- `USEFUL`：提升同类别和同内容分数。
- `NOT_INTERESTED`：隐藏同内容并降低同类别；同类别累计两次负反馈后，该类别退出当前推荐。
- 排序后优先保证类别多样性，再补足 Top 3。

## B 端 API

管理员预览：

```http
GET /api/admin/recommendations/preview/{elderlyId}?familyUserId={optional}
```

执行站内投放：

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
- `recommendation_delivery`：面向家属的真实投放流水。
- `recommendation_feedback`：点击、有用、不感兴趣反馈。

## 安全边界

- 家属只能读取和反馈自己关联老人的推荐。
- 内容池只包含健康记录提醒、安全提醒、生活服务、作息与家庭陪伴等通用关怀内容。
- 推荐原因来自确定性的系统事实和反馈信号，可直接展示，不由模型编造。
- 当前投放通道为站内模拟 `IN_APP`，不声称已连接真实 Push 平台。

## 下一步

1. 增加管理员 B 端预览/投放页面。
2. 增加家属 C 端推荐流和反馈按钮。
3. 注册 `RecommendationTool`，允许医护 AI 助手回答“现在适合给她推荐什么”。
4. 根据需要加入实验分桶、频控、静默时段和更细的事件召回。
