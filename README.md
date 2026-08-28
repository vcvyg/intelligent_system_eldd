# 智慧养老管理系统

`Java 21` `Spring Boot 3` `SQL Server` `MyBatis Plus` `Spring Security` `JWT` `WebSocket` `Redis`

面向养老院管理员、医护人员和家属的多角色业务系统。项目覆盖用户认证与权限控制、老人档案、健康数据、医护排班、设备与定位、预警处理、家属服务、实时沟通等流程，并在医护工作台中加入基于真实业务数据的 Tool-grounded AI 助手。

## 项目定位

- 使用 Spring Boot 分层架构组织 Controller、Service、Repository、DTO、VO 和 Entity。
- 使用 Spring Security + JWT 管理登录态与接口访问权限。
- 使用 MyBatis Plus 访问 SQL Server，配合统一返回结构、参数校验、异常处理和跨域配置完成后端接口治理。
- 使用静态页面与 JavaScript 调用 RESTful API，覆盖管理员端、医护端、家属端的核心页面流程。
- 引入 Redis、WebSocket/STOMP、邮件、Quartz 等组件，为实时消息、未读数、通知和定时任务扩展留出基础。
- 医护 AI 助手采用“权限校验 → 规划查询 → Tool 执行 → 事实汇总 → 推荐追问”的方式工作，可选接入 OpenAI-compatible 模型做语言整理，默认不向外部模型发送医疗数据。

## 核心业务

| 模块 | 当前能力 |
| --- | --- |
| 用户与权限 | 注册、登录、邮箱验证码、密码重置、JWT 鉴权、多角色接口隔离 |
| 老人管理 | 老人档案、房间、家属关系、医护分配、健康数据 |
| 医护工作台 | 排班、巡诊、健康记录、请假申请、服务记录与缴费通知 |
| 医护 AI 助手 | 多工具业务问答、老人上下文记忆、权限复核、健康/告警/照护安排汇总、推荐追问、医疗安全拦截 |
| 家属服务 | 探访预约、服务进度、缴费记录、定位与围栏信息 |
| 告警处理 | 健康/设备预警、处理状态流转、医护分配、统计与任务列表 |
| 实时沟通 | 家属与医护围绕老人建立沟通群，支持多类型消息与历史记录 |

## 实时聊天模块

聊天模块是当前项目与客服平台 JD 最贴近的实现之一。

### 已实现能力

- 按老人维度建立家属与医护沟通群，分别提供家属端和医护端入口。
- 通过 WebSocket + STOMP 推送群消息，保留 HTTP 发送接口作为降级链路。
- 聊天消息落库，支持分页拉取历史记录并按时间顺序展示。
- 支持文本、图片、语音和文件消息，保留发送者、角色、时间和附件信息。
- 使用 Redis 维护群组未读数，为实时会话列表和提醒能力提供基础。
- 补充聊天群组访问校验，家属和医护只能读取自己关联老人的聊天记录。

### 代码入口

- `src/main/java/org/example/persion/controller/family/FamilyChatController.java`
- `src/main/java/org/example/persion/controller/medical/MedicalChatController.java`
- `src/main/java/org/example/persion/controller/medical/MedicalChatWsController.java`
- `src/main/java/org/example/persion/config/WebSocketConfig.java`
- `src/main/java/org/example/persion/entity/ChatMessage.java`

### 审查与优化点

1. 当前实时消息链路已经具备 WebSocket、消息持久化、附件和未读数基础，适合继续扩展客服会话能力。
2. 当前消息发送仍依赖应用内 Simple Broker；若面向高并发客服场景，可切换到外部消息代理或消息队列，增加削峰、重试和消费监控。
3. WebSocket 控制器和 HTTP 控制器都在解析多媒体消息，后续可抽出统一的消息命令 DTO、校验器和消息组装服务。
4. 当前聊天侧更偏“沟通群”，若要贴近在线客服，可继续增加会话接入队列、坐席状态、转接、快捷回复、已读回执和会话质检。
5. AI 查询能力与人工沟通链路保持分离：`medical-chat.html` 继续表示真实医护与家属沟通，AI 助手通过独立工作台提供系统查询和回复辅助，避免混淆人工身份。

