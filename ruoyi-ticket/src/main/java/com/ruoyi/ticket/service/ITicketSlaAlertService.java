package com.ruoyi.ticket.service;

import com.ruoyi.ticket.dto.TicketSlaAlertQueryDTO;
import com.ruoyi.ticket.vo.TicketSlaAlertVO;

import java.util.List;

/** 工单 SLA 告警 Service 接口。 */
public interface ITicketSlaAlertService {
    List<TicketSlaAlertVO> selectAlertList(TicketSlaAlertQueryDTO query);
    TicketSlaAlertVO selectAlertById(Long alertId);
    int scanOverdue();
}
