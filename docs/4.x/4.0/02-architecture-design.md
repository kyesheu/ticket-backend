# v4.0 架构设计

状态：规划中

## 总体设计

v4.0 在现有 Java 后端、Python AI 服务、MySQL、Redis、Elasticsearch 架构上增加“问答前置”和“自动转人工”链路。

```text
Frontend
  -> Java Backend
     -> Ticket Domain
     -> Knowledge Domain
     -> AI Orchestration
        -> Python AI Service
           -> Elasticsearch
           -> LLM
           -> Embedding Model
```

Java 继续负责权限、工单、流程、日志、分派落库和事务边界。Python AI 服务负责知识检索、历史工单检索、RAG 回答、分类建议和处理人建议。

## 模块边界

### Frontend

- `AI 智能问答`：普通用户第一入口。
- `我的工单`：用户查看转人工后的工单。
- `我的待办`：处理人查看待处理工单。
- `工单处理`：处理动作和 AI 辅助并列展示。
- `知识库管理`：文档和分类维护。

### Java Backend

- AI 问答代理：对接 Python AI 问答 API。
- AI 会话管理：保存用户问题、AI 回答、引用来源和解决状态。
- 转人工建单：将 AI 会话转为工单。
- 自动分派编排：调用 AI 分诊，按规则决定是否自动采纳。
- 工单处理工作台：聚合工单、AI 辅助、历史记录和附件。

### Python AI Service

- 知识库检索：从 Elasticsearch 查询相似知识片段。
- 历史工单检索：查询相似已关闭工单和处理方案。
- RAG 回答：基于证据生成回答。
- 问题分类：输出分类、优先级和理由。
- 分派建议：基于问题类型和候选处理人输出建议处理人。

## 新增接口

### Java 对前端

```text
POST /ticket/ai/ask
POST /ticket/ai/escalate
GET  /ticket/ai/session/{sessionId}
POST /ticket/ai/session/{sessionId}/resolved
GET  /ticket/workbench/my-todo
POST /ticket/{ticketId}/process
```

### Java 对 Python

```text
POST /api/v1/qa/ask
POST /api/v1/qa/classify
POST /api/v1/qa/dispatch-suggest
```

## AI 问答返回结构

```text
answer              AI 回答
confidence          回答置信度
need_human          是否建议转人工
sources             知识库和历史工单来源
suggested_category  建议分类
suggested_priority  建议优先级
follow_up_questions 需要用户补充的问题
```

## 转人工建单策略

转人工时 Java 后端负责创建工单，并将 AI 会话内容写入工单扩展字段或关联表。

工单内容至少包含：

- 用户原始问题
- AI 初始回答
- AI 引用来源
- 用户补充说明
- AI 分类和优先级建议
- AI 分派建议

## 自动分派策略

自动分派分两层：

1. AI 输出建议：分类、优先级、处理人、理由、置信度。
2. Java 执行业务规则校验：候选人有效、权限有效、分类匹配、置信度达标。

只有同时满足以下条件才自动分派：

- 推荐处理人存在且启用。
- 推荐处理人具备对应分类处理权限。
- 分类置信度达到阈值。
- 处理人置信度达到阈值。
- 工单状态允许分派。

否则进入待分派。

## 处理人工作台

处理页面不再只展示工单字段，而是聚合以下信息：

- 工单基础信息。
- 用户原始问题。
- AI 初始回答。
- AI 推荐处理方案。
- 相似知识。
- 相似历史工单。
- 回复草稿。
- 操作日志。
- 附件。

处理人可以基于这些信息完成回复、转派、关闭和附件上传。

## 降级设计

- Python AI 服务不可用时，AI 问答页面提示服务暂不可用。
- 转人工仍可创建普通工单。
- 自动分派失败时，工单进入待分派。
- 知识库检索失败时，可仅基于历史工单或返回降级提示。
- LLM 调用失败时，不阻断人工建单。

## 安全约束

- AI 不直接关闭工单。
- AI 不直接修改用户权限、系统配置或知识库内容。
- AI 生成内容必须带来源或明确标注为建议。
- 敏感字段不进入大模型请求。
- 所有 AI 自动分派动作必须记录审计日志。
