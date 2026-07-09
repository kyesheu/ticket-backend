# v4.0 数据库设计

状态：规划中

## 设计目标

v4.0 数据结构围绕 AI 问答会话、转人工建单、自动分派和处理闭环展开。核心原则是：AI 产生的内容要可追踪，自动分派要可审计，用户问题和工单之间要有关联。

## 新增表

### ticket_ai_session

记录一次用户 AI 问答会话。

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| user_id | bigint | 提问用户 |
| question | text | 用户原始问题 |
| answer | text | AI 回答 |
| confidence | decimal(5,4) | 回答置信度 |
| need_human | tinyint | 是否建议转人工 |
| status | varchar(32) | active/resolved/escalated |
| ticket_id | bigint | 转人工后关联工单 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

### ticket_ai_session_source

记录 AI 回答引用的知识库或历史工单来源。

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| session_id | bigint | AI 会话 ID |
| source_type | varchar(32) | knowledge/ticket |
| source_id | varchar(128) | 来源 ID |
| title | varchar(255) | 来源标题 |
| snippet | text | 证据片段 |
| score | decimal(8,4) | 相似度 |
| create_time | datetime | 创建时间 |

### ticket_ai_escalation

记录 AI 问答转人工建单过程。

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| session_id | bigint | AI 会话 ID |
| ticket_id | bigint | 工单 ID |
| user_id | bigint | 操作用户 |
| user_comment | text | 用户补充说明 |
| ai_summary | text | AI 摘要 |
| create_time | datetime | 创建时间 |

### ticket_dispatch_rule

维护分类和处理人的规则映射，作为 AI 分派校验依据。

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| category_id | bigint | 工单分类 |
| handler_id | bigint | 默认处理人 |
| priority | varchar(16) | 可选优先级范围 |
| enabled | tinyint | 是否启用 |
| remark | varchar(500) | 说明 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

### ticket_ai_dispatch_log

记录 AI 自动分派建议和采纳结果。

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| ticket_id | bigint | 工单 ID |
| session_id | bigint | AI 会话 ID |
| suggested_category_id | bigint | AI 建议分类 |
| suggested_priority | varchar(16) | AI 建议优先级 |
| suggested_assignee_id | bigint | AI 建议处理人 |
| confidence | decimal(5,4) | 综合置信度 |
| decision | varchar(32) | auto_assigned/manual_required/rejected |
| reason | varchar(1000) | 分派理由或失败原因 |
| create_time | datetime | 创建时间 |

## 工单表增量字段

现有工单表建议增加以下字段。

| 字段 | 类型 | 说明 |
|---|---|---|
| source_type | varchar(32) | manual/ai_escalation |
| ai_session_id | bigint | 来源 AI 会话 |
| ai_summary | text | AI 问答摘要 |
| dispatch_mode | varchar(32) | manual/ai_auto |
| dispatch_reason | varchar(1000) | 分派原因 |

## 状态枚举

### AI 会话状态

```text
active      用户正在问答
resolved    用户确认已解决
escalated   已转人工建单
```

### 分派决策

```text
auto_assigned    已自动分派
manual_required  需要人工分派
rejected         建议未采纳
```

## 索引建议

```text
ticket_ai_session(user_id, create_time)
ticket_ai_session(status, create_time)
ticket_ai_session(ticket_id)
ticket_ai_session_source(session_id)
ticket_ai_escalation(session_id)
ticket_ai_escalation(ticket_id)
ticket_dispatch_rule(category_id, enabled)
ticket_ai_dispatch_log(ticket_id, create_time)
```

## 数据保留

- AI 问答会话默认长期保留，用于问题追溯和后续优化。
- AI 引用来源只保存摘要和来源 ID，不复制完整文档正文。
- 自动分派日志不可物理删除，应按审计数据处理。
- 用户上传附件继续沿用现有附件表和存储策略。
