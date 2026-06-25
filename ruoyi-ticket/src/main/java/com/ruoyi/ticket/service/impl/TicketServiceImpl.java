package com.ruoyi.ticket.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.ticket.dto.TicketAssignDTO;
import com.ruoyi.ticket.dto.TicketCancelDTO;
import com.ruoyi.ticket.dto.TicketConfirmDTO;
import com.ruoyi.ticket.dto.TicketCreateDTO;
import com.ruoyi.ticket.dto.TicketProcessDTO;
import com.ruoyi.ticket.dto.TicketQueryDTO;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.service.ITicketService;
import com.ruoyi.ticket.vo.TicketListVO;
import com.ruoyi.ticket.vo.TicketVO;

/**
 * 工单 Service 实现
 *
 * @author ticket
 */
@Service
public class TicketServiceImpl implements ITicketService {

    @Autowired
    private TicketMapper ticketMapper;

    @Override
    public List<TicketListVO> selectTicketList(TicketQueryDTO query) {
        // TODO: 阶段五实现
        return null;
    }

    @Override
    public TicketVO selectTicketById(Long ticketId) {
        // TODO: 阶段五实现
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTicket(TicketCreateDTO dto) {
        // TODO: 阶段五实现
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTicket(Long ticketId, TicketAssignDTO dto) {
        // TODO: 阶段五实现
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processTicket(Long ticketId, TicketProcessDTO dto) {
        // TODO: 阶段五实现
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmTicket(Long ticketId, TicketConfirmDTO dto) {
        // TODO: 阶段五实现
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTicket(Long ticketId, TicketCancelDTO dto) {
        // TODO: 阶段五实现
    }
}