## 医护 AI 助手（Tool-grounded Agent）

医护 AI 助手不是让大模型直接“看病”，而是把现有业务数据封装成受权限约束的查询工具。医护可以自然语言提问，系统自动组合所需数据后回答。

### 已实现能力

- 支持一个问题同时执行多个查询，例如“王阿姨住哪，最近心率和告警怎么样？”会组合房间、健康和告警工具。
- 支持会话上下文：先选择/提到一位老人后，可以继续问“那她近期有什么照护安排？”。
- 每轮重新校验当前医护与目标老人的负责关系，显式访问未分配老人直接返回 403，会话记忆不能绕过权限。
- 回答同时返回 Tool Trace、数据来源和推荐追问，便于前端解释“查了什么、答案来自哪里”。
- “近期照护安排”不会虚构不存在的数据模型，而是明确组合当前系统中的近期巡诊/健康记录和待执行服务安排。
- 对诊断、处方、停药/换药、剂量调整和治疗方案请求做安全拦截，只提供系统记录供医护人员判断。
- 可选接入 OpenAI-compatible 模型做语言整理；默认关闭，模型失败自动回退到确定性的工具事实回答。

### 代码入口

- `src/main/java/org/example/persion/controller/medical/MedicalAiAssistantController.java`
- `src/main/java/org/example/persion/service/impl/MedicalAiAssistantServiceImpl.java`
- `src/main/java/org/example/persion/service/impl/MedicalAiModelEnhancer.java`
- `src/main/resources/static/medical-ai-assistant.html`
- `src/main/resources/static/js/medical-ai-assistant.js`
- `docs/MEDICAL_AI_ASSISTANT.md`

详细架构、API、模型开关、隐私边界和 Tool 扩展方式见 [`docs/MEDICAL_AI_ASSISTANT.md`](docs/MEDICAL_AI_ASSISTANT.md)。

## 告警任务流与工单化方向

仓库当前没有单独的 `Ticket` 工单实体。现有最接近工单的实现是告警任务处理流：预警进入待处理池后，由管理员或医护人员分配、接单、处理和关闭。

### 已实现能力

- 告警状态覆盖 `待处理`、`处理中`、`已处理`、`已忽略`。
- 管理员可创建告警并分配医护人员。
- 医护人员可接单处理告警，系统记录处理人、处理时间和处理结果。
- 提供当前医护人员的任务接口 `GET /api/medical/alerts/tasks/my`，返回可接单和正在处理的告警任务。
- 对已处理或已忽略任务增加关闭态校验，避免重复分配、重复完成和重复忽略。
- 告警列表、统计与医护端页面可承载待办筛选和任务处理入口。

### 代码入口

- `src/main/java/org/example/persion/entity/AlertRecord.java`
- `src/main/java/org/example/persion/controller/admin/AdminAlertController.java`
- `src/main/java/org/example/persion/controller/medical/MedicalAlertController.java`
- `src/main/java/org/example/persion/service/impl/AlertServiceImpl.java`
- `src/main/java/org/example/persion/repository/AlertRecordMapper.java`

### 审查与优化点

1. 现有告警任务流已经覆盖分配、接单、完成和关闭边界，可作为客服工单系统的状态流基础。
2. 若要升级成真正的工单模块，应新增 `CustomerTicket`、`TicketMessage`、`TicketAssignment`、`TicketStatusHistory` 等模型，而不是继续把告警字段无限扩张。
3. 工单系统还应补充优先级、SLA、来源渠道、自动分配策略、转派、催办、超时升级和操作审计。
4. 告警任务目前更偏医护业务处理；若面向客服平台，可把聊天会话与工单关联，支持从会话自动建单和工单回流通知。
5. AI 扩展可以继续加入告警详情查询、处理建议草稿和人工确认后的 Tool 写操作，但关键业务变更应保留人工确认、幂等和审计。

