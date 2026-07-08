# v3.3 架构设计

状态：完成

## 部署单元

- Java：ticket-admin，承载 Ticket 和 ticket-ticket。
- Python：ai-service，仅提供内部版本化 HTTP API。
- 数据：MySQL、Redis、Elasticsearch；模型和 Embedding 为外部依赖。

## 运行约束

- Java 和 Python 分别提供 liveness、readiness；readiness 区分必需和可降级依赖。
- 所有跨服务请求传递 traceId，日志结构化且不记录敏感正文。
- 超时、限流、重试和响应上限均可配置并有安全默认值。
- MySQL 恢复后可重放检索事件；知识索引和历史工单索引有明确重建流程。
- 发布采用可回滚制品和向后兼容数据库迁移；禁止依赖手工修改生产数据。

## 阶段 58 观测契约

- Java 通过 Spring Boot Actuator 暴露 `/actuator/health/liveness`、`/actuator/health/readiness`、`/actuator/metrics` 和 `/actuator/prometheus`。
- `/actuator/health/**` 允许匿名访问，用于容器探针；metrics 和 prometheus 默认仍受系统鉴权保护，生产环境应由内网采集器访问。
- MySQL、Redis 属于必需依赖，由 Spring/Druid/Redis 健康检查决定 readiness。
- `ticketAi`、`ticketSearch` 属于可降级依赖，健康明细返回 `required=false` 和 `UP/DEGRADED/DISABLED`，不因 AI 或检索故障阻断普通工单流程。
- Java 入口过滤器读取或生成 `X-Trace-Id`，写入响应头和 MDC；日志格式包含 `traceId=` 字段。
- Java 调 Python AI 服务时透传 `X-Trace-Id`；Python API 回显该响应头，缺失时生成新值。
