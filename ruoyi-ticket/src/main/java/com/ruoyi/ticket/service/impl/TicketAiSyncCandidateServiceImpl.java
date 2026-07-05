package com.ruoyi.ticket.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.ticket.dto.TicketAiClosedTicketSyncDTO;
import com.ruoyi.ticket.enums.TicketStatus;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.model.TicketAiSyncCandidate;
import com.ruoyi.ticket.service.ITicketAiSyncCandidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 历史工单同步候选查询 Service 实现。
 */
@Service
public class TicketAiSyncCandidateServiceImpl implements ITicketAiSyncCandidateService {

    private static final int MAX_BATCH_SIZE = 100;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final TicketMapper ticketMapper;
    private final ZoneId zoneId;

    @Autowired
    public TicketAiSyncCandidateServiceImpl(TicketMapper ticketMapper) {
        this(ticketMapper, ZoneId.systemDefault());
    }

    TicketAiSyncCandidateServiceImpl(TicketMapper ticketMapper, ZoneId zoneId) {
        this.ticketMapper = ticketMapper;
        this.zoneId = zoneId;
    }

    @Override
    public List<TicketAiClosedTicketSyncDTO> selectCandidatesAfter(Long lastTicketId, Integer limit) {
        if (lastTicketId == null || lastTicketId < 0L) {
            throw new ServiceException("同步游标必须大于等于 0");
        }
        if (limit == null || limit <= 0 || limit > MAX_BATCH_SIZE) {
            throw new ServiceException("同步批量大小必须在 1 到 100 之间");
        }
        return ticketMapper.selectAiSyncCandidatesAfter(lastTicketId, limit).stream()
                .filter(this::isEligible)
                .map(this::toDto)
                .toList();
    }

    private boolean isEligible(TicketAiSyncCandidate candidate) {
        return TicketStatus.CLOSED.name().equals(candidate.getStatus())
                && StringUtils.hasText(candidate.getSolution())
                && candidate.getClosedTime() != null
                && candidate.getCreatedTime() != null
                && candidate.getSourceGeneration() != null;
    }

    private TicketAiClosedTicketSyncDTO toDto(TicketAiSyncCandidate candidate) {
        TicketAiClosedTicketSyncDTO dto = new TicketAiClosedTicketSyncDTO();
        dto.setTicketId(candidate.getTicketId());
        dto.setTitle(candidate.getTitle());
        dto.setCategory(candidate.getCategory());
        dto.setDescription(candidate.getDescription());
        dto.setSolution(candidate.getSolution().trim());
        dto.setStatus(TicketStatus.CLOSED.name());
        dto.setTags(buildTags(candidate));
        dto.setCreatedTime(formatTime(candidate.getCreatedTime().toInstant()));
        dto.setClosedTime(formatTime(candidate.getClosedTime().toInstant()));
        dto.setSourceGeneration(candidate.getSourceGeneration());
        return dto;
    }

    private List<String> buildTags(TicketAiSyncCandidate candidate) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (StringUtils.hasText(candidate.getCategory())) {
            tags.add(candidate.getCategory());
        }
        if (StringUtils.hasText(candidate.getPriority())) {
            tags.add(candidate.getPriority());
        }
        return List.copyOf(tags);
    }

    private String formatTime(Instant instant) {
        return TIME_FORMATTER.format(instant.atZone(zoneId));
    }
}
