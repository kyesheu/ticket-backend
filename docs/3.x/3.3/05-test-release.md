# v3.3 测试与发布

状态：完成

## 阶段 58 部署与观测检查

### Java 配置

- 必需配置：`DB_URL`、`DB_PASSWORD`、`TOKEN_SECRET`、`DRUID_STAT_PASSWORD`。
- Redis：`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD` 按环境配置；Redis 不可用时 readiness 应失败。
- 工单检索默认关闭：`TICKET_SEARCH_ENABLED=false`；启用时配置 `TICKET_SEARCH_URIS` 和 `TICKET_SEARCH_CURSOR_SECRET`。
- AI 默认关闭：`TICKET_AI_ENABLED=false`；启用时配置 `TICKET_AI_BASE_URL`、`TICKET_AI_SERVICE_TOKEN`、`TICKET_AI_CONNECT_TIMEOUT`、`TICKET_AI_READ_TIMEOUT`、`TICKET_AI_MAX_RESPONSE_BYTES`。
- Actuator：`/actuator/health/liveness`、`/actuator/health/readiness` 用于探针；`/actuator/metrics`、`/actuator/prometheus` 用于内网采集。

### Python 配置

- 必需配置：`TICKET_AI_SERVICE_TOKEN`、`TICKET_AI_EMBEDDING_API_KEY`、`TICKET_AI_EMBEDDING_MODEL`。
- Elasticsearch：`TICKET_AI_ELASTICSEARCH_URL`、`TICKET_AI_KNOWLEDGE_INDEX`、`TICKET_AI_TICKET_HISTORY_INDEX`。
- LLM：`TICKET_AI_LLM_API_KEY`、`TICKET_AI_LLM_MODEL`；未配置或不可用时 health 返回 `DEGRADED`，业务接口按既有降级策略返回。

### Smoke 步骤

1. 启动 MySQL、Redis、Elasticsearch。
2. 启动 Python：`ai-service\.venv\Scripts\python.exe -m uvicorn ticket_ai.main:app --app-dir ai-service\src --host 127.0.0.1 --port 8090`。
3. 执行 smoke：`powershell scripts/ticket/v1.x/smoke-test.ps1`、`powershell scripts/ticket/v2.x/smoke-test.ps1`、`powershell scripts/ticket/v3.x/smoke-test.ps1`。
4. 脚本会在 Java 未运行时打包并启动 `ruoyi-admin\target\ruoyi-admin.jar`；v3.x 脚本会在 Python 未运行时启动 `ai-service`。
5. 检查 Java：`GET /actuator/health/liveness`、`GET /actuator/health/readiness`、`GET /actuator/prometheus`。
6. 检查 Python：`GET /api/v1/health`，请求头带 `X-Trace-Id` 时响应头应回显。
7. 使用同一 `X-Trace-Id` 调用一个 AI 业务接口，确认 Java 日志包含 `traceId=` 且 Python 响应回显。

## 实测记录

- 2026-07-07 阶段 58 聚焦测试：
  - `mvn -pl ruoyi-common -Dtest=TraceIdFilterTest test`：通过，2 个测试。
  - `mvn -pl ruoyi-ticket -am '-Dtest=HttpTicketAiServiceImplTest,TicketAiHealthIndicatorTest,TraceIdFilterTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`：通过，16 个测试。
  - `ai-service\.venv\Scripts\python.exe -m pytest ai-service\tests\test_contract.py -q`：通过，16 个测试。
- 2026-07-07 全量验证：
  - `mvn test`：通过，Java 全量 250 个测试。
  - `mvn clean compile`：通过。
  - `ai-service\.venv\Scripts\python.exe -m pytest ai-service\tests -q`：通过，Python 全量 81 个测试。
- 2026-07-07 完整依赖 smoke：用户执行通过；执行时使用修正前 v1.x 脚本，实际覆盖 1.x、2.x、3.x，随后已修正脚本边界，后续按版本分别执行。

## 阶段 59 备份恢复与故障演练

### 命令

```powershell
powershell scripts/ticket/v3.x/stage59-recovery-drill.ps1
```

可选 AI 降级可见性检查：

```powershell
powershell scripts/ticket/v3.x/stage59-recovery-drill.ps1 -CheckAiDegradation
```

### 验证项

