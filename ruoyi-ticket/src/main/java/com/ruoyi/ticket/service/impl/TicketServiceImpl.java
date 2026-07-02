package com.ruoyi.ticket.service.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.ticket.domain.Ticket;
import com.ruoyi.ticket.domain.TicketOperationLog;
import com.ruoyi.ticket.domain.TicketSlaPolicy;
import com.ruoyi.ticket.dto.TicketAssignDTO;
import com.ruoyi.ticket.dto.TicketCancelDTO;
import com.ruoyi.ticket.dto.TicketConfirmDTO;
import com.ruoyi.ticket.dto.TicketCreateDTO;
import com.ruoyi.ticket.dto.TicketProcessDTO;
import com.ruoyi.ticket.dto.TicketQueryDTO;
import com.ruoyi.ticket.enums.TicketOperationType;
import com.ruoyi.ticket.enums.TicketPriority;
import com.ruoyi.ticket.enums.TicketStatus;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.mapper.TicketOperationLogMapper;
import com.ruoyi.ticket.mapper.TicketSlaPolicyMapper;
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

    /** 工单编号前缀 */
    private static final String TICKET_NO_PREFIX = "TK";

    /** 默认优先级 */
    private static final String DEFAULT_PRIORITY = TicketPriority.MEDIUM.name();

    /** 每分钟毫秒数 */
    private static final long MILLIS_PER_MINUTE = 60_000L;

    /** 未超时标记 */
    private static final String NOT_OVERDUE = "0";

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private TicketOperationLogMapper ticketOperationLogMapper;

    @Autowired
    private TicketSlaPolicyMapper ticketSlaPolicyMapper;

    @Override
    public List<TicketListVO> selectTicketList(TicketQueryDTO query) {
        // 数据范围控制：管理员看全部，普通用户只看自己相关工单
        Long currentUserId = SecurityUtils.getUserId();
        if (SecurityUtils.isAdmin()) {
            // 管理员也显式设置 dataScope，防止用户通过 query params 注入 SQL
            query.getParams().put("dataScope", "1 = 1");
        } else {
            query.getParams().put("dataScope",
                    "t.creator_id = " + currentUserId + " OR t.assignee_id = " + currentUserId);
        }
        return ticketMapper.selectTicketList(query);
    }

    @Override
    public TicketVO selectTicketById(Long ticketId) {
        TicketVO vo = ticketMapper.selectTicketById(ticketId);
        if (vo == null) {
            throw new ServiceException("工单不存在");
        }
        // 查询评论列表和操作日志（阶段六完善 Service 注入后补充）
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTicket(TicketCreateDTO dto) {
        // 生成工单编号
        String ticketNo = generateTicketNo();
        // 确定优先级
        String priority = StringUtils.isNotBlank(dto.getPriority())
                ? dto.getPriority() : DEFAULT_PRIORITY;
        TicketSlaPolicy slaPolicy = ticketSlaPolicyMapper.selectEnabledPolicyByPriority(priority);
        if (slaPolicy == null) {
            throw new ServiceException("该优先级未配置启用的 SLA 策略");
        }

        Date createTime = new Date();

        Ticket ticket = new Ticket();
        ticket.setTicketNo(ticketNo);
        ticket.setTitle(dto.getTitle());
        ticket.setContent(dto.getContent());
        ticket.setCategoryId(dto.getCategoryId());
        ticket.setPriority(priority);
        ticket.setStatus(TicketStatus.NEW.name());
        ticket.setCreatorId(SecurityUtils.getUserId());
        ticket.setDeptId(SecurityUtils.getDeptId());
        ticket.setResponseDueAt(addMinutes(createTime, slaPolicy.getResponseMinutes()));
        ticket.setResolveDueAt(addMinutes(createTime, slaPolicy.getResolveMinutes()));
        ticket.setResponseOverdue(NOT_OVERDUE);
        ticket.setResolveOverdue(NOT_OVERDUE);
        ticket.setDelFlag("0");
        ticket.setCreateBy(SecurityUtils.getUsername());
        ticket.setCreateTime(createTime);
        ticket.setUpdateBy(SecurityUtils.getUsername());
        ticket.setUpdateTime(createTime);

        ticketMapper.insertTicket(ticket);

        // 写入创建日志
        saveOperationLog(ticket.getTicketId(), TicketOperationType.CREATE,
                null, TicketStatus.NEW, null);

        return ticket.getTicketId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTicket(Long ticketId, TicketAssignDTO dto) {
        Ticket ticket = getTicketOrThrow(ticketId);
        TicketStatus currentStatus = toTicketStatus(ticket.getStatus());

        // 状态校验
        if (!currentStatus.canTransitionTo(TicketStatus.PROCESSING)) {
            throw new ServiceException("当前状态不允许分派");
        }
        // 校验指派人存在
        if (dto.getAssigneeId() == null) {
            throw new ServiceException("指派人不能为空");
        }
        if (ticketMapper.checkUserExists(dto.getAssigneeId()) == 0) {
            throw new ServiceException("指派人不存在");
        }

        TicketStatus fromStatus = currentStatus;
        ticket.setStatus(TicketStatus.PROCESSING.name());
        ticket.setAssigneeId(dto.getAssigneeId());
        ticket.setProcessedAt(new Date());
        ticket.setUpdateBy(SecurityUtils.getUsername());
        ticket.setUpdateTime(new Date());
        ticketMapper.updateTicket(ticket);

        saveOperationLog(ticketId, TicketOperationType.ASSIGN, fromStatus,
                TicketStatus.PROCESSING, dto.getComment());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processTicket(Long ticketId, TicketProcessDTO dto) {
        Ticket ticket = getTicketOrThrow(ticketId);
        TicketStatus currentStatus = toTicketStatus(ticket.getStatus());

        // 状态校验
        if (!currentStatus.canTransitionTo(TicketStatus.WAIT_CONFIRM)) {
            throw new ServiceException("当前状态不允许处理");
        }
        // 校验当前用户是指派人
        Long currentUserId = SecurityUtils.getUserId();
        if (ticket.getAssigneeId() == null
                || !ticket.getAssigneeId().equals(currentUserId)) {
            throw new ServiceException("您不是该工单的指派人，无法处理");
        }
        // 处理备注必填
        if (StringUtils.isBlank(dto.getComment())) {
            throw new ServiceException("处理备注不能为空");
        }

        TicketStatus fromStatus = currentStatus;
        ticket.setStatus(TicketStatus.WAIT_CONFIRM.name());
        ticket.setUpdateBy(SecurityUtils.getUsername());
        ticket.setUpdateTime(new Date());
        ticketMapper.updateTicket(ticket);

        saveOperationLog(ticketId, TicketOperationType.PROCESS, fromStatus,
                TicketStatus.WAIT_CONFIRM, dto.getComment());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmTicket(Long ticketId, TicketConfirmDTO dto) {
        Ticket ticket = getTicketOrThrow(ticketId);
        TicketStatus currentStatus = toTicketStatus(ticket.getStatus());

        // 状态校验
        if (!currentStatus.canTransitionTo(TicketStatus.CLOSED)) {
            throw new ServiceException("当前状态不允许确认");
        }
        // 校验当前用户是创建人或管理员
        Long currentUserId = SecurityUtils.getUserId();
        if (!Objects.equals(ticket.getCreatorId(), currentUserId) && !SecurityUtils.isAdmin()) {
            throw new ServiceException("您不是该工单的创建人，无法确认");
        }

        TicketStatus fromStatus = currentStatus;
        ticket.setStatus(TicketStatus.CLOSED.name());
        ticket.setClosedAt(new Date());
        ticket.setUpdateBy(SecurityUtils.getUsername());
        ticket.setUpdateTime(new Date());
        ticketMapper.updateTicket(ticket);

        saveOperationLog(ticketId, TicketOperationType.CONFIRM, fromStatus,
                TicketStatus.CLOSED, dto.getComment());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTicket(Long ticketId, TicketCancelDTO dto) {
        Ticket ticket = getTicketOrThrow(ticketId);
        TicketStatus currentStatus = toTicketStatus(ticket.getStatus());

        // 状态校验：只有 NEW 和 PROCESSING 可以取消
        if (!currentStatus.canTransitionTo(TicketStatus.CANCELLED)) {
            throw new ServiceException("当前状态不允许取消");
        }
        // 校验当前用户是创建人或管理员
        Long currentUserId = SecurityUtils.getUserId();
        if (!Objects.equals(ticket.getCreatorId(), currentUserId) && !SecurityUtils.isAdmin()) {
            throw new ServiceException("您不是该工单的创建人，无法取消");
        }
        // 取消原因必填
        if (StringUtils.isBlank(dto.getComment())) {
            throw new ServiceException("取消原因不能为空");
        }

        TicketStatus fromStatus = currentStatus;
        ticket.setStatus(TicketStatus.CANCELLED.name());
        ticket.setUpdateBy(SecurityUtils.getUsername());
        ticket.setUpdateTime(new Date());
        ticketMapper.updateTicket(ticket);

        saveOperationLog(ticketId, TicketOperationType.CANCEL, fromStatus,
                TicketStatus.CANCELLED, dto.getComment());
    }

    /**
     * 生成工单编号，格式 TK + yyyyMMdd + 4 位序号
     */
    private synchronized String generateTicketNo() {
        String todayPrefix = TICKET_NO_PREFIX + new SimpleDateFormat("yyyyMMdd").format(new Date());
        String maxNo = ticketMapper.selectMaxTicketNo(todayPrefix);
        if (StringUtils.isBlank(maxNo)) {
            return todayPrefix + "0001";
        }
        // 取后 4 位数字递增
        String seqPart = maxNo.substring(maxNo.length() - 4);
        int nextSeq = Integer.parseInt(seqPart) + 1;
        return todayPrefix + String.format("%04d", nextSeq);
    }

    /**
     * 在指定时间上增加分钟数。
     */
    private Date addMinutes(Date baseTime, Integer minutes) {
        if (minutes == null || minutes <= 0) {
            throw new ServiceException("SLA 策略时限无效");
        }
        return new Date(baseTime.getTime() + minutes * MILLIS_PER_MINUTE);
    }

    /**
     * 查询工单实体，不存在则抛异常
     */
    private Ticket getTicketOrThrow(Long ticketId) {
        Ticket ticket = ticketMapper.selectTicketEntityById(ticketId);
        if (ticket == null) {
            throw new ServiceException("工单不存在");
        }
        return ticket;
    }

    /**
     * 将数据库中的 String 状态转为枚举
     */
    private TicketStatus toTicketStatus(String status) {
        try {
            return TicketStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new ServiceException("未知的工单状态: " + status);
        }
    }

    /**
     * 写入操作日志
     */
    private void saveOperationLog(Long ticketId, TicketOperationType operationType,
                                  TicketStatus fromStatus, TicketStatus toStatus, String comment) {
        TicketOperationLog log = new TicketOperationLog();
        log.setTicketId(ticketId);
        log.setOperationType(operationType.name());
        log.setFromStatus(fromStatus != null ? fromStatus.name() : null);
        log.setToStatus(toStatus.name());
        log.setOperatorId(SecurityUtils.getUserId());
        log.setOperatorName(SecurityUtils.getUsername());
        log.setComment(comment);
        log.setOperateTime(new Date());
        ticketOperationLogMapper.insertLog(log);
    }
}
