-- 工单系统 v3.1 增量脚本，前置版本：ticket-v2.3.sql + v3.0 AI 服务

CREATE TABLE ticket_ai_triage_suggestion (
    suggestion_id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'AI分诊建议ID',
    ticket_id              BIGINT        NOT NULL COMMENT '工单ID',
    ticket_updated_at      DATETIME      NOT NULL COMMENT '生成建议时的工单更新时间',
    suggested_category_id  BIGINT        DEFAULT NULL COMMENT '建议分类ID',
    suggested_priority     VARCHAR(20)   DEFAULT NULL COMMENT '建议优先级',
    suggested_assignee_id  BIGINT        DEFAULT NULL COMMENT '建议处理人ID',
    confidence             DECIMAL(5,4)  NOT NULL DEFAULT 0.0000 COMMENT '置信度，范围0到1',
    reason_summary         VARCHAR(1000) DEFAULT NULL COMMENT '脱敏后的简短理由',
    source_refs            JSON          DEFAULT NULL COMMENT '来源引用摘要，不保存正文和提示词',
    suggestion_status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '建议状态：PENDING APPLIED REJECTED EXPIRED',
    final_category_id      BIGINT        DEFAULT NULL COMMENT '用户最终选择分类ID',
    final_priority         VARCHAR(20)   DEFAULT NULL COMMENT '用户最终选择优先级',
    final_assignee_id      BIGINT        DEFAULT NULL COMMENT '用户最终选择处理人ID',
    operated_by            BIGINT        DEFAULT NULL COMMENT '操作人ID',
    operated_at            DATETIME      DEFAULT NULL COMMENT '操作时间',
    create_time            DATETIME      NOT NULL COMMENT '创建时间',
    update_time            DATETIME      NOT NULL COMMENT '更新时间',
    PRIMARY KEY (suggestion_id),
    KEY idx_ai_triage_ticket_time (ticket_id, create_time),
    KEY idx_ai_triage_status_time (suggestion_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI分诊建议表';
