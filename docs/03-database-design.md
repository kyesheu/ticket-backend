# 03 — 数据库设计

> v2.3 | 2026-07-04 | MySQL 8.0 + InnoDB

## 设计原则

- 表名/列名全小写下划线，字符集 `utf8mb4`，引擎 `InnoDB`
- 主键 `BIGINT AUTO_INCREMENT`，时间用 `DATETIME`，枚举存 `VARCHAR`
- `del_flag` 遵循若依约定：`0` 存在，`2` 删除
- 不建物理外键，一致性由 Service 层校验

## 复用的若依表

| 表 | ticket 使用方式 |
|---|---|
| `sys_user` | 工单存 `creator_id`/`assignee_id`，展示时 LEFT JOIN 取 `nick_name`/`user_name` |
| `sys_dept` | 工单存 `dept_id`，展示时 LEFT JOIN 取 `dept_name` |
| `sys_role` / `sys_menu` | 权限校验走 RuoYi `@PreAuthorize`，ticket 不直查 |

## 新增表关系

```
ticket ──N:1──▶ ticket_category
ticket ──1:N──▶ ticket_comment
ticket ──1:N──▶ ticket_operation_log
ticket ──N:1──▶ ticket_sla_policy（创建时读取，不保存外键）
ticket ──1:N──▶ ticket_sla_alert
```

## ticket — 工单主表

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `ticket_id` | BIGINT | Y | AUTO_INCREMENT | 主键 |
| `ticket_no` | VARCHAR(32) | Y | | `TK202606250001`，唯一 |
| `title` | VARCHAR(200) | Y | | 标题 |
| `content` | TEXT | N | NULL | 内容 |
| `category_id` | BIGINT | N | NULL | → `ticket_category.category_id` |
| `priority` | VARCHAR(10) | Y | `MEDIUM` | LOW / MEDIUM / HIGH / URGENT |
| `status` | VARCHAR(20) | Y | `NEW` | NEW / PROCESSING / WAIT_CONFIRM / CLOSED / CANCELLED |
| `creator_id` | BIGINT | Y | | → `sys_user.user_id` |
| `assignee_id` | BIGINT | N | NULL | → `sys_user.user_id` |
| `dept_id` | BIGINT | Y | | → `sys_dept.dept_id` |
| `processed_at` | DATETIME | N | NULL | 首次进入 PROCESSING 的时间 |
| `closed_at` | DATETIME | N | NULL | 进入 CLOSED 的时间 |
| `response_due_at` | DATETIME | N | NULL | 首次响应截止时间快照 |
| `resolve_due_at` | DATETIME | N | NULL | 解决截止时间快照 |
| `response_overdue` | CHAR(1) | Y | `0` | `0` 未超时，`1` 已超时 |
| `resolve_overdue` | CHAR(1) | Y | `0` | `0` 未超时，`1` 已超时 |
| `del_flag` | CHAR(1) | Y | `0` | `0` 存在 `2` 删除 |
| `create_by` | VARCHAR(64) | N | `''` | BaseEntity 审计字段 |
| `create_time` | DATETIME | N | NULL | BaseEntity 审计字段 |
| `update_by` | VARCHAR(64) | N | `''` | BaseEntity 审计字段 |
| `update_time` | DATETIME | N | NULL | BaseEntity 审计字段 |
| `remark` | VARCHAR(500) | N | NULL | BaseEntity 审计字段 |

索引：`PRIMARY(ticket_id)` / `uk_ticket_no` / `idx_status` / `idx_creator_id` / `idx_assignee_id` / `idx_category_id` / `idx_create_time` / `idx_response_scan(response_overdue, response_due_at, status)` / `idx_resolve_scan(resolve_overdue, resolve_due_at, status)`

查询用户名/部门名的 SQL 模式：

```sql
SELECT t.*, u.nick_name AS creator_name, a.nick_name AS assignee_name, d.dept_name
FROM ticket t
LEFT JOIN sys_user u ON u.user_id = t.creator_id
LEFT JOIN sys_user a ON a.user_id = t.assignee_id
LEFT JOIN sys_dept d ON d.dept_id = t.dept_id
WHERE t.del_flag = '0'
```

## ticket_category — 工单分类表

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `category_id` | BIGINT | Y | AUTO_INCREMENT | 主键 |
| `parent_id` | BIGINT | N | `0` | 父分类，`0` 为根 |
| `category_name` | VARCHAR(50) | Y | | 分类名 |
| `ancestors` | VARCHAR(500) | N | `''` | 祖级路径 `0,1,5` |
| `order_num` | INT | N | `0` | 排序 |
| `status` | CHAR(1) | N | `0` | `0` 正常 `1` 停用 |
| `del_flag` | CHAR(1) | Y | `0` | `0` 存在 `2` 删除 |
| + BaseEntity 审计字段 | | | | |

索引：`PRIMARY(category_id)` / `idx_parent_id`

