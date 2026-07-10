# Ticket Backend

企业工单与知识库智能助手平台后端，面向企业内部 IT 支持、客服和售后场景。员工可先通过 AI 检索知识库获得参考答案；无法可靠解决时可一键转人工，系统自动创建工单并按规则分派处理人。处理完成后，可将有效方案沉淀回知识库，形成 AI 自助问答、人工处理和知识复用的闭环。

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
| v3.0 | Python LangChain 知识库 RAG 与工单辅助 | ✅ |
| v3.1 | 受控 AI 分诊 | ✅ |
| v3.2 | AI 运营与评测闭环 | ✅ |
| v3.3 | 生产化收尾与最终交付 | ✅ |
| v4.0 | AI 前置问答、转人工、自动分派、处理人工作台与知识沉淀 | ✅ |

> **项目状态：v4.0 已完成，进入维护阶段。**

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
├── ai-service/           # ★ Python FastAPI 智能辅助服务
├── docs/                 # 设计文档
├── scripts/ticket/       # 按 v1.x、v2.x、v3.x 分类的接口冒烟测试
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
| RabbitMQ | 3.13 |
| Python / FastAPI | 3.x（ai-service） |
| LangChain | 1.x |
| Embedding / LLM API | DashScope compatible API |

## 核心流程

```text
知识库文档导入
  -> AI 智能问答
  -> 用户一键转人工
  -> 自动创建工单与分派
  -> 处理人工作台处理
  -> 关闭工单并沉淀知识
```

AI 服务只负责检索、理解和生成答案或建议；工单创建、分派、处理等写操作统一由 Java 后端完成权限校验、参数校验、操作审计和数据落库。

## 工单状态流转

```
NEW ──分派──▶ PROCESSING ──处理──▶ WAIT_CONFIRM ──确认──▶ CLOSED
 │                 │
 └──取消───────────┘
```

## 快速开始

```bash
# 1. 配置 .env（Windows）
copy .env.example .env

# 2. 启动 MySQL、Redis、Elasticsearch
# MySQL、Redis 和 Elasticsearch 由本地 Docker 环境提供。

# 3. 初始化数据库
# 按 sql/README.md 中的顺序执行基础表和 ticket-v1.0.sql 至 ticket-v4.0.sql。

# 4. 启动后端、Python AI 服务和本地 RabbitMQ
start.bat
```

`start.bat` 会读取根目录 `.env`，启动或复用 Python AI 服务，并创建或启动 `ticket-rabbitmq` Docker 容器。MySQL、Redis 和 Elasticsearch 需要预先启动。

本地地址：

- 应用：`http://localhost:8080`
- Swagger：`http://localhost:8080/swagger-ui.html`
- RabbitMQ 管理台：`http://localhost:15672`
- 默认管理员：`admin` / `admin123`

停止 Java 和 Python 服务：

```bash
stop.bat
```

## 测试

```bash
mvn -pl ruoyi-ticket -am -Dmaven.test.skip=true package
ai-service\.venv\Scripts\python.exe -m pytest ai-service\tests -q
powershell scripts/ticket/v3.x/smoke-test.ps1
```

导入 Postman：`scripts/ticket/v1.x/Ticket-API.postman_collection.json`

## 设计文档

| 文档 | 内容 |
|---|---|
| [文档索引](docs/README.md) | 版本入口 |
| [1.x 文档](docs/1.x/) | 工单主流程、SLA、通知评价、部门数据权限 |
| [2.x 文档](docs/2.x/) | 动态流程、自定义字段、附件、ES 检索 |
| [3.x 文档](docs/3.x/) | AI 知识库、分诊、运营闭环与生产化收尾 |
| [4.0 文档](docs/4.x/4.0/) | AI 前置问答、转人工、自动分派、工作台与知识沉淀 |
