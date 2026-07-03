# 02 — 架构与设计规范

> v2.0 | 2026-07-02

## 模块架构

```
ruoyi-admin         ← 唯一启动入口，不写业务代码
├── ruoyi-framework  ← Security、JWT、AOP、Config
├── ruoyi-system     ← RBAC（用户/角色/菜单/部门）
├── ruoyi-common     ← BaseEntity、BaseController、工具类
├── ruoyi-quartz     ← 定时任务
├── ruoyi-generator  ← 代码生成
└── ruoyi-ticket     ← ★ 新增 工单模块（只依赖 ruoyi-common）
```

`ruoyi-ticket` 对若依的依赖是单向只读的：只依赖 `ruoyi-common`，工单表只存 `userId`/`deptId`，展示名称在 Mapper XML 中 `LEFT JOIN` 查询，不注入 RuoYi Service。

## ruoyi-ticket 目录结构

```
ruoyi-ticket/src/main/java/com/ruoyi/ticket/
├── controller/       # TicketController、TicketCategoryController、TicketCommentController
├── service/          # ITicketService 等接口
│   └── impl/         # TicketServiceImpl 等实现
├── mapper/           # TicketMapper 等接口
├── domain/           # Ticket、TicketCategory、TicketComment、TicketOperationLog
├── dto/              # TicketCreateDTO 等请求体
├── vo/               # TicketVO、TicketListVO、TicketCategoryTreeVO
└── enums/            # TicketStatus、TicketPriority、TicketOperationType

ruoyi-ticket/src/main/resources/mapper/   ← MyBatis XML
```

## 分层规范

```
Controller → Service(接口) → Mapper
   ↓              ↓              ↓
  参数校验      业务逻辑       数据访问
  调用Service   状态流转       只做单表CRUD
  返回结果      事务控制       不写业务逻辑

Domain ← 数据库表实体，字段驼峰，继承 BaseEntity
DTO    ← 前端请求体，独立于 Domain，加校验注解
VO     ← 后端响应体，按场景裁剪字段（列表少字段，详情多字段）
Enum   ← 状态/优先级/操作类型，所有枚举存 String（name()），不用 ordinal()
```

分层方向不可逆：Controller → Service → Mapper。

## Controller 规范

- 必须继承 `BaseController`
- 分页用 `startPage()` + `getDataTable(list)` → `TableDataInfo`
- 非分页用 `success(data)` / `toAjax(rows)` → `AjaxResult`
- 写操作加 `@Log(title = "...", businessType = ...)`
- 权限统一用 `@PreAuthorize("@ss.hasPermi('ticket:xxx:xxx')")`
- DTO 用 `@Validated` 开启校验
- Controller 只做三件事：接参数、调 Service、返回结果。不写 if/else 业务判断

```java
@RestController
@RequestMapping("/ticket/xxx")
public class TicketXxxController extends BaseController {
    @Autowired
    private ITicketXxxService xxxService;

    @PreAuthorize("@ss.hasPermi('ticket:xxx:list')")
    @GetMapping("/list")
    public TableDataInfo list(XxxQueryDTO query) {
        startPage();
        return getDataTable(xxxService.selectList(query));
    }

    @Log(title = "工单管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('ticket:xxx:add')")
    @PostMapping
    public AjaxResult add(@Validated @RequestBody XxxCreateDTO dto) {
        return toAjax(xxxService.create(dto));
    }
}
```

## Service 规范

