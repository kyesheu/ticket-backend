# 03 — 数据库设计

> 版本: v1.0 | 日期: 2026-06-25 | MySQL 8.0 + InnoDB

---

## 1. 设计原则

- 表名/列名全小写，下划线分隔
- 字符集 `utf8mb4`，引擎 `InnoDB`
- 主键统一 `BIGINT AUTO_INCREMENT`
- 时间字段用 `DATETIME`，不用 `TIMESTAMP`
- 枚举值存 `VARCHAR`（枚举的 `name()`），不用 `ordinal()`
- `del_flag` 遵循若依约定：`0` 存在，`2` 删除
- v1.0 不创建数据库外键，统一使用逻辑外键。数据一致性由 Service 层校验，避免影响若依原有表结构和后续数据迁移

---

## 2. 复用的若依表（不新增、不修改）

| 表 | 用途 | ticket 模块的使用方式 |
|---|---|---|
| `sys_user` | 用户 | 工单保存 `creator_id` / `assignee_id`，展示时 LEFT JOIN 取 `nick_name` / `user_name` |
| `sys_dept` | 部门 | 工单保存 `dept_id`，展示时 LEFT JOIN 取 `dept_name` |
| `sys_role` | 角色 | 权限校验走 RuoYi `@PreAuthorize`，ticket 不直查 |
| `sys_menu` | 菜单权限 | ticket 权限标识在 RuoYi 菜单管理中配置，不直查 |
| `sys_post` | 岗位 | v1.0 不涉及 |
| `sys_oper_log` | API 操作日志 | `@Log` 注解自动写入，ticket 不直查 |

**ticket 业务代码禁止直接写入若依基础表。权限菜单、角色授权等配置通过 RuoYi 后台管理页面维护。**

---

## 3. 新增表总览

```
ticket ──1:N──▶ ticket_comment
  │
  └──1:N──▶ ticket_operation_log
  │
  └──N:1──▶ ticket_category
```

---

## 4. ticket — 工单主表

### 用途

记录每条工单的完整信息，是工单模块的核心聚合根。

### 字段设计

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `ticket_id` | BIGINT | Y | AUTO_INCREMENT | 主键 |
| `ticket_no` | VARCHAR(32) | Y | — | 工单编号，格式 `TK202606250001`，唯一 |
| `title` | VARCHAR(200) | Y | — | 工单标题 |
| `content` | TEXT | N | NULL | 工单内容（富文本） |
| `category_id` | BIGINT | N | NULL | 分类 ID → `ticket_category.category_id` |
| `priority` | VARCHAR(10) | Y | `MEDIUM` | 优先级：`LOW` / `MEDIUM` / `HIGH` / `URGENT` |
| `status` | VARCHAR(20) | Y | `NEW` | 状态：`NEW` / `PROCESSING` / `WAIT_CONFIRM` / `CLOSED` / `CANCELLED` |
| `creator_id` | BIGINT | Y | — | 创建人 ID → `sys_user.user_id` |
| `assignee_id` | BIGINT | N | NULL | 指派人 ID → `sys_user.user_id` |
| `dept_id` | BIGINT | Y | — | 创建人所属部门 ID → `sys_dept.dept_id` |
| `processed_at` | DATETIME | N | NULL | 首次进入 PROCESSING 的时间 |
| `closed_at` | DATETIME | N | NULL | 进入 CLOSED 的时间 |
| `del_flag` | CHAR(1) | Y | `0` | 逻辑删除：`0` 存在，`2` 删除 |
| `create_by` | VARCHAR(64) | N | `''` | 创建人账号（BaseEntity） |
| `create_time` | DATETIME | N | NULL | 创建时间（BaseEntity） |
| `update_by` | VARCHAR(64) | N | `''` | 更新人账号（BaseEntity） |
| `update_time` | DATETIME | N | NULL | 更新时间（BaseEntity） |
| `remark` | VARCHAR(500) | N | NULL | 备注（BaseEntity） |

### 索引

| 索引名 | 类型 | 字段 | 说明 |
|---|---|---|---|
| PRIMARY | 主键 | `ticket_id` | — |
| `uk_ticket_no` | 唯一 | `ticket_no` | 工单编号唯一 |
| `idx_status` | 普通 | `status` | 按状态筛选 |
| `idx_creator_id` | 普通 | `creator_id` | 按创建人筛选 |
| `idx_assignee_id` | 普通 | `assignee_id` | 按指派人筛选 |
| `idx_category_id` | 普通 | `category_id` | 按分类筛选 |
| `idx_create_time` | 普通 | `create_time` | 按时间排序 |

### 与若依表的关系

```
ticket.creator_id   ──→ sys_user.user_id    (只读 LEFT JOIN)
ticket.assignee_id  ──→ sys_user.user_id    (只读 LEFT JOIN)
ticket.dept_id      ──→ sys_dept.dept_id    (只读 LEFT JOIN)
```

查询用户/部门名称时的 SQL 示例：

