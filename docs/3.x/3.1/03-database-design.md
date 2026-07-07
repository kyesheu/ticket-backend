# v3.1 数据库设计

状态：完成

增量脚本：sql/ticket-v3.1.sql

新增 ticket_ai_triage_suggestion：

- suggestion_id、ticket_id、ticket_updated_at：标识建议及生成时工单版本。
- suggested_category_id、suggested_priority、suggested_assignee_id：结构化建议。
- confidence、reason_summary、source_refs：置信度、短理由和来源引用。
- suggestion_status：PENDING、APPLIED、REJECTED、EXPIRED。
- final_category_id、final_priority、final_assignee_id：用户最终选择。
- operated_by、operated_at、create_time：审计字段。

索引覆盖 ticket_id + create_time、suggestion_status + create_time。记录不保存完整工单内容、提示词和密钥。
