# 02 — 架构与设计规范

> 2.x | 2026-07-04 | 状态: ✅ 已完成

## v2.0 动态流程设计

### 核心 seam

动态流程仍归属 `ruoyi-ticket`，不引入 Flowable，不修改基础模块。以 `ITicketWorkflowEngine` 作为核心
seam，向工单模块提供小接口：启动实例、完成任务、退回任务、取消实例、终止实例。版本解析、图校验、
条件路由、处理人解析、并发控制、状态映射和日志写入均隐藏在实现内。

```text
TicketService / WorkflowTaskController
                  |
                  v
        ITicketWorkflowEngine
                  |
      +-----------+------------+
      |           |            |
  Definition   Runtime      Assignee
  validation   routing      resolution
      |           |            |
      +-----------+------------+
                  |
 WorkflowDefinition/Node/Transition Mapper
 WorkflowInstance/Task Mapper + Ticket Mapper
```

Controller 不读取流程图自行判断，现有业务 Service 不复制路由和处理人规则。Mapper 只负责持久化，
流程不变量集中在引擎实现中，通过 `ITicketWorkflowEngine` 的可观察结果测试。

### 定义模型与发布约束

- `ticket_workflow_definition`：版本化流程定义，`workflow_key + version` 唯一。
- `ticket_workflow_node`：节点类型限定为 `START / ASSIGN / PROCESS / CONFIRM / END`。
- `ticket_workflow_transition`：连线条件限定为白名单字段、运算符和值，不存储脚本或 SQL。
- 草稿版本可整体编辑；发布时校验一个开始节点、至少一个结束节点、节点可达、人工节点处理人完整、
  条件分支存在唯一默认连线。
- 已发布定义及其节点、连线不可更新和删除；新版本从旧版本复制为新草稿后编辑。

### 运行模型

- `ticket_workflow_instance` 一对一关联新工单并引用不可变定义版本。
- `ticket_workflow_task` 保存人工节点任务。任务状态为 `PENDING / COMPLETED / RETURNED / CANCELLED / TERMINATED`。
- 每个运行实例最多一个 `PENDING` 任务。进入节点时解析处理人并写入任务快照。
- 完成任务使用条件更新 `WHERE task_status = 'PENDING'` 实现乐观并发控制；影响行数不是 1 时按重复处理拒绝。
- 路由在一个事务内完成：关闭旧任务、选择连线、推进实例、创建新任务、更新工单、写操作日志、触发通知。

### 处理人规则

`assignee_type` 仅允许 `USER / ROLE / CREATOR_DEPT_LEADER / TICKET_ASSIGNEE / TICKET_CREATOR`：

- `USER`：定义保存用户 ID，节点激活时校验用户有效并固化到任务。
- `ROLE`：任务保存角色 ID，处理时校验当前用户拥有该启用角色及 ticket 处理权限。
- `CREATOR_DEPT_LEADER`：按工单 `dept_id` 快照查询部门负责人，节点激活时固化负责人用户 ID。
- `TICKET_ASSIGNEE`：读取工单当前指派人并固化到任务，用于标准流程处理节点。
- `TICKET_CREATOR`：读取工单创建人并固化到任务，用于标准流程确认节点。

无法解析处理人时不得跳过节点，当前动作事务回滚并返回明确业务错误。

### 兼容现有工单语义

- `ticket.status` 保留为列表、SLA、通知和统计使用的粗粒度业务状态；流程节点是细粒度运行状态。
- 内置标准流程表达原 `NEW → PROCESSING → WAIT_CONFIRM → CLOSED` 行为，旧接口通过兼容适配调用流程引擎。
- v2.0 前创建且没有流程实例的工单继续由原状态机处理，避免在线迁移风险。
- 工单访问仍先经过 `ITicketAccessPolicy`；任务处理权限是在对象访问权基础上的附加校验。

## v2.1 自定义字段设计

### 核心 seam

