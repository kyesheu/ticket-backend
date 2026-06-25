# 02 — 架构与设计规范

> 版本: v1.0 | 日期: 2026-06-25

---

## 1. 整体架构

```
┌──────────────────────────────────────────────────┐
│                   ruoyi-admin                     │
│        (Spring Boot 启动入口，引入所有模块)         │
│                                                    │
│  ┌──────────────────┐                             │
│  │ RuoYi Controller │                             │
│  │ /system/*        │                             │
│  │ /monitor/*       │                             │
│  └────────┬─────────┘                             │
└───────────┼───────────────────────────────────────┘
            │
   ┌────────▼────────┐    ┌──────────────────────┐
   │ ruoyi-framework  │    │   ruoyi-ticket       │
   │ (Security, JWT,  │    │   (工单业务)          │
   │  AOP, Config)    │    │   Controller         │
   └────────┬─────────┘    │   Service / Impl     │
            │              │   Mapper / XML        │
   ┌────────▼────────┐    │   Domain / DTO / VO   │
   │  ruoyi-system    │    │   Enum               │
   │  (RBAC 业务)     │    └──────────┬───────────┘
   └────────┬─────────┘               │
            │                         │
   ┌────────▼─────────────────────────▼───────────┐
   │              ruoyi-common                      │
   │  (BaseEntity, BaseController, AjaxResult,      │
   │   SecurityUtils, ServiceException, 注解, 工具)  │
   └────────────────────────────────────────────────┘
```

- **ruoyi-admin**: 唯一启动入口，负责引入业务模块并统一启动 Spring Boot 应用，不写业务代码
- **ruoyi-ticket**: 新增，工单业务全在这（含 Controller / Service / Mapper），依赖 `ruoyi-common`，不依赖 `ruoyi-framework` / `ruoyi-system`。Controller 由 Spring Boot 自动扫描加载
- **ruoyi-framework / ruoyi-system / ruoyi-common / ruoyi-quartz / ruoyi-generator**: 不改动

---

## 2. 若依基础模块与 ticket 模块的关系

ticket 模块对 RuoYi 的依赖是 **单向的、只读的**：

- 依赖 `ruoyi-common` 中的 `BaseEntity`、`BaseController`、`AjaxResult`、`TableDataInfo`、`SecurityUtils`、`ServiceException`、`@Log` 注解等
- 不注入 `ISysUserService` / `ISysDeptService`。工单表只保存 `userId`、`deptId`，展示创建人/处理人/部门名称时在 Mapper XML 中只读 `LEFT JOIN sys_user` / `sys_dept`
- **禁止对若依基础表做写操作**
- `@PreAuthorize` 走 RuoYi 的 `PermissionService`（`@ss`），ticket 只提供权限字符串
- 不走 RuoYi 的 `DataScope` 数据权限（v1.0 手动控制）

---

## 3. 模块依赖关系

```
ruoyi-admin
├── ruoyi-framework
├── ruoyi-system
├── ruoyi-quartz
├── ruoyi-generator
└── ruoyi-ticket        ← 新增这一行
    └── ruoyi-common     ← ticket 只依赖 common
```

`ruoyi-ticket/pom.xml` 只需：

```xml
<dependency>
    <groupId>com.ruoyi</groupId>
    <artifactId>ruoyi-common</artifactId>
</dependency>
```

`ruoyi-admin/pom.xml` 新增：

```xml
<dependency>
    <groupId>com.ruoyi</groupId>
    <artifactId>ruoyi-ticket</artifactId>
</dependency>
```

根 `pom.xml` 的 `<modules>` 和 `<dependencyManagement>` 各加一行 `ruoyi-ticket`。

---

## 4. ruoyi-ticket 目录结构

