# v1.0 架构设计

状态：已完成

## 模块边界

- ruoyi-admin：启动入口和配置，不承载工单业务。
- ruoyi-ticket：Controller、Service、Mapper、Domain、DTO、VO、Enum。
- ruoyi-ticket 仅依赖 ruoyi-common；用户和部门名称通过 Mapper 查询，不依赖 ruoyi-system Service。

## 分层

Controller 负责鉴权、参数校验和响应；Service 负责状态机、权限规则和事务；Mapper 只负责数据访问。依赖方向固定为 Controller → Service → Mapper。

## 关键约束

- 状态、优先级和操作类型使用 String 枚举，禁止 ordinal。
- 状态更新和操作日志写入同一事务。
- 管理员可查看全部；普通用户仅查看本人创建或当前指派给本人的工单。
- 业务异常统一使用 ServiceException，参数校验交给 DTO。
- SQL 使用参数绑定；动态排序必须经过白名单校验。
