# 01 — 项目规格

> v1.0 | 2026-06-25 | 状态: 实现中

## 项目

企业流程工单管理系统，基于 RuoYi-Vue Spring Boot 3 二开。复用登录、权限、用户、部门、菜单等基础能力，新增 `ruoyi-ticket` 工单模块。

**解决的问题**：派活无记录难追溯、进度不透明、无法统计工单量、无统一服务入口。

## v1.0 功能范围

| 功能 | 说明 |
|---|---|
| 工单创建 | 标题、内容、分类、优先级 |
| 工单列表 | 分页 + 状态/优先级/分类/关键词/日期筛选 |
| 工单详情 | 基本信息 + 分类/用户/部门名称 + 评论 + 操作日志 |
| 工单分派 | 管理员指派处理人，NEW → PROCESSING |
| 工单处理 | 处理人提交结果，PROCESSING → WAIT_CONFIRM |
| 工单确认 | 创建人确认关闭，WAIT_CONFIRM → CLOSED |
| 工单取消 | 创建人或管理员取消，NEW/PROCESSING → CANCELLED |
| 评论 | 沟通记录，INTERNAL 内部备注 / EXTERNAL 公开评论 |
| 操作日志 | 每次流转记录 who / from / to / comment |
| 分类管理 | 树形分类，parentId + ancestors 路径 |

## v1.0 不做什么

SLA / 满意度 / 附件 / 模板 / 通知推送 / 批量操作 / 数据权限 / 父子工单 / ES 检索 / 导出 / Flowable / Python / RAG / Agent

## 工单状态流转

```
NEW ──分派──▶ PROCESSING ──处理──▶ WAIT_CONFIRM ──确认──▶ CLOSED
 │                 │
 └──取消───────────┘
```

- 5 个状态，5 条流转，`TicketStatus` 枚举内部 `allowedTransitions` 控制
- `CLOSED` / `CANCELLED` 终态
- 每次流转强制写 `ticket_operation_log`
- 不做可视化流程设计器，不做条件分支

## Redis 边界

v1.0 不引入新的 Redis 缓存。仅维持 RuoYi 原有：Token 持久化、验证码、在线用户统计。工单列表走 MySQL + PageHelper。

## 验收标准

1. 创建工单后 `ticketNo` 格式 `TK202606250001`，`status = NEW`
2. 分派后 `status = PROCESSING`，`assigneeId` 更新，`processedAt` 有值
3. 处理后 `status = WAIT_CONFIRM`，处理备注必填
4. 确认后 `status = CLOSED`，`closedAt` 有值
5. `NEW` / `PROCESSING` 可取消 → `CANCELLED`
6. 非法流转返回错误（终态不可操作、非指派人不可处理、非创建人不可确认/取消）
7. 每次流转 `ticket_operation_log` 一条记录
8. 详情返回完整信息（分类名、创建人/指派人昵称、部门名、评论、日志）
9. 列表支持多条件筛选和分页

## 后续规划

| 版本 | 方向 |
|---|---|
| v1.1 | SLA 时效管理、超时告警 |
| v1.2 | 通知推送、满意度评价 |
| v1.3 | 部门级数据权限 |
| v2.0 | 动态流程、自定义字段、附件、ES |
| v2.1 | 知识库 / RAG / Agent |
