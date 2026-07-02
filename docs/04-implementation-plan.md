# 04 — 实施计划

> v1.1 | 2026-07-02 | v1.0 已完成，v1.1 待实施

## 阶段一：准备模块结构 ✅

建 `ruoyi-ticket` Maven 模块，打通编译。

- 创建 `ruoyi-ticket/` 标准 Maven 目录 + `pom.xml`（只依赖 `ruoyi-common`）
- 根 `pom.xml` 加 `<module>` + `<dependencyManagement>`
- `ruoyi-admin/pom.xml` 加依赖
- 创建 `resources/mapper/` 目录
- 验收：`mvn clean compile` 通过

## 阶段二：数据库初始化 ✅

- 执行 `sql/ticket-v1.0.sql`（4 张表 + 默认分类 + 菜单权限）
- 验收：4 张表存在，8 条分类数据，16 条菜单权限

## 阶段三：基础代码结构 ✅

按功能需要建 Domain、DTO、VO、Enum、Mapper、Service、Controller 骨架。

- `TicketOperationLog` 不继承 `BaseEntity`
- `TicketStatus` 含 `allowedTransitions` 静态块
- 验收：编译通过，字段与数据库列对应

## 阶段四：工单分类模块 ✅

- `TicketCategoryMapper.xml`：列表、树查询、详情、新增、修改、删除、子节点计数、唯一性校验
- `TicketCategoryServiceImpl`：树构建（内存递归）、新增自动计算 `ancestors`、修改级联更新子孙、删除校验子节点
- `TicketCategoryController`：list / tree / getInfo / add / edit / remove
- 验收：树结构正确、新增后出现在正确位置、有子节点时拒绝删除

## 阶段五：工单主流程（核心） ✅

- `TicketMapper.xml`：列表（含数据范围）、详情（4 表 JOIN）、实体查询、新增、更新、逻辑删除、编号查询、用户校验
- `TicketServiceImpl`：创建（编号生成）、分派、处理、确认、取消。每次流转校验状态 + 写日志
- `TicketController`：list / detail / create / assign / process / confirm / cancel / logs
- `TicketCommentServiceImpl`：添加评论、查询评论
- `TicketCommentController`：评论列表、添加评论
- 验收：10 条 v1.0 验收标准全部通过

## 阶段六：评论和操作日志 ✅

- `TicketOperationLogMapper.xml`：按工单查询、新增
- 确认阶段五的分派/处理/确认/取消日志写入正确
- 验收：创建工单产生 CREATE 日志，每次流转产生对应日志，评论列表正常

## 阶段七：权限和数据范围 ✅

- 所有 CUD 接口加 `@PreAuthorize`
- `TicketServiceImpl.selectTicketList` 中实现数据范围：管理员看全部，普通用户看自己的
- 验收：无权限返回 401，普通用户列表只看到自己的工单

## 阶段八：联调收尾 ✅

- 启动应用，Swagger 确认 ticket 接口可见
- 接口冒烟测试全部通过
- Code Review 发现 4 个 Bug，已修复
- CI/CD 流水线已配置
- PR 模板已创建

## v1.1 阶段九：增量数据库与基础模型 ✅

- 先列策略校验、时限计算、迁移回填的合法/非法/边界测试用例
- 新增 `sql/ticket-v1.1.sql`：工单 SLA 字段、策略表、告警表、默认数据、权限和 Quartz 任务
- 只建立当前阶段需要的 Domain、Enum、Mapper 骨架及 Mapper XML
- POJO 实现 `Serializable` 并声明 `serialVersionUID`
- 不修改 `ruoyi-common`、`ruoyi-system`、`ruoyi-quartz` 等基础模块
- 验证：SQL 人工审查，`mvn test`，`mvn clean compile`

## v1.1 阶段十：SLA 策略管理 ✅

- 先列策略列表、查询、修改、唯一性、数值关系和停用场景测试用例
- 实现策略 Mapper、Service、Controller，仅支持查询、新增和修改，不提供物理删除
- 强制校验 `responseMinutes > 0` 且 `resolveMinutes > responseMinutes`
- 验证：Service Mockito 单元测试、权限测试、策略接口 smoke

## v1.1 阶段十一：工单时限快照与展示 ✅

- 先列四种优先级、策略缺失/停用、临界截止时间、策略修改不追溯的测试用例
- 创建工单时在同一事务内读取策略并写入截止时间快照
- 扩展列表、详情 VO 和查询条件，支持响应/解决超时筛选
- 不改变 v1.0 状态机和操作日志语义
- 验证：`TicketServiceImpl` 单元测试、列表和详情接口 smoke

## v1.1 阶段十二：超时扫描与幂等告警 ✅

- 先列响应超时、解决超时、准时完成、终态、重复扫描和并发扫描测试用例
- 实现 `ITicketSlaService`、`TicketSlaTask`、告警 Mapper/Service
- 分页扫描活动工单，更新超时标记并写入不可变告警
- 使用唯一键保证 `(ticket_id, alert_type)` 幂等，任务禁止并发
- 验证：Service Mockito 单元测试、重复执行测试、Quartz 手工执行测试

## v1.1 阶段十三：告警查询、权限与联调收尾 ⏳

- 实现告警分页、详情和管理员手工补扫接口
- 补齐菜单权限和普通用户数据边界
- 扩展 `scripts/ticket/smoke-test.ps1` 覆盖 v1.0 回归与 v1.1 主流程
- 执行 `mvn test` → `mvn clean compile` → 启动后端 → smoke
- 更新 Swagger 和 `docs/05-test-release.md` 的实测结果
- 未获得全部测试结果前不得将 v1.1 标记完成
