# v1.3 数据库设计

状态：已完成

本版本不新增表和字段。

- 部门归属使用 ticket.dept_id 快照。
- 自定义部门复用 sys_role_dept。
- 部门树复用 sys_dept.ancestors。
- 角色状态、权限和数据范围读取 RuoYi 现有表。
