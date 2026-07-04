# 02 — 架构与设计规范

> 3.x | 2026-07-04 | 状态: 📝 规划中

## v3.0 知识库、RAG 与工单助手设计

### 系统与调用方向

v3.0 新增独立 `ai-service/` Python 服务。Java 保持业务主系统，Python 形成 AI 深模块；两者仅通过版本化内部 HTTP interface 协作。

```text
RuoYi / ruoyi-ticket
        |
        | HTTP: ticket context / document import
        v
Python FastAPI AI service
        |
        | LangChain
        v
document loader / vector store / embedding / chat model
```

- Java 侧建立 `ITicketAiService` seam 和 HTTP adapter，只暴露文档导入、相似检索、处理建议、回复草稿四个业务用例。
- Controller 先通过 `ITicketAccessPolicy` 校验工单，再由 Java 组装最小 `TicketAiContext`；禁止把用户 ID、工单 ID 交给 Python 后由其直查业务库。
- Python 侧隐藏 LangChain chain、loader、splitter、embedding、vector store 和模型实现，HTTP interface 不暴露 LangChain 类型。
- Python 只返回检索结果、引用、处理建议和回复草稿，不提供工单写接口。
- Java 单元测试使用 HTTP adapter mock；Python 使用 fake embedding/chat model；跨系统契约由端到端测试覆盖。

### 知识来源与同步

- 导入文档由 Java 管理入口接收并转发 Python；Python 完成解析、切片、向量化和向量库存储。
- 相似历史工单仅使用已关闭且具有处理结果的工单快照。Java 负责筛选和脱敏，通过内部同步接口推送 Python。
- 文档和历史工单使用不同 `sourceType`，统一返回 `sourceId`、标题、片段和相似度，便于 Java 展示来源。
- v3.0 第一版不实现知识版本发布工作流、段落级 ACL 和复杂异步任务编排；失败可重试且不得留下可查询的半成品数据。

### RAG 与工单辅助

- Java 请求包含工单编号、标题、描述、分类、优先级和必要的处理上下文，不传服务器路径或鉴权凭据。
- Python 固定执行：规范化问题 → 检索文档与历史工单 → 组装证据 → LangChain 调用模型 → 校验结构和引用 → 返回。
- “处理建议”和“回复草稿”是两个明确输出字段，避免让模型产生可执行动作。
- 回复草稿由前端展示并允许处理人编辑；正式提交仍调用现有评论或处理接口。

### 安全与故障隔离

- 知识文本和工单内容均视为不可信数据，置于明确的数据区段，禁止其覆盖系统指令或工具策略。
- Java 调用配置独立的连接/读取超时、响应大小上限和总开关；不做无限重试。
- 服务间认证使用独立凭据，具体配置在实施阶段确定；不得复用用户 JWT 或把模型密钥交给 Java。
- Python、向量库或模型故障只影响四个 AI 用例，不回滚工单事务。
- 两端普通日志均不记录完整工单描述、文档正文、模型密钥或完整提示词。
