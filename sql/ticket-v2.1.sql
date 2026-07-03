-- 工单系统 v2.1 增量脚本，前置版本：ticket-v2.0.sql

ALTER TABLE ticket_workflow_transition
    ADD COLUMN condition_key VARCHAR(64) DEFAULT NULL COMMENT '自定义字段条件key' AFTER condition_field;

CREATE TABLE ticket_custom_field_definition (
    field_id        BIGINT         NOT NULL AUTO_INCREMENT COMMENT '字段定义ID',
    category_id     BIGINT         NOT NULL COMMENT '工单分类ID',
    field_key       VARCHAR(64)    NOT NULL COMMENT '分类内稳定字段key',
    field_name      VARCHAR(100)   NOT NULL COMMENT '字段名称',
    field_type      VARCHAR(30)    NOT NULL COMMENT '字段类型',
    required_flag   CHAR(1)        NOT NULL DEFAULT '0' COMMENT '是否必填：0否 1是',
    default_value   TEXT           DEFAULT NULL COMMENT '默认值',
    max_length      INT            DEFAULT NULL COMMENT '文本最大长度',
    min_number      DECIMAL(20, 6) DEFAULT NULL COMMENT '最小数值',
    max_number      DECIMAL(20, 6) DEFAULT NULL COMMENT '最大数值',
    sort_order      INT            NOT NULL DEFAULT 0 COMMENT '排序',
    status          CHAR(1)        NOT NULL DEFAULT '0' COMMENT '状态：0启用 1停用',
    create_by       VARCHAR(64)    DEFAULT '' COMMENT '创建人',
    create_time     DATETIME       DEFAULT NULL COMMENT '创建时间',
    update_by       VARCHAR(64)    DEFAULT '' COMMENT '更新人',
    update_time     DATETIME       DEFAULT NULL COMMENT '更新时间',
    remark          VARCHAR(500)   DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (field_id),
    UNIQUE KEY uk_category_field_key (category_id, field_key),
    KEY idx_field_category (category_id, status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单自定义字段定义表';

CREATE TABLE ticket_custom_field_option (
    option_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '选项ID',
    field_id        BIGINT       NOT NULL COMMENT '字段定义ID',
    option_value    VARCHAR(100) NOT NULL COMMENT '稳定选项值',
    option_label    VARCHAR(100) NOT NULL COMMENT '展示标签',
    sort_order      INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status          CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态：0启用 1停用',
    create_by       VARCHAR(64)  DEFAULT '' COMMENT '创建人',
    create_time     DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by       VARCHAR(64)  DEFAULT '' COMMENT '更新人',
    update_time     DATETIME     DEFAULT NULL COMMENT '更新时间',
    remark          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (option_id),
    UNIQUE KEY uk_field_option_value (field_id, option_value),
    KEY idx_option_field (field_id, status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单自定义字段选项表';

CREATE TABLE ticket_custom_field_value (
    value_id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字段值ID',
    ticket_id               BIGINT       NOT NULL COMMENT '工单ID',
    field_id                BIGINT       NOT NULL COMMENT '字段定义ID',
    field_key_snapshot      VARCHAR(64)  NOT NULL COMMENT '字段key快照',
    field_name_snapshot     VARCHAR(100) NOT NULL COMMENT '字段名称快照',
    field_type_snapshot     VARCHAR(30)  NOT NULL COMMENT '字段类型快照',
    normalized_value        TEXT         DEFAULT NULL COMMENT '规范化值',
    display_value_snapshot  TEXT         DEFAULT NULL COMMENT '展示值快照',
    sort_order_snapshot     INT          NOT NULL DEFAULT 0 COMMENT '排序快照',
    create_time             DATETIME     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (value_id),
    UNIQUE KEY uk_ticket_field (ticket_id, field_id),
    KEY idx_value_ticket (ticket_id, sort_order_snapshot),
    KEY idx_value_field (field_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单自定义字段值表';

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon,
     create_by, create_time, update_by, update_time, remark)
VALUES
    (2035, '自定义字段', 2000, 8, 'custom-field', 'ticket/custom-field/index', NULL,
     'ticketCustomField', 1, 0, 'C', '0', '0', 'ticket:custom-field:list', 'edit',
     'admin', NOW(), 'admin', NOW(), '工单自定义字段管理'),
    (2036, '字段查询', 2035, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
     'ticket:custom-field:query', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2037, '字段新增', 2035, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
     'ticket:custom-field:add', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2038, '字段修改', 2035, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
     'ticket:custom-field:edit', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2039, '字段表单', 2001, 12, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
     'ticket:custom-field:form', '#', 'admin', NOW(), 'admin', NOW(), '');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2035 AND 2039;
