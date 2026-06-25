# 03 — 数据库设计

> v1.0 | 2026-06-25 | MySQL 8.0 + InnoDB

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
| `del_flag` | CHAR(1) | Y | `0` | `0` 存在 `2` 删除 |
| `create_by` | VARCHAR(64) | N | `''` | BaseEntity 审计字段 |
| `create_time` | DATETIME | N | NULL | BaseEntity 审计字段 |
| `update_by` | VARCHAR(64) | N | `''` | BaseEntity 审计字段 |
| `update_time` | DATETIME | N | NULL | BaseEntity 审计字段 |
| `remark` | VARCHAR(500) | N | NULL | BaseEntity 审计字段 |

索引：`PRIMARY(ticket_id)` / `uk_ticket_no` / `idx_status` / `idx_creator_id` / `idx_assignee_id` / `idx_category_id` / `idx_create_time`

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

见项目 `sql/ticket-v1.0.sql`，包含 4 张表 DDL + 默认分类数据 + 菜单权限配置。
