package com.ruoyi.ticket.dto;
import java.io.Serial;

import java.io.Serializable;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 创建工单请求体
 *
 * @author ticket
 */
@Schema(description = "创建工单请求")
public class TicketCreateDTO implements Serializable {

    @Serial
private static final long serialVersionUID = 1L;

    @Schema(description = "工单标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "工单内容")
    private String content;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "优先级：LOW/MEDIUM/HIGH/URGENT，默认 MEDIUM")
    private String priority;

    @Schema(description = "自定义字段")
    private List<TicketCustomFieldInputDTO> customFields;

    @Schema(description = "待绑定的临时附件ID")
    private List<Long> attachmentIds;

    @Schema(description = "来源类型：MANUAL/AI_ESCALATION")
    private String sourceType;

    @Schema(description = "来源 AI 问答会话 ID")
    private Long aiSessionId;

    @Schema(description = "AI 问答摘要")
    private String aiSummary;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public List<TicketCustomFieldInputDTO> getCustomFields() { return customFields; }
    public void setCustomFields(List<TicketCustomFieldInputDTO> customFields) { this.customFields = customFields; }
    public List<Long> getAttachmentIds() { return attachmentIds; }
    public void setAttachmentIds(List<Long> attachmentIds) { this.attachmentIds = attachmentIds; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getAiSessionId() { return aiSessionId; }
    public void setAiSessionId(Long aiSessionId) { this.aiSessionId = aiSessionId; }
    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
}
