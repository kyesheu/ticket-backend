package com.ruoyi.ticket.dto;
import java.io.Serial;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.ticket.model.TicketAccessScope;

/**
 * 工单列表查询请求体
 *
 * @author ticket
 */
@Schema(description = "工单查询条件")
public class TicketQueryDTO implements Serializable {

    @Serial
private static final long serialVersionUID = 1L;

    @Schema(description = "状态筛选：NEW/PROCESSING/WAIT_CONFIRM/CLOSED/CANCELLED")
    private String status;

    @Schema(description = "优先级筛选：LOW/MEDIUM/HIGH/URGENT")
    private String priority;

    @Schema(description = "分类ID筛选")
    private Long categoryId;

    @Schema(description = "关键词（模糊匹配标题和内容）")
    private String keyword;

    @Schema(description = "开始时间")
    private Date beginTime;

    @Schema(description = "结束时间")
    private Date endTime;

    @Schema(description = "响应是否超时：0否 1是")
    private String responseOverdue;

    @Schema(description = "解决是否超时：0否 1是")
    private String resolveOverdue;

    /** 仅允许 Service 写入的处理人筛选。 */
    @JsonIgnore
    private Long assigneeId;

    @Schema(description = "页码")
    private Integer pageNum;

    @Schema(description = "每页条数")
    private Integer pageSize;

    private Map<String, Object> params;

    /** 仅允许 Service 写入的工单访问范围。 */
    @JsonIgnore
    private TicketAccessScope accessScope;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Date getBeginTime() { return beginTime; }
    public void setBeginTime(Date beginTime) { this.beginTime = beginTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public String getResponseOverdue() { return responseOverdue; }
    public void setResponseOverdue(String responseOverdue) { this.responseOverdue = responseOverdue; }
    public String getResolveOverdue() { return resolveOverdue; }
    public void setResolveOverdue(String resolveOverdue) { this.resolveOverdue = resolveOverdue; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public Map<String, Object> getParams() {
        if (params == null) { params = new HashMap<>(); }
        return params;
    }
    public void setParams(Map<String, Object> params) { this.params = params; }
    public TicketAccessScope getAccessScope() { return accessScope; }
    public void setAccessScope(TicketAccessScope accessScope) { this.accessScope = accessScope; }
}
