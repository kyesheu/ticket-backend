package com.ruoyi.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单分类流程绑定请求参数
 *
 * @author ticket
 */
public class TicketWorkflowBindDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "分类 ID 不能为空")
    private Long categoryId;

    @NotBlank(message = "流程标识不能为空")
    private String workflowKey;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getWorkflowKey() { return workflowKey; }
    public void setWorkflowKey(String workflowKey) { this.workflowKey = workflowKey; }
}
