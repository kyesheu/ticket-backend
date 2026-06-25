# 04 — 实施计划

> v1.0 | 2026-06-25 | 8 个阶段

## 阶段一：准备模块结构

建 `ruoyi-ticket` Maven 模块，打通编译。

- 创建 `ruoyi-ticket/` 标准 Maven 目录 + `pom.xml`（只依赖 `ruoyi-common`）
- 根 `pom.xml` 加 `<module>` + `<dependencyManagement>`
- `ruoyi-admin/pom.xml` 加依赖
- 创建 `resources/mapper/` 目录
- 验收：`mvn clean compile` 通过

## 阶段二：数据库初始化

- 执行 `sql/ticket-v1.0.sql`（4 张表 + 默认分类 + 菜单权限）
- 验收：4 张表存在，至少 3 条分类数据，菜单权限可见

## 阶段三：基础代码结构

按功能需要建 Domain、DTO、VO、Enum、Mapper、Service、Controller 骨架。

- `TicketOperationLog` 不继承 `BaseEntity`
- `TicketStatus` 含 `allowedTransitions` 静态块
- 验收：编译通过，字段与数据库列对应

## 阶段四：工单分类模块

- `TicketCategoryMapper.xml`：列表、树查询、详情、新增、修改、删除、子节点计数、唯一性校验
- `TicketCategoryServiceImpl`：树构建（内存递归）、新增自动计算 `ancestors`、修改级联更新子孙、删除校验子节点
- `TicketCategoryController`：list / tree / getInfo / add / edit / remove
- 验收：树结构正确、新增后出现在正确位置、有子节点时拒绝删除

## 阶段五：工单主流程（核心）

- `TicketMapper.xml`：列表（含数据范围）、详情（4 表 JOIN）、实体查询、新增、更新、逻辑删除、编号查询、用户校验
- `TicketServiceImpl`：创建（编号生成）、分派、处理、确认、取消。每次流转校验状态 + 写日志
- `TicketController`：list / detail / create / assign / process / confirm / cancel / logs
- `TicketCommentServiceImpl`：添加评论、查询评论
- `TicketCommentController`：评论列表、添加评论
- 验收：10 条 v1.0 验收标准全部通过

## 阶段六：评论和操作日志

- `TicketOperationLogMapper.xml`：按工单查询、新增
- 确认阶段五的分派/处理/确认/取消日志写入正确
- 验收：创建工单产生 CREATE 日志，每次流转产生对应日志，评论列表正常

## 阶段七：权限和数据范围

- 所有 CUD 接口加 `@PreAuthorize`
- `TicketServiceImpl.selectTicketList` 中实现数据范围：管理员看全部，普通用户看自己的
- 验收：无权限返回 403，普通用户列表只看到自己的工单

## 阶段八：联调收尾

- 启动应用，Swagger 确认 ticket 接口可见
- 按验收标准逐条测试（合法 + 非法流转）
- `git diff` 确认无基础模块意外修改
- 提交代码
