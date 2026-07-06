# v2.3 架构设计

状态：已完成

## 边界

- ITicketSearchService：查询、游标和权限复核。
- ITicketSearchIndexer：MySQL 投影、单工单索引和全量重建。
- 调度器：抢占 PENDING 事件、恢复超时 PROCESSING、退避重试和失败补偿。

## 安全与一致性

- 事件与业务修改同事务提交；索引写入异步执行。
- 索引文档只保存检索快照，不保存附件路径和操作日志。
- 固定别名 ticket-search 指向版本化物理索引。
- 返回 opaque 游标和 hasMore，不暴露原始总命中数。
- 高亮字段白名单、长度受限并转义 HTML。
