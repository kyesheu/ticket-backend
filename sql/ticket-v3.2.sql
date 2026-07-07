SET NAMES utf8mb4;
-- 工单系统 v3.2 增量脚本，前置版本：ticket-v3.1.sql

CREATE TABLE ticket_ai_feedback (
    feedback_id      BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'AI反馈ID',
    ticket_id        BIGINT        NOT NULL COMMENT '工单ID',
    target_type      VARCHAR(20)   NOT NULL COMMENT '反馈目标类型：ASSIST TRIAGE',
    target_id        BIGINT        NOT NULL COMMENT '反馈目标ID',
    feedback_value   VARCHAR(20)   NOT NULL COMMENT '反馈值：USEFUL NOT_USEFUL',
    adopted          TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否采纳',
    comment          VARCHAR(500)  DEFAULT NULL COMMENT '可选短评，不保存模型输出或工单正文',
    evaluator_id     BIGINT        NOT NULL COMMENT '评价人ID',
    create_time      DATETIME      NOT NULL COMMENT '创建时间',
    PRIMARY KEY (feedback_id),
    UNIQUE KEY uk_ai_feedback_evaluator_target (evaluator_id, target_type, target_id),
    KEY idx_ai_feedback_ticket (ticket_id),
    KEY idx_ai_feedback_target_time (target_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI建议反馈表';
