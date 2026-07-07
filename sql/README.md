# SQL 迁移脚本

按顺序执行：

```
ry_20260417.sql      →  RuoYi 基础表（部门、用户、角色、菜单等）
quartz.sql           →  Quartz 定时任务表
ticket-v1.0.sql      →  工单主流程、分类、评论、操作日志
ticket-v1.1.sql      →  SLA 时效管理、超时告警
ticket-v1.2.sql      →  站内通知、满意度评价
ticket-v2.0.sql      →  动态流程引擎
ticket-v2.1.sql      →  自定义字段
ticket-v2.2.sql      →  附件管理
ticket-v2.3.sql      →  Elasticsearch 检索事件
ticket-v3.1.sql      →  AI 分诊建议
ticket-v3.2.sql      →  AI 反馈与评测
```

## 无增量 SQL 的版本

| 版本 | 原因 |
|---|---|
| v1.3 | 部门数据权限，复用 RuoYi 现有表 |
| v3.0 | AI 知识库与 RAG，数据存 Elasticsearch，无 MySQL 新表 |
| v3.3 | 生产化收尾，不新增业务表 |

## 约定

- `SET NAMES utf8mb4` 作为每个文件首行。
- 文件头注释注明版本和前置依赖。
- 每个版本只新增，不修改已发布版本的 DDL。