- 接口 `I` 前缀，实现 `Impl` 后缀
- 实现加 `@Service`，注入 Mapper 用 `@Autowired`
- 状态流转校验在 Service 层：`TicketStatus.valueOf(status).canTransitionTo(target)`
- 每次流转同一事务内写 `ticket_operation_log`
- 获取当前用户用 `SecurityUtils.getUserId()` / `SecurityUtils.getUsername()`
- 方法命名：`select*` 查询 / `insert*` 新增 / `update*` 修改 / `delete*` 删除 / 动词 业务动作

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void assign(Long ticketId, TicketAssignDTO dto) {
    Ticket ticket = ticketMapper.selectTicketEntityById(ticketId);
    if (ticket == null) throw new ServiceException("工单不存在");

    TicketStatus current = TicketStatus.valueOf(ticket.getStatus());
    if (!current.canTransitionTo(TicketStatus.PROCESSING))
        throw new ServiceException("当前状态不允许分派");

    ticket.setStatus(TicketStatus.PROCESSING.name());
    ticket.setAssigneeId(dto.getAssigneeId());
    ticketMapper.updateTicket(ticket);
    saveOperationLog(ticketId, TicketOperationType.ASSIGN, current, TicketStatus.PROCESSING, dto.getComment());
}
```

## Mapper 与 SQL 规范

- 复杂查询走 XML，不在 Java 拼 SQL
- 分页方法签名为 `List<T> selectXxxList(XxxQueryDTO query)`，不加 Page 参数（PageHelper 自动拦截）
- 多条件用 `<if>` 动态拼接，避免 `WHERE 1=1`
- LIKE 在 Java 侧拼好 `%keyword%` 再传入
- 排序字段用 `${}` 时先 `SqlUtil.escapeOrderBySql` 白名单校验
- 列名用数据库下划线，Java 驼峰，MyBatis 自动映射（`mapUnderscoreToCamelCase`）

## 枚举设计

### TicketStatus（含状态流转规则）

```java
public enum TicketStatus {
    NEW("待分派"), PROCESSING("处理中"), WAIT_CONFIRM("待确认"),
    CLOSED("已关闭"), CANCELLED("已取消");

    private Set<TicketStatus> allowed = Set.of();

    static {
        NEW.allowed = Set.of(PROCESSING, CANCELLED);
        PROCESSING.allowed = Set.of(WAIT_CONFIRM, CANCELLED);
        WAIT_CONFIRM.allowed = Set.of(CLOSED);
        // CLOSED、CANCELLED 终态，allowed 为空
    }

