# 05 — 测试与发布策略

> 版本: v1.0 | 日期: 2026-06-25

---

## 1. 启动前检查

逐项确认，全部通过后再启动：

- [ ] MySQL 已启动，`application-druid.yml` 中连接信息正确
- [ ] Redis 已启动，`application.yml` 中 host/port 正确，密码一致
- [ ] RuoYi 原有表已通过初始化 SQL 创建（`sys_user`、`sys_dept`、`sys_role`、`sys_menu` 等）
- [ ] ticket 4 张表已通过 `docs/03-database-design.md` §8 SQL 创建
- [ ] 默认分类数据已插入 `ticket_category`
- [ ] 工单菜单和权限标识已在 RuoYi 后台配置
- [ ] 根 `pom.xml` 含 `ruoyi-ticket` 的 `<module>` 和 `<dependencyManagement>`
- [ ] `ruoyi-admin/pom.xml` 含 `ruoyi-ticket` 依赖
- [ ] `mvn clean compile` 通过，无编译错误

---

## 2. 后端启动验证

1. 运行 `ruoyi-admin` 中的 `RuoYiApplication`
2. 观察控制台：无 `ERROR` 日志，无 Bean 注入失败
3. 访问 `http://localhost:8080`，返回 "欢迎使用 RuoYi" 或类似提示
4. 调用 `POST /login`，用默认管理员账号登录，返回 token
5. 访问 `http://localhost:8080/swagger-ui.html`，页面正常打开
6. Swagger 中选择 ticket 相关 group（如有配置），ticket 接口列表可见
7. 如 Swagger 未显示 ticket 接口，检查 Springdoc 扫描配置和包路径

---

## 3. 数据库验证

逐项在数据库工具中确认：

- [ ] `ticket_category` 表存在，至少 3 条默认数据
- [ ] `ticket` 表存在，字段类型与设计一致
- [ ] `ticket_comment` 表存在
- [ ] `ticket_operation_log` 表存在
- [ ] `del_flag` 字段默认值为 `'0'`（ticket / ticket_category / ticket_comment）
- [ ] `ticket.priority` / `ticket.status` / `ticket.creator_id` / `ticket.dept_id` / `ticket.del_flag` 为 NOT NULL
- [ ] `ticket_category.del_flag` / `ticket_comment.del_flag` 为 NOT NULL
- [ ] 索引全部存在（`uk_ticket_no`、各 `idx_*`）
- [ ] 无数据库级外键约束（逻辑外键，一致性由 Service 层保证）
- [ ] 随便查一条分类数据 `SELECT * FROM ticket_category WHERE del_flag = '0'`，确认 `ancestors` 字段格式正确

---

## 4. 接口测试清单

所有测试通过 Swagger UI 或 Postman 执行。以下用 `{{token}}` 表示登录后获取的 Authorization header。

### 5.1 工单分类

#### GET /ticket/category/tree — 分类树

| 项目 | 内容 |
|---|---|
| 测试目标 | 获取分类树结构 |
| 前置条件 | 已登录，有 `ticket:category:list` 权限 |
| 请求方式 | GET，无参数 |
| 预期结果 | 返回树形 JSON，根节点 children 含一级分类，一级分类 children 含二级分类 |

#### GET /ticket/category/list — 分类列表

| 项目 | 内容 |
|---|---|
| 测试目标 | 获取平铺分类列表 |
| 前置条件 | 已登录 |
| 预期结果 | 返回所有 `del_flag = '0'` 的分类，按 `order_num` 排序 |

#### POST /ticket/category — 新增分类

| 项目 | 内容 |
|---|---|
| 测试目标 | 新增一条分类 |
| 前置条件 | 有 `ticket:category:add` 权限 |
| 请求方式 | POST，body: `{"parentId": 1, "categoryName": "测试分类", "orderNum": 1}` |
| 预期结果 | 返回成功，`ancestors` 自动计算，再查 tree 可见新节点 |

#### PUT /ticket/category — 修改分类

