package com.ruoyi.ticket.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 分诊请求。
 */
public class TicketAiTriageRequestDTO implements Serializable {

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
    @Size(max = 10000)
    private String description;

    private Long currentCategoryId;

    @Size(max = 100)
    private String currentCategoryName;

    @Size(max = 20)
    private String currentPriority;

    @NotNull
    private LocalDateTime ticketUpdatedAt;

    @Valid
    @NotEmpty
    private List<CategoryCandidate> categoryCandidates;

    @NotEmpty
    private List<@NotBlank @Size(max = 20) String> priorityCandidates;

    @Valid
    @NotEmpty
    private List<AssigneeCandidate> assigneeCandidates;

    @NotNull
    @Min(1)
    @Max(20)
    private Integer topK;

    public String getContractVersion() { return contractVersion; }
    public void setContractVersion(String contractVersion) { this.contractVersion = contractVersion; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getCurrentCategoryId() { return currentCategoryId; }
    public void setCurrentCategoryId(Long currentCategoryId) { this.currentCategoryId = currentCategoryId; }
    public String getCurrentCategoryName() { return currentCategoryName; }
    public void setCurrentCategoryName(String currentCategoryName) { this.currentCategoryName = currentCategoryName; }
    public String getCurrentPriority() { return currentPriority; }
    public void setCurrentPriority(String currentPriority) { this.currentPriority = currentPriority; }
    public LocalDateTime getTicketUpdatedAt() { return ticketUpdatedAt; }
    public void setTicketUpdatedAt(LocalDateTime ticketUpdatedAt) { this.ticketUpdatedAt = ticketUpdatedAt; }
    public List<CategoryCandidate> getCategoryCandidates() { return categoryCandidates; }
    public void setCategoryCandidates(List<CategoryCandidate> categoryCandidates) { this.categoryCandidates = categoryCandidates; }
    public List<String> getPriorityCandidates() { return priorityCandidates; }
    public void setPriorityCandidates(List<String> priorityCandidates) { this.priorityCandidates = priorityCandidates; }
    public List<AssigneeCandidate> getAssigneeCandidates() { return assigneeCandidates; }
    public void setAssigneeCandidates(List<AssigneeCandidate> assigneeCandidates) { this.assigneeCandidates = assigneeCandidates; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }

    /**
     * 分类候选。
     */
    public static class CategoryCandidate implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @NotNull
        @Positive
        private Long categoryId;

        @NotBlank
        @Size(max = 100)
        private String categoryName;

        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    }

    /**
     * 处理人候选。
     */
    public static class AssigneeCandidate implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @NotNull
        @Positive
        private Long userId;

        @NotBlank
        @Size(max = 30)
        private String userName;

        @Size(max = 30)
        private String nickName;

        private Long deptId;

        @Size(max = 30)
        private String deptName;

        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private Double workloadScore;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getNickName() { return nickName; }
        public void setNickName(String nickName) { this.nickName = nickName; }
        public Long getDeptId() { return deptId; }
        public void setDeptId(Long deptId) { this.deptId = deptId; }
        public String getDeptName() { return deptName; }
        public void setDeptName(String deptName) { this.deptName = deptName; }
        public Double getWorkloadScore() { return workloadScore; }
        public void setWorkloadScore(Double workloadScore) { this.workloadScore = workloadScore; }
    }
}
