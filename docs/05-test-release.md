# 05 — 测试与发布

> v2.0 | 2026-07-03 | 状态: ✅ 全部通过

## 启动前检查

- [x] MySQL / Redis 已启动，`.env` 配置正确
- [x] RuoYi 基础表已创建（`sys_user`、`sys_dept` 等）
- [x] ticket 4 张表已创建，8 条默认分类已插入
- [x] `mvn clean compile` 通过

> v1.3 已在上述环境完成全量验证。

## v1.3 部门数据权限测试清单

| 场景 | 预期 |
|---|---|
| 全部数据权限 | 可访问全部工单 |
| 自定义部门 | 仅访问角色配置部门及本人参与工单 |
| 本部门 | 仅访问 `ticket.dept_id` 为当前部门及本人参与工单 |
| 本部门及下级 | 可访问当前部门、全部下级部门及本人参与工单 |
| 仅本人 | 仅访问本人创建或当前指派给本人的工单 |
| 多个启用角色 | 各角色有效范围取并集 |
| 停用或无 ticket 权限角色 | 不扩大数据范围 |
| 跨部门分派 | 指派人可查看、评论并处理工单 |
| 直接访问范围外 ID | 详情、评论、日志、评价均返回“工单不存在” |
| 伪造 `params.dataScope` | 请求参数不影响服务端数据范围 |
| 用户调岗 | 历史工单仍按创建时 `ticket.dept_id` 归属 |

### 阶段二十实测结果（2026-07-02）

- 访问策略与工单 Service 目标测试：23/23 通过。
- 全量单元测试：72/72 通过；`mvn clean compile` 通过。
- v1.0–v1.2 回归 smoke：46/46 通过。
- 临时“仅本人”角色接口验证：范围外 0 条、伪造 `params.dataScope` 后仍为 0 条、跨部门分派后为 1 条。
- 临时测试用户和角色已清理；五种范围组合逻辑由访问策略单元测试覆盖。

### 阶段二十一实测结果（2026-07-02）

- 对象访问相关目标测试：32/32 通过；全量单元测试和 `mvn clean compile` 通过。
- 范围外详情、评论查询、评论新增、操作日志、评价详情和状态操作均返回“工单不存在”。
- 跨部门指派后，当前指派人可正常访问工单详情。
- 对象级接口 smoke：7/7 通过；临时测试用户和角色已清理。

## 启动验证

1. 运行 `RuoYiApplication`，控制台无 ERROR，无 Bean 注入失败
2. `curl http://localhost:8080` 返回欢迎信息
3. Swagger：`http://localhost:8080/swagger-ui.html` 正常打开，ticket 接口可见

## 接口测试

> 所有测试可通过 `scripts/ticket/smoke-test.ps1` 自动执行。

### 分类

| 接口 | 预期 |
|---|---|
| `GET /ticket/category/tree` | 返回树形 JSON，含一级分类 + 子分类 |
| `POST /ticket/category` | 新增成功，`ancestors` 自动计算 |
| `PUT /ticket/category` | 修改成功 |
| `DELETE /ticket/category/{id}` (无子节点) | 成功 |
| `DELETE /ticket/category/{id}` (有子节点) | 返回错误提示 |

### 工单创建

| 接口 | 预期 |
|---|---|
| `POST /ticket` (完整字段) | `ticketNo` 格式 `TK20260625xxxx`，`status = NEW` |
| `POST /ticket` (不传 priority) | 默认 `MEDIUM` |
| `POST /ticket` (title 为空) | 校验错误 |

### 工单列表

| 筛选条件 | 预期 |
|---|---|
| 无参数 | 分页返回全部 |
| `?status=NEW` | 只返回 NEW |
| `?priority=HIGH` | 只返回 HIGH |
| `?categoryId=1` | 只返回该分类 |
| `?keyword=打印机` | 模糊匹配 title/content |
| 普通用户 | 只看自己创建或指派给自己的 |

### 工单详情

| 接口 | 预期 |
|---|---|
| `GET /ticket/{id}` | 含分类名、创建人/指派人昵称、部门名、评论列表、操作日志 |
| `GET /ticket/{不存在id}` | 错误提示 |

### 合法状态流转

| 操作 | 从 → 到 | 预期 |
|---|---|---|
| `PUT /{id}/assign` | NEW → PROCESSING | 成功，`processedAt` 有值 |
| `PUT /{id}/process` | PROCESSING → WAIT_CONFIRM | 成功 |
| `PUT /{id}/confirm` | WAIT_CONFIRM → CLOSED | 成功，`closedAt` 有值 |
| `PUT /{id}/cancel` | NEW → CANCELLED | 成功 |
| `PUT /{id}/cancel` | PROCESSING → CANCELLED | 成功 |