```
ruoyi-ticket/src/main/java/com/ruoyi/ticket/
├── controller/
│   ├── TicketController.java
│   ├── TicketCategoryController.java
│   └── TicketCommentController.java
├── service/
│   ├── ITicketService.java
│   ├── ITicketCategoryService.java
│   ├── ITicketCommentService.java
│   ├── ITicketOperationLogService.java
│   └── impl/
│       ├── TicketServiceImpl.java
│       ├── TicketCategoryServiceImpl.java
│       ├── TicketCommentServiceImpl.java
│       └── TicketOperationLogServiceImpl.java
├── mapper/
│   ├── TicketMapper.java
│   ├── TicketCategoryMapper.java
│   ├── TicketCommentMapper.java
│   └── TicketOperationLogMapper.java
├── domain/
│   ├── Ticket.java
│   ├── TicketCategory.java
│   ├── TicketComment.java
│   └── TicketOperationLog.java
├── dto/                          ← 请求体 DTO，与 Domain 分离
│   ├── TicketCreateDTO.java
│   ├── TicketAssignDTO.java
│   ├── TicketProcessDTO.java
│   ├── TicketConfirmDTO.java
│   ├── TicketCancelDTO.java
│   ├── TicketQueryDTO.java
│   └── TicketCommentDTO.java
├── vo/                           ← 响应体 VO，按场景裁剪字段
│   ├── TicketVO.java
│   ├── TicketListVO.java
│   └── TicketCategoryTreeVO.java
└── enums/
    ├── TicketStatus.java
    ├── TicketPriority.java
    └── TicketOperationType.java
```

`resources/mapper/` 下放 MyBatis XML（项目配置 `classpath*:mapper/**/*Mapper.xml` 会自动扫描）。

---

## 5. 分层规范

```
Controller  → 接收请求、参数校验、调用 Service、返回结果
                不写业务逻辑，不直接调 Mapper

Service     → 业务逻辑、状态流转校验、事务控制
                接口定义在 service/，实现放在 service/impl/

Mapper      → 数据库访问，只做单表 CRUD，不写业务逻辑
                复杂 SQL 放在 XML，不在 Java 里拼接

Domain      → 数据库表对应的实体，继承 BaseEntity
                字段命名用驼峰，与数据库列名下划线映射

DTO         → 前端传给后端的请求体，独立于 Domain
                加 @NotNull / @NotBlank 校验注解

VO          → 后端返回给前端的响应体，按场景裁剪字段
                列表场景用 ListVO（少字段），详情场景用完整 VO

Enum        → 枚举类，状态、优先级、操作类型均用枚举
                不写魔法字符串
```

分层依赖方向：`Controller → Service(接口) → Mapper`，不可反向。

---

## 6. Controller 设计规范

### 6.1 基础模板

```java
@RestController
@RequestMapping("/ticket/xxx")
public class TicketXxxController extends BaseController {

    @Autowired
    private ITicketXxxService xxxService;

    @PreAuthorize("@ss.hasPermi('ticket:ticket:list')")
    @GetMapping("/list")
    public TableDataInfo list(TicketQueryDTO query) {
        startPage();
        List<TicketListVO> list = xxxService.selectList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('ticket:ticket:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(xxxService.selectById(id));
    }

    @Log(title = "工单管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:ticket:add')")
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TicketCreateDTO dto) {
        return toAjax(xxxService.create(dto));
    }
}
```

### 6.2 核心规则

- 必须继承 `com.ruoyi.common.core.controller.BaseController`
- 分页接口用 `startPage()` + `getDataTable(list)` 返回 `TableDataInfo`
- 非分页接口用 `success(data)` / `error("原因")` / `toAjax(rows)` 返回 `AjaxResult`
- 创建/修改操作加 `@Log` 注解记录到 `sys_oper_log`
- 权限用 `@PreAuthorize("@ss.hasPermi('...')")`，不加 `@ss.hasRole`（角色靠菜单分配控制）
- DTO 用 `@Validated` 开启校验，校验规则写在 DTO 字段上
- Controller 方法只做三件事：接参数、调 Service、返回结果。**不写 if/else 业务判断**

---

## 7. Service 设计规范

### 7.1 接口与实现分离

```
service/ITicketService.java       ← 接口
service/impl/TicketServiceImpl.java ← 实现
```