| 项目 | 内容 |
|---|---|
| 测试目标 | 修改分类名称 |
| 前置条件 | 有 `ticket:category:edit` 权限 |
| 预期结果 | 返回成功，名称已更新 |

#### DELETE /ticket/category/{id} — 删除分类

| 项目 | 内容 |
|---|---|
| 测试目标 | 删除无子节点的分类 |
| 前置条件 | 有 `ticket:category:remove` 权限，分类无子节点 |
| 预期结果 | 返回成功，`del_flag` 变为 `'2'` |
| 补充测试 | 删除有子节点的分类 → 返回错误提示 |

---

### 5.2 工单创建

#### POST /ticket — 创建工单

| 项目 | 内容 |
|---|---|
| 测试目标 | 用户创建一条工单 |
| 前置条件 | 已登录，有 `ticket:ticket:add` 权限 |
| 请求方式 | POST，body: `{"title": "测试工单", "content": "测试内容", "categoryId": 1, "priority": "MEDIUM"}` |
| 预期结果 | 返回 200，`ticketNo` 格式 `TK20260625xxxx`，`status = NEW`，`creator_id = 当前用户`，`dept_id = 当前用户部门` |

| 补充测试 | 预期结果 |
|---|---|
| title 为空 | 返回校验错误 |
| 不传 priority | 默认 `MEDIUM` |
| 不传 categoryId | `category_id = NULL` |

---

### 5.3 工单列表

#### GET /ticket/list — 工单列表

| 项目 | 内容 |
|---|---|
| 测试目标 | 分页查询工单列表 |
| 前置条件 | 已登录，有 `ticket:ticket:list` 权限，已有至少 2 条工单 |
| 请求方式 | GET，`?pageNum=1&pageSize=10` |
| 预期结果 | 返回分页数据，含 `total`、`rows` |

| 筛选条件测试 | 预期结果 |
|---|---|
| `?status=NEW` | 只返回 NEW 状态 |
| `?priority=HIGH` | 只返回 HIGH 优先级 |
| `?categoryId=1` | 只返回该分类下的工单 |
| `?keyword=打印机` | title 或 content 含"打印机" |
| `?beginTime=2026-06-01&endTime=2026-06-30` | 只返回该时间范围内创建的工单 |
| 普通用户 | 只看自己创建或指派给自己的 |

---

### 5.4 工单详情

#### GET /ticket/{id} — 工单详情

| 项目 | 内容 |
|---|---|
| 测试目标 | 查看工单完整信息 |
| 前置条件 | 已登录，有 `ticket:ticket:query` 权限 |
| 预期结果 | 返回工单基本信息 + 分类名称 + 创建人昵称 + 部门名称 + 评论列表 + 操作日志 |
| 补充测试 | 传入不存在的 id → 返回错误提示 |

---

### 5.5 工单分派

#### PUT /ticket/{id}/assign — 分派工单

| 项目 | 内容 |
|---|---|
| 测试目标 | 管理员将 NEW 状态工单指派给处理人 |
| 前置条件 | 工单状态为 NEW，有 `ticket:ticket:assign` 权限 |
| 请求方式 | PUT，body: `{"assigneeId": 10}` |
| 预期结果 | 返回成功，`status = PROCESSING`，`assignee_id = 10`，`processed_at` 有值 |

| 补充测试 | 预期结果 |
|---|---|
| assigneeId 为空或不存在 | 返回错误提示 |
| 工单状态不是 NEW | 返回"当前状态不允许分派" |
| 无分派权限的用户调用 | 返回 403 |

---

### 5.6 工单处理

#### PUT /ticket/{id}/process — 处理工单

| 项目 | 内容 |
|---|---|
| 测试目标 | 指派人完成处理，提交待确认 |
| 前置条件 | 工单状态为 PROCESSING，当前用户是指派人，有 `ticket:ticket:process` 权限 |
| 请求方式 | PUT，body: `{"comment": "已处理完成，请确认"}` |
| 预期结果 | 返回成功，`status = WAIT_CONFIRM` |

