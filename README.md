# Ticket Backend

企业流程工单管理系统后端，基于 RuoYi-Vue Spring Boot 3 二次开发。

> v1.0 状态：设计完成，待实现

---

## 项目简介

在 RuoYi 通用后台管理框架之上新增 `ruoyi-ticket` 工单模块，提供工单创建、分派、处理、确认、关闭/取消的主流程闭环，附带分类管理、评论沟通和操作审计。

RuoYi 负责登录认证、RBAC 权限、操作日志、接口文档等基础能力；ticket 模块负责工单业务。必要时可对基础模块做小范围调整。

---

## 技术栈

| 组件 | 版本 |
|---|---|
| JDK | 21 |
| Spring Boot | 3.5 |
| MyBatis | 3.0 |
| MySQL | 8.0 |
| Redis | 7.x（仅用于 Token 持久化） |
| Spring Security + JWT | RuoYi 内置 |

---

## 模块结构

```
ticket-backend/
├── ruoyi-admin/          # 启动入口（只改 pom.xml 加依赖）
├── ruoyi-framework/      # Security, JWT, AOP, Config
├── ruoyi-system/         # RBAC 业务
├── ruoyi-common/         # BaseEntity, 工具类, 注解
├── ruoyi-quartz/         # 定时任务
├── ruoyi-generator/      # 代码生成
├── ruoyi-ticket/         # ✨ 新增 工单模块
│   └── src/main/java/com/ruoyi/ticket/
│       ├── controller/   # Ticket / TicketCategory / TicketComment Controller
│       ├── service/      # Service 接口 + Impl
│       ├── mapper/       # MyBatis Mapper 接口 + XML
│       ├── domain/       # Ticket / TicketCategory / TicketComment / TicketOperationLog
│       ├── dto/          # 请求体 DTO
│       ├── vo/           # 响应体 VO
│       └── enums/        # TicketStatus / TicketPriority / TicketOperationType
└── docs/                 # 设计文档
    ├── 01-project-spec.md
    ├── 02-architecture-design.md
    ├── 03-database-design.md
    ├── 04-implementation-plan.md
    ├── 05-test-release.md
    └── CONTEXT.md
```

---

## 工单状态流转

```
NEW ──分派──▶ PROCESSING ──完成处理──▶ WAIT_CONFIRM ──确认──▶ CLOSED
 │                 │
 └──取消───────────┘
```

- `CLOSED` / `CANCELLED` 为终态
- 流转规则由 `TicketStatus` 枚举内部控制，Service 层统一校验
- 每次流转写入 `ticket_operation_log`

---

## 快速开始

### 环境要求

- JDK 21
- Maven 3.9+
- MySQL 8.0+
- Redis 7.x

### 启动步骤

```bash
# 1. 创建数据库并执行 RuoYi 初始化 SQL（sys_* 表）
# 2. 执行 docs/03-database-design.md §8 的 ticket 建表 SQL
# 3. 修改 ruoyi-admin/src/main/resources/application-druid.yml 数据库连接
# 4. 修改 ruoyi-admin/src/main/resources/application.yml 中 Redis 连接
# 5. 编译启动
mvn clean compile
# 在 IDE 中运行 ruoyi-admin 的 RuoYiApplication
```

### 访问

- 应用：`http://localhost:8080`
- Swagger：`http://localhost:8080/swagger-ui.html`
- 默认管理员：`admin` / `admin123`

---

## v1.0 功能范围

| 功能 | 说明 |
|---|---|
| 工单创建 | 标题、内容、分类、优先级 |
| 工单列表 | 分页 + 状态/优先级/分类/关键词/日期筛选 |
| 工单详情 | 含分类名、创建人/指派人昵称、部门、评论、操作日志 |
| 工单分派 | 管理员分配处理人，NEW → PROCESSING |
| 工单处理 | 指派人提交处理结果，PROCESSING → WAIT_CONFIRM |
| 工单确认 | 创建人确认关闭，WAIT_CONFIRM → CLOSED |
| 工单取消 | 创建人或管理员取消，NEW/PROCESSING → CANCELLED |
| 评论 | 添加/查看，INTERNAL 内部备注 / EXTERNAL 公开评论 |
| 操作日志 | 每次流转记录 who / from / to / comment |

### v1.0 不做什么

不接 Python / RAG / Agent / RabbitMQ / Flowable / 微服务。不做 SLA / 满意度 / 附件 / 通知 / 模板 / ES。

详见 [docs/01-project-spec.md](docs/01-project-spec.md)。

---

## 设计文档

| 文档 | 内容 |
|---|---|
| [01-project-spec.md](docs/01-project-spec.md) | 项目规格、功能范围、关键决策、验收标准 |
| [02-architecture-design.md](docs/02-architecture-design.md) | 架构图、分层规范、Controller/Service/Mapper/Enum 规范 |
| [03-database-design.md](docs/03-database-design.md) | 4 张业务表 DDL、字段说明、索引、与若依表关系 |
| [04-implementation-plan.md](docs/04-implementation-plan.md) | 8 阶段实施计划，每阶段含目标/产出/验收 |
| [05-test-release.md](docs/05-test-release.md) | 接口测试清单、状态流转/权限/日志测试、发布检查 |

---

## 致谢

本项目基于 [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue) Spring Boot 3 分支构建。感谢若依开源社区。