`ITicketCustomFieldService` 是自定义字段运行时 seam，向工单创建和详情提供小接口：查询分类表单、校验并保存值、查询工单值、按 key 读取规范化值。字段类型转换、选项解析、快照和错误语义隐藏在实现内。

```text
TicketService / TicketWorkflowEngine
              |
              v
   ITicketCustomFieldService
              |
   Definition + Option + Value Mapper
```

字段管理使用独立 `ITicketCustomFieldDefinitionService`，负责定义与选项的原子保存。Controller 不执行类型判断，流程引擎不直接解析 JSON。

### 定义与快照

- 定义归属分类，字段 key 和类型不可变，禁止物理删除。
- 选项使用稳定 value 和可变 label；value 在字段内唯一。
- 工单值同时保存定义 ID、字段 key/name/type 快照、规范化值和展示标签快照。
- 详情从值表读取历史快照，不实时关联定义覆盖名称或标签。

### 流程条件扩展

`ticket_workflow_transition` 新增 `condition_key`。`condition_field = CUSTOM_FIELD` 时，key 必填且符合字段 key 格式；值仍通过参数化 SQL 保存。引擎通过 `ITicketCustomFieldService` 读取规范化值，缺失或类型不兼容返回“不命中”。

## v2.3 Elasticsearch 检索设计

### 边界与核心 seam

检索能力归属 `ruoyi-ticket`。`ITicketSearchService` 负责查询和权限安全，`ITicketSearchIndexer` 负责单工单投影与全量重建；Controller 不组装 Elasticsearch DSL，业务 Service 不直接调用 ES。

```text
Ticket / Comment transaction              SearchController
          |                                      |
          v                                      v
 ticket_search_event                    ITicketSearchService
          |                              | ES candidate query
 scheduled dispatcher                    | batch access recheck
          |                              v
          v                         Elasticsearch alias
 ITicketSearchIndexer <--- MySQL ticket projection
```

### 一致性与失败隔离

- 创建、修改、流转、附件元数据展示变化和评论变更在原业务事务中追加事件；事务回滚时事件一并回滚。
- 调度器以条件更新抢占待处理事件，按事件 ID 幂等消费；成功标记完成，失败记录次数、下次执行时间和脱敏错误摘要。
- 索引文档携带 `source_event_id`。脚本化更新仅在新事件 ID 更大时覆盖，防止乱序回写旧快照。
- ES 故障不回滚工单业务事务。超过重试上限的事件进入 `FAILED`，管理员可重置后补偿。
- 全量重建按数据库主键游标分批读取，写入版本化物理索引；文档数与抽样结果通过后原子切换 `ticket-search` 别名。

### 查询与权限

- ES 查询字段、排序字段、过滤字段和高亮字段全部使用服务端白名单，不透传客户端 DSL。
- 数据范围先转换为 ES `filter` 缩小候选集：管理员全部、部门树范围、本人创建或本人指派。
- `ITicketAccessPolicy` 对候选 ID 批量复核后才返回安全结果；不足一页时继续拉取候选，直至填满或游标耗尽。接口只返回 `hasMore`，不暴露 ES 原始总命中数。
- API 返回 opaque `nextCursor`，内部编码稳定排序值和查询摘要；篡改或跨查询复用游标时拒绝请求。
- 高亮仅允许标题、描述和评论纯文本，返回前执行长度限制和 HTML 转义，仅保留服务端插入的高亮标记。

### 索引映射

- 固定别名 `ticket-search` 指向 `ticket-search-v{timestamp}` 物理索引。
- `ticket_no` 使用 `keyword`；标题、描述、评论使用中文 analyzer 的 `text` 子字段并保留 keyword/标准分词能力。
- 状态、优先级、分类、用户和部门使用 `keyword` 或 `long`；时间使用 `date`。
- 自定义字段 v2.3 不进入全文索引，避免动态 mapping 膨胀；仍可通过详情读取。

