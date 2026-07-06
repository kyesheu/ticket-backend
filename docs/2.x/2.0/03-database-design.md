# v2.0 数据库设计

状态：已完成

增量脚本：sql/ticket-v2.0.sql

| 表/字段 | 用途 | 关键约束 |
|---|---|---|
| ticket_category.workflow_key | 分类绑定流程 | 可空，绑定稳定 key |
| ticket_workflow_definition | 版本化定义 | workflow_key + version 唯一 |
| ticket_workflow_node | 节点 | definition_id + node_key 唯一 |
| ticket_workflow_transition | 连线和白名单条件 | 按来源节点和顺序索引 |
| ticket_workflow_instance | 工单流程实例 | ticket_id 唯一 |
| ticket_workflow_task | 人工任务和处理历史 | 实例、状态、处理人索引 |

发布定义不可更新或删除；运行历史不覆盖。
