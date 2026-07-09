SET NAMES utf8mb4;
-- 工单系统 v4.0 增量脚本，前置版本：ticket-v3.2.sql
-- v4.0 新增 AI 智能问答入口和转人工建单权限。

CREATE TABLE IF NOT EXISTS ticket_ai_session (
    session_id  BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'AI会话ID',
    user_id     BIGINT        NOT NULL COMMENT '提问用户ID',
    question    TEXT          NOT NULL COMMENT '用户原始问题',
    answer      TEXT          DEFAULT NULL COMMENT 'AI回答',
    suggestion  TEXT          DEFAULT NULL COMMENT 'AI处理建议',
    confidence  DECIMAL(5,4)  NOT NULL DEFAULT 0 COMMENT '回答置信度',
    need_human  CHAR(1)       NOT NULL DEFAULT '1' COMMENT '是否建议转人工：0否 1是',
    degraded    CHAR(1)       NOT NULL DEFAULT '0' COMMENT '是否降级：0否 1是',
    reason      VARCHAR(255)  DEFAULT NULL COMMENT '降级原因',
    status      VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/RESOLVED/ESCALATED',
    ticket_id   BIGINT        DEFAULT NULL COMMENT '转人工后工单ID',
    create_time DATETIME      DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME      DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (session_id),
    KEY idx_ai_session_user_time (user_id, create_time),
    KEY idx_ai_session_status_time (status, create_time),
    KEY idx_ai_session_ticket (ticket_id)
) COMMENT='AI问答会话表';

CREATE TABLE IF NOT EXISTS ticket_ai_session_source (
    source_ref_id BIGINT        NOT NULL AUTO_INCREMENT COMMENT '来源引用ID',
    session_id    BIGINT        NOT NULL COMMENT 'AI会话ID',
    source_type   VARCHAR(32)   NOT NULL COMMENT '来源类型',
    source_id     VARCHAR(128)  NOT NULL COMMENT '来源ID',
    title         VARCHAR(255)  DEFAULT NULL COMMENT '来源标题',
    snippet       TEXT          DEFAULT NULL COMMENT '引用片段',
    score         DECIMAL(8,4)  NOT NULL DEFAULT 0 COMMENT '相似度',
    metadata_json TEXT          DEFAULT NULL COMMENT '元数据JSON',
    create_time   DATETIME      DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (source_ref_id),
    KEY idx_ai_session_source_session (session_id)
) COMMENT='AI问答引用来源表';

CREATE TABLE IF NOT EXISTS ticket_ai_escalation (
    escalation_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '转人工记录ID',
    session_id    BIGINT       DEFAULT NULL COMMENT 'AI会话ID',
    ticket_id     BIGINT       NOT NULL COMMENT '工单ID',
    user_id       BIGINT       NOT NULL COMMENT '操作用户ID',
    user_comment  TEXT         DEFAULT NULL COMMENT '用户补充说明',
    ai_summary    TEXT         DEFAULT NULL COMMENT 'AI回答摘要',
    create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (escalation_id),
    KEY idx_ai_escalation_session (session_id),
    KEY idx_ai_escalation_ticket (ticket_id)
) COMMENT='AI问答转人工记录表';

SET @ddl = IF(
    (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'source_type') = 0,
    'ALTER TABLE ticket ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT ''MANUAL'' COMMENT ''来源类型：MANUAL/AI_ESCALATION'' AFTER resolve_overdue',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'ai_session_id') = 0,
    'ALTER TABLE ticket ADD COLUMN ai_session_id BIGINT DEFAULT NULL COMMENT ''来源AI会话ID'' AFTER source_type',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'ai_summary') = 0,
    'ALTER TABLE ticket ADD COLUMN ai_summary TEXT DEFAULT NULL COMMENT ''AI问答摘要'' AFTER ai_session_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'dispatch_mode') = 0,
    'ALTER TABLE ticket ADD COLUMN dispatch_mode VARCHAR(32) NOT NULL DEFAULT ''MANUAL'' COMMENT ''分派方式：MANUAL/AI_AUTO'' AFTER ai_summary',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'dispatch_reason') = 0,
    'ALTER TABLE ticket ADD COLUMN dispatch_reason VARCHAR(1000) DEFAULT NULL COMMENT ''分派原因'' AFTER dispatch_mode',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS ticket_dispatch_rule (
    rule_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分派规则ID',
    category_id BIGINT       NOT NULL COMMENT '工单分类ID',
    handler_id  BIGINT       NOT NULL COMMENT '默认处理人ID',
    priority    VARCHAR(16)  DEFAULT NULL COMMENT '适用优先级，空表示全部',
    enabled     CHAR(1)      NOT NULL DEFAULT '1' COMMENT '是否启用：0否 1是',
    create_by   VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by   VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    remark      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (rule_id),
    UNIQUE KEY uk_dispatch_rule_category_handler_priority (category_id, handler_id, priority),
    KEY idx_dispatch_rule_category_enabled (category_id, enabled)
) COMMENT='工单自动分派规则表';

