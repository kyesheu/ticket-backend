package com.ruoyi.ticket.domain;

import com.ruoyi.common.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;

/** 工单自定义字段定义实体。 */
public class TicketCustomFieldDefinition extends BaseEntity {
    @Serial private static final long serialVersionUID = 1L;
    private Long fieldId;
    private Long categoryId;
    private String fieldKey;
    private String fieldName;
    private String fieldType;
    private String requiredFlag;
    private String defaultValue;
    private Integer maxLength;
    private BigDecimal minNumber;
    private BigDecimal maxNumber;
    private Integer sortOrder;
    private String status;

    public Long getFieldId() { return fieldId; }
    public void setFieldId(Long fieldId) { this.fieldId = fieldId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }
    public String getRequiredFlag() { return requiredFlag; }
    public void setRequiredFlag(String requiredFlag) { this.requiredFlag = requiredFlag; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public Integer getMaxLength() { return maxLength; }
    public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }
    public BigDecimal getMinNumber() { return minNumber; }
    public void setMinNumber(BigDecimal minNumber) { this.minNumber = minNumber; }
    public BigDecimal getMaxNumber() { return maxNumber; }
    public void setMaxNumber(BigDecimal maxNumber) { this.maxNumber = maxNumber; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
