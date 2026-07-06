# 04 — 实施计划

> 3.x | 2026-07-04 | 状态: 🚧 进行中

## v3.0 阶段四十四：HTTP 契约与 Python 服务骨架 ✅

- 先列服务可用/不可用、超时、非法响应、超大响应、鉴权失败和契约版本不兼容测试用例
- 新增独立 `ai-service/`，建立 FastAPI、LangChain 配置、健康检查和测试骨架；不新增 Java AI Maven 模块
- 定义文档导入、历史工单同步、相似检索和工单辅助的版本化 HTTP 契约
- Java 建立 `ITicketAiService` 与 HTTP adapter 骨架，AI 默认关闭且不影响现有应用启动
- 是否涉及基础模块：只修改 `ruoyi-ticket` 和 `ruoyi-admin` 必要配置；不改 `ruoyi-common`、`ruoyi-framework`、`ruoyi-system`
- 验证：Java 契约/配置测试、Python pytest、`mvn test`、`mvn clean compile`

## v3.0 阶段四十五：知识库文档导入 ✅

- 先列合法文档、空文件、非法类型、大小边界、解析失败、重复导入和向量化失败测试用例
- Java 提供管理员导入入口并转发受控文件；Python 完成解析、切片、Embedding 和向量库存储
- 同一来源重复导入幂等覆盖，失败不得留下可查询的半成品切片
- 第一版只支持明确白名单格式，不实现 OCR、版本发布流和复杂文档管理
- 验证：Python loader/splitter/vector store 测试、Java 权限测试、文档导入 smoke

## v3.0 阶段四十六：历史工单同步与相似知识检索 ✅

- 先列已关闭/未关闭、无处理结果、重复同步、敏感字段排除、文档命中和历史工单命中测试用例
- Java 将符合条件的已关闭工单脱敏快照同步到 Python，MySQL 仍是工单事实唯一来源
- Python 根据当前工单标题和描述检索知识文档与相似历史工单，返回来源、片段和相似度
- Java 在调用前执行 `ITicketAccessPolicy`，Python 不直连工单数据库、不接收用户 JWT
- 验证：同步幂等测试、检索相关性测试、对象权限测试、相似检索 smoke

## v3.0 阶段四十七：处理建议与回复草稿

- 先列正常证据、无证据、恶意文档、模型超时、非法结构、伪造引用和超长输出测试用例
- Python 使用 LangChain 完成检索增强生成，一次返回 `suggestion`、`replyDraft` 和 `sources`
- Java 展示建议与可编辑回复草稿，不自动评论或触发任何工单动作
- 输出必须能映射到本次检索来源；无可靠证据时明确降级，不生成伪造答案
- 验证：Python chain 测试、Java adapter/Controller 测试、端到端工单辅助 smoke

## v3.0 阶段四十八：安全、故障隔离与联调收尾

- 建立小型固定评测集，覆盖知识命中、相似工单、建议可用性、引用、拒答和提示词注入
- 补齐服务间认证、超时、限流、日志脱敏、健康检查和 Python/向量库/模型故障降级
- 扩展 `scripts/ticket/v3.x/smoke-test.ps1` 覆盖四项功能以及 v1.0–v2.3 完整回归
- 执行 Python 测试 → `mvn test` → `mvn clean compile` → 启动完整依赖 → 端到端 smoke
- 更新当前目录 `05-test-release.md` 实测结果；全部门禁通过后才将 v3.0 标记完成
