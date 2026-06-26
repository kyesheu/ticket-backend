# Ticket Backend

企业流程工单管理系统后端，基于 RuoYi-Vue Spring Boot 3 二次开发。

> v1.0 已完成 ✅ | 2026-06-26

## 模块结构

```
├── ruoyi-admin/          # 启动入口
├── ruoyi-framework/      # Security, JWT, AOP
├── ruoyi-system/         # RBAC 业务
├── ruoyi-common/         # BaseEntity, 工具类
├── ruoyi-quartz/         # 定时任务
├── ruoyi-generator/      # 代码生成
├── ruoyi-ticket/         # ★ 工单模块（32 个 Java 文件 + 4 个 Mapper XML）
│   ├── src/main/java/    # Controller / Service / Mapper / Domain / DTO / VO / Enum
│   ├── src/main/resources/mapper/  # MyBatis XML
│   └── src/test/         # 34 个单元测试
├── docs/                 # 设计文档
├── scripts/ticket/       # 接口冒烟测试（PS / Bash / Postman）
├── sql/                  # 建表 SQL
└── .github/workflows/    # CI/CD
```

## 技术栈

| 组件 | 版本 |
|---|---|
| JDK | 21 |
| Spring Boot | 3.5 |
| MyBatis | 3.0 |
| MySQL | 8.0 |
| Redis | 7.x |

## 工单状态流转

```
NEW ──分派──▶ PROCESSING ──处理──▶ WAIT_CONFIRM ──确认──▶ CLOSED
 │                 │
 └──取消───────────┘
```

## 快速开始

```bash
# 1. 配置 .env（复制 .env.example 修改）
cp .env.example .env

# 2. 初始化数据库
mysql -u root -p < sql/ry_20260417.sql      # RuoYi 基础表
mysql -u root -p < sql/ticket-v1.0.sql      # ticket 业务表

# 3. 编译
mvn clean compile

# 4. 运行测试
mvn test

# 5. 启动
# IDE 运行 ruoyi-admin/RuoYiApplication
```

- 应用：`http://localhost:8080`
- Swagger：`http://localhost:8080/swagger-ui.html`
- 默认管理员：`admin` / `admin123`

## 测试

```bash
mvn test                                    # 34 个单元测试
powershell scripts/ticket/smoke-test.ps1    # 接口冒烟测试
```

导入 Postman：`scripts/ticket/Ticket-API.postman_collection.json`

## v1.0 功能

工单创建、分派、处理、确认、取消、分类树、评论、操作日志、权限控制。

详见 [docs/01-project-spec.md](docs/01-project-spec.md)。

## 设计文档

| 文档 | 内容 |
|---|---|
| [01-project-spec.md](docs/01-project-spec.md) | 项目边界、功能范围、验收标准 |
| [02-architecture-design.md](docs/02-architecture-design.md) | 架构、分层规范、枚举设计 |
| [03-database-design.md](docs/03-database-design.md) | 4 张表 DDL、索引 |
| [04-implementation-plan.md](docs/04-implementation-plan.md) | 8 阶段实施计划 |
| [05-test-release.md](docs/05-test-release.md) | 测试清单、Bug 修复记录 |

## 致谢

基于 [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue) Spring Boot 3 分支构建。
