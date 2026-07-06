SET NAMES utf8mb4;
-- 工单系统 v2.3 增量脚本，前置版本：ticket-v2.2.sql

CREATE TABLE ticket_search_event (
    event_id        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '检索事件ID',
    ticket_id       BIGINT        DEFAULT NULL COMMENT '工单ID；全量重建事件可为空',
    event_type      VARCHAR(20)   NOT NULL COMMENT '事件类型：UPSERT DELETE REBUILD',
    event_status    VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING PROCESSING SUCCEEDED FAILED',
    retry_count     INT           NOT NULL DEFAULT 0 COMMENT '已重试次数',
    next_retry_at   DATETIME      DEFAULT NULL COMMENT '下次允许重试时间',
    claimed_at      DATETIME      DEFAULT NULL COMMENT 'Worker抢占时间',
    processed_at    DATETIME      DEFAULT NULL COMMENT '处理完成时间',
    error_message   VARCHAR(1000) DEFAULT NULL COMMENT '截断脱敏后的错误摘要',
    create_time     DATETIME      NOT NULL COMMENT '创建时间',
    update_time     DATETIME      NOT NULL COMMENT '更新时间',
    PRIMARY KEY (event_id),
    KEY idx_search_event_dispatch (event_status, next_retry_at, event_id),
    KEY idx_search_event_ticket (ticket_id, event_id),
    KEY idx_search_event_claim (event_status, claimed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单检索事务事件表';

CREATE TABLE ticket_search_rebuild (
    rebuild_id       BIGINT        NOT NULL COMMENT '固定主键，当前版本使用1',
    rebuild_status   VARCHAR(20)   NOT NULL COMMENT '重建状态：IDLE RUNNING SUCCEEDED FAILED',
    index_name       VARCHAR(128)  DEFAULT NULL COMMENT '本次物理索引名',
    total_count      BIGINT        NOT NULL DEFAULT 0 COMMENT '预计文档总数',
    processed_count  BIGINT        NOT NULL DEFAULT 0 COMMENT '已处理文档数',
    last_ticket_id   BIGINT        NOT NULL DEFAULT 0 COMMENT '主键游标',
    start_event_id   BIGINT        NOT NULL DEFAULT 0 COMMENT '重建开始时事件水位',
    max_ticket_id    BIGINT        NOT NULL DEFAULT 0 COMMENT '重建开始时工单主键上界',
    error_message    VARCHAR(500)  DEFAULT NULL COMMENT '脱敏错误摘要',
    started_at       DATETIME      DEFAULT NULL COMMENT '开始时间',
    ended_at         DATETIME      DEFAULT NULL COMMENT '结束时间',
    update_time      DATETIME      NOT NULL COMMENT '更新时间',
    PRIMARY KEY (rebuild_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单检索全量重建状态表';

INSERT INTO ticket_search_rebuild
    (rebuild_id, rebuild_status, total_count, processed_count, last_ticket_id,
     start_event_id, max_ticket_id, update_time)
VALUES (1, 'IDLE', 0, 0, 0, 0, 0, NOW());

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon,
     create_by, create_time, update_by, update_time, remark)
VALUES
    (2044, '工单检索', 2000, 9, 'search', 'ticket/search/index', NULL, 'ticketSearch',
     1, 0, 'C', '0', '0', 'ticket:search:query', 'search', 'admin', NOW(), 'admin', NOW(), ''),
    (2045, '检索查询', 2044, 1, '', NULL, NULL, '',
     1, 0, 'F', '0', '0', 'ticket:search:query', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2046, '索引重建', 2044, 2, '', NULL, NULL, '',
     1, 0, 'F', '0', '0', 'ticket:search:rebuild', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2047, '失败补偿', 2044, 3, '', NULL, NULL, '',
     1, 0, 'F', '0', '0', 'ticket:search:retry', '#', 'admin', NOW(), 'admin', NOW(), '');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2044 AND 2047;

-- 默认暂停；配置并启用 Elasticsearch 后由管理员手工启用。
INSERT INTO sys_job
    (job_name, job_group, invoke_target, cron_expression, misfire_policy,
     concurrent, status, create_by, create_time, remark)
VALUES
    ('工单检索事件同步', 'TICKET', 'ticketSearchTask.dispatch', '0/10 * * * * ?', '3',
     '1', '1', 'admin', NOW(), '每10秒同步工单检索事件，启用前需配置Elasticsearch');