### 非法状态流转

| 操作 | 场景 | 预期 |
|---|---|---|
| assign | CLOSED / PROCESSING / CANCELLED | 错误"不允许分派" |
| process | NEW / CLOSED / CANCELLED | 错误"不允许处理" |
| process (非指派人) | PROCESSING | 错误"不是指派人" |
| process (comment 为空) | PROCESSING | 错误"不能为空" |
| confirm | NEW / PROCESSING / CLOSED / CANCELLED | 错误"不允许确认" |
| confirm (非创建人且非管理员) | WAIT_CONFIRM | 错误"不是创建人" |
| cancel | WAIT_CONFIRM / CLOSED / CANCELLED | 错误"不允许取消" |
| cancel (comment 为空) | NEW | 错误"不能为空" |
| cancel (非创建人且非管理员) | NEW | 错误"不是创建人" |
| assign (assigneeId 不存在) | NEW | 错误"指派人不存在" |

### 评论

| 接口 | 预期 |
|---|---|
| `POST /{ticketId}/comment` | 成功，详情中可见 |
| `GET /{ticketId}/comment` | 按时间倒序，含评论人昵称 |
| `POST` (content 为空) | 校验错误 |

### 操作日志

| 操作 | 预期日志 |
|---|---|
| 创建工单 | `operationType = CREATE`，`fromStatus = null`，`toStatus = NEW` |
| 分派 | `operationType = ASSIGN`，`fromStatus = NEW`，`toStatus = PROCESSING` |
| 处理 | `operationType = PROCESS`，`fromStatus = PROCESSING`，`toStatus = WAIT_CONFIRM` |
| 确认 | `operationType = CONFIRM`，`fromStatus = WAIT_CONFIRM`，`toStatus = CLOSED` |
| 取消 | `operationType = CANCEL`，`fromStatus = NEW/PROCESSING`，`toStatus = CANCELLED` |

### 权限

| 场景 | 预期 |
|---|---|
| 未登录访问任何接口 | 401 |
| 无 `ticket:ticket:add` 权限创建工单 | 403 |
| 普通用户查看列表 | 只看自己的工单 |
| 管理员查看列表 | 全部工单 |

## 发布检查

- [x] `mvn clean compile` 通过
- [x] `mvn test` — 34/34 全部通过
- [x] `scripts/ticket/smoke-test.ps1` — 全部通过
- [x] `git diff` 确认基础模块仅修复 URL 废弃 API，无破坏性修改
- [x] Swagger ticket 接口文档完整
- [x] CI/CD — GitHub Actions 流水线已配置
- [x] PR 模板 — `.github/PULL_REQUEST_TEMPLATE.md`
- [x] PR 已创建 — `feature/module-skeleton` → `main`

## Bug 修复记录

| # | 严重度 | 问题 | 修复 |
|---|---|---|---|
| 1 | 🔴 CRITICAL | SQL 注入：`${params.dataScope}` 可被管理员注入 | 显式设置 dataScope，阻断用户输入 |
| 2 | 🟠 HIGH | 祖先路径更新遗漏直接子节点 | 精确匹配 + 前缀匹配覆盖 |
| 3 | 🟡 MEDIUM | 工单编号生成竞态 | `generateTicketNo()` 加 synchronized |
| 4 | 🟡 MEDIUM | `getCreatorId().equals()` NPE | 改用 `Objects.equals()` |
| 5 | 🟢 LOW | `new URL()` 废弃 API | 改用 `URI.create().toURL()` |

## v1.1 测试清单

### SLA 策略

| 场景 | 预期 |
|---|---|
| 查询四种默认策略 | 优先级唯一，时限值与增量 SQL 一致 |
| 新增重复优先级 | 拒绝并提示策略已存在 |
| `responseMinutes <= 0` | 参数校验失败 |
| `resolveMinutes <= responseMinutes` | 业务校验失败 |
| 停用策略后创建对应优先级工单 | 拒绝创建 |

### 时限快照

| 场景 | 预期 |
|---|---|
| 创建四种优先级工单 | 按各自策略生成响应和解决截止时间 |
| 修改策略后查询旧工单 | 旧工单截止时间不变 |
| 截止时间恰好等于完成时间 | 不超时 |
| 列表按超时标记筛选 | 只返回符合条件且有权查看的工单 |

