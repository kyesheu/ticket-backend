# v1.0 数据库设计

状态：已完成

增量脚本：sql/ticket-v1.0.sql

| 表 | 用途 | 关键约束 |
|---|---|---|
| ticket | 工单事实 | ticket_no 唯一；保存创建人、部门、指派人和状态 |
| ticket_category | 树形分类 | parent_id、ancestors；同层名称约束 |
| ticket_comment | 评论 | 关联工单；类型为 INTERNAL 或 EXTERNAL |
| ticket_operation_log | 流转日志 | 不可变；记录操作者、操作类型和前后状态 |

不使用数据库外键和级联操作。用户、部门仅保存 ID，MySQL 是业务事实唯一来源。
