# v3.1 架构设计

状态：规划

## 调用链

TicketAiTriageController → ITicketAiTriageService → ITicketAccessPolicy / 候选集查询 → ITicketAiService → POST /api/v1/tickets/triage。

## 责任

- Java 组装工单快照、合法分类、合法处理人和当前业务版本。
- Python 结合 RAG 证据对候选集排序，返回结构化建议。
- Java 校验响应仍属于候选集，保存建议记录。
- 确认接口按 suggestionId 读取记录，重新校验工单 updateTime、权限和候选有效性，再调用现有业务 Service。

## 约束

- 不修改 v3.0 /assist 契约；新增 /triage。
- 置信度范围为 0–1；低于配置阈值时只返回降级结果。
- 建议记录不保存完整提示词、工单正文或服务凭据。
- 同一建议只能进入 APPLIED、REJECTED 或 EXPIRED 一种终态。