    public boolean canTransitionTo(TicketStatus target) {
        return this.allowed.contains(target);
    }
}
```

### TicketPriority / TicketOperationType

```java
public enum TicketPriority { LOW("低"), MEDIUM("中"), HIGH("高"), URGENT("紧急"); }
public enum TicketOperationType { CREATE("创建"), ASSIGN("分派"), PROCESS("处理"), CONFIRM("确认"), CANCEL("取消"); }
```

- 存数据库用 `name()`（String），不用 `ordinal()`
- DTO/VO 状态字段用 String，Service 层转枚举校验

## 权限设计

格式：`模块:对象:操作`

```
ticket:ticket:list / query / add / assign / process / confirm / cancel
ticket:log:list
ticket:category:list / query / add / edit / remove
ticket:comment:list / add
```

Controller 用 `@PreAuthorize("@ss.hasPermi('...')")`。v1.0 不走 `@DataScope`，数据范围在 Service 层手动控制：

- `SecurityUtils.isAdmin()` → 查看全部
- 其他 → `creator_id = 当前用户 OR assignee_id = 当前用户`

## 事务规范

- `@Transactional(rollbackFor = Exception.class)`，只在 Service 层加
- 需要事务：状态流转（update ticket + insert log）、分类删除（删父+子）
- 纯查询不加事务
- 粒度尽量小，一个方法一个事务

## 异常规范

- 业务异常抛 `ServiceException("消息")`，由 `GlobalExceptionHandler` 统一捕获
- 参数校验异常由 DTO 的 `@NotBlank`/`@NotNull` + Controller 的 `@Validated` 触发，框架统一处理
- 不自行 catch `ServiceException`、参数校验异常、权限异常

## 命名规范

| 层 | 格式 | 示例 |
|---|---|---|
| Controller | `Ticket{Name}Controller` | `TicketController` |
| Service 接口 | `ITicket{Name}Service` | `ITicketService` |
| Service 实现 | `Ticket{Name}ServiceImpl` | `TicketServiceImpl` |
| Mapper | `Ticket{Name}Mapper` | `TicketMapper` |
| Domain | `Ticket{Name}` | `Ticket` |
| DTO | `Ticket{Name}DTO` | `TicketCreateDTO` |
| VO | `Ticket{Name}VO` | `TicketVO` |
| Enum | `Ticket{Name}` | `TicketStatus` |

方法：`select*List` / `select*ById` / `insert*` / `update*` / `delete*ById` / 业务动词

数据库：表名 `ticket_xxx`，主键 `表名_id`，索引 `idx_字段名`，唯一 `uk_字段名`

## AI 能力预留

- `ticket_comment.commentType` 预留 `INTERNAL` 类型，后续 AI Agent 可写入内部分析
- 状态枚举用 String 存储，扩展不影响存量数据
- `ticket_operation_log` 是后续智能分派、相似工单推荐的训练数据基础
- 后续 AI 模块新增 `ruoyi-ai`，通过调用 ticket Service 接口获取数据，保持边界清晰

## v1.1 SLA 设计

### 新增组件

```text
controller/       TicketSlaPolicyController、TicketSlaAlertController
service/          ITicketSlaPolicyService、ITicketSlaAlertService、ITicketSlaService
service/impl/     对应实现
mapper/           TicketSlaPolicyMapper、TicketSlaAlertMapper
domain/           TicketSlaPolicy、TicketSlaAlert
dto/              TicketSlaPolicyDTO、TicketSlaAlertQueryDTO
vo/               TicketSlaPolicyVO、TicketSlaAlertVO、TicketSlaStatusVO
enums/            TicketSlaAlertType
task/             TicketSlaTask
```

### 依赖与调用方向

`ruoyi-ticket` 仍只依赖 `ruoyi-common`。`TicketSlaTask` 作为 Spring Bean 暴露
`scanOverdue()`，由现有 Quartz 的 `sys_job.invoke_target` 配置
`ticketSlaTask.scanOverdue` 调用。不得让 `ruoyi-ticket` 依赖 `ruoyi-quartz`，也不得在
`ruoyi-quartz` 中新增 ticket 业务代码。

### 创建时限快照

`TicketServiceImpl.insertTicket` 在同一事务中查询启用策略并计算：

```text
responseDueAt = createTime + responseMinutes
resolveDueAt  = createTime + resolveMinutes
```

`resolveMinutes` 必须大于 `responseMinutes`。策略变更只影响新工单。

### 扫描与幂等

`ITicketSlaService.scanOverdue()` 分页扫描需要判定的工单，单页处理，避免一次加载全部数据。
每种超时在同一事务中更新工单标记并插入告警。`ticket_sla_alert` 使用
`uk_ticket_alert_type(ticket_id, alert_type)` 保证并发和重复执行幂等；插入发生唯一键冲突时按
“已处理”结束，不重复记录。

扫描只负责事实判定和持久化，不发送外部消息。Quartz Job 禁止并发执行，默认 Cron 为
`0 0/5 * * * ?`。

### 权限

```text
ticket:sla:list / query / add / edit
ticket:sla-alert:list / query / scan
```

策略维护和手工扫描仅管理员权限开放。普通用户在工单列表、详情中只能看到其有权查看工单的
SLA 状态，不直接访问全量告警列表。

## v1.2 通知与评价设计

- `ticket_notification`、`ticket_satisfaction` 均归属 `ruoyi-ticket`，不依赖 `ruoyi-system` Service。
- 状态流转、评论和 SLA 告警在原事务内调用通知 Service。
- 通知查询从登录态取得 recipientId，禁止前端指定用户 ID。
- `event_key` 使用事件类型和不可变来源 ID 组成，数据库唯一键负责最终幂等。
- 评价 Service 校验工单状态、创建人和唯一性，数据库唯一键处理并发重复提交。

权限：`ticket:notification:list/read`、`ticket:satisfaction:add/query/list/statistics`。

## v1.3 部门级数据权限设计

### 访问策略模块

在 `ruoyi-ticket` 内建立统一的工单访问策略 seam，集中处理角色范围和参与人例外。Controller
不拼接范围条件，业务 Service 不复制角色遍历逻辑。

```text
Ticket Controller / Service
          |
          v
ITicketAccessPolicy
          |
          +-- 列表：写入服务端生成的数据范围条件
          +-- 单条：校验 ticketId 是否在可访问范围内
          |
          v
