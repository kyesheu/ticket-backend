# 03 — 数据库设计

> v1.1 | 2026-07-02 | MySQL 8.0 + InnoDB

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
