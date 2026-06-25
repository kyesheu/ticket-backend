# 04 — 实施计划

> 版本: v1.0 | 日期: 2026-06-25 | 8 个阶段

---

## 阶段一：准备模块结构

### 目标

搭建 `ruoyi-ticket` Maven 模块，打通编译和 Spring Boot 自动扫描。

### 主要任务

1. 创建 `ruoyi-ticket/` 目录及标准 Maven 目录结构
2. 编写 `ruoyi-ticket/pom.xml`，只依赖 `ruoyi-common`
3. 根 `pom.xml` 的 `<modules>` 新增 `<module>ruoyi-ticket</module>`
4. 根 `pom.xml` 的 `<dependencyManagement>` 新增 `ruoyi-ticket` 依赖声明
5. `ruoyi-admin/pom.xml` 新增 `ruoyi-ticket` 依赖
6. 在 `ruoyi-ticket` 下创建 `resources/mapper/` 目录，确认 MyBatis XML 会被 `classpath*:mapper/**/*Mapper.xml` 扫描
7. `mvn clean compile` 通过

### 产出文件

```
ruoyi-ticket/pom.xml
ruoyi-ticket/src/main/java/com/ruoyi/ticket/  (空包)
ruoyi-ticket/src/main/resources/mapper/         (空目录)
根 pom.xml                                       (已修改)
ruoyi-admin/pom.xml                              (已修改)
```

### 验收标准

- `mvn clean compile` 无报错
- `ruoyi-ticket` 模块在 IDE 中被识别为 Maven 模块
- 若依原有模块全部不变

### 注意事项

- 不新建 `application.yml`，ticket 模块复用 `ruoyi-admin` 中的配置
- 不修改 `ruoyi-common`、`ruoyi-framework`、`ruoyi-system`
- 包名统一 `com.ruoyi.ticket`，MyBatis typeAliases 自动扫描 `com.ruoyi.**.domain`

---

## 阶段二：数据库初始化

### 目标

在 MySQL 中创建 4 张业务表，插入基础分类数据，在 RuoYi 后台配置菜单和权限标识。

### 主要任务

1. 执行 `docs/03-database-design.md` 中的 4 段建表 SQL
2. 插入默认分类数据（如「IT支持」「行政后勤」「其他」）
3. 登录 RuoYi 后台 → 系统管理 → 菜单管理，添加工单管理菜单树
4. 逐条录入权限标识（共 14 个）
5. 将菜单权限分配给测试角色

### 产出文件

```
SQL 脚本可直接从 docs/03-database-design.md §8 复制
```

### 验收标准

- `ticket_category`、`ticket`、`ticket_comment`、`ticket_operation_log` 表存在且表结构正确
- 至少 3 条分类数据
- RuoYi 左侧菜单出现「工单管理」目录
- `ticket:ticket:list` 等权限标识在菜单管理中可见

### 注意事项

- 建表语句中的 `DEFAULT CHARSET=utf8mb4` 和 `ENGINE=InnoDB` 必须保留
- 菜单类型：工单管理用 `M`（目录），子菜单用 `C`（菜单），按钮用 `F`（按钮）
- 权限标识不要加多余空格

---

## 阶段三：基础代码结构

### 目标

创建所有 Java 文件骨架——domain、dto、vo、enum、mapper、service、controller——无业务逻辑，仅确保编译通过。

### 主要任务

1. 创建 4 个 Domain 类，继承 `BaseEntity`（`TicketOperationLog` 除外）
2. 创建 3 个 Enum 类：`TicketStatus`（含 `allowedTransitions`）、`TicketPriority`、`TicketOperationType`
3. 创建 DTO：`TicketCreateDTO`、`TicketAssignDTO`、`TicketProcessDTO`、`TicketConfirmDTO`、`TicketCancelDTO`、`TicketQueryDTO`、`TicketCommentDTO`
4. 创建 VO：`TicketVO`（详情用）、`TicketListVO`（列表用）、`TicketCategoryTreeVO`
5. 创建 4 个 Mapper 接口
6. 创建 4 个 Service 接口 + Impl（Impl 暂留空方法体）
7. 创建 3 个 Controller，继承 `BaseController`，Controller 先按阶段逐步实现，不一次性生成空接口
8. `mvn clean compile` 通过

### 产出文件

详见 `docs/02-architecture-design.md` §4 目录结构。

### 验收标准

