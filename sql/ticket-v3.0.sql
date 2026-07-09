SET NAMES utf8mb4;
-- 工单系统 v3.0 增量脚本，前置版本：ticket-v2.3.sql
-- v3.0 AI 知识库正文存储在 Python/Elasticsearch；分类和菜单存储在 MySQL。

CREATE TABLE IF NOT EXISTS knowledge_category (
    category_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    parent_id     BIGINT       NOT NULL DEFAULT 0 COMMENT '父分类ID',
    category_name VARCHAR(50)  NOT NULL COMMENT '分类名称',
    ancestors     VARCHAR(255) NOT NULL DEFAULT '0' COMMENT '祖级路径',
    order_num     INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status        CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态：0正常 1停用',
    del_flag      CHAR(1)      NOT NULL DEFAULT '0' COMMENT '删除标志：0存在 2删除',
    create_by     VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by     VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time   DATETIME     DEFAULT NULL COMMENT '更新时间',
    remark        VARCHAR(500) DEFAULT '' COMMENT '备注',
    PRIMARY KEY (category_id),
    KEY idx_parent (parent_id),
    UNIQUE KEY uk_knowledge_category_name (parent_id, category_name, del_flag)
) COMMENT='知识库分类表';

INSERT INTO knowledge_category
    (category_id, parent_id, category_name, ancestors, order_num, status, del_flag,
     create_by, create_time, update_by, update_time, remark)
VALUES
    (1, 0, '通用知识', '0', 1, '0', '0', 'admin', NOW(), 'admin', NOW(), '默认知识库分类')
ON DUPLICATE KEY UPDATE
    category_name = VALUES(category_name),
    update_by = 'admin',
    update_time = NOW();

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon,
     create_by, create_time, update_by, update_time, remark)
VALUES
    (2048, '知识库', 0, 6, 'knowledge', NULL, NULL, 'knowledge',
     1, 0, 'M', '0', '0', '', 'document', 'admin', NOW(), 'admin', NOW(), 'AI 知识库'),
    (2054, '文档管理', 2048, 1, 'document', 'ticket/knowledge/index', NULL, 'knowledgeDocument',
     1, 0, 'C', '0', '0', 'ticket:ai:document:list', 'document', 'admin', NOW(), 'admin', NOW(), 'AI 知识库文档管理'),
    (2055, '分类管理', 2048, 2, 'category', 'ticket/knowledge/category/index', NULL, 'knowledgeCategory',
     1, 0, 'C', '0', '0', 'ticket:knowledge-category:list', 'tree', 'admin', NOW(), 'admin', NOW(), '知识库分类管理'),
    (2049, '知识文档查询', 2054, 1, '', NULL, NULL, '',
     1, 0, 'F', '0', '0', 'ticket:ai:document:query', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2050, '知识文档上传', 2054, 2, '', NULL, NULL, '',
     1, 0, 'F', '0', '0', 'ticket:ai:document:import', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2051, '知识文档重导', 2054, 3, '', NULL, NULL, '',
     1, 0, 'F', '0', '0', 'ticket:ai:document:edit', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2052, '知识文档删除', 2054, 4, '', NULL, NULL, '',
     1, 0, 'F', '0', '0', 'ticket:ai:document:remove', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2053, '同步历史工单', 2054, 5, '', NULL, NULL, '',
     1, 0, 'F', '0', '0', 'ticket:ai:history:sync', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2056, '知识库分类查询', 2055, 1, '', NULL, NULL, '',
     1, 0, 'F', '0', '0', 'ticket:knowledge-category:query', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2057, '知识库分类新增', 2055, 2, '', NULL, NULL, '',
     1, 0, 'F', '0', '0', 'ticket:knowledge-category:add', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2058, '知识库分类修改', 2055, 3, '', NULL, NULL, '',
     1, 0, 'F', '0', '0', 'ticket:knowledge-category:edit', '#', 'admin', NOW(), 'admin', NOW(), ''),
    (2059, '知识库分类删除', 2055, 4, '', NULL, NULL, '',
     1, 0, 'F', '0', '0', 'ticket:knowledge-category:remove', '#', 'admin', NOW(), 'admin', NOW(), '')
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    parent_id = VALUES(parent_id),
    order_num = VALUES(order_num),
    path = VALUES(path),
    component = VALUES(component),
    route_name = VALUES(route_name),
    visible = VALUES(visible),
    status = VALUES(status),
    perms = VALUES(perms),
    icon = VALUES(icon),
    update_by = 'admin',
    update_time = NOW(),
    remark = VALUES(remark);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (2048, 2049, 2050, 2051, 2052, 2053, 2054);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2055 AND 2059;
