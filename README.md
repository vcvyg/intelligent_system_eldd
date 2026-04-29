智慧养老管理系统｜Java / Spring Boot / SQL Server

基于 Spring Boot 3 + Java 21 开发的养老院信息管理系统，面向管理员、医护人员、家属等多角色使用场景，支持老人信息管理、健康数据记录、医护排班、请假管理、用户认证与权限控制等功能。

项目采用 Controller-Service-Repository 分层架构，结合 Entity、DTO、VO 完成数据模型拆分；使用 MyBatis Plus 操作 SQL Server 数据库，并封装统一响应结果、异常处理、参数校验和跨域配置。通过 Spring Security + JWT 实现登录认证与接口权限控制，前端通过 Ajax 携带 Token 调用后端 RESTful API。

项目中集成 Redis、WebSocket等组件，用于扩展缓存、实时消息通知和定时任务场景，提升系统在养老业务管理中的实时性与可扩展性。
