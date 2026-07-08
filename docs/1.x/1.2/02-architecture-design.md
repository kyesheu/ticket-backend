# v1.2 架构设计

状态：已完成

- ticket_notification 和 ticket_satisfaction 归属 ticket-ticket。
- 状态流转、评论和 SLA 告警在原业务事务内调用通知 Service。
- 通知查询从登录态取得 recipientId，接口不接受目标用户 ID。
- event_key 由事件类型和不可变来源 ID 组成，数据库唯一键最终兜底。
- 评价 Service 校验对象权限、工单状态、创建人和唯一性。
