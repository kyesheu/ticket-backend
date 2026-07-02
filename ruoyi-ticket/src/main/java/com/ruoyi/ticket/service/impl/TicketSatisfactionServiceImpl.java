package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.ticket.domain.Ticket;
import com.ruoyi.ticket.domain.TicketSatisfaction;
import com.ruoyi.ticket.dto.TicketSatisfactionCreateDTO;
import com.ruoyi.ticket.dto.TicketSatisfactionQueryDTO;
import com.ruoyi.ticket.enums.TicketStatus;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.mapper.TicketSatisfactionMapper;
import com.ruoyi.ticket.service.ITicketSatisfactionService;
import com.ruoyi.ticket.vo.TicketSatisfactionStatisticsVO;
import com.ruoyi.ticket.vo.TicketSatisfactionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/** 工单满意度 Service 实现。 */
@Service
public class TicketSatisfactionServiceImpl implements ITicketSatisfactionService {

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private TicketSatisfactionMapper ticketSatisfactionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSatisfaction(Long ticketId, TicketSatisfactionCreateDTO dto) {
        Ticket ticket = ticketMapper.selectTicketEntityById(ticketId);
        if (ticket == null) {
            throw new ServiceException("工单不存在");
        }
        Long userId = SecurityUtils.getUserId();
        if (!Objects.equals(ticket.getCreatorId(), userId)) {
            throw new ServiceException("只有工单创建人可以评价");
        }
        if (!TicketStatus.CLOSED.name().equals(ticket.getStatus())) {
            throw new ServiceException("只有已关闭工单可以评价");
        }
        validateScore(dto.getScore());
        if (dto.getContent() != null && dto.getContent().length() > 500) {
            throw new ServiceException("评价内容不能超过 500 个字符");
        }
        if (ticketSatisfactionMapper.selectByTicketId(ticketId) != null) {
            throw new ServiceException("该工单已评价");
        }

        TicketSatisfaction satisfaction = new TicketSatisfaction();
        satisfaction.setTicketId(ticketId);
        satisfaction.setEvaluatorId(userId);
        satisfaction.setScore(dto.getScore());
        satisfaction.setContent(dto.getContent());
        satisfaction.setCreateTime(new Date());
        try {
            ticketSatisfactionMapper.insertSatisfaction(satisfaction);
        } catch (DuplicateKeyException exception) {
            throw new ServiceException("该工单已评价");
        }
        return satisfaction.getSatisfactionId();
    }

    @Override
    public TicketSatisfactionVO selectByTicketId(Long ticketId) {
        return ticketSatisfactionMapper.selectVOByTicketId(ticketId);
    }

    @Override
    public List<TicketSatisfactionVO> selectSatisfactionList(TicketSatisfactionQueryDTO query) {
        return ticketSatisfactionMapper.selectSatisfactionList(query);
    }

    @Override
    public TicketSatisfactionStatisticsVO selectStatistics(TicketSatisfactionQueryDTO query) {
        return ticketSatisfactionMapper.selectStatistics(query);
    }

    private void validateScore(Integer score) {
        if (score == null || score < 1 || score > 5) {
            throw new ServiceException("评分必须在 1 到 5 之间");
        }
    }
}
