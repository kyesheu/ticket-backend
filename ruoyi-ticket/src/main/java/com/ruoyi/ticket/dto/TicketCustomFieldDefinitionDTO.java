package com.ruoyi.ticket.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/** 自定义字段定义请求参数。 */
public class TicketCustomFieldDefinitionDTO implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    @NotNull(message = "分类ID不能为空") private Long categoryId;
    @NotBlank(message = "字段key不能为空") private String fieldKey;
    @NotBlank(message = "字段名称不能为空") private String fieldName;
    @NotBlank(message = "字段类型不能为空") private String fieldType;
    private String requiredFlag;
    private String defaultValue;
    private Integer maxLength;
    private BigDecimal minNumber;
    private BigDecimal maxNumber;
    private Integer sortOrder;
    private String status;
    private String remark;
    @Valid private List<TicketCustomFieldOptionDTO> options;

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
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<TicketCustomFieldOptionDTO> getOptions() { return options; }
    public void setOptions(List<TicketCustomFieldOptionDTO> options) { this.options = options; }
}