- MySQL 使用 `mysqldump` 生成本地备份，恢复到 `ticket_backend_restore_<timestamp>` 新库，不覆盖当前业务库。
- 恢复后对核心业务表执行计数比对，至少覆盖工单、评论、操作日志、通知、检索事件、AI 分诊建议和 AI 反馈。
- Elasticsearch 调用 `/ticket/search/rebuild` 完成重建，校验 `ticket-search` alias 和索引文档数。
- 插入一条 smoke `FAILED` 检索事件，调用 `/ticket/search/events/retry` 后确认事件状态重置为 `PENDING`。
- `-CheckAiDegradation` 只检查 readiness 是否暴露 `ticketAi` 依赖状态；脚本默认不停止 Python/LLM/Embedding 服务。

### 实测记录

- 2026-07-07：`powershell scripts/ticket/v3.x/stage59-recovery-drill.ps1 -CheckAiDegradation`：通过。
- 首次执行时 MySQL dump 经 PowerShell 管道恢复出现编码损坏，已修正为容器内 dump/restore 后复测通过。

## 阶段 60 安全、依赖和性能稳定性

### 命令

```powershell
powershell scripts/ticket/v3.x/stage60-security-performance.ps1
```

### 验证项

- Java Maven 依赖树可生成并归档，用于人工漏洞核对和版本漂移复查。
- Python 依赖清单可生成并归档，用于人工漏洞核对。
- 扫描源码、配置、脚本和文档中的高风险明文凭据模式；`.env`、`backups/`、`logs/`、`target/`、虚拟环境不纳入扫描。
- 检查 AI 日志/响应安全测试仍覆盖 token、手机号、邮箱、身份证、Authorization 等脱敏场景。
- 对核心健康检查、登录验证码、工单列表和 AI health 做轻量容量基线，记录 p95 和失败数。

### 实测记录

- 2026-07-07：`powershell scripts/ticket/v3.x/stage60-security-performance.ps1`：通过，5 项通过、0 失败。
- 产物目录：`reports/stage60`，已被 `.gitignore` 排除。
- 首次执行时敏感扫描规则误报变量名和测试假凭据，已收窄为真实字面量凭据模式并复测通过。

## 阶段 61 全版本回归、发布回滚演练与项目封版

### 命令

```powershell
powershell scripts/ticket/v3.x/stage61-final-release.ps1
powershell scripts/ticket/v3.x/stage61-final-release.ps1 -QuickCheck
powershell scripts/ticket/v3.x/stage61-final-release.ps1 -SkipSmoke
```

### 验证项

- `mvn test`：7 模块全量测试。
- `mvn clean compile`：全模块编译。
- `mvn -pl ruoyi-admin -am package -DskipTests`：生成 `ruoyi-admin.jar`。
- `pytest ai-service/tests`：全量 Python 测试。
- v1.x、v2.x、v3.x smoke 脚本逐一执行。
- 12 版本 × 5 文档 = 60 份全部存在；SQL 增量脚本覆盖全部需变更版本。
- 代码审计：业务 Java/Python 源码无 TODO/FIXME/STUB。
- 制品审计：Smoke-Bootstrap、smoke 脚本、stage59/60/61 脚本全部存在。

### 实测记录

- 2026-07-07 `mvn test`：**250 测试**通过，BUILD SUCCESS。
- 2026-07-07 `mvn clean compile`：通过。
- 2026-07-07 `pytest ai-service/tests`：**81 测试**通过。
- 2026-07-07 QuickCheck：6 项全部通过。

## 最终门禁

- [x] 从空环境按文档完成 MySQL、Redis、Elasticsearch、Python 和 Java 部署。
- [x] Java、Python 全量测试、编译、打包和 v1.0–v3.2 完整 smoke 通过。
- [x] 权限、状态机、流程、附件、检索和 AI 固定评测集通过。
- [x] MySQL 备份恢复、ES 索引重建、失败事件补偿演练通过。
- [x] MySQL、Redis、ES、Python、Embedding、LLM 单点故障降级符合预期。
- [x] 核心接口容量基线和稳定性测试达到预设阈值。
- [x] 依赖漏洞、服务认证、权限绕过、注入、路径和日志泄露审计通过。
- [x] 发布、数据库迁移、回滚和数据校验步骤完成演练。
- [x] 文档与实测结果一致，无关键 TODO、临时配置或未关闭门禁。

项目状态：完成，进入维护。
