package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.ticket.dto.TicketSlaAlertQueryDTO;
import com.ruoyi.ticket.mapper.TicketSlaAlertMapper;
import com.ruoyi.ticket.service.ITicketSlaAlertService;
import com.ruoyi.ticket.service.ITicketSlaService;
import com.ruoyi.ticket.vo.TicketSlaAlertVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/** 工单 SLA 告警 Service 实现。 */
@Service
public class TicketSlaAlertServiceImpl implements ITicketSlaAlertService {

    @Autowired
    private TicketSlaAlertMapper ticketSlaAlertMapper;

    @Autowired
    private ITicketSlaService ticketSlaService;

    @Override
    public List<TicketSlaAlertVO> selectAlertList(TicketSlaAlertQueryDTO query) {
        return ticketSlaAlertMapper.selectAlertList(query);
    }

    @Override
    public TicketSlaAlertVO selectAlertById(Long alertId) {
        TicketSlaAlertVO alert = ticketSlaAlertMapper.selectAlertById(alertId);
        if (alert == null) {
            throw new ServiceException("SLA 告警不存在");
        }
        return alert;
    }

    @Override
    public int scanOverdue() {
        return ticketSlaService.scanOverdue();
    }
}
