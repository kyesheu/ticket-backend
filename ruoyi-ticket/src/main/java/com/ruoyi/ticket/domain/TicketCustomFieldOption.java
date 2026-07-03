package com.ruoyi.ticket.domain;

import com.ruoyi.common.core.domain.BaseEntity;

import java.io.Serial;

/** 工单自定义字段选项实体。 */
public class TicketCustomFieldOption extends BaseEntity {
    @Serial private static final long serialVersionUID = 1L;
    private Long optionId;
    private Long fieldId;
    private String optionValue;
    private String optionLabel;
    private Integer sortOrder;
    private String status;

    public Long getOptionId() { return optionId; }
    public void setOptionId(Long optionId) { this.optionId = optionId; }
    public Long getFieldId() { return fieldId; }
    public void setFieldId(Long fieldId) { this.fieldId = fieldId; }
    public String getOptionValue() { return optionValue; }
    public void setOptionValue(String optionValue) { this.optionValue = optionValue; }
    public String getOptionLabel() { return optionLabel; }
    public void setOptionLabel(String optionLabel) { this.optionLabel = optionLabel; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
