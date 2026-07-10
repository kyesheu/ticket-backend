package com.ruoyi.ticket.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import java.io.Serial;

/**
 * AI 自动分派规则。
 */
public class TicketDispatchRule extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long ruleId;
    private Long categoryId;
    private Long handlerId;
    private String priority;
    private String enabled;

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(Long handlerId) {
        this.handlerId = handlerId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }
}
