# v3.0 数据库设计

状态：完成

本版本不新增 Java MySQL 表。

- MySQL 继续作为工单事实唯一来源。
- Python 在独立 Elasticsearch 索引保存知识文档切片和已关闭工单脱敏快照。
- 向量记录至少包含 source_type、source_id、title、chunk_index、content、content_hash 和检索元数据。
- source_type 仅允许 DOCUMENT、CLOSED_TICKET。
- 同一来源按稳定 ID 和内容摘要幂等覆盖，失败不能暴露半成品代次。
- 密钥不得写入数据库、索引元数据或日志。