## ticket_comment — 工单评论表

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `comment_id` | BIGINT | Y | AUTO_INCREMENT | 主键 |
| `ticket_id` | BIGINT | Y | | → `ticket.ticket_id` |
| `user_id` | BIGINT | N | NULL | → `sys_user.user_id` |
| `content` | TEXT | Y | | 评论内容 |
| `comment_type` | VARCHAR(10) | N | `EXTERNAL` | INTERNAL 内部备注 / EXTERNAL 公开评论 |
| `del_flag` | CHAR(1) | Y | `0` | `0` 存在 `2` 删除 |
| + BaseEntity 审计字段 | | | | |

索引：`PRIMARY(comment_id)` / `idx_ticket_id`

## ticket_operation_log — 工单操作日志表

> 不继承 BaseEntity —— 审计日志不可变，不可修改、不可删除。

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `log_id` | BIGINT | Y | AUTO_INCREMENT | 主键 |
| `ticket_id` | BIGINT | Y | | → `ticket.ticket_id` |
| `operation_type` | VARCHAR(20) | Y | | CREATE / ASSIGN / PROCESS / CONFIRM / CANCEL |
| `from_status` | VARCHAR(20) | N | NULL | 变更前状态，创建时为 NULL |
| `to_status` | VARCHAR(20) | N | NULL | 变更后状态 |
| `operator_id` | BIGINT | N | NULL | → `sys_user.user_id` |
| `operator_name` | VARCHAR(30) | N | NULL | 冗余存储，方便查询 |
| `comment` | TEXT | N | NULL | 操作备注 |
| `operate_time` | DATETIME | N | NULL | 操作时间 |

索引：`PRIMARY(log_id)` / `idx_ticket_id` / `idx_operate_time`

## 完整建表 SQL

v1.0 基线见 `sql/ticket-v1.0.sql`。v1.1 实现阶段新增 `sql/ticket-v1.1.sql`，只包含增量 DDL、默认策略、Quartz 任务和菜单权限，禁止修改已发布基线脚本。

## ticket_sla_policy — SLA 策略表

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `policy_id` | BIGINT | Y | AUTO_INCREMENT | 主键 |
| `priority` | VARCHAR(10) | Y | | LOW / MEDIUM / HIGH / URGENT，唯一 |
| `response_minutes` | INT | Y | | 首次响应时限，正整数 |
| `resolve_minutes` | INT | Y | | 解决时限，必须大于响应时限 |
| `status` | CHAR(1) | Y | `0` | `0` 启用，`1` 停用 |
| `create_by` | VARCHAR(64) | N | `''` | 创建人 |
| `create_time` | DATETIME | N | NULL | 创建时间 |
| `update_by` | VARCHAR(64) | N | `''` | 更新人 |
| `update_time` | DATETIME | N | NULL | 更新时间 |
| `remark` | VARCHAR(500) | N | NULL | 备注 |

索引：`PRIMARY(policy_id)` / `uk_priority(priority)`。

默认策略：`LOW=480/4320`、`MEDIUM=240/1440`、`HIGH=60/480`、`URGENT=15/120`，单位均为分钟。

## ticket_sla_alert — SLA 告警表

> 告警是不可变事实记录，不继承 `BaseEntity`，不提供修改和删除接口。

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `alert_id` | BIGINT | Y | AUTO_INCREMENT | 主键 |
| `ticket_id` | BIGINT | Y | | 工单 ID |
| `alert_type` | VARCHAR(30) | Y | | RESPONSE_OVERDUE / RESOLUTION_OVERDUE |
| `due_at` | DATETIME | Y | | 触发告警的截止时间快照 |
| `detected_at` | DATETIME | Y | | 扫描发现超时的时间 |
| `overdue_minutes` | INT | Y | | 发现时已超时分钟数，非负 |

索引：`PRIMARY(alert_id)` / `uk_ticket_alert_type(ticket_id, alert_type)` / `idx_detected_at(detected_at)` / `idx_alert_type(alert_type)`。

## v1.1 数据迁移

- 新字段先允许 `NULL`，保证在线执行增量 DDL 时兼容已有数据。
- 插入四条默认策略后，根据已有工单的 `priority` 和 `create_time` 回填两个截止时间。
- 对已有工单按 `processed_at`、`closed_at` 和截止时间初始化超时标记；`CANCELLED` 保持未超时。
- 回填不生成历史告警，由首次扫描为仍活动且已超时的工单生成告警。

## v1.2 新增表

`ticket_notification`：`notification_id`、`ticket_id`、`recipient_id`、
`notification_type`、`event_key`、`title`、`content`、`read_status`、`read_time`、`create_time`。

索引：`uk_recipient_event(recipient_id, event_key)`、
`idx_recipient_read(recipient_id, read_status, create_time)`、`idx_ticket_id(ticket_id)`。

`ticket_satisfaction`：`satisfaction_id`、`ticket_id`、`evaluator_id`、`score`、`content`、`create_time`。

索引：`uk_ticket_id(ticket_id)`、`idx_evaluator_id(evaluator_id)`、`idx_create_time(create_time)`。
评分使用 `TINYINT` 并限制 1–5。v1.2 使用独立增量脚本 `sql/ticket-v1.2.sql`。

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
