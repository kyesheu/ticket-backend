# v1.1 架构设计

状态：已完成

## 组件

- SLA 策略：Controller → ITicketSlaPolicyService → Mapper。
- SLA 判定：ITicketSlaService 负责截止时间和超时事实。
- 调度入口：TicketSlaTask 暴露 scanOverdue，由现有 Quartz 配置调用。

## 约束

- ruoyi-ticket 不依赖 ruoyi-quartz，ruoyi-quartz 不写工单业务。
- 创建工单时在原事务中查询启用策略并写截止时间快照。
- 扫描按页处理；更新超时标记和插入告警使用事务。
- 唯一键兜底重复或并发扫描；扫描只记录事实，不发送外部消息。
