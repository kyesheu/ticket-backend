package com.ruoyi.ticket.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单流程任务动作参数
 *
 * @author ticket
 */
public class TicketWorkflowTaskActionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long assigneeId;
    private String comment;

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