CREATE TABLE IF NOT EXISTS ticket_ai_dispatch_log (
    log_id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'AI分派日志ID',
    ticket_id             BIGINT        NOT NULL COMMENT '工单ID',
    session_id            BIGINT        DEFAULT NULL COMMENT 'AI会话ID',
    suggested_category_id BIGINT        DEFAULT NULL COMMENT 'AI建议分类ID',
    suggested_priority    VARCHAR(16)   DEFAULT NULL COMMENT 'AI建议优先级',
    suggested_assignee_id BIGINT        DEFAULT NULL COMMENT 'AI建议处理人ID',
    confidence            DECIMAL(5,4)  NOT NULL DEFAULT 0 COMMENT '综合置信度',
    decision              VARCHAR(32)   NOT NULL COMMENT '决策：auto_assigned/manual_required/rejected',
    reason                VARCHAR(1000) DEFAULT NULL COMMENT '分派理由或失败原因',
    create_time           DATETIME      DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (log_id),
    KEY idx_ai_dispatch_ticket_time (ticket_id, create_time),
    KEY idx_ai_dispatch_decision_time (decision, create_time)
) COMMENT='AI自动分派日志表';

INSERT IGNORE INTO ticket_dispatch_rule
    (category_id, handler_id, priority, enabled, create_by, create_time, update_by, update_time, remark)
SELECT c.category_id, u.user_id, NULL, '1', 'admin', NOW(), 'admin', NOW(), 'v4默认规则：分类可自动分派给正常用户'
FROM ticket_category c
JOIN sys_user u ON u.del_flag = '0' AND u.status = '0'
WHERE c.del_flag = '0'
  AND c.status = '0'
  AND u.user_id <> 1;

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon,
     create_by, create_time, update_by, update_time, remark)
VALUES
    (2059, '我的待办', 2000, 0, 'workbench', 'ticket/workbench/index', NULL, 'ticketWorkbench',
     1, 0, 'C', '0', '0', 'ticket:workbench:list', 'tickets', 'admin', NOW(), 'admin', NOW(), '处理人工作台'),
    (2060, 'AI 助手', 0, 5, 'ai', NULL, NULL, 'ai',
     1, 0, 'M', '0', '0', '', 'education', 'admin', NOW(), 'admin', NOW(), 'AI 前置问答目录'),
    (2061, '智能问答', 2060, 1, 'ask', 'ticket/ai/ask/index', NULL, 'aiAsk',
     1, 0, 'C', '0', '0', 'ticket:ai:ask', 'education', 'admin', NOW(), 'admin', NOW(), 'AI 前置问答入口'),
    (2062, '转人工建单', 2061, 1, '', NULL, NULL, '',
     1, 0, 'F', '0', '0', 'ticket:ai:escalate', '#', 'admin', NOW(), 'admin', NOW(), '')
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    parent_id = VALUES(parent_id),
    order_num = VALUES(order_num),
    path = VALUES(path),
    component = VALUES(component),
    route_name = VALUES(route_name),
    is_frame = VALUES(is_frame),
    is_cache = VALUES(is_cache),
    menu_type = VALUES(menu_type),
    visible = VALUES(visible),
    status = VALUES(status),
    perms = VALUES(perms),
    icon = VALUES(icon),
    update_by = 'admin',
    update_time = NOW(),
    remark = VALUES(remark);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (2059, 2060, 2061, 2062);