### 扫描与告警

| 场景 | 预期 |
|---|---|
| NEW 工单超过响应截止时间 | 响应超时标记为 `1`，生成响应告警 |
| PROCESSING/WAIT_CONFIRM 超过解决截止时间 | 解决超时标记为 `1`，生成解决告警 |
| 首次分派或关闭发生在截止时间前 | 不生成对应告警 |
| 首次分派或关闭发生在截止时间后 | 标记超时并生成对应告警 |
| CLOSED 工单再次扫描 | 不新增告警，已有超时事实不改变 |
| CANCELLED 工单扫描 | 不生成新告警 |
| 同一批数据连续扫描两次 | 每种告警类型最多一条记录 |
| 两次任务并发执行 | 唯一键阻止重复告警，任务无未处理异常 |

### 权限与回归

| 场景 | 预期 |
|---|---|
| 普通用户维护策略或手工补扫 | 403 |
| 普通用户查看全量告警 | 403 |
| 普通用户查看自己的工单详情 | 仅返回该工单 SLA 信息 |
| v1.0 分类、主流程、评论、日志 smoke | 全部继续通过 |

## v1.1 发布门禁

- [x] `sql/ticket-v1.1.sql` 在 v1.0 数据库成功执行，2 张表、4 个字段、4 条策略正确
- [x] 迁移后存量活动工单截止时间回填正确
- [x] `mvn test` — 51/51 全部通过
- [x] `mvn clean compile` 通过
- [x] 后端启动无 ERROR
- [x] `scripts/ticket/smoke-test.ps1` — 35/35 全部通过
- [x] Quartz 手工执行、重复扫描幂等和自动任务配置验证通过
- [x] Swagger SLA 接口文档完整

## v1.2 发布门禁

- [x] 六类通知事件、用户隔离、未读数和已读操作测试通过
- [x] 评价身份、状态、唯一性、评分边界和统计测试通过
- [x] `sql/ticket-v1.2.sql` 在 v1.1 数据库执行成功，2 张表和 6 条权限正确
- [x] `mvn test` — 65/65 全部通过，`mvn clean compile` 通过
- [x] 后端启动无 ERROR，v1.0–v1.2 smoke 46/46 通过

## v1.3 发布门禁

- [x] 五种角色数据范围和多角色并集接口验证通过
- [x] 请求侧 `params.dataScope` 注入不影响服务端范围
- [x] 范围外详情、评论、日志、评价和状态操作统一返回“工单不存在”
- [x] 跨部门指派后当前指派人可访问工单
- [x] `mvn test` — 75/75 全部通过
- [x] `mvn clean compile` 通过
- [x] 后端启动无 ERROR，完整 smoke 61/61 通过
- [x] 临时角色和用户均由 smoke 脚本清理

## v2.0 动态流程测试清单

### 定义与版本

| 场景 | 预期 |
|---|---|
| 合法草稿发布 | 发布成功并成为当前版本 |
| 缺少 START/END、存在不可达节点 | 拒绝发布 |
| 人工节点未配置处理人 | 拒绝发布 |
| 条件分支无默认连线或多个默认连线 | 拒绝发布 |
| 修改已发布定义 | 拒绝且数据不变 |
| 复制并发布新版本 | 新工单使用新版本，旧实例保持旧版本 |
| 分类未绑定流程 | 使用内置标准流程 |

### 路由与处理人

| 场景 | 预期 |
|---|---|
| 优先级、分类、创建人部门条件命中 | 分别进入目标节点 |
| 无普通条件命中 | 进入唯一默认分支 |
| 伪造字段、运算符或表达式 | 保存或发布时拒绝 |
| 指定用户节点 | 仅固化用户可处理 |
| 指定角色节点 | 任一有效角色成员可处理，其他用户拒绝 |
| 创建人部门负责人节点 | 使用工单部门快照解析并固化负责人 |
| 工单指派人节点 | 使用当前指派人并固化到任务 |
| 工单创建人节点 | 使用创建人并固化到任务 |
| 无有效用户、角色或部门负责人 | 当前动作回滚，不跳过节点 |

### 运行与兼容

