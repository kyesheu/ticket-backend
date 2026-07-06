# v1.1 数据库设计

状态：已完成

增量脚本：sql/ticket-v1.1.sql

## 变更

- ticket 增加 response_due_at、resolve_due_at、response_overdue、resolve_overdue。
- ticket_sla_policy：priority 唯一；response_minutes > 0；resolve_minutes > response_minutes；支持启停。
- ticket_sla_alert：记录告警类型、截止时间、发现时间和超时分钟数。

关键索引：uk_priority(priority)、uk_ticket_alert_type(ticket_id, alert_type)、idx_detected_at(detected_at)。迁移先允许新增字段为空，再回填存量工单；历史回填不补造告警。
