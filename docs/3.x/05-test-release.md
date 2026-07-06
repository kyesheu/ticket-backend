# 05 — 测试与发布

> 3.x | 2026-07-04 | 状态: 🚧 进行中

## v3.0 RAG 问答与工单辅助测试清单（规划）

| 场景 | 预期 |
|---|---|
| 合法文档导入 | Python 完成解析、切片、向量化并可检索 |
| 空文件、非法类型、超限内容或解析失败 | 明确拒绝且不留下可查询半成品 |
| 同一文档或历史工单重复同步 | 幂等覆盖，不产生重复有效切片 |
| 未关闭或无有效处理结果工单 | 不进入历史工单知识源 |
| 工单问题命中文档和历史工单 | 返回相关片段、来源 ID 和相似度 |
| 越权访问工单辅助接口 | Java 在调用 Python 前拒绝 |
| 文档或工单内容包含提示词注入 | 作为不可信数据，不改变系统指令 |
| 检索无可靠证据 | 明确降级，不伪造处理建议或来源 |
| 模型返回非法结构、伪造引用或超长输出 | Python 拒绝结果，Java 返回可识别错误 |
| 正常生成工单辅助 | 同时返回处理建议、回复草稿和来源 |
| 用户查看或编辑回复草稿 | 不自动新增评论或改变工单状态 |
| Python、向量库或模型不可用 | v1.0–v2.3 工单业务继续可用 |

### 阶段四十四契约测试用例

| 类型 | 场景 | 预期 |
|---|---|---|
| 合法 | Python 健康检查使用 v1 契约 | 返回 `UP` 和 `v1` |
| 合法 | 携带正确服务凭据调用业务接口 | 通过认证并进入对应接口 |
| 非法 | 缺失或错误服务凭据 | 返回 401，不执行业务逻辑 |
| 非法 | 请求声明未知契约版本 | 返回 400 契约版本错误 |
| 非法 | Python 返回非 JSON 或缺少必填字段 | Java 转换为统一 AI 服务异常 |
| 边界 | Python 无法连接或超过读取超时 | Java 快速失败，不影响工单事务 |
| 边界 | Python 响应超过配置上限 | Java 中止读取并返回统一异常 |
| 边界 | `ticket.ai.enabled=false` | 不创建 HTTP 客户端和 adapter Bean |

### 阶段四十五文档导入测试用例

| 类型 | 场景 | 预期 |
|---|---|---|
| 合法 | UTF-8 `.txt`、`.md` 和含文本 `.pdf` | 解析、切片、向量化并返回切片数量 |
| 合法 | 同一 `sourceId` 和相同内容重复导入 | 幂等返回，不产生重复有效切片 |
| 合法 | 同一 `sourceId` 导入新内容 | 新切片完整替换旧切片 |
| 非法 | 空文件、空白文本、非法 Base64 | 返回 422，不调用 Embedding 或向量库 |
| 非法 | 非白名单扩展名、扩展名与 MIME 不匹配 | 返回 422 |
| 非法 | 加密 PDF、扫描 PDF 或 PDF 无文本 | 返回 422，不做 OCR |
| 边界 | 文件名 255 字符、内容达到配置上限 | 可导入；超过一位即拒绝 |
| 边界 | Embedding 或向量库写入失败 | 返回明确失败，旧有效切片保持可查询 |

### 阶段四十六历史工单同步与相似知识检索测试用例

| 类型 | 场景 | 预期 |
|---|---|---|
| 合法 | CLOSED 且有有效处理结果的工单 | 生成脱敏快照并同步到 Python 历史工单索引 |
| 非法 | 工单状态不是 CLOSED | 不进入同步候选，不调用 Python |
| 非法 | CLOSED 工单无有效 solution | 不进入同步候选，不调用 Python |
| 边界 | 同一 ticket_id 与 source_generation 重复同步 | 使用稳定文档 ID 幂等覆盖，不产生重复文档 |
| 边界 | 同一 ticket_id 同步新 source_generation | 新代次替换旧代次，旧代次不再有效 |
| 安全 | 标题、描述或解决方案包含手机号、邮箱、身份证、token、密码 | Java 脱敏后再传输，Python 不接收敏感原文 |
| 合法 | 当前问题命中知识文档 | 返回 knowledge_document、来源 ID、片段、相似度和 metadata |
| 合法 | 当前问题命中历史工单 | 返回 history_ticket、来源 ID、解决方案片段、相似度和 metadata |
| 安全 | 用户有当前工单对象权限 | Java 完成权限校验后才调用 Python |
| 非法 | 用户无当前工单对象权限 | Java 拒绝请求且不调用 Python |

