# v3.2 数据库设计

状态：规划

增量脚本：sql/ticket-v3.2.sql

新增 ticket_ai_feedback：

- feedback_id、ticket_id、target_type、target_id：关联 ASSIST 或 TRIAGE 结果。
- feedback_value：USEFUL、NOT_USEFUL。
- adopted：是否采纳；comment：可选短评。
- evaluator_id、create_time：评价人和时间。

唯一键覆盖 evaluator_id + target_type + target_id，避免重复反馈；索引覆盖 ticket_id、target_type 和 create_time。禁止保存完整模型输出和工单正文。

知识文档元数据仍由 Python/Elasticsearch 管理，不在 Java MySQL 建镜像表。
