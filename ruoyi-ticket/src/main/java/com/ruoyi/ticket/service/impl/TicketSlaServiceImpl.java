package com.ruoyi.ticket.service.impl;

import com.ruoyi.ticket.domain.Ticket;
import com.ruoyi.ticket.domain.TicketSlaAlert;
import com.ruoyi.ticket.enums.TicketSlaAlertType;
import com.ruoyi.ticket.enums.TicketNotificationType;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.mapper.TicketSlaAlertMapper;
import com.ruoyi.ticket.service.ITicketSlaService;
import com.ruoyi.ticket.service.ITicketNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 工单 SLA 超时扫描 Service 实现
 *
 * @author ticket
 */
@Service
public class TicketSlaServiceImpl implements ITicketSlaService {

    private static final int SCAN_BATCH_SIZE = 100;
    private static final long MILLIS_PER_MINUTE = 60_000L;

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private TicketSlaAlertMapper ticketSlaAlertMapper;

    @Autowired
    private ITicketNotificationService ticketNotificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int scanOverdue() {
        Date detectedAt = new Date();
        int responseAlertCount = scanResponseOverdue(detectedAt);
        int resolveAlertCount = scanResolveOverdue(detectedAt);
        return responseAlertCount + resolveAlertCount;
    }

    private int scanResponseOverdue(Date detectedAt) {
        int alertCount = 0;
        while (true) {
            List<Ticket> tickets = ticketMapper.selectResponseOverdueCandidates(detectedAt, SCAN_BATCH_SIZE);
            if (tickets.isEmpty()) {
                return alertCount;
            }
            int processedCount = 0;
            for (Ticket ticket : tickets) {
                if (ticketMapper.markResponseOverdue(ticket.getTicketId()) == 1) {
                    insertAlert(ticket, TicketSlaAlertType.RESPONSE_OVERDUE,
                            ticket.getResponseDueAt(), detectedAt);
                    alertCount++;
                    processedCount++;
                }
            }
            if (processedCount == 0) {
                return alertCount;
            }
        }
    }

    private int scanResolveOverdue(Date detectedAt) {
        int alertCount = 0;
        while (true) {
            List<Ticket> tickets = ticketMapper.selectResolveOverdueCandidates(detectedAt, SCAN_BATCH_SIZE);
            if (tickets.isEmpty()) {
                return alertCount;
            }
            int processedCount = 0;
            for (Ticket ticket : tickets) {
                if (ticketMapper.markResolveOverdue(ticket.getTicketId()) == 1) {
                    insertAlert(ticket, TicketSlaAlertType.RESOLUTION_OVERDUE,
                            ticket.getResolveDueAt(), detectedAt);
                    alertCount++;
                    processedCount++;
                }
            }
            if (processedCount == 0) {
                return alertCount;
            }
        }
    }

    private void insertAlert(Ticket ticket, TicketSlaAlertType alertType, Date dueAt, Date detectedAt) {
        TicketSlaAlert alert = new TicketSlaAlert();
        alert.setTicketId(ticket.getTicketId());
        alert.setAlertType(alertType.name());
        alert.setDueAt(dueAt);
        alert.setDetectedAt(detectedAt);
        alert.setOverdueMinutes(calculateOverdueMinutes(dueAt, detectedAt));
        ticketSlaAlertMapper.insertAlert(alert);
        Long recipientId = alertType == TicketSlaAlertType.RESPONSE_OVERDUE
                ? ticket.getCreatorId()
                : (ticket.getAssigneeId() != null ? ticket.getAssigneeId() : ticket.getCreatorId());
        ticketNotificationService.createNotification(ticket.getTicketId(), recipientId, null,
                TicketNotificationType.SLA_OVERDUE, "SLA_OVERDUE:" + alert.getAlertId(),
                "工单 SLA 超时", alertType.getLabel());
    }

    private int calculateOverdueMinutes(Date dueAt, Date detectedAt) {
        long overdueMillis = Math.max(0L, detectedAt.getTime() - dueAt.getTime());
        long overdueMinutes = overdueMillis / MILLIS_PER_MINUTE;
        return Math.toIntExact(Math.min(overdueMinutes, Integer.MAX_VALUE));
    }
}
