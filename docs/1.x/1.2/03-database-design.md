# v1.2 数据库设计

状态：已完成

增量脚本：sql/ticket-v1.2.sql

| 表 | 关键字段 | 关键索引 |
|---|---|---|
| ticket_notification | ticket_id、recipient_id、notification_type、event_key、read_status、read_time | uk_recipient_event、idx_recipient_read、idx_ticket_id |
| ticket_satisfaction | ticket_id、evaluator_id、score、content | uk_ticket_id、idx_evaluator_id、idx_create_time |

score 限制为 1–5。通知和评价不物理删除。