接口命名 `I` 前缀，实现命名 `Impl` 后缀，与 RuoYi 保持一致。

### 7.2 核心规则

- 接口类加 `@Service` 注解（在 Impl 上）
- 注入 Mapper 用 `@Autowired`
- **状态流转校验必须在 Service 层**：`currentStatus.canTransitionTo(targetStatus)`，校验失败抛 `ServiceException`
- **每次状态流转必须写 `ticket_operation_log`**，在同一事务内完成
- 工单表只保存 `userId`、`deptId`，需展示用户/部门名称时在 Mapper XML 中 `LEFT JOIN` 查询，不在 Service 层注入 RuoYi 的 Service
- 获取当前用户用 `SecurityUtils.getUserId()` / `SecurityUtils.getLoginUser()`
- 方法名：`select*` 查询，`insert*` / `create*` 新增，`update*` 修改，`delete*` 删除，`assign*` / `process*` / `confirm*` / `cancel*` 业务动作

### 7.3 状态流转示例

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void assign(Long ticketId, Long assigneeId) {
    Ticket ticket = ticketMapper.selectById(ticketId);
    if (ticket == null) {
        throw new ServiceException("工单不存在");
    }
    // 状态校验
    TicketStatus current = TicketStatus.valueOf(ticket.getStatus());
    if (!current.canTransitionTo(TicketStatus.PROCESSING)) {
        throw new ServiceException("当前状态不允许分派");
    }
    // 更新状态
    ticket.setStatus(TicketStatus.PROCESSING.name());
    ticket.setAssigneeId(assigneeId);
    ticketMapper.updateById(ticket);
    // 写操作日志
    saveOperationLog(ticketId, TicketOperationType.ASSIGN,
        current, TicketStatus.PROCESSING, null);
}
```

---

## 8. Mapper 与 SQL 规范

### 8.1 Mapper 接口

- 放在 `com.ruoyi.ticket.mapper` 包下
- 用 MyBatis 注解或 XML，复杂查询统一走 XML
- 方法名清晰表达意图：`selectTicketList` / `selectTicketById` / `insertTicket` / `updateTicket` / `deleteTicketById`
- 不写业务逻辑，不包含状态校验，不抛业务异常

### 8.2 分页查询

分页方法签名为 `List<T> selectXxxList(XxxQueryDTO query)`，不加 `Page` 参数。PageHelper 在 Controller 层 `startPage()` 自动拦截。

### 8.3 SQL 规范

- XML 中的列名用数据库下划线，Java 字段用驼峰，MyBatis 自动映射（`mapUnderscoreToCamelCase`）
- 多条件查询用 `<if>` 动态拼接，避免 `WHERE 1=1`
- LIKE 查询在 Java 侧拼好 `%keyword%` 再传入，不在 SQL 中拼
- 排序字段用 `${}` 时**必须先白名单校验**（用 RuoYi 的 `SqlUtil.escapeOrderBySql`）
- 批量操作用 `<foreach>`，注意 MySQL `max_allowed_packet` 限制

---

## 9. 状态枚举设计规范

### 9.1 TicketStatus

```java
public enum TicketStatus {
    NEW("待分派"),
    PROCESSING("处理中"),
    WAIT_CONFIRM("待确认"),
    CLOSED("已关闭"),
    CANCELLED("已取消");

    private final String label;

    static {
        NEW.allowed = Set.of(PROCESSING, CANCELLED);
        PROCESSING.allowed = Set.of(WAIT_CONFIRM, CANCELLED);
        WAIT_CONFIRM.allowed = Set.of(CLOSED);
        // CLOSED, CANCELLED 终态，allowed 为空
    }

    private Set<TicketStatus> allowed = Set.of();

    public boolean canTransitionTo(TicketStatus target) {
        return this.allowed.contains(target);
    }

