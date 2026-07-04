# Ticket Backend

企业流程工单管理系统后端，基于 RuoYi-Vue Spring Boot 3 二次开发。

## 版本状态

| 版本 | 功能 | 状态 |
|---|---|---|
| v1.0 | 工单主流程、分类、评论、操作日志 | ✅ |
| v1.1 | SLA 时效管理、超时告警 | ✅ |
| v1.2 | 站内通知、满意度评价 | ✅ |
| v1.3 | 部门级数据权限 | ✅ |
| v2.0 | 自研动态流程引擎 | ✅ |
| v2.1 | 自定义字段 | ✅ |
| v2.2 | 附件管理 | ✅ |
| v2.3 | Elasticsearch 全文检索 | ✅ |
| v3.0 | Python LangChain 知识库 RAG 与工单辅助 | 🚧 |

## 模块结构

```
├── ruoyi-admin/          # 启动入口
├── ruoyi-framework/      # Security, JWT, AOP
├── ruoyi-system/         # RBAC 业务
├── ruoyi-common/         # BaseEntity, 工具类
├── ruoyi-quartz/         # 定时任务
├── ruoyi-generator/      # 代码生成
├── ruoyi-ticket/         # ★ 工单模块
│   ├── src/main/java/    # Controller / Service / Mapper / Domain / DTO / VO / Enum
│   ├── src/main/resources/mapper/  # MyBatis XML
│   └── src/test/         # 单元测试
├── ai-service/           # ★ Python FastAPI 智能辅助服务（v3.0）
├── docs/                 # 设计文档
├── scripts/ticket/       # 接口冒烟测试
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
| Elasticsearch | 8.x |
| Python / FastAPI | 3.x（v3.0 ai-service） |

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
mvn test                                    # 单元测试
powershell scripts/ticket/smoke-test.ps1    # 接口冒烟测试
```

导入 Postman：`scripts/ticket/Ticket-API.postman_collection.json`

## 设计文档

| 文档 | 内容 |
|---|---|
| [文档索引](docs/README.md) | 版本入口 |
| [1.x 文档](docs/1.x/) | 工单主流程、SLA、通知评价、部门数据权限 |
| [2.x 文档](docs/2.x/) | 动态流程、自定义字段、附件、ES 检索 |
| [3.x 文档](docs/3.x/) | Python LangChain RAG 与工单辅助 |
