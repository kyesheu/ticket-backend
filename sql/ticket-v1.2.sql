-- 工单系统 v1.2 增量脚本，前置版本：ticket-v1.1.sql

CREATE TABLE ticket_notification (
    notification_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    ticket_id         BIGINT       NOT NULL COMMENT '工单ID',
    recipient_id      BIGINT       NOT NULL COMMENT '接收用户ID',
    notification_type VARCHAR(30)  NOT NULL COMMENT '通知类型',
    event_key         VARCHAR(64)  NOT NULL COMMENT '事件幂等键',
    title             VARCHAR(200) NOT NULL COMMENT '通知标题',
    content           VARCHAR(500) DEFAULT NULL COMMENT '通知内容',
    read_status       CHAR(1)      NOT NULL DEFAULT '0' COMMENT '已读状态：0未读 1已读',
    read_time         DATETIME     DEFAULT NULL COMMENT '已读时间',
    create_time       DATETIME     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (notification_id),
    UNIQUE KEY uk_recipient_event (recipient_id, event_key),
    KEY idx_recipient_read (recipient_id, read_status, create_time),
    KEY idx_ticket_id (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单站内通知表';

CREATE TABLE ticket_satisfaction (
    satisfaction_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    ticket_id       BIGINT       NOT NULL COMMENT '工单ID',
    evaluator_id    BIGINT       NOT NULL COMMENT '评价人ID',
    score           TINYINT      NOT NULL COMMENT '评分：1-5',
    content         VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
    create_time     DATETIME     NOT NULL COMMENT '评价时间',
    PRIMARY KEY (satisfaction_id),
    UNIQUE KEY uk_ticket_id (ticket_id),
    KEY idx_evaluator_id (evaluator_id),
    KEY idx_create_time (create_time),
    CONSTRAINT chk_ticket_satisfaction_score CHECK (score BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单满意度评价表';

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon,
     create_by, create_time, update_by, update_time, remark)
VALUES
    (2023, '我的通知', 2000, 5, 'notification', 'ticket/notification/index', NULL,
     'ticketNotification', 1, 0, 'C', '0', '0', 'ticket:notification:list', 'message',
     'admin', NOW(), 'admin', NOW(), '工单站内通知'),
    (2024, '评价管理', 2000, 6, 'satisfaction', 'ticket/satisfaction/index', NULL,
     'ticketSatisfaction', 1, 0, 'C', '0', '0', 'ticket:satisfaction:list', 'star',
     'admin', NOW(), 'admin', NOW(), '工单满意度评价'),
    (2025, '通知已读', 2023, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
     'ticket:notification:read', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2026, '提交评价', 2001, 10, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
     'ticket:satisfaction:add', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2027, '评价查询', 2024, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
     'ticket:satisfaction:query', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2028, '评价统计', 2024, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
     'ticket:satisfaction:statistics', '#', 'admin', NOW(), 'admin', NOW(), '');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2023 AND 2028;
