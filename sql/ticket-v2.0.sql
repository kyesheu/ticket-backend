SET NAMES utf8mb4;
-- 工单系统 v2.0 增量脚本，前置版本：ticket-v1.2.sql

ALTER TABLE ticket_category
    ADD COLUMN workflow_key VARCHAR(64) DEFAULT NULL COMMENT '绑定流程标识' AFTER status,
    ADD KEY idx_workflow_key (workflow_key);

CREATE TABLE ticket_workflow_definition (
    definition_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '流程定义ID',
    workflow_key     VARCHAR(64)  NOT NULL COMMENT '流程稳定标识',
    workflow_name    VARCHAR(100) NOT NULL COMMENT '流程名称',
    version          INT          NOT NULL COMMENT '版本号',
    definition_status VARCHAR(20) NOT NULL COMMENT '状态：DRAFT/PUBLISHED/DISABLED',
    is_current       CHAR(1)      NOT NULL DEFAULT '0' COMMENT '是否当前发布版本：0否 1是',
    create_by        VARCHAR(64)  DEFAULT '' COMMENT '创建人',
    create_time      DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by        VARCHAR(64)  DEFAULT '' COMMENT '更新人',
    update_time      DATETIME     DEFAULT NULL COMMENT '更新时间',
    remark           VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (definition_id),
    UNIQUE KEY uk_workflow_version (workflow_key, version),
    KEY idx_workflow_current (workflow_key, is_current, definition_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单流程定义表';

CREATE TABLE ticket_workflow_node (
    node_id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '流程节点ID',
    definition_id   BIGINT       NOT NULL COMMENT '流程定义ID',
    node_key         VARCHAR(64)  NOT NULL COMMENT '节点标识',
    node_name        VARCHAR(100) NOT NULL COMMENT '节点名称',
    node_type        VARCHAR(20)  NOT NULL COMMENT '类型：START/ASSIGN/PROCESS/CONFIRM/END',
    assignee_type    VARCHAR(30)  DEFAULT NULL COMMENT '处理人类型',
    assignee_value   BIGINT       DEFAULT NULL COMMENT '用户或角色ID',
    sort_order       INT          NOT NULL DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (node_id),
    UNIQUE KEY uk_definition_node (definition_id, node_key),
    KEY idx_node_definition (definition_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单流程节点表';

CREATE TABLE ticket_workflow_transition (
    transition_id     BIGINT      NOT NULL AUTO_INCREMENT COMMENT '流程连线ID',
    definition_id    BIGINT       NOT NULL COMMENT '流程定义ID',
    source_node_key  VARCHAR(64)  NOT NULL COMMENT '来源节点标识',
    target_node_key  VARCHAR(64)  NOT NULL COMMENT '目标节点标识',
    condition_field  VARCHAR(30)  DEFAULT NULL COMMENT '条件字段',
    condition_operator VARCHAR(10) DEFAULT NULL COMMENT '条件运算符：EQ/IN',
    condition_value  VARCHAR(500) DEFAULT NULL COMMENT '条件值',
    is_default       CHAR(1)      NOT NULL DEFAULT '0' COMMENT '是否默认连线：0否 1是',
    sort_order       INT          NOT NULL DEFAULT 0 COMMENT '匹配顺序',
    PRIMARY KEY (transition_id),
    KEY idx_transition_source (definition_id, source_node_key, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单流程连线表';

CREATE TABLE ticket_workflow_instance (
    instance_id      BIGINT      NOT NULL AUTO_INCREMENT COMMENT '流程实例ID',
    ticket_id        BIGINT      NOT NULL COMMENT '工单ID',
    definition_id   BIGINT      NOT NULL COMMENT '锁定的流程定义ID',
    workflow_status VARCHAR(20) NOT NULL COMMENT '状态：RUNNING/COMPLETED/CANCELLED/TERMINATED',
    current_node_key VARCHAR(64) DEFAULT NULL COMMENT '当前节点标识',
    started_at      DATETIME    NOT NULL COMMENT '启动时间',
    ended_at        DATETIME    DEFAULT NULL COMMENT '结束时间',
    create_time     DATETIME    NOT NULL COMMENT '创建时间',
    update_time     DATETIME    DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (instance_id),
    UNIQUE KEY uk_instance_ticket (ticket_id),
    KEY idx_instance_status (workflow_status, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单流程实例表';

CREATE TABLE ticket_workflow_task (
    task_id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '流程任务ID',
    instance_id          BIGINT       NOT NULL COMMENT '流程实例ID',
    node_key             VARCHAR(64)  NOT NULL COMMENT '节点标识快照',
    node_name            VARCHAR(100) NOT NULL COMMENT '节点名称快照',
    task_status          VARCHAR(20)  NOT NULL COMMENT '任务状态',
    assignee_type        VARCHAR(30)  NOT NULL COMMENT '处理人类型',
    assignee_ref_id      BIGINT       DEFAULT NULL COMMENT '定义中的用户或角色ID',
    resolved_assignee_id BIGINT       DEFAULT NULL COMMENT '解析后的用户ID',
    completed_by         BIGINT       DEFAULT NULL COMMENT '实际处理用户ID',
    action_type          VARCHAR(20)  DEFAULT NULL COMMENT '完成动作',
    comment              VARCHAR(500) DEFAULT NULL COMMENT '处理意见',
    created_at           DATETIME     NOT NULL COMMENT '任务创建时间',
    completed_at         DATETIME     DEFAULT NULL COMMENT '任务完成时间',
    PRIMARY KEY (task_id),
    KEY idx_task_instance (instance_id, created_at),
    KEY idx_task_user (resolved_assignee_id, task_status),
    KEY idx_task_role (assignee_ref_id, task_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单流程任务表';

INSERT INTO ticket_workflow_definition
    (definition_id, workflow_key, workflow_name, version, definition_status,
     is_current, create_by, create_time, update_by, update_time, remark)
VALUES
    (1, 'STANDARD', '标准工单流程', 1, 'PUBLISHED', '1',
     'admin', NOW(), 'admin', NOW(), '兼容原五状态工单流程');

INSERT INTO ticket_workflow_node
    (definition_id, node_key, node_name, node_type, assignee_type, assignee_value, sort_order)
VALUES
    (1, 'START', '开始', 'START', NULL, NULL, 1),
    (1, 'ASSIGN', '分派工单', 'ASSIGN', 'ROLE', 1, 2),
    (1, 'PROCESS', '处理工单', 'PROCESS', 'TICKET_ASSIGNEE', NULL, 3),
    (1, 'CONFIRM', '确认结果', 'CONFIRM', 'TICKET_CREATOR', NULL, 4),
    (1, 'END', '结束', 'END', NULL, NULL, 5);

INSERT INTO ticket_workflow_transition
    (definition_id, source_node_key, target_node_key, condition_field,
     condition_operator, condition_value, is_default, sort_order)
VALUES
    (1, 'START', 'ASSIGN', NULL, NULL, NULL, '1', 1),
    (1, 'ASSIGN', 'PROCESS', NULL, NULL, NULL, '1', 1),
    (1, 'PROCESS', 'CONFIRM', NULL, NULL, NULL, '1', 1),
    (1, 'CONFIRM', 'END', NULL, NULL, NULL, '1', 1);

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon,
     create_by, create_time, update_by, update_time, remark)
VALUES
    (2029, '流程管理', 2000, 7, 'workflow', 'ticket/workflow/index', NULL,
     'ticketWorkflow', 1, 0, 'C', '0', '0', 'ticket:workflow:list', 'tree',
     'admin', NOW(), 'admin', NOW(), '工单流程定义管理'),
    (2030, '流程查询', 2029, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
     'ticket:workflow:query', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2031, '流程新增', 2029, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
     'ticket:workflow:add', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2032, '流程修改', 2029, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
     'ticket:workflow:edit', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2033, '流程发布', 2029, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
     'ticket:workflow:publish', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2034, '流程任务', 2001, 11, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
     'ticket:workflow:task', '#', 'admin', NOW(), 'admin', NOW(), '');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2029 AND 2034;
