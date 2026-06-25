# 05 — 测试与发布

> v1.0 | 2026-06-25

## 启动前检查

- [ ] MySQL / Redis 已启动，`.env` 配置正确
- [ ] RuoYi 基础表已创建（`sys_user`、`sys_dept` 等）
- [ ] ticket 4 张表已创建，默认分类已插入
- [ ] `mvn clean compile` 通过

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

- [ ] `mvn clean compile` 通过
- [ ] `mvn test` 全部通过
- [ ] `scripts/ticket/smoke-test.ps1` 全部通过
- [ ] `git diff` 确认无基础模块意外修改
- [ ] Swagger ticket 接口文档完整
