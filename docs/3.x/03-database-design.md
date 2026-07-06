# 03 — 数据库设计

> 3.x | 2026-07-04 | 状态: 🚧 进行中

## v3.0 数据归属（规划）

- v3.0 第一版不新增 Java 工单业务表，不创建 `sql/ticket-v3.0.sql`。
- Python AI 服务拥有知识文档元数据、切片和向量索引；其存储结构在 `ai-service/` 内独立管理，Java Mapper 不访问这些数据。
- 向量记录至少包含 `source_type`、`source_id`、`title`、`chunk_index`、`content`、`content_hash` 和必要的检索元数据。
- `source_type` 第一版只允许 `DOCUMENT / CLOSED_TICKET`；同一来源重复导入按稳定来源 ID 与内容摘要幂等覆盖。
- 历史工单事实仍以 Java MySQL 为唯一来源，Python 中只保存用于相似检索的脱敏快照，可随时重建。
- 模型、Embedding 和服务认证密钥不写入任何数据库或向量元数据。
