package com.ruoyi.ticket.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 已关闭工单知识同步请求。
 */
public class TicketAiClosedTicketSyncDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String contractVersion = "v1";
    @NotNull
    @Positive
    private Long ticketId;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 100)
    private String category;

    @NotBlank
    @Size(max = 10000)
    private String description;

    @NotBlank
    @Size(max = 10000)
    private String solution;

    @NotBlank
    @Pattern(regexp = "CLOSED")
    private String status;

    @NotNull
    @Size(max = 20)
    private List<@NotBlank @Size(max = 50) String> tags;

    @NotNull
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T.*(?:Z|[+-]\\d{2}:\\d{2})$")
    private String createdTime;

    @NotNull
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T.*(?:Z|[+-]\\d{2}:\\d{2})$")
    private String closedTime;

    @NotNull
    @Positive
    private Long sourceGeneration;

    public String getContractVersion() { return contractVersion; }
    public void setContractVersion(String contractVersion) { this.contractVersion = contractVersion; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getCreatedTime() { return createdTime; }
    public void setCreatedTime(String createdTime) { this.createdTime = createdTime; }
    public String getClosedTime() { return closedTime; }
    public void setClosedTime(String closedTime) { this.closedTime = closedTime; }
    public Long getSourceGeneration() { return sourceGeneration; }
    public void setSourceGeneration(Long sourceGeneration) { this.sourceGeneration = sourceGeneration; }
}
