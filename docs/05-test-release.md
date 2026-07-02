# 05 — 测试与发布

> v1.2 | 2026-07-02 | v1.1 已通过，v1.2 测试计划

## 启动前检查

- [x] MySQL / Redis 已启动，`.env` 配置正确
- [x] RuoYi 基础表已创建（`sys_user`、`sys_dept` 等）
- [x] ticket 4 张表已创建，8 条默认分类已插入
- [x] `mvn clean compile` 通过

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

- [ ] 六类通知事件、用户隔离、未读数和已读操作测试通过
- [ ] 评价身份、状态、唯一性、评分边界和统计测试通过
- [ ] `sql/ticket-v1.2.sql` 在 v1.1 数据库执行成功
- [ ] `mvn test`、`mvn clean compile` 全部通过
- [ ] 后端启动无 ERROR，v1.0–v1.2 smoke 全部通过