## 服务记录与支付通知

支付板块当前实现的是养老服务场景下的账单通知闭环，不等同于第三方支付网关接入。

### 已实现能力

- 医护端按老人和家属联系人创建缴费通知，支持费用项目、金额、到期日和备注。
- 家属端查看待支付项目、确认支付方式并查看支付历史。
- 待支付、已支付、已取消状态分离，医护端可撤销尚未支付的缴费通知。
- 医护工作台汇总待缴费用金额，便于从服务记录追踪到费用闭环。

### 审查与优化点

1. 当前支付动作属于站内确认支付，适合展示业务闭环；若进入生产环境，应接入真实支付渠道、回调验签、幂等和对账。
2. 账单通知与服务记录已经可以串起“服务上报 -> 费用通知 -> 家属确认”的流程，后续可进一步关联服务单、工单和财务凭证。
3. 若面向客服场景，可在会话和任务流中暴露账单摘要，帮助坐席解释费用来源和处理家属疑问。

## 与客服 / AI 应用研发方向的匹配点

| 关注点 | 项目证据 |
| --- | --- |
| 前后端全链路开发 | 静态页面、JavaScript 调用、Spring Boot REST API、SQL Server 数据模型 |
| 实时会话 | WebSocket/STOMP、消息落库、历史分页、Redis 未读数 |
| 工单与任务流 | 告警任务分配、接单、处理、关闭校验、个人任务列表 |
| 多角色协作 | 管理员、医护、家属的接口和页面隔离 |
| Agent / Tool Use | 医护 AI 助手按问题组合房间、健康、告警、照护安排等只读 Tool，并暴露 Tool Trace 与数据来源 |
| Agent 安全边界 | 医护-老人权限复核、会话上下文隔离、医疗决策拦截、外部模型默认关闭与失败降级 |

## 技术栈

- 后端：Java 21、Spring Boot 3.5、Spring Web、Spring Security、JWT
- 数据：SQL Server、MyBatis Plus、Redis
- 实时通信：WebSocket、STOMP、SockJS
- AI 应用：Tool-grounded Query Agent、会话上下文、可解释 Tool Trace、OpenAI-compatible 可选模型润色
- 工程支持：Validation、Quartz、Mail、Lombok、Hutool、FastJSON2、GitHub Actions
- 前端：HTML、CSS、JavaScript、Ajax

## 运行说明

1. 准备 Java 21、Maven 和 SQL Server。
2. 根据 `src/main/resources/application.properties` 配置数据库、Redis、JWT、邮件和地图 API 参数。
3. 执行 SQL 脚本初始化业务表。
4. 启动项目：

```powershell
.\mvnw.cmd spring-boot:run
```

5. 浏览器访问登录页后，按角色进入管理员端、医护端或家属端功能页。医护登录后可从工作台侧边栏进入 `AI 助手`。

可选模型配置见 `docs/MEDICAL_AI_ASSISTANT.md`；默认无需配置模型即可运行工具事实模式。

## 测试

原有聊天访问边界与告警任务流测试可执行：

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd '-Dtest=AlertServiceImplTest,FamilyChatControllerTest,MedicalChatControllerTest' test
```

医护 AI 助手聚焦测试：

```powershell
.\mvnw.cmd '-Dtest=MedicalAiAssistantServiceImplTest,MedicalAiAssistantControllerTest' test
```

对应 GitHub Actions 工作流还会执行 `medical-ai-assistant.js` 语法检查。

## 后续路线

1. 将医护 AI 助手的只读 Tool Registry 继续扩展到家属联系人、告警详情和医护本人排班；新增写 Tool 时增加 Human Approval、幂等与审计。
2. 从告警任务流拆出正式客服工单模块，沉淀工单状态机和操作审计。
3. 为聊天模块引入会话分配、坐席工作台、消息可靠投递和会话质检，并让 AI 助手生成可由医护确认的回复草稿。
4. 为高并发消息与任务分发接入 MQ、限流、幂等和可观测性指标。