- 所有 Java 文件编译通过
- Domain 字段与数据库列一一对应
- `TicketStatus` 枚举包含 5 个状态值 + `allowedTransitions` 静态块
- `TicketOperationType` 枚举包含 `CREATE` / `ASSIGN` / `PROCESS` / `CONFIRM` / `CANCEL`

### 注意事项

- `TicketOperationLog` 不继承 `BaseEntity`
- Domain 字段用驼峰，数据库列用下划线，MyBatis 自动映射
- DTO 上的校验注解（`@NotBlank` 等）在业务阶段再补

---

## 阶段四：工单分类模块

### 目标

完成分类的 CRUD + 树查询，可作为独立功能交付测试。

### 主要任务

1. 编写 `TicketCategoryMapper` 接口和 XML：`selectCategoryList`、`selectCategoryById`、`selectCategoryTree`、`insertCategory`、`updateCategory`、`deleteCategoryById`
2. 实现 `TicketCategoryServiceImpl`：树形构建逻辑（`parentId` + `ancestors`）
3. 实现 `TicketCategoryController`：list / tree / getInfo / add / edit / remove
4. 分类新增时自动计算 `ancestors`：`父节点.ancestors + "," + 父节点.category_id`
5. 分类删除时校验是否有子节点，有则不允许删除（或级联删除）
6. 加 `@Log` 注解和 `@PreAuthorize`

### 产出文件

```
TicketCategoryMapper.java / TicketCategoryMapper.xml
TicketCategoryServiceImpl.java
TicketCategoryController.java
```

### 验收标准

- `/ticket/category/tree` 返回正确树结构
- 新增分类后再查树，新节点出现在正确位置
- 删除无子节点的分类成功，删除有子节点的分类返回错误提示
- 停用的分类在树中仍然返回（前端控制显隐）

### 注意事项

- 树形构建参考 RuoYi `SysDept` 的 `buildDeptTree` 逻辑
- `ancestors` 字段用于快速查询子孙，不要漏掉
- 分类名称校验唯一性（同父级下不重名）

---

## 阶段五：工单主流程

### 目标

实现工单创建、列表、详情、分派、处理、确认、取消 7 个核心操作。**这是 v1.0 的核心阶段。**

### 主要任务

1. **创建工单**：生成 `ticketNo`（`TK` + 日期 + 4 位序号），校验分类存在，`status = NEW`，`creator_id` / `dept_id` 从当前登录用户取
2. **工单列表**：分页 + 多条件筛选（status / priority / categoryId / keyword / dateRange），按角色控制数据范围
3. **工单详情**：单条查询 + LEFT JOIN 分类/创建人/指派人/部门名称 + 评论列表 + 操作日志
4. **工单分派**：校验 `NEW → PROCESSING` 流转合法，校验指派人存在，更新 `assignee_id` + `status` + `processed_at`
5. **工单处理**：校验 `PROCESSING → WAIT_CONFIRM` 流转合法，校验当前用户是指派人，处理备注必填
6. **工单确认**：校验 `WAIT_CONFIRM → CLOSED` 流转合法，校验当前用户是创建人（或管理员），更新 `closed_at`
7. **工单取消**：校验 `NEW/PROCESSING → CANCELLED` 流转合法，校验当前用户是创建人或管理员，取消原因必填
8. **每次流转写 `ticket_operation_log`**，在同一事务内

### 产出文件

```
TicketMapper.java / TicketMapper.xml
TicketServiceImpl.java
TicketController.java
```

### 验收标准

- 创建工单后 `ticket_no` 唯一且格式正确，`status = NEW`
- 列表支持按状态/优先级/分类/关键词/日期筛选，分页正确
- 详情返回完整信息（含分类名、创建人昵称、部门名、评论、日志）
- 分派后状态变为 `PROCESSING`，`assignee_id` 更新，`processed_at` 有值
- 非指派人调用处理接口返回错误
- 处理时备注为空返回校验错误
- 确认后状态变为 `CLOSED`，`closed_at` 有值
- 非创建人（且非管理员）调用确认/取消返回错误
- `CLOSED` 或 `CANCELLED` 工单再进行任何流转操作均返回错误
- 每次流转 `ticket_operation_log` 新增一条记录

### 注意事项

- `ticketNo` 生成 v1.0 先使用数据库方式，避免引入新的 Redis 业务依赖；后续高并发再评估 Redis INCR
- 状态流转校验统一封装在 `TicketStatus.canTransitionTo()`，不要散落在各个方法中
- 所有流转操作加 `@Transactional(rollbackFor = Exception.class)`

