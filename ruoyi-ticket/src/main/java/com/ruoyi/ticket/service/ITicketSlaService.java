package com.ruoyi.ticket.service;

/**
 * 工单 SLA 超时扫描 Service 接口
 *
 * @author ticket
 */
public interface ITicketSlaService {

    /**
     * 扫描超时工单并生成幂等告警。
     *
     * @return 本次新增告警数量
     */
    int scanOverdue();
}
