# v2.1 数据库设计

状态：已完成

增量脚本：sql/ticket-v2.1.sql

| 表/字段 | 用途 | 关键约束 |
|---|---|---|
| ticket_custom_field_definition | 字段定义和校验参数 | category_id + field_key 唯一 |
| ticket_custom_field_option | 单选/多选选项 | field_id + option_value 唯一 |
| ticket_custom_field_value | 工单值和展示快照 | ticket_id + field_id 唯一 |
| ticket_workflow_transition.condition_key | 自定义字段条件 key | CUSTOM_FIELD 时必填 |

值记录不可修改或删除；历史展示不依赖当前定义和选项。