| 补充测试 | 预期结果 |
|---|---|
| 非指派人调用 | 返回错误提示 |
| comment 为空 | 返回校验错误 |
| 工单状态不是 PROCESSING | 返回"当前状态不允许处理" |

---

### 5.7 工单确认

#### PUT /ticket/{id}/confirm — 确认工单

| 项目 | 内容 |
|---|---|
| 测试目标 | 创建人确认处理结果，关闭工单 |
| 前置条件 | 工单状态为 WAIT_CONFIRM，当前用户是创建人，有 `ticket:ticket:confirm` 权限 |
| 请求方式 | PUT，body: `{"comment": "确认没问题"}`或空 body |
| 预期结果 | 返回成功，`status = CLOSED`，`closed_at` 有值 |

| 补充测试 | 预期结果 |
|---|---|
| 非创建人（且非管理员）调用 | 返回错误提示 |
| 工单状态不是 WAIT_CONFIRM | 返回"当前状态不允许确认" |
| 管理员确认非自己的工单 | 允许 |

---

### 5.8 工单取消

#### PUT /ticket/{id}/cancel — 取消工单

| 项目 | 内容 |
|---|---|
| 测试目标 | 创建人或管理员取消工单 |
| 前置条件 | 工单状态为 NEW 或 PROCESSING，当前用户是创建人或管理员，有 `ticket:ticket:cancel` 权限 |
| 请求方式 | PUT，body: `{"comment": "不需要处理了"}` |
| 预期结果 | 返回成功，`status = CANCELLED` |

| 补充测试 | 预期结果 |
|---|---|
| 非创建人且非管理员调用 | 返回错误提示 |
| 工单状态为 WAIT_CONFIRM | 返回"当前状态不允许取消" |
| 工单状态为 CLOSED | 返回"当前状态不允许取消" |
| comment 为空 | 返回校验错误 |

---

### 5.9 评论

#### POST /ticket/{ticketId}/comment — 添加评论

| 项目 | 内容 |
|---|---|
| 测试目标 | 给工单添加评论 |
| 前置条件 | 已登录，有 `ticket:comment:add` 权限 |
| 请求方式 | POST，body: `{"content": "请尽快处理", "commentType": "EXTERNAL"}` |
| 预期结果 | 返回成功，工单详情中评论列表新增一条 |

| 补充测试 | 预期结果 |
|---|---|
| content 为空 | 返回校验错误 |
| commentType 不传 | 默认 `EXTERNAL` |

#### GET /ticket/{ticketId}/comments — 查看评论

| 项目 | 内容 |
|---|---|
| 测试目标 | 查看工单的所有评论 |
| 前置条件 | 已登录，有 `ticket:comment:list` 权限 |
| 预期结果 | 返回评论列表，按时间倒序，含评论人昵称 |

---

### 5.10 操作日志

#### GET /ticket/{ticketId}/logs — 查看操作日志

| 项目 | 内容 |
|---|---|
| 测试目标 | 查看工单的操作日志 |
| 前置条件 | 已登录，有 `ticket:log:list` 权限，工单已经过至少一次流转 |
| 预期结果 | 返回日志列表，按时间倒序，含 `operationType`、`fromStatus`、`toStatus`、`operatorName`、`comment` |

---

## 5. 状态流转测试

### 6.1 合法流转

| # | 操作 | 从 → 到 | 预期结果 |
|---|---|---|---|
| 1 | 分派 | NEW → PROCESSING | 成功 |
| 2 | 处理 | PROCESSING → WAIT_CONFIRM | 成功 |
| 3 | 确认 | WAIT_CONFIRM → CLOSED | 成功 |
| 4 | 取消(NEW) | NEW → CANCELLED | 成功 |
| 5 | 取消(PROCESSING) | PROCESSING → CANCELLED | 成功 |

### 6.2 非法流转