TicketMapper + sys_role_dept + sys_dept
```

`ITicketAccessPolicy` 保持小接口：一个方法应用列表范围，一个方法断言单条工单可访问。
角色合并、部门树、自定义部门、参与人例外和拒绝语义均隐藏在实现中。

### 范围语义

```text
可访问工单 = 角色部门范围内工单 OR creator_id = 当前用户 OR assignee_id = 当前用户
```

- 超级管理员直接放行。
- 全部数据权限直接放行。
- 自定义部门读取 `sys_role_dept`。
- 本部门匹配 `ticket.dept_id`。
- 本部门及下级通过 `sys_dept.ancestors` 匹配。
- 仅本人不增加部门条件，由创建人/指派人条件覆盖。
- 多个启用且具备当前 ticket 权限的角色取并集。

`ticket.dept_id` 是创建时部门快照。数据权限查询不得改为实时关联创建人的当前部门，否则用户调岗会
改变历史工单归属。

### SQL 与安全

- 只允许服务端策略模块写入 `params.dataScope`，每次查询前先清空外部值。
- 动态 SQL 只能由固定模板、数值型用户 ID、角色 ID组成，不接受请求参数中的 SQL 片段。
- 列表查询和按 ID 可见性查询复用同一范围片段，避免列表不可见但详情可直达。
- 无权访问与工单不存在统一抛出 `ServiceException("工单不存在")`。
- 不修改 `ruoyi-framework` 的通用 `DataScopeAspect`；工单的“创建人或指派人”双用户字段语义由
  `ruoyi-ticket` 自己封装，避免污染基础模块。

## v2.0 动态流程设计

### 核心 seam

动态流程仍归属 `ruoyi-ticket`，不引入 Flowable，不修改基础模块。以 `ITicketWorkflowEngine` 作为核心
seam，向工单模块提供小接口：启动实例、完成任务、退回任务、取消实例、终止实例。版本解析、图校验、
条件路由、处理人解析、并发控制、状态映射和日志写入均隐藏在实现内。

```text
TicketService / WorkflowTaskController
                  |
                  v
        ITicketWorkflowEngine
                  |
      +-----------+------------+
      |           |            |
  Definition   Runtime      Assignee
  validation   routing      resolution
      |           |            |
      +-----------+------------+
                  |
 WorkflowDefinition/Node/Transition Mapper
 WorkflowInstance/Task Mapper + Ticket Mapper
```

Controller 不读取流程图自行判断，现有业务 Service 不复制路由和处理人规则。Mapper 只负责持久化，
流程不变量集中在引擎实现中，通过 `ITicketWorkflowEngine` 的可观察结果测试。

### 定义模型与发布约束

- `ticket_workflow_definition`：版本化流程定义，`workflow_key + version` 唯一。
- `ticket_workflow_node`：节点类型限定为 `START / ASSIGN / PROCESS / CONFIRM / END`。
- `ticket_workflow_transition`：连线条件限定为白名单字段、运算符和值，不存储脚本或 SQL。
- 草稿版本可整体编辑；发布时校验一个开始节点、至少一个结束节点、节点可达、人工节点处理人完整、
  条件分支存在唯一默认连线。
- 已发布定义及其节点、连线不可更新和删除；新版本从旧版本复制为新草稿后编辑。

### 运行模型

- `ticket_workflow_instance` 一对一关联新工单并引用不可变定义版本。
- `ticket_workflow_task` 保存人工节点任务。任务状态为 `PENDING / COMPLETED / RETURNED / CANCELLED / TERMINATED`。
- 每个运行实例最多一个 `PENDING` 任务。进入节点时解析处理人并写入任务快照。
- 完成任务使用条件更新 `WHERE task_status = 'PENDING'` 实现乐观并发控制；影响行数不是 1 时按重复处理拒绝。
- 路由在一个事务内完成：关闭旧任务、选择连线、推进实例、创建新任务、更新工单、写操作日志、触发通知。

### 处理人规则

`assignee_type` 仅允许 `USER / ROLE / CREATOR_DEPT_LEADER / TICKET_ASSIGNEE / TICKET_CREATOR`：

- `USER`：定义保存用户 ID，节点激活时校验用户有效并固化到任务。
- `ROLE`：任务保存角色 ID，处理时校验当前用户拥有该启用角色及 ticket 处理权限。
- `CREATOR_DEPT_LEADER`：按工单 `dept_id` 快照查询部门负责人，节点激活时固化负责人用户 ID。
- `TICKET_ASSIGNEE`：读取工单当前指派人并固化到任务，用于标准流程处理节点。
- `TICKET_CREATOR`：读取工单创建人并固化到任务，用于标准流程确认节点。

无法解析处理人时不得跳过节点，当前动作事务回滚并返回明确业务错误。

### 兼容现有工单语义

- `ticket.status` 保留为列表、SLA、通知和统计使用的粗粒度业务状态；流程节点是细粒度运行状态。
- 内置标准流程表达原 `NEW → PROCESSING → WAIT_CONFIRM → CLOSED` 行为，旧接口通过兼容适配调用流程引擎。
- v2.0 前创建且没有流程实例的工单继续由原状态机处理，避免在线迁移风险。
- 工单访问仍先经过 `ITicketAccessPolicy`；任务处理权限是在对象访问权基础上的附加校验。
