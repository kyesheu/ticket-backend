# 项目文档索引

文档按版本族拆分。开发前只读取当前版本及其依赖的历史版本文档，并严格按照当前版本的 `04-implementation-plan.md` 分阶段推进。

| 版本目录 | 状态 | 范围 |
|---|---|---|
| [`1.x`](./1.x/) | 已完成 | v1.0–v1.3：工单主流程、SLA、通知评价、部门数据权限 |
| [`2.x`](./2.x/) | 已完成 | v2.0–v2.3：动态流程、自定义字段、附件、Elasticsearch 检索 |
| [`3.x`](./3.x/) | 规划中 | v3.0：Python LangChain 知识库 RAG 与工单辅助 |

每个版本目录包含：

| 文档 | 内容 |
|---|---|
| `01-project-spec.md` | 功能范围、规则和验收标准 |
| `02-architecture-design.md` | 架构、模块 seam 和安全约束 |
| `03-database-design.md` | 数据结构、索引和数据归属 |
| `04-implementation-plan.md` | 分阶段实施计划 |
| `05-test-release.md` | 测试清单、发布门禁和实测记录 |
