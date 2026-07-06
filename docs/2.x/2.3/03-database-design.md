# v2.3 数据库设计

状态：已完成

增量脚本：sql/ticket-v2.3.sql

ticket_search_event 保存 event_id、ticket_id、event_type、event_status、retry_count、next_retry_at、claimed_at、processed_at、error_message 和审计时间。

- event_type：UPSERT、DELETE、REBUILD。
- event_status：PENDING、PROCESSING、SUCCEEDED、FAILED。
- error_message 只保存截断脱敏摘要。
- 同一 ticket 可有多条事件，消费者按 event_id 保证新事件覆盖旧事件。

索引覆盖待调度事件、工单事件序列和超时 claim 恢复。
