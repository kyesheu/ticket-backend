# CLAUDE.md

## 项目说明

本项目是基于 RuoYi-Vue Spring Boot 3 二次开发的企业流程工单管理系统后端。

v1.0 新增 `ruoyi-ticket` 模块，完成工单创建、分派、处理、确认、关闭/取消、评论、操作日志等主流程。

目标是在理解若依基础能力的前提下扩展业务模块；如确有必要，可以对基础模块做小范围、可解释、可回滚的调整。

## 先读文档

改代码前，按任务阅读相关文档：

- `docs/01-project-spec.md`：项目边界
- `docs/02-architecture-design.md`：架构规范
- `docs/03-database-design.md`：数据库设计
- `docs/04-implementation-plan.md`：实施计划
- `docs/05-test-release.md`：测试发布

开发顺序必须按照 `docs/04-implementation-plan.md` 分阶段推进，不要跳阶段。

写代码时遵守 `alibaba-java-coding-guidelines-skill`。

## 开发流程

每次开发前先说明：

1. 当前处于实施计划的哪个阶段
2. 准备改哪些文件
3. 为什么要改
4. 是否涉及若依基础模块
5. 如何验证

每次只做一个阶段或一个功能，不要一次性生成大量无关代码。

## 测试要求

每完成一个功能，必须完成自测。

优先补充单元测试或核心逻辑测试，尤其是：

- `TicketStatus` 状态流转
- 工单分派 / 处理 / 确认 / 取消
- 非法状态流转
- 权限与数据范围判断
- 操作日志写入

如果当前测试环境不完整，也必须至少完成：

- `mvn clean compile`
- Swagger / Postman 接口自测
- 合法流程测试
- 非法流程测试
- 权限测试
- 说明测试结果

不要在没有验证的情况下声称功能完成。