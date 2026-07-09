SET NAMES utf8mb4;
-- 工单系统 v4.0 增量脚本，前置版本：ticket-v3.2.sql
-- v4.0 新增 AI 智能问答入口和转人工建单权限。

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon,
     create_by, create_time, update_by, update_time, remark)
VALUES
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
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (2060, 2061, 2062);