### 阶段四十七处理建议与回复草稿测试用例

| 类型 | 场景 | 预期 |
|---|---|---|
| 合法 | 检索到可靠知识文档或历史工单证据 | 返回 suggestion、replyDraft、sources，degraded=false |
| 边界 | 未检索到可靠证据 | degraded=true，reason=no_reliable_evidence，不生成建议或草稿 |
| 安全 | 证据包含忽略规则、伪造引用、自动关闭或输出凭据等指令 | 证据按不可信数据隔离，不能覆盖系统规则 |
| 边界 | LLM 调用超时 | degraded=true，reason=model_timeout，不影响工单业务 |
| 非法 | LLM 返回非法 JSON、缺字段或错误类型 | degraded=true，reason=invalid_model_output |
| 安全 | LLM 返回本次检索结果之外的 source_id | 拒绝整个生成结果并标记 forged_source_reference |
| 边界 | suggestion 或 replyDraft 超过长度限制 | 分别截断到 4000 和 6000 字符 |
| 安全 | 用户无当前工单对象权限 | Java 在调用 Python 前拒绝 |
| 合法 | Java 收到降级响应 | 原样返回前端，不新增评论、不处理工单、不改变状态 |

### 阶段四十七手动验证命令

```powershell
cd ai-service
.\.venv\Scripts\python.exe -m pytest
.\.venv\Scripts\python.exe scripts\smoke_ticket_assist.py
cd ..
mvn test
mvn clean compile
```

### 阶段四十八安全、故障隔离与联调验证清单

- [x] 固定评测集包含知识命中、历史命中、混合命中、无证据、提示词注入和伪造引用
- [x] Java→Python 统一使用 `X-Service-Token`，Python 使用常量时间比较校验
- [x] Java HTTP 连接/读取超时、Python Embedding/LLM/Elasticsearch 超时均有明确配置
- [x] AI 检索与辅助接口具备简单进程内固定窗口限流，普通工单 CRUD 不受影响
- [x] Java 同步快照和 Python 日志文本具备手机号、邮箱、身份证及凭据脱敏
- [x] 健康检查返回服务状态、Elasticsearch、Embedding 配置和 LLM 配置状态且不暴露密钥
- [x] Python、Embedding、Elasticsearch 和 LLM 故障返回带 reason 的降级结果
- [x] AI 不新增评论、不处理工单、不触发状态流转
- [ ] 启动完整依赖后执行 v1.0–v3.0 集成 smoke
- [ ] 人工确认 smoke 使用独立 smoke/test 索引并完成数据清理

### 阶段四十八完整门禁顺序

```powershell
cd ai-service
.\.venv\Scripts\python.exe -m pytest
.\.venv\Scripts\python.exe -m compileall -q src tests scripts
.\.venv\Scripts\python.exe scripts\smoke_ticket_assist.py
.\.venv\Scripts\python.exe scripts\smoke_similar_search.py
cd ..
mvn test
mvn clean compile
# 启动 MySQL、Redis、Elasticsearch、Python AI 服务和 Java 服务
$env:TICKET_AI_SMOKE_ENABLED="true"
$env:TICKET_AI_KNOWLEDGE_INDEX="ticket-knowledge-smoke"
$env:TICKET_AI_TICKET_HISTORY_INDEX="ticket-history-smoke"
.\scripts\ticket\v3.x\smoke-test.ps1
```

### 阶段四十八实测结果

