# v3.2 测试与发布

状态：完成

## 必测

- 文档分页、详情、重复导入、并发导入、删除、重复删除和失败恢复。
- 删除完成后旧切片不可查询，新代次切换前旧代次继续有效。
- ASSIST/TRIAGE 反馈、重复反馈、无权工单和伪造 targetId。
- 生成量、降级率、采纳率、有用率和时间范围统计。
- 相同评测集跨版本结果可比，敏感内容已脱敏。

## 发布门禁

- [ ] 知识生命周期和反馈测试通过。
- [ ] 固定评测集达到 v3.2 预设阈值且无安全回退。
- [ ] SQL、pytest、Maven 测试、编译及完整 smoke 通过。
- [ ] v1.0–v3.1 全量回归通过。
- [ ] 管理操作审计完整，删除和失败恢复可验证。

## 阶段 54 实测记录

- 2026-07-07：`E:\project\ticket\ticket-backend\ai-service\.venv\Scripts\python.exe -m pytest ai-service\tests\test_contract.py ai-service\tests\test_knowledge.py`，22 passed。
- 2026-07-07：`E:\project\ticket\ticket-backend\ai-service\.venv\Scripts\python.exe -m pytest ai-service\tests`，80 passed。
- 2026-07-07：`mvn -pl ticket-ticket "-Dtest=TicketAiDocumentServiceImplTest,HttpTicketAiServiceImplTest,TicketAiFeedbackModelTest,TicketAiFeedbackMapperXmlTest,TicketAiFeedbackSqlTest" test`，24 passed。
- 2026-07-07：`mvn -pl ticket-ticket test`，247 passed。
- 2026-07-07：`mvn -pl ticket-ticket clean compile`，BUILD SUCCESS。
- 2026-07-07：`mvn test`，BUILD SUCCESS，ticket-ticket 247 passed。
- 2026-07-07：`mvn clean compile`，BUILD SUCCESS。
- 2026-07-07：v3.x smoke 未执行；`http://localhost:8080/captchaImage` 3 秒超时，当前没有可用 Java 服务响应。现有 smoke 脚本仍为 v3.0-v3.1，尚未补充 v3.2 阶段 54 文档管理契约 smoke。
