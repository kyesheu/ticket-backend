# v3.2 数据库设计

状态：开发中（阶段 54）

增量脚本：sql/ticket-v3.2.sql

新增 ticket_ai_feedback：

- feedback_id、ticket_id、target_type、target_id：关联 ASSIST 或 TRIAGE 结果。
- feedback_value：USEFUL、NOT_USEFUL。
- adopted：是否采纳；comment：可选短评。
- evaluator_id、create_time：评价人和时间。

唯一键覆盖 evaluator_id + target_type + target_id，避免重复反馈；索引覆盖 ticket_id、target_type 和 create_time。禁止保存完整模型输出和工单正文。

知识文档元数据仍由 Python/Elasticsearch 管理，不在 Java MySQL 建镜像表。

## 阶段 54 DDL

- 主键：`feedback_id`。
- 唯一键：`uk_ai_feedback_evaluator_target (evaluator_id, target_type, target_id)`。
- 普通索引：`idx_ai_feedback_ticket (ticket_id)`、`idx_ai_feedback_target_time (target_type, create_time)`。
- `feedback_value`、`target_type` 暂用 `varchar` 保存枚举值，由 Java 枚举和 Service 校验约束。
