package com.ruoyi.ticket.dto;

import jakarta.validation.constraints.NotBlank;

import java.io.Serial;
import java.io.Serializable;

/** 自定义字段选项请求参数。 */
public class TicketCustomFieldOptionDTO implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long optionId;
    @NotBlank(message = "选项值不能为空") private String optionValue;
    @NotBlank(message = "选项标签不能为空") private String optionLabel;
    private Integer sortOrder;
    private String status;

    public Long getOptionId() { return optionId; }
    public void setOptionId(Long optionId) { this.optionId = optionId; }
    public String getOptionValue() { return optionValue; }
    public void setOptionValue(String optionValue) { this.optionValue = optionValue; }
    public String getOptionLabel() { return optionLabel; }
    public void setOptionLabel(String optionLabel) { this.optionLabel = optionLabel; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
