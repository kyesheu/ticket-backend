SET NAMES utf8mb4;
-- 工单系统 v2.2 增量脚本，前置版本：ticket-v2.1.sql

CREATE TABLE ticket_attachment (
    attachment_id      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '附件ID',
    ticket_id          BIGINT        DEFAULT NULL COMMENT '绑定后的工单ID，临时附件为空',
    business_type      VARCHAR(20)   DEFAULT NULL COMMENT '业务类型：TICKET工单 COMMENT评论，临时附件为空',
    business_id        BIGINT        DEFAULT NULL COMMENT '绑定后的业务记录ID，临时附件为空',
    original_name      VARCHAR(255)  NOT NULL COMMENT '原始文件名',
    storage_path       VARCHAR(500)  NOT NULL COMMENT '文件存储相对路径',
    content_type       VARCHAR(100)  DEFAULT NULL COMMENT 'MIME类型',
    file_extension     VARCHAR(20)   NOT NULL COMMENT '文件扩展名',
    file_size          BIGINT        NOT NULL COMMENT '文件大小，单位字节',
    uploader_id        BIGINT        NOT NULL COMMENT '上传用户ID',
    create_by          VARCHAR(64)   DEFAULT '' COMMENT '创建人',
    create_time        DATETIME      NOT NULL COMMENT '创建时间',
    deleted_flag       CHAR(1)       NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
    delete_by          VARCHAR(64)   DEFAULT '' COMMENT '删除人',
    delete_time        DATETIME      DEFAULT NULL COMMENT '删除时间',
    storage_deleted_flag CHAR(1)     NOT NULL DEFAULT '0' COMMENT '物理文件清理标志：0待清理 1已清理',
    storage_delete_time DATETIME     DEFAULT NULL COMMENT '物理文件清理时间',
    PRIMARY KEY (attachment_id),
    UNIQUE KEY uk_attachment_storage_path (storage_path),
    KEY idx_attachment_ticket (ticket_id, deleted_flag, create_time),
    KEY idx_attachment_business (business_type, business_id, deleted_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单附件表';

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon,
     create_by, create_time, update_by, update_time, remark)
VALUES
    (2040, '附件查询', 2001, 13, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
     'ticket:attachment:list', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2041, '附件上传', 2001, 14, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
     'ticket:attachment:upload', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2042, '附件下载', 2001, 15, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
     'ticket:attachment:download', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2043, '附件删除', 2001, 16, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
     'ticket:attachment:remove', '#', 'admin', NOW(), 'admin', NOW(), '');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2040 AND 2043;
