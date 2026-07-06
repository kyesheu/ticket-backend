# v2.0 架构设计

状态：已完成

## 核心模块

ITicketWorkflowEngine 提供启动、完成、退回、取消和终止接口，内部封装定义校验、版本解析、条件路由、处理人解析、并发控制及状态映射。

## 不变量

- 定义必须有一个 START、至少一个 END，所有节点可达，条件分支有唯一默认连线。
- 节点类型限定 START、ASSIGN、PROCESS、CONFIRM、END。
- 处理人限定 USER、ROLE、CREATOR_DEPT_LEADER、TICKET_ASSIGNEE、TICKET_CREATOR。
- 完成任务使用 PENDING 条件更新做乐观并发控制。
- ticket.status 保留粗粒度业务状态；实例和任务保存细粒度流程状态。
- 对象访问权与任务处理权分开校验。
