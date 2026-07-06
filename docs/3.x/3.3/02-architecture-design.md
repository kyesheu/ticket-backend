# v3.3 架构设计

状态：规划

## 部署单元

- Java：ruoyi-admin，承载 RuoYi 和 ruoyi-ticket。
- Python：ai-service，仅提供内部版本化 HTTP API。
- 数据：MySQL、Redis、Elasticsearch；模型和 Embedding 为外部依赖。

## 运行约束

- Java 和 Python 分别提供 liveness、readiness；readiness 区分必需和可降级依赖。
- 所有跨服务请求传递 traceId，日志结构化且不记录敏感正文。
- 超时、限流、重试和响应上限均可配置并有安全默认值。
- MySQL 恢复后可重放检索事件；知识索引和历史工单索引有明确重建流程。
- 发布采用可回滚制品和向后兼容数据库迁移；禁止依赖手工修改生产数据。