| 门禁 | 状态 | 结果 |
|---|---|---|
| Python pytest | 已执行 | 63 个测试通过，0 失败 |
| Python compileall | 已执行 | `src`、`tests`、`scripts` 编译通过 |
| 无外部写入工单辅助 smoke | 已执行 | 建议、草稿、来源返回且状态、评论不变 |
| 真实 Elasticsearch 相似检索 smoke | 待手动执行 | 需要真实 Elasticsearch |
| Maven 全量测试 | 已执行 | 全量通过，0 失败 |
| Maven 全模块编译 | 已执行 | 全模块编译成功 |
| 完整依赖启动 | 待手动执行 | 待填写 |
| `scripts/ticket/v3.x/smoke-test.ps1` | 已执行 | 84 通过，0 失败，0 跳过；使用独立 smoke AI 索引 |

> v3.0 尚未标记完成。仅当上述门禁全部通过并填写实测结果后，才允许更新为“v3.0：完成”。

## v3.0 发布门禁（规划）

- [ ] Java↔Python v1 HTTP 契约、服务认证、超时和响应限制测试通过
- [ ] 文档导入、切片、向量化、重复导入和失败清理测试通过
- [ ] 历史工单筛选、脱敏同步、幂等和相似检索测试通过
- [ ] 处理建议、回复草稿、来源校验、拒答和提示词注入测试通过
- [ ] 固定评测集达到实施前确定的检索命中、引用和拒答阈值
- [ ] v1.0–v2.3 单元测试与完整 smoke 全部回归通过
- [ ] Python pytest、`mvn test`、`mvn clean compile` 和打包通过
- [ ] Java、Python、向量库和模型适配器启动正常，端到端 smoke 通过，日志无敏感数据

### 阶段四十四实测结果（2026-07-04）

- 新增独立 `ai-service/`，使用 FastAPI `0.139.0`、LangChain `1.3.11` 和 Pydantic Settings `2.14.2`；未新增 Java AI Maven 模块。
- 固定 `/api/v1` HTTP 契约，包含健康检查、文档导入、关闭工单同步、相似知识检索和工单辅助路由；后四项按阶段返回明确 `501` 骨架响应。
- Python 服务凭据无默认值；业务接口缺失或错误凭据返回 `401`，未知契约版本被参数校验拒绝。
- Java 新增 `ITicketAiService` seam 和 JDK HTTP Client adapter，默认关闭；覆盖服务凭据、超时、响应大小、非法 JSON、非成功状态和契约版本校验。
- Python pytest 5/5 通过；Java AI 目标测试 11/11 通过。
- 全量 `mvn test` 通过：196 个测试，0 失败；`mvn clean compile` 全模块通过。
- Python HTTP smoke 通过：健康检查 `UP/v1`、缺失凭据 `401`、合法凭据进入阶段骨架 `501`。
- 未修改 `.env`、数据库、`ruoyi-common`、`ruoyi-framework`、`ruoyi-system` 或现有工单业务流程。

### 阶段四十五实测结果（2026-07-04）

- Java 新增管理员知识文档上传入口，校验稳定来源 ID、空文件、文件名、10 MB 上限、扩展名和 MIME；仅支持 UTF-8 TXT、Markdown 和文本型 PDF。
- Python 使用 LangChain Text Splitters 解析切片，使用 OpenAI-compatible Embedding；模型、base URL 和 API key 只从运行环境读取。
- Elasticsearch 使用独立 `ticket-knowledge-v1` 索引；新代次完成 Embedding 和批量写入后才切换为有效，随后清理旧代次。
- 同一 `sourceId` 重复导入执行完整替换；Embedding 失败发生在写入前，不影响旧有效切片。
- Python pytest 13/13 通过；Java AI 目标测试 15/15 通过。
- 全量 `mvn test` 通过：200 个测试，0 失败；`mvn clean compile` 全模块通过。
- 真实 Elasticsearch smoke 连续导入两个版本后保留 9 个有效切片、0 个失效切片。
- 未修改 `.env`、MySQL 表、基础模块或现有工单业务流程。