```sql
SELECT t.*,
       u.nick_name AS creator_name,
       a.nick_name AS assignee_name,
       d.dept_name
FROM ticket t
LEFT JOIN sys_user u ON u.user_id = t.creator_id
LEFT JOIN sys_user a ON a.user_id = t.assignee_id
LEFT JOIN sys_dept d ON d.dept_id = t.dept_id
WHERE t.del_flag = '0'
```

---

## 5. ticket_category — 工单分类表

### 用途

工单类型的树形分类（如「硬件设备 > 打印机」「软件系统 > OA」），前端渲染为级联选择器。

### 字段设计

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `category_id` | BIGINT | Y | AUTO_INCREMENT | 主键 |
| `parent_id` | BIGINT | N | `0` | 父分类 ID，`0` 为根节点 |
| `category_name` | VARCHAR(50) | Y | — | 分类名称 |
| `ancestors` | VARCHAR(500) | N | `''` | 祖级路径，如 `0,1,5` |
| `order_num` | INT | N | `0` | 排序 |
| `status` | CHAR(1) | N | `0` | `0` 正常，`1` 停用 |
| `del_flag` | CHAR(1) | Y | `0` | `0` 存在，`2` 删除 |
| `create_by` | VARCHAR(64) | N | `''` | 创建人账号（BaseEntity） |
| `create_time` | DATETIME | N | NULL | 创建时间（BaseEntity） |
| `update_by` | VARCHAR(64) | N | `''` | 更新人账号（BaseEntity） |
| `update_time` | DATETIME | N | NULL | 更新时间（BaseEntity） |
| `remark` | VARCHAR(500) | N | NULL | 备注（BaseEntity） |

### 索引

| 索引名 | 类型 | 字段 | 说明 |
|---|---|---|---|
| PRIMARY | 主键 | `category_id` | — |
| `idx_parent_id` | 普通 | `parent_id` | 查询子分类 |

---

## 6. ticket_comment — 工单评论表

### 用途

工单处理过程中的沟通记录。v1.0 不提供评论编辑功能；如需删除，只允许管理员逻辑删除，不做物理删除。

### 字段设计

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `comment_id` | BIGINT | Y | AUTO_INCREMENT | 主键 |
| `ticket_id` | BIGINT | Y | — | 工单 ID → `ticket.ticket_id` |
| `user_id` | BIGINT | N | NULL | 评论人 ID → `sys_user.user_id` |
| `content` | TEXT | Y | — | 评论内容 |
| `comment_type` | VARCHAR(10) | N | `EXTERNAL` | `INTERNAL` 内部备注 / `EXTERNAL` 公开评论 |
| `del_flag` | CHAR(1) | Y | `0` | `0` 存在，`2` 删除 |
| `create_by` | VARCHAR(64) | N | `''` | 创建人账号（BaseEntity） |
| `create_time` | DATETIME | N | NULL | 创建时间（BaseEntity） |
| `update_by` | VARCHAR(64) | N | `''` | 更新人账号（BaseEntity） |
| `update_time` | DATETIME | N | NULL | 更新时间（BaseEntity） |
| `remark` | VARCHAR(500) | N | NULL | 备注（BaseEntity） |

### 索引

| 索引名 | 类型 | 字段 | 说明 |
|---|---|---|---|
| PRIMARY | 主键 | `comment_id` | — |
| `idx_ticket_id` | 普通 | `ticket_id` | 按工单查评论 |

### 与若依表的关系

```
ticket_comment.user_id  ──→ sys_user.user_id  (只读 LEFT JOIN)
```

---

## 7. ticket_operation_log — 工单操作日志表

### 用途

业务级审计日志，记录每次状态流转的完整信息。日志不可修改、不可删除。

> 不继承 `BaseEntity`——审计日志不可变，不需要 `createBy`/`updateBy`/`delFlag`。

### 字段设计

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `log_id` | BIGINT | Y | AUTO_INCREMENT | 主键 |
| `ticket_id` | BIGINT | Y | — | 工单 ID → `ticket.ticket_id` |
| `operation_type` | VARCHAR(20) | Y | — | `CREATE` / `ASSIGN` / `PROCESS` / `CONFIRM` / `CANCEL` |
| `from_status` | VARCHAR(20) | N | NULL | 变更前状态，创建时为 NULL |
| `to_status` | VARCHAR(20) | N | NULL | 变更后状态 |
| `operator_id` | BIGINT | N | NULL | 操作人 ID → `sys_user.user_id` |
| `operator_name` | VARCHAR(30) | N | NULL | 操作人账号名（冗余，方便查询） |
| `comment` | TEXT | N | NULL | 操作备注（操作人填写的处理说明） |
| `operate_time` | DATETIME | N | NULL | 操作时间 |

### 索引

| 索引名 | 类型 | 字段 | 说明 |
|---|---|---|---|
| PRIMARY | 主键 | `log_id` | — |
| `idx_ticket_id` | 普通 | `ticket_id` | 按工单查操作历史 |
| `idx_operate_time` | 普通 | `operate_time` | 按时间排序 |

