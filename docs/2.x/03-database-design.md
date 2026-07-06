# 03 — 数据库设计

> 2.x | 2026-07-04 | 状态: ✅ 已完成

## v2.0 动态流程表

v2.0 使用独立增量脚本 `sql/ticket-v2.0.sql`，不得修改已发布脚本。`ticket_category` 新增可空字段
`workflow_key VARCHAR(64)`；`ticket` 不增加可变当前节点字段，运行状态以实例表为准。

### ticket_workflow_definition

主要字段：`definition_id`、`workflow_key`、`workflow_name`、`version`、`definition_status`、
`is_current`、BaseEntity 审计字段。状态为 `DRAFT / PUBLISHED / DISABLED`。

索引：`uk_workflow_version(workflow_key, version)`、
`idx_workflow_current(workflow_key, is_current, definition_status)`。

### ticket_workflow_node

主要字段：`node_id`、`definition_id`、`node_key`、`node_name`、`node_type`、`assignee_type`、
`assignee_value`、`sort_order`。节点类型为 `START / ASSIGN / PROCESS / CONFIRM / END`，开始和结束节点不配置处理人。

索引：`uk_definition_node(definition_id, node_key)`、`idx_node_definition(definition_id)`。

### ticket_workflow_transition

主要字段：`transition_id`、`definition_id`、`source_node_key`、`target_node_key`、`condition_field`、
`condition_operator`、`condition_value`、`is_default`、`sort_order`。条件字段只允许 `PRIORITY / CATEGORY / CREATOR_DEPT`，
运算符只允许 `EQ / IN`；默认连线的条件字段和值为空。

索引：`idx_transition_source(definition_id, source_node_key, sort_order)`。

### ticket_workflow_instance

主要字段：`instance_id`、`ticket_id`、`definition_id`、`workflow_status`、`current_node_key`、
`started_at`、`ended_at`、`create_time`、`update_time`。状态为 `RUNNING / COMPLETED / CANCELLED / TERMINATED`。

索引：`uk_instance_ticket(ticket_id)`、`idx_instance_status(workflow_status, update_time)`。

### ticket_workflow_task

主要字段：`task_id`、`instance_id`、`node_key`、`node_name`、`task_status`、`assignee_type`、
`assignee_ref_id`、`resolved_assignee_id`、`completed_by`、`action_type`、`comment`、`created_at`、`completed_at`。

索引：`idx_task_instance(instance_id, created_at)`、
`idx_task_user(resolved_assignee_id, task_status)`、`idx_task_role(assignee_ref_id, task_status)`。
实例最多一个待办的约束由事务和任务条件更新共同保证；MySQL 8.0 不使用依赖状态值的伪唯一索引。

## v2.1 自定义字段表

v2.1 使用独立增量脚本 `sql/ticket-v2.1.sql`，新增 3 张表，并为 `ticket_workflow_transition` 增加可空字段 `condition_key VARCHAR(64)`。

### ticket_custom_field_definition

主要字段：`field_id`、`category_id`、`field_key`、`field_name`、`field_type`、`required_flag`、`default_value`、`max_length`、`min_number`、`max_number`、`sort_order`、`status`、BaseEntity 审计字段。

索引：`uk_category_field_key(category_id, field_key)`、`idx_field_category(category_id, status, sort_order)`。

### ticket_custom_field_option

主要字段：`option_id`、`field_id`、`option_value`、`option_label`、`sort_order`、`status`、BaseEntity 审计字段。

索引：`uk_field_option_value(field_id, option_value)`、`idx_option_field(field_id, status, sort_order)`。

### ticket_custom_field_value

主要字段：`value_id`、`ticket_id`、`field_id`、`field_key_snapshot`、`field_name_snapshot`、`field_type_snapshot`、`normalized_value`、`display_value_snapshot`、`sort_order_snapshot`、`create_time`。

索引：`uk_ticket_field(ticket_id, field_id)`、`idx_value_ticket(ticket_id, sort_order_snapshot)`、`idx_value_field(field_id)`。值记录不可修改和删除。

## v2.3 检索事件表

v2.3 使用独立增量脚本 `sql/ticket-v2.3.sql`，只新增事务事件表和菜单权限，不修改已发布脚本。Elasticsearch 索引是可重建投影，不替代 MySQL 表。

### ticket_search_event

主要字段：`event_id`、`ticket_id`、`event_type`、`event_status`、`retry_count`、`next_retry_at`、`claimed_at`、`processed_at`、`error_message`、`create_time`、`update_time`。

- `event_type`：`UPSERT / DELETE / REBUILD`；工单当前只物理保留，常规业务使用 `UPSERT`。
- `event_status`：`PENDING / PROCESSING / SUCCEEDED / FAILED`。

- `error_message` 只保存截断后的脱敏摘要，不保存请求、凭据或完整 ES 响应。
- Worker 崩溃后，超过 claim 超时的 `PROCESSING` 事件可重新置为 `PENDING`。

索引：`PRIMARY(event_id)`、`idx_event_dispatch(event_status, next_retry_at, event_id)`、`idx_event_ticket(ticket_id, event_id)`、`idx_event_claim(event_status, claimed_at)`。

不对 `ticket_id` 建唯一约束：同一事务可合并事件，但并发事务必须各自留下事实记录，由消费者按 `event_id` 保证最终顺序。

