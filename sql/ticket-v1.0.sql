-- ============================================
-- Ticket 工单模块建表 SQL
-- 数据库: ry-vue
-- 版本: v1.0
-- 日期: 2026-06-25
-- ============================================

-- ============================================
-- 工单分类表
-- ============================================
DROP TABLE IF EXISTS ticket_category;
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
DROP TABLE IF EXISTS ticket;
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
DROP TABLE IF EXISTS ticket_comment;
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
DROP TABLE IF EXISTS ticket_operation_log;
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

-- ============================================
-- 默认分类数据
-- ============================================
INSERT INTO ticket_category (category_id, parent_id, category_name, ancestors, order_num, status, create_by, create_time) VALUES
(1, 0, 'IT支持',    '0',   1, '0', 'admin', NOW()),
(2, 0, '行政后勤',  '0',   2, '0', 'admin', NOW()),
(3, 0, '其他',      '0',   3, '0', 'admin', NOW()),
(4, 1, '硬件设备',  '0,1', 1, '0', 'admin', NOW()),
(5, 1, '软件系统',  '0,1', 2, '0', 'admin', NOW()),
(6, 1, '网络问题',  '0,1', 3, '0', 'admin', NOW()),
(7, 2, '办公用品',  '0,2', 1, '0', 'admin', NOW()),
(8, 2, '环境卫生',  '0,2', 2, '0', 'admin', NOW());

-- ============================================
-- 菜单和权限配置
-- ============================================
-- 工单管理目录 (M)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(2000, '工单管理', 0, 5, 'ticket', NULL, NULL, '', 1, 0, 'M', '0', '0', NULL, 'tree', 'admin', NOW(), 'admin', NOW(), '工单管理根目录'),

-- 工单列表 (C)
(2001, '工单列表', 2000, 1, 'ticket', 'ticket/ticket/index', NULL, '', 1, 0, 'C', '0', '0', 'ticket:ticket:list', 'list', 'admin', NOW(), 'admin', NOW(), '工单列表页'),
-- 分类管理 (C)
(2002, '分类管理', 2000, 2, 'category', 'ticket/category/index', NULL, '', 1, 0, 'C', '0', '0', 'ticket:category:list', 'tree', 'admin', NOW(), 'admin', NOW(), '工单分类管理页'),

-- 工单列表按钮 (F)
(2003, '工单查询', 2001, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'ticket:ticket:query', '#', 'admin', NOW(), 'admin', NOW(), ''),
(2004, '工单新增', 2001, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'ticket:ticket:add', '#', 'admin', NOW(), 'admin', NOW(), ''),
(2005, '工单分派', 2001, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'ticket:ticket:assign', '#', 'admin', NOW(), 'admin', NOW(), ''),
(2006, '工单处理', 2001, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'ticket:ticket:process', '#', 'admin', NOW(), 'admin', NOW(), ''),
(2007, '工单确认', 2001, 5, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'ticket:ticket:confirm', '#', 'admin', NOW(), 'admin', NOW(), ''),
(2008, '工单取消', 2001, 6, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'ticket:ticket:cancel', '#', 'admin', NOW(), 'admin', NOW(), ''),
(2009, '操作日志', 2001, 7, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'ticket:log:list', '#', 'admin', NOW(), 'admin', NOW(), ''),
(2010, '评论列表', 2001, 8, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'ticket:comment:list', '#', 'admin', NOW(), 'admin', NOW(), ''),
(2011, '评论新增', 2001, 9, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'ticket:comment:add', '#', 'admin', NOW(), 'admin', NOW(), ''),

-- 分类管理按钮 (F)
(2012, '分类查询', 2002, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'ticket:category:query', '#', 'admin', NOW(), 'admin', NOW(), ''),
(2013, '分类新增', 2002, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'ticket:category:add', '#', 'admin', NOW(), 'admin', NOW(), ''),
(2014, '分类修改', 2002, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'ticket:category:edit', '#', 'admin', NOW(), 'admin', NOW(), ''),
(2015, '分类删除', 2002, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'ticket:category:remove', '#', 'admin', NOW(), 'admin', NOW(), '');

-- 分配菜单权限给超级管理员角色
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2000 AND 2015;
