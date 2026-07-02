package com.ruoyi.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分派工单请求体
 *
 * @author ticket
 */
@Schema(description = "分派工单请求")
public class TicketAssignDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "指派人用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long assigneeId;

    @Schema(description = "分派备注")
    private String comment;

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
