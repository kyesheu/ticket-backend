-- 工单系统 v1.1 增量脚本
-- 前置版本：ticket-v1.0.sql
-- MySQL 8.0+

-- ============================================
-- 工单 SLA 字段与扫描索引
-- ============================================
ALTER TABLE ticket
    ADD COLUMN response_due_at DATETIME DEFAULT NULL COMMENT '首次响应截止时间快照' AFTER closed_at,
    ADD COLUMN resolve_due_at DATETIME DEFAULT NULL COMMENT '解决截止时间快照' AFTER response_due_at,
    ADD COLUMN response_overdue CHAR(1) NOT NULL DEFAULT '0' COMMENT '响应是否超时：0否 1是' AFTER resolve_due_at,
    ADD COLUMN resolve_overdue CHAR(1) NOT NULL DEFAULT '0' COMMENT '解决是否超时：0否 1是' AFTER response_overdue,
    ADD KEY idx_response_scan (response_overdue, response_due_at, status),
    ADD KEY idx_resolve_scan (resolve_overdue, resolve_due_at, status);

-- ============================================
-- SLA 策略表
-- ============================================
CREATE TABLE ticket_sla_policy (
    policy_id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '策略ID',
    priority         VARCHAR(10)  NOT NULL COMMENT '工单优先级',
    response_minutes INT          NOT NULL COMMENT '首次响应时限（分钟）',
    resolve_minutes  INT          NOT NULL COMMENT '解决时限（分钟）',
    status           CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态：0启用 1停用',
    create_by        VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time      DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by        VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time      DATETIME     DEFAULT NULL COMMENT '更新时间',
    remark           VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (policy_id),
    UNIQUE KEY uk_priority (priority),
    CONSTRAINT chk_ticket_sla_response_minutes CHECK (response_minutes > 0),
    CONSTRAINT chk_ticket_sla_resolve_minutes CHECK (resolve_minutes > response_minutes)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单SLA策略表';

INSERT INTO ticket_sla_policy
    (priority, response_minutes, resolve_minutes, status, create_by, create_time, remark)
VALUES
    ('LOW', 480, 4320, '0', 'admin', NOW(), '低优先级默认策略'),
    ('MEDIUM', 240, 1440, '0', 'admin', NOW(), '中优先级默认策略'),
    ('HIGH', 60, 480, '0', 'admin', NOW(), '高优先级默认策略'),
    ('URGENT', 15, 120, '0', 'admin', NOW(), '紧急工单默认策略');

-- ============================================
-- SLA 告警表
-- ============================================
CREATE TABLE ticket_sla_alert (
    alert_id        BIGINT      NOT NULL AUTO_INCREMENT COMMENT '告警ID',
    ticket_id       BIGINT      NOT NULL COMMENT '工单ID',
    alert_type      VARCHAR(30) NOT NULL COMMENT '告警类型',
    due_at          DATETIME    NOT NULL COMMENT 'SLA截止时间',
    detected_at     DATETIME    NOT NULL COMMENT '发现超时时间',
    overdue_minutes INT         NOT NULL COMMENT '发现时已超时分钟数',
    PRIMARY KEY (alert_id),
    UNIQUE KEY uk_ticket_alert_type (ticket_id, alert_type),
    KEY idx_detected_at (detected_at),
    KEY idx_alert_type (alert_type),
    CONSTRAINT chk_ticket_sla_overdue_minutes CHECK (overdue_minutes >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单SLA告警表';

-- ============================================
-- 存量工单 SLA 快照回填
-- ============================================
UPDATE ticket t
INNER JOIN ticket_sla_policy p ON p.priority = t.priority AND p.status = '0'
SET t.response_due_at = DATE_ADD(t.create_time, INTERVAL p.response_minutes MINUTE),
    t.resolve_due_at = DATE_ADD(t.create_time, INTERVAL p.resolve_minutes MINUTE)
WHERE t.create_time IS NOT NULL
  AND (t.response_due_at IS NULL OR t.resolve_due_at IS NULL);

UPDATE ticket
SET response_overdue = CASE
        WHEN status = 'CANCELLED' THEN '0'
        WHEN processed_at IS NOT NULL AND processed_at > response_due_at THEN '1'
        ELSE '0'
    END,
    resolve_overdue = CASE
        WHEN status = 'CANCELLED' THEN '0'
        WHEN closed_at IS NOT NULL AND closed_at > resolve_due_at THEN '1'
        ELSE '0'
    END
WHERE response_due_at IS NOT NULL
  AND resolve_due_at IS NOT NULL;

-- ============================================
-- 菜单权限
-- ============================================
INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon,
     create_by, create_time, update_by, update_time, remark)
VALUES
    (2016, 'SLA策略', 2000, 3, 'sla', 'ticket/sla/index', NULL, 'ticketSla',
     1, 0, 'C', '0', '0', 'ticket:sla:list', 'time-range',
     'admin', NOW(), 'admin', NOW(), '工单SLA策略管理页'),
    (2017, 'SLA告警', 2000, 4, 'sla-alert', 'ticket/sla-alert/index', NULL, 'ticketSlaAlert',
     1, 0, 'C', '0', '0', 'ticket:sla-alert:list', 'bell',
     'admin', NOW(), 'admin', NOW(), '工单SLA告警页'),
    (2018, 'SLA策略查询', 2016, 1, '', NULL, NULL, '',
     1, 0, 'F', '0', '0', 'ticket:sla:query', '#',
     'admin', NOW(), 'admin', NOW(), ''),
    (2019, 'SLA策略新增', 2016, 2, '', NULL, NULL, '',
     1, 0, 'F', '0', '0', 'ticket:sla:add', '#',
     'admin', NOW(), 'admin', NOW(), ''),
    (2020, 'SLA策略修改', 2016, 3, '', NULL, NULL, '',
     1, 0, 'F', '0', '0', 'ticket:sla:edit', '#',
     'admin', NOW(), 'admin', NOW(), ''),
    (2021, 'SLA告警查询', 2017, 1, '', NULL, NULL, '',
     1, 0, 'F', '0', '0', 'ticket:sla-alert:query', '#',
     'admin', NOW(), 'admin', NOW(), ''),
    (2022, 'SLA手工补扫', 2017, 2, '', NULL, NULL, '',
     1, 0, 'F', '0', '0', 'ticket:sla-alert:scan', '#',
     'admin', NOW(), 'admin', NOW(), '');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2016 AND 2022;

-- TicketSlaTask 在阶段十二实现；任务先暂停，避免 Bean 尚未就绪时触发。
INSERT INTO sys_job
    (job_name, job_group, invoke_target, cron_expression, misfire_policy,
     concurrent, status, create_by, create_time, remark)
VALUES
    ('工单SLA超时扫描', 'TICKET', 'ticketSlaTask.scanOverdue', '0 0/5 * * * ?', '3',
     '1', '0', 'admin', NOW(), '每5分钟扫描工单SLA超时');