| 场景 | 预期 |
|---|---|
| 完成当前任务 | 旧任务完成并生成唯一下一任务 |
| 重复或并发完成 | 最多一个请求成功，不重复流转 |
| 退回上一人工节点 | 生成新任务，历史任务完整保留 |
| 首节点退回或跨节点退回 | 拒绝 |
| 创建人/管理员取消 | 实例和工单均取消，待办关闭 |
| 管理员终止 | 实例终止、工单取消，非管理员 403 |
| v2.0 前历史工单 | 不创建实例，原接口仍可完成流程 |
| 标准流程新工单 | 旧接口行为及状态、日志、通知兼容 |
| 越权访问实例或任务 | 返回“工单不存在”或权限错误，不泄露数据 |

## v2.0 发布门禁

- [x] `sql/ticket-v2.0.sql` 在 v1.3 数据库执行成功，5 张表、分类字段、标准流程和权限正确
- [x] 定义图校验、版本不可变、条件路由、五种处理人解析、并发幂等测试全部通过
- [x] v1.0–v1.3 单元测试和 smoke 全部回归通过
- [x] `mvn test` 与 `mvn clean compile` 通过
- [x] 后端启动无 ERROR，Swagger 中 v2.0 接口完整
- [x] `scripts/ticket/smoke-test.ps1` 完整通过；开发库测试记录已记录，未自动删除

### 阶段二十三实测结果（2026-07-02）

- `sql/ticket-v2.0.sql` 已在 Docker MySQL 的 v1.3 数据库执行成功。
- 5 张流程表、分类绑定字段、1 个标准流程、5 个节点、4 条连线和 6 条菜单记录正确。
- 流程基础模型测试 3/3 通过，全量单元测试 78/78 通过。
- 流程 Mapper XML 未使用 `${}` 拼接；`mvn clean compile` 全模块通过。

### 阶段二十四实测结果（2026-07-02）

- 流程定义 Service 目标测试 6/6 通过，全量 `mvn test` 和 `mvn clean compile` 通过。
- 接口 smoke 覆盖列表、创建草稿、详情、发布、重复发布拒绝和分类绑定，全部通过。
- 发布时校验 START/END、节点可达、处理人、默认连线和条件白名单。
- smoke 保留发布定义 `SMOKE_1782994011`（definition_id=2），未执行自动删除。

### 阶段二十五实测结果（2026-07-02）

- 标准流程补充分派节点，当前结构为 `START → ASSIGN → PROCESS → CONFIRM → END`。
- 流程引擎启动目标测试 7/7 通过，覆盖版本回退、优先级路由和五种处理人规则。
- 全量 `mvn test` 和 `mvn clean compile` 通过。
- 创建接口 smoke 生成 ticket_id=25、instance_id=2，锁定 definition_id=1，当前节点为 `ASSIGN`，待办唯一。
- 首次失败的 smoke 额外保留一条同类测试工单；未执行自动删除。

### 阶段二十六实测结果（2026-07-02）

- 任务动作与查询目标测试 13/13 通过，全量单元测试、`mvn clean compile` 和打包通过。
- 条件更新保证同一待办只完成一次，重复提交在生成下一任务前被拒绝。
- 接口 smoke 覆盖我的待办、任务详情、分派、处理、确认、重复提交、取消和管理员终止，全部通过。
- 最终 smoke 数据：完成 ticket_id=29、取消 ticket_id=30、终止 ticket_id=31；未执行自动删除。

### 阶段二十七实测结果（2026-07-03）

- 新流程工单通过旧 `assign/process/confirm/cancel` 接口适配当前流程任务；历史无实例工单保留原状态机。
- 兼容与任务动作目标测试 31/31 通过，全量 `mvn test`、`mvn clean compile` 和打包通过。
- v1.0–v1.3 完整 smoke 61/61 通过，覆盖 SLA、通知、评价、评论、日志和部门数据权限。
- `smoke-test.ps1` 必须使用 PowerShell 7（`pwsh`）执行；Windows PowerShell 5.1 无法正确解析 UTF-8 无 BOM 中文脚本。

### 阶段二十八实测结果（2026-07-03）

- 发布并发使用 `SELECT ... FOR UPDATE` 按 `workflow_key` 串行化，发布校验新增环路拒绝。
- `ruoyi-ticket` 单元测试 109/109 通过，`mvn clean compile` 和打包通过。
- 主 smoke 脚本已整合三组 v2.0 流程测试，最终 64/64 通过。
- OpenAPI 已包含流程定义和流程任务接口；后端启动、关闭正常。
- Docker 数据库存在 5 张流程表，无重复当前版本，无孤立节点、任务或实例。
- 未发现阶段 TODO、`${}` SQL 拼接、基础模块改动或未预期配置变更。