    public Set<TicketStatus> allowedTransitions() {
        return Collections.unmodifiableSet(this.allowed);
    }
}
```

### 9.2 TicketPriority

```java
public enum TicketPriority {
    LOW("低"),
    MEDIUM("中"),
    HIGH("高"),
    URGENT("紧急");
}
```

### 9.3 TicketOperationType

```java
public enum TicketOperationType {
    CREATE("创建"),
    ASSIGN("分派"),
    PROCESS("处理"),
    CONFIRM("确认"),
    CANCEL("取消");
}
```

### 9.4 规范要点

- 所有枚举用 `String` 类型存储（数据库存 `name()` 即枚举常量名），不用 `ordinal()`
- 状态流转规则和状态定义放在同一个枚举文件内，修改流转只需要改一处
- DTO/VO 中的状态字段用 `String` 类型，在 Service 层转枚举校验，Controller 不感知枚举类型

---

## 10. 权限控制规范

### 10.1 权限字符串格式

沿用 RuoYi 的 `模块:对象:操作` 格式：

```
ticket:ticket:list       ticket:ticket:query       ticket:ticket:add
ticket:ticket:assign     ticket:ticket:process     ticket:ticket:confirm
ticket:ticket:cancel     ticket:log:list
ticket:category:list     ticket:category:add       ticket:category:edit
ticket:category:remove   ticket:category:query
ticket:comment:list      ticket:comment:add
```

### 10.2 校验方式

Controller 方法上统一用 `@PreAuthorize("@ss.hasPermi('ticket:xxx')")`。

### 10.3 v1.0 列表数据范围

不走 RuoYi 的 `@DataScope` 注解。数据范围在 Service 层做基础判断：

- 角色含 `admin` → 查看全部工单
- 其他角色 → 只看 `creator_id = 当前用户` 或 `assignee_id = 当前用户` 的工单

### 10.4 管理员判断

优先沿用 RuoYi 原有 `SecurityUtils.isAdmin(userId)` 逻辑，ticket 模块不自行定义管理员规则。

---

## 11. 操作日志设计规范

### 11.1 双层日志

| 日志 | 记录内容 | 写入方式 | 用途 |
|---|---|---|---|
| `sys_oper_log` | HTTP 请求参数、返回结果、耗时、IP | Controller 加 `@Log` 注解，AOP 自动写入 | 运维排查 |
| `ticket_operation_log` | 状态流转 who / from / to / comment | Service 中手动构造并写入 | 业务审计 |

### 11.2 写入规则

- **每次状态流转**必须在同一事务内写一条 `ticket_operation_log`
- 创建工单时写一条 `operationType = "CREATE"`，`fromStatus = null, toStatus = NEW`
- 日志不可修改、不可删除（没有 `delFlag`，不继承 `BaseEntity`）

### 11.3 @Log 注解使用

- 工单创建/分派/处理/确认/取消/分类增删改 → `@Log(title = "工单管理", businessType = BusinessType.XXX)`
- 查询/详情接口不加 `@Log`
- `businessType` 映射：新增→`INSERT`，修改→`UPDATE`，分派/确认/取消→`UPDATE`

---

## 12. 事务边界规范

- 事务注解用 `org.springframework.transaction.annotation.Transactional`
- 只在 Service 层加 `@Transactional(rollbackFor = Exception.class)`
- **不**在 Controller 加事务
- 需要事务的场景：状态流转（update ticket + insert log）、分类删除（删父节点 + 删子节点）
- 纯查询方法不加事务
- 事务粒度尽量小，一个方法一个事务，不跨 Service 嵌套（需要时用 `Propagation.REQUIRES_NEW` 写独立日志）

---

## 13. 异常处理规范

### 13.1 业务异常

抛 RuoYi 的 `ServiceException`，由 `GlobalExceptionHandler` 统一捕获返回 `AjaxResult.error()`：

```java
throw new ServiceException("当前状态不允许分派");
```

不带 `code` 参数时默认 HTTP 500，可在常量或枚举中预定义错误码。

### 13.2 参数校验异常

DTO 上用 `jakarta.validation.constraints` 注解（`@NotBlank` / `@NotNull` / `@Size`），Controller 加 `@Validated`，校验失败时 Spring 自动抛 `MethodArgumentNotValidException`，`GlobalExceptionHandler` 已处理。

### 13.3 不自行 catch 的异常

- `ServiceException` → 框架统一处理
- 参数校验异常 → 框架统一处理
- 权限异常 `AccessDeniedException` → 框架统一处理

只在需要降级处理或提供默认值时 catch。

---

## 14. 命名规范

### 14.1 包名

`com.ruoyi.ticket.{controller|service|service.impl|mapper|domain|dto|vo|enums}`

### 14.2 类名

| 层 | 格式 | 示例 |
|---|---|---|
| Controller | `Ticket{Name}Controller` | `TicketController` |
| Service 接口 | `ITicket{Name}Service` | `ITicketService` |
| Service 实现 | `Ticket{Name}ServiceImpl` | `TicketServiceImpl` |
| Mapper | `Ticket{Name}Mapper` | `TicketMapper` |
| Domain | `Ticket{Name}` | `Ticket`, `TicketCategory` |
| DTO | `Ticket{Name}DTO` | `TicketCreateDTO` |
| VO | `Ticket{Name}VO` | `TicketVO`, `TicketListVO` |
| Enum | `Ticket{Name}` | `TicketStatus` |

### 14.3 方法名

| 操作 | 前缀 | 示例 |
|---|---|---|
| 查询列表 | `select*List` | `selectTicketList` |
| 查询单条 | `select*ById` | `selectTicketById` |
| 新增 | `insert*` | `insertTicket` |
| 修改 | `update*` | `updateTicket` |
| 删除 | `delete*ById` | `deleteTicketById` |
| 业务动作 | 动词 | `assign`, `process`, `confirm`, `cancel` |

### 14.4 变量名

- Service/Mapper 变量：小驼峰类名，如 `ticketMapper`、`ticketService`
- 方法参数：语义化，如 `ticketId`、`assigneeId`，不用 `id`、`uid`

### 14.5 数据库命名

- 表名：`ticket`、`ticket_category`、`ticket_comment`、`ticket_operation_log`
- 列名：下划线分隔，如 `ticket_id`、`assignee_id`、`create_time`
- 主键：`表名_id`（单表）或 `id`
- 索引：`idx_字段名`，唯一索引 `uk_字段名`

---

## 15. 不允许修改的若依基础模块

以下文件/目录**绝对不改**：

| 模块 | 不可改动内容 |
|---|---|
| `ruoyi-framework/` | 全部。Security 配置、JWT Filter、LogAspect、全局异常处理、Redis 配置 |
| `ruoyi-system/` | 全部。现有 Service/Mapper/Domain，ticket 模块不注入也不调用 |
| `ruoyi-common/` | 全部。BaseEntity、BaseController、AjaxResult、异常类、工具类、注解 |
| `ruoyi-admin/` | **只改 pom.xml**（加 ruoyi-ticket 依赖），不新增也不修改任何 Controller/Service |
| `ruoyi-quartz/` | 全部不动 |
| `ruoyi-generator/` | 全部不动 |
| 根 `pom.xml` | **只加** `<module>` 和 `<dependencyManagement>` 中的 ruoyi-ticket，不改现有配置 |
| `application.yml` | 不新增 ticket 专属配置（v1.0 无需求） |

**唯一可写的位置**：新建的 `ruoyi-ticket/` 模块全部文件。

---

## 16. 后续 AI 能力预留说明

v1.0 不实现 AI 能力，但在设计上做以下预留：

- `ticket_comment` 表的 `commentType` 字段预留了 `INTERNAL` 类型，后续 AI Agent 可写入内部分析备注
- `ticket` 表结构独立于 RuoYi，后续接入 RAG 知识库检索时工单数据可直接作为检索源
- 状态枚举用 `String` 存储（非 `ordinal`），后续扩展状态不影响存量数据
- `ticket_operation_log` 记录了完整的流转历史，是后续智能分派、相似工单推荐的训练数据基础

后续版本接入 AI 时，建议新增 `ruoyi-ai` 模块（而非改 ticket 模块），通过调用 ticket Service 接口获取工单数据，保持模块边界清晰。
