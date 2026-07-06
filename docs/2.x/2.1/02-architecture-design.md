# v2.1 架构设计

状态：已完成

- ITicketCustomFieldDefinitionService 管理定义和选项。
- ITicketCustomFieldService 提供分类表单、值校验保存、详情查询和规范化值读取。
- Controller 不判断字段类型，流程引擎不直接解析存储 JSON。
- 工单值保存 field key、名称、类型、规范化值和选项标签快照。
- 流程引擎通过 Service seam 读取规范化值，不直接查询字段值表。
