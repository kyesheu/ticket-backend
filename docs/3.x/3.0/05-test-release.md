# v3.0 测试与发布

状态：未发布

## 已验证

- Python pytest：63 个测试通过；compileall 通过。
- Java Maven 全量测试通过；全模块编译成功。
- 无外部写入的工单辅助 smoke 通过。
- 主 smoke：84 通过、0 失败、0 跳过，使用独立 smoke 索引。
- 服务认证、超时、限流、脱敏、来源校验和故障降级测试通过。

## 待完成

- [ ] 使用真实 Elasticsearch 执行相似检索 smoke。
- [ ] 启动 MySQL、Redis、Elasticsearch、Python 和 Java 完整依赖并记录结果。
- [ ] 确认测试索引隔离和数据清理。

以上三项完成前，v3.0 不得标记完成，v3.1 不进入实现。
