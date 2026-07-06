# v3.0 测试与发布

状态：完成

## 已验证

- Python pytest：64 个测试通过；compileall 通过。
- Java Maven 全量测试通过；全模块编译成功。
- 无外部写入的工单辅助 smoke 通过。
- 主 smoke：84 通过、0 失败、0 跳过，使用独立 smoke 索引。
- 服务认证、超时、限流、脱敏、来源校验和故障降级测试通过。
- 真实 Elasticsearch 相似检索 smoke 通过。
- MySQL、Redis、Elasticsearch、Python 和 Java 完整依赖启动验证通过。
- 测试索引使用 smoke/test 命名并已确认清理。

## 发布结论

- [x] 使用真实 Elasticsearch 执行相似检索 smoke。
- [x] 启动 MySQL、Redis、Elasticsearch、Python 和 Java 完整依赖并记录结果。
- [x] 确认测试索引隔离和数据清理。

v3.0 发布门禁已关闭，允许进入 v3.1。