---

## 阶段六：评论和操作日志

### 目标

完善评论添加/查询，确保操作日志覆盖所有流转场景。

### 主要任务

1. 编写 `TicketCommentMapper` 和 XML：`insertComment`、`selectCommentsByTicketId`
2. 实现 `TicketCommentServiceImpl`：添加评论（校验工单存在、`commentType` 枚举校验）
3. 在工单创建时写入 `CREATE` 日志（`fromStatus = null, toStatus = NEW`）
4. 确认阶段五的分派/处理/确认/取消日志写入正确
5. 实现 `GET /ticket/{ticketId}/comments` 和 `POST /ticket/{ticketId}/comment`
6. 实现 `GET /ticket/{ticketId}/logs`

### 产出文件

```
TicketCommentMapper.java / TicketCommentMapper.xml
TicketCommentServiceImpl.java
TicketCommentController.java
TicketOperationLogMapper.java / TicketOperationLogMapper.xml
TicketOperationLogServiceImpl.java
```

### 验收标准

- 添加评论后工单详情中可查看到
- 评论按时间倒序排列
- 创建工单产生一条 `operationType = CREATE` 的日志
- 分派/处理/确认/取消各产生对应日志
- 操作日志按时间倒序，前端可直接渲染时间线

### 注意事项

- 评论和日志皆为只增不删，Controller 不提供修改/删除接口
- `operator_name` 冗余存储，避免日志查询时 JOIN 用户表

---

## 阶段七：权限和数据范围

### 目标

所有 Controller 方法加权限校验，列表查询按角色限制数据范围。

### 主要任务

1. 确认阶段二中菜单和权限标识已配置，角色已分配
2. 所有 CUD 接口加 `@PreAuthorize("@ss.hasPermi('ticket:xxx:xxx')")`
3. `TicketServiceImpl.selectTicketList` 中实现数据范围控制：
   - `SecurityUtils.isAdmin(userId)` → 查询全部
   - 非管理员 → 查询 `creator_id = 当前用户 OR assignee_id = 当前用户`
4. 分派操作校验：仅管理员或具有 `ticket:ticket:assign` 权限的角色
5. 处理操作校验：仅当前指派人
6. 确认/取消操作校验：仅创建人或管理员

### 产出文件

```
阶段三~六已创建的 Controller / Service（补充权限注解和数据范围逻辑）
```

### 验收标准

- 无权限的用户调用接口返回 403
- 普通用户列表只看到自己创建或指派给自己的工单
- 管理员列表看到全部工单
- 非指派人调用处理接口返回错误
- 非创建人且非管理员调用确认/取消返回错误

### 注意事项

- 不走 RuoYi 的 `@DataScope` 注解，v1.0 手动控制
- 管理员判断只用 `SecurityUtils.isAdmin(userId)`，ticket 模块不自行判断
- 权限字符串不要写错，前后端一致

---

## 阶段八：联调与收尾

### 目标

全流程联调，确保 Swagger 可测试、状态流转正确、Git 提交规范。

### 主要任务

1. 启动应用，打开 Swagger UI，确认 ticket 相关接口出现在文档中
2. 按 v1.0 验收标准逐条测试（`docs/01-project-spec.md` §验收标准）
3. 测试非法流转：CLOSED 工单再分派/处理/确认/取消，预期全部报错
4. 测试权限：无权限角色调用接口，预期 403
5. 测试列表筛选：按状态/优先级/分类/关键词/日期组合筛选
6. 检查代码是否符合 Alibaba Java Coding Guidelines
7. `git status` 确认只新增了 `ruoyi-ticket/`、`docs/`、修改了 `pom.xml` 系列
8. 提交代码，commit message: `feat: add ticket module v1.0`
9. 更新项目 README，补充模块说明和启动方式

### 产出文件

```
README.md（更新）
```

### 验收标准

- Swagger 中 ticket 接口分组正确，请求/响应示例可读
- 10 条 v1.0 验收标准全部通过
- 非法流转全部返回明确错误提示
- Git diff 中无 RuoYi 基础模块的意外修改
- 代码无明显的 Checkstyle / Alibaba 规范违规

### 注意事项

- 如 Swagger 未显示 ticket 接口，再检查 Springdoc 扫描配置和包路径
- 提交前做一次 `mvn clean compile` 确认无编译错误
- README 只补充项目说明，不改原有内容
