# v3.0 架构设计

状态：完成

## 边界

ruoyi-ticket 通过版本化内部 HTTP 调用独立 ai-service。ITicketAiService 隔离 HTTP 适配，Python API 不暴露 LangChain 类型。

- Java：ITicketAccessPolicy、最小上下文、历史工单筛选与脱敏、超时和响应上限。
- Python：loader、splitter、embedding、Elasticsearch、RAG chain 和结构校验。
- 来源统一返回 sourceType、sourceId、title、snippet、score 和 metadata。

## 安全与故障

- 服务间使用独立 X-Service-Token，不复用用户凭据。
- 文档和工单文本按不可信数据隔离，不能覆盖系统指令。
- 限制请求、响应、输出长度和调用时间，不做无限重试。
- AI 故障不进入工单事务，不影响 v1.0–v2.3 业务。
- 日志不记录完整正文、提示词和密钥。
