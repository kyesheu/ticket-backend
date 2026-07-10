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
import com.ruoyi.ticket.enums.TicketNotificationType;
import com.ruoyi.ticket.enums.TicketPriority;
import com.ruoyi.ticket.enums.TicketStatus;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.mapper.TicketOperationLogMapper;
import com.ruoyi.ticket.mapper.TicketSlaPolicyMapper;
import com.ruoyi.ticket.mapper.TicketWorkflowInstanceMapper;
import com.ruoyi.ticket.dto.TicketWorkflowTaskActionDTO;
import com.ruoyi.ticket.service.ITicketService;
import com.ruoyi.ticket.service.ITicketNotificationService;
import com.ruoyi.ticket.service.ITicketAccessPolicy;
import com.ruoyi.ticket.service.ITicketWorkflowEngine;
import com.ruoyi.ticket.service.ITicketCustomFieldService;
import com.ruoyi.ticket.service.ITicketAttachmentService;
import com.ruoyi.ticket.service.ITicketSearchEventService;
import com.ruoyi.ticket.enums.TicketAttachmentBusinessType;
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

    private static final String SOURCE_TYPE_MANUAL = "MANUAL";
    private static final String DISPATCH_MODE_MANUAL = "MANUAL";

    /** 工单列表权限字符 */
    private static final String TICKET_LIST_PERMISSION = "ticket:ticket:list";

    private static final String TICKET_QUERY_PERMISSION = "ticket:ticket:query";
    private static final String TICKET_ASSIGN_PERMISSION = "ticket:ticket:assign";
    private static final String TICKET_PROCESS_PERMISSION = "ticket:ticket:process";
    private static final String TICKET_CONFIRM_PERMISSION = "ticket:ticket:confirm";
    private static final String TICKET_CANCEL_PERMISSION = "ticket:ticket:cancel";

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private TicketOperationLogMapper ticketOperationLogMapper;

    @Autowired
    private TicketSlaPolicyMapper ticketSlaPolicyMapper;

    @Autowired
    private ITicketNotificationService ticketNotificationService;

    @Autowired
    private ITicketAccessPolicy ticketAccessPolicy;

    @Autowired
    private ITicketWorkflowEngine ticketWorkflowEngine;

    @Autowired
    private TicketWorkflowInstanceMapper ticketWorkflowInstanceMapper;

    @Autowired
    private ITicketCustomFieldService ticketCustomFieldService;

    @Autowired
    private ITicketAttachmentService ticketAttachmentService;

    @Autowired
    private ITicketSearchEventService ticketSearchEventService;

    @Override
    public List<TicketListVO> selectTicketList(TicketQueryDTO query) {
        query.setAccessScope(ticketAccessPolicy.resolveScope(TICKET_LIST_PERMISSION));
        return ticketMapper.selectTicketList(query);
    }

    @Override
    public List<TicketListVO> selectMyTodoTickets(TicketQueryDTO query) {
        query.setStatus(TicketStatus.PROCESSING.name());
        query.setAssigneeId(SecurityUtils.getUserId());
        query.setAccessScope(ticketAccessPolicy.resolveScope(TICKET_PROCESS_PERMISSION));
        return ticketMapper.selectTicketList(query);
    }

    @Override
    public TicketVO selectTicketById(Long ticketId) {
        ticketAccessPolicy.assertCanAccess(ticketId, TICKET_QUERY_PERMISSION);
        TicketVO vo = ticketMapper.selectTicketById(ticketId);
        if (vo == null) {
            throw new ServiceException("工单不存在");
        }
        vo.setCustomFields(ticketCustomFieldService.selectValueSnapshots(ticketId));
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
        ticket.setSourceType(StringUtils.isBlank(dto.getSourceType()) ? SOURCE_TYPE_MANUAL : dto.getSourceType());
        ticket.setAiSessionId(dto.getAiSessionId());
        ticket.setAiSummary(dto.getAiSummary());
        ticket.setDispatchMode(DISPATCH_MODE_MANUAL);
        ticket.setDispatchReason(null);
        ticket.setDelFlag("0");
        ticket.setCreateBy(SecurityUtils.getUsername());
        ticket.setCreateTime(createTime);
        ticket.setUpdateBy(SecurityUtils.getUsername());
        ticket.setUpdateTime(createTime);

        ticketMapper.insertTicket(ticket);

        // 写入创建日志
        saveOperationLog(ticket.getTicketId(), TicketOperationType.CREATE,
                null, TicketStatus.NEW, null);

        ticketCustomFieldService.validateAndSave(ticket.getTicketId(), ticket.getCategoryId(), dto.getCustomFields());

        ticketAttachmentService.bindAttachments(ticket.getTicketId(), TicketAttachmentBusinessType.TICKET,
                ticket.getTicketId(), dto.getAttachmentIds());

        ticketWorkflowEngine.startInstance(ticket);

        ticketSearchEventService.publishUpsert(ticket.getTicketId());

        return ticket.getTicketId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTicket(Long ticketId, TicketAssignDTO dto) {
        if (ticketWorkflowInstanceMapper.selectInstanceByTicketId(ticketId) != null) {
            TicketWorkflowTaskActionDTO action = new TicketWorkflowTaskActionDTO();
            action.setAssigneeId(dto.getAssigneeId()); action.setComment(dto.getComment());
            ticketWorkflowEngine.completeCurrentTask(ticketId, action);
            return;
        }
        Ticket ticket = getTicketOrThrow(ticketId, TICKET_ASSIGN_PERMISSION);
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
        notifyTicket(ticket, dto.getAssigneeId(), TicketNotificationType.ASSIGNED,
                "ASSIGNED:" + ticketId, "工单已分派", "您收到一条新工单");
        ticketSearchEventService.publishUpsert(ticketId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processTicket(Long ticketId, TicketProcessDTO dto) {
        if (ticketWorkflowInstanceMapper.selectInstanceByTicketId(ticketId) != null) {
            TicketWorkflowTaskActionDTO action = new TicketWorkflowTaskActionDTO(); action.setComment(dto.getComment());
            ticketWorkflowEngine.completeCurrentTask(ticketId, action);
            return;
        }
        Ticket ticket = getTicketOrThrow(ticketId, TICKET_PROCESS_PERMISSION);
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
        notifyTicket(ticket, ticket.getCreatorId(), TicketNotificationType.PROCESSED,
                "PROCESSED:" + ticketId, "工单处理完成", "工单等待您的确认");
        ticketSearchEventService.publishUpsert(ticketId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmTicket(Long ticketId, TicketConfirmDTO dto) {
        if (ticketWorkflowInstanceMapper.selectInstanceByTicketId(ticketId) != null) {
            TicketWorkflowTaskActionDTO action = new TicketWorkflowTaskActionDTO(); action.setComment(dto.getComment());
            ticketWorkflowEngine.completeCurrentTask(ticketId, action);
            return;
        }
        Ticket ticket = getTicketOrThrow(ticketId, TICKET_CONFIRM_PERMISSION);
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
        notifyTicket(ticket, ticket.getAssigneeId(), TicketNotificationType.CLOSED,
                "CLOSED:" + ticketId, "工单已关闭", "工单已由创建人确认关闭");
        ticketSearchEventService.publishUpsert(ticketId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTicket(Long ticketId, TicketCancelDTO dto) {
        if (ticketWorkflowInstanceMapper.selectInstanceByTicketId(ticketId) != null) {
            ticketWorkflowEngine.cancelInstance(ticketId, dto.getComment());
            return;
        }
        Ticket ticket = getTicketOrThrow(ticketId, TICKET_CANCEL_PERMISSION);
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
        Long recipientId = ticket.getAssigneeId() != null ? ticket.getAssigneeId() : ticket.getCreatorId();
        notifyTicket(ticket, recipientId, TicketNotificationType.CANCELLED,
                "CANCELLED:" + ticketId, "工单已取消", "工单已被取消");
        ticketSearchEventService.publishUpsert(ticketId);
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
    private Ticket getTicketOrThrow(Long ticketId, String permission) {
        ticketAccessPolicy.assertCanAccess(ticketId, permission);
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

    private void notifyTicket(Ticket ticket, Long recipientId, TicketNotificationType type,
                              String eventKey, String title, String content) {
        ticketNotificationService.createNotification(ticket.getTicketId(), recipientId,
                SecurityUtils.getUserId(), type, eventKey, title, content);
    }
}