| # | 操作 | 场景 | 预期结果 |
|---|---|---|---|
| 1 | 分派 CLOSED 工单 | CLOSED → ? | 返回错误 |
| 2 | 分派 PROCESSING 工单 | PROCESSING → ? | 返回错误 |
| 3 | 处理 NEW 工单 | NEW → ? | 返回错误 |
| 4 | 处理 CLOSED 工单 | CLOSED → ? | 返回错误 |
| 5 | 确认 NEW 工单 | NEW → ? | 返回错误 |
| 6 | 确认 PROCESSING 工单 | PROCESSING → ? | 返回错误 |
| 7 | 取消 CLOSED 工单 | CLOSED → ? | 返回错误 |
| 8 | 取消 CANCELLED 工单 | CANCELLED → ? | 返回错误 |
| 9 | 取消 WAIT_CONFIRM 工单 | WAIT_CONFIRM → ? | 返回错误 |
| 10 | 对已取消工单做任何操作 | CANCELLED → ? | 全部返回错误 |

### 6.3 终态确认

- `CLOSED` 工单：任何流转操作均失败
- `CANCELLED` 工单：任何流转操作均失败

---

## 6. 权限测试

| # | 测试场景 | 预期结果 |
|---|---|---|
| 1 | 未登录访问 `/ticket/list` | 返回 401 |
| 2 | 无 `ticket:ticket:add` 权限的用户创建工单 | 返回 403 |
| 3 | 无 `ticket:ticket:assign` 权限的用户分派工单 | 返回 403 |
| 4 | 普通用户 A 查看工单列表 | 只看到 `creator_id = A` 或 `assignee_id = A` 的工单 |
| 5 | 管理员查看工单列表 | 看到全部工单 |
| 6 | 非指派人 B 处理指派给 A 的工单 | 返回错误 |
| 7 | 用户 B 确认用户 A 创建的工单（B 非管理员） | 返回错误 |
| 8 | 用户 B 取消用户 A 创建的工单（B 非管理员） | 返回错误 |
| 9 | 管理员确认非自己创建的工单 | 允许 |
| 10 | 有分派权限的用户分派工单 | 允许 |

---

## 7. 操作日志测试

### 8.1 业务日志覆盖

| # | 操作 | 预期 ticket_operation_log |
|---|---|---|
| 1 | 创建工单 | `operation_type = CREATE`，`from_status = NULL`，`to_status = NEW` |
| 2 | 分派 | `operation_type = ASSIGN`，`from_status = NEW`，`to_status = PROCESSING` |
| 3 | 处理 | `operation_type = PROCESS`，`from_status = PROCESSING`，`to_status = WAIT_CONFIRM` |
| 4 | 确认 | `operation_type = CONFIRM`，`from_status = WAIT_CONFIRM`，`to_status = CLOSED` |
| 5 | 取消 | `operation_type = CANCEL`，`from_status = NEW 或 PROCESSING`，`to_status = CANCELLED` |

### 8.2 日志内容验证

- `operator_id` 为实际操作人 ID
- `operator_name` 为操作人账号名
- `comment` 为操作时填写的备注内容
- `operate_time` 记录操作时间
- 日志不可修改、无删除接口

### 8.3 双层日志区分

| 日志表 | 记录者 | 内容 |
|---|---|---|
| `sys_oper_log` | `@Log` 注解自动写入 | API 请求参数、返回结果、耗时、IP |
| `ticket_operation_log` | Service 层手动写入 | 状态流转 who / from / to / comment |

验证：一次分派操作后，`sys_oper_log` 有一条 API 日志，`ticket_operation_log` 有一条业务日志。

---

## 8. 发布前检查

代码层面：

- [ ] `mvn clean compile` 通过，无编译错误
- [ ] `git diff` 确认改动符合预期，无意外修改
- [ ] 代码无明显的 Alibaba Java Coding Guidelines 违规

数据库层面：

- [ ] SQL 首次执行前确认目标库不存在同名 ticket 表；如需重复执行，先手动备份并清理旧表，避免误删数据。
- [ ] 4 张表的目标库中已存在且结构正确
- [ ] 默认分类数据插入语句有去重逻辑或标记为「仅首次执行」

文档层面：

- [ ] README 已补充 ticket 模块说明
- [ ] Swagger 可正常访问，ticket 接口文档完整