# v3.3 数据库设计

状态：完成

本版本不新增业务表。

## 收尾要求

- 审查 v1.0–v3.2 全部增量 SQL 的执行顺序、幂等失败保护和回滚边界。
- 记录 MySQL 备份、恢复和校验步骤；恢复后执行核心表计数与抽样一致性检查。
- Elasticsearch 视为可重建投影，不纳入业务事实备份；知识源无法重建的部分必须单独备份。
- 明确事件表、操作日志、通知和 AI 记录的保留与归档策略。
- 使用 EXPLAIN 检查核心列表、待办、事件调度和统计查询索引。

## 阶段 59 备份恢复边界

- MySQL 是业务事实源，备份使用 `mysqldump --single-transaction --routines --triggers`。
- 恢复演练不得覆盖当前业务库；统一恢复到 `ticket_backend_restore_<timestamp>` 新库，再比对核心表计数。
- 核心表计数至少覆盖：`ticket`、`ticket_comment`、`ticket_operation_log`、`ticket_notification`、`ticket_search_event`、`ticket_ai_triage_suggestion`、`ticket_ai_feedback`。
- Elasticsearch 工单索引是 MySQL 投影，通过 `/ticket/search/rebuild` 重建；重建后校验 `ticket-search` alias 只指向一个版本化索引，并校验 `_count` 与重建进度一致。
- 失败事件补偿演练只插入一条 smoke `FAILED` 检索事件，并调用 `/ticket/search/events/retry` 验证事件重置为 `PENDING`。
- AI 降级演练只校验 Java readiness 暴露 `ticketAi` 可降级依赖状态；停止外部服务必须由执行人显式操作，不由脚本默认执行。