---

## 8. 完整建表 SQL

```sql
-- ============================================
-- 工单分类表
-- ============================================
CREATE TABLE ticket_category (
    category_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id     BIGINT       DEFAULT 0 COMMENT '父分类ID，0为根节点',
    category_name VARCHAR(50)  NOT NULL COMMENT '分类名称',
    ancestors     VARCHAR(500) DEFAULT '' COMMENT '祖级路径，如 0,1,5',
    order_num     INT          DEFAULT 0 COMMENT '排序',
    status        CHAR(1)      DEFAULT '0' COMMENT '状态：0正常 1停用',
    del_flag      CHAR(1)      NOT NULL DEFAULT '0' COMMENT '删除标志：0存在 2删除',
    create_by     VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by     VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time   DATETIME     DEFAULT NULL COMMENT '更新时间',
    remark        VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (category_id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单分类表';


-- ============================================
-- 工单主表
-- ============================================
CREATE TABLE ticket (
    ticket_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_no    VARCHAR(32)  NOT NULL COMMENT '工单编号',
    title        VARCHAR(200) NOT NULL COMMENT '工单标题',
    content      TEXT         DEFAULT NULL COMMENT '工单内容',
    category_id  BIGINT       DEFAULT NULL COMMENT '分类ID',
    priority     VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级：LOW/MEDIUM/HIGH/URGENT',
    status       VARCHAR(20)  NOT NULL DEFAULT 'NEW' COMMENT '状态：NEW/PROCESSING/WAIT_CONFIRM/CLOSED/CANCELLED',
    creator_id   BIGINT       NOT NULL COMMENT '创建人ID',
    assignee_id  BIGINT       DEFAULT NULL COMMENT '指派人ID',
    dept_id      BIGINT       NOT NULL COMMENT '创建人部门ID',
    processed_at DATETIME     DEFAULT NULL COMMENT '首次进入处理中的时间',
    closed_at    DATETIME     DEFAULT NULL COMMENT '关闭时间',
    del_flag     CHAR(1)      NOT NULL DEFAULT '0' COMMENT '删除标志：0存在 2删除',
    create_by    VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time  DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by    VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time  DATETIME     DEFAULT NULL COMMENT '更新时间',
    remark       VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (ticket_id),
    UNIQUE KEY uk_ticket_no (ticket_no),
    KEY idx_status (status),
    KEY idx_creator_id (creator_id),
    KEY idx_assignee_id (assignee_id),
    KEY idx_category_id (category_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单主表';


-- ============================================
-- 工单评论表
-- ============================================
CREATE TABLE ticket_comment (
    comment_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id    BIGINT       NOT NULL COMMENT '工单ID',
    user_id      BIGINT       DEFAULT NULL COMMENT '评论人ID',
    content      TEXT         NOT NULL COMMENT '评论内容',
    comment_type VARCHAR(10)  DEFAULT 'EXTERNAL' COMMENT '评论类型：INTERNAL内部备注 EXTERNAL公开评论',
    del_flag     CHAR(1)      NOT NULL DEFAULT '0' COMMENT '删除标志：0存在 2删除',
    create_by    VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time  DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by    VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time  DATETIME     DEFAULT NULL COMMENT '更新时间',
    remark       VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (comment_id),
    KEY idx_ticket_id (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单评论表';


-- ============================================
-- 工单操作日志表
-- ============================================
CREATE TABLE ticket_operation_log (
    log_id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id      BIGINT       NOT NULL COMMENT '工单ID',
    operation_type VARCHAR(20)  NOT NULL COMMENT '操作类型：CREATE/ASSIGN/PROCESS/CONFIRM/CANCEL',
    from_status    VARCHAR(20)  DEFAULT NULL COMMENT '变更前状态',
    to_status      VARCHAR(20)  DEFAULT NULL COMMENT '变更后状态',
    operator_id    BIGINT       DEFAULT NULL COMMENT '操作人ID',
    operator_name  VARCHAR(30)  DEFAULT NULL COMMENT '操作人账号名',
    comment        TEXT         DEFAULT NULL COMMENT '操作备注',
    operate_time   DATETIME     DEFAULT NULL COMMENT '操作时间',
    PRIMARY KEY (log_id),
    KEY idx_ticket_id (ticket_id),
    KEY idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单操作日志表';
```

---

## 9. v1.0 不建的表

| 不建的表 | 说明 | 计划版本 |
|---|---|---|
| SLA 相关表 | 响应时限、解决时限、超时告警 | v1.1 |
| 满意度评价表 | 结单后评分 | v1.2 |
| 附件表 | 工单附件上传 | v2.0 |
| 工单模板表 | 预设工单模板 | v2.0 |
| 通知表 | 短信/邮件/站内信 | v1.2 |
| AI/RAG/Agent 表 | 知识库、向量存储、Agent 会话 | v2.1 |
| 多租户表 | 租户隔离 | 无计划 |
| Flowable ACT_* 表 | 工作流引擎 | 无计划 |
