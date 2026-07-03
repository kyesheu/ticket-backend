package com.ruoyi.ticket.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** 工单自定义字段值快照实体。 */
public class TicketCustomFieldValue implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long valueId;
    private Long ticketId;
    private Long fieldId;
    private String fieldKeySnapshot;
    private String fieldNameSnapshot;
    private String fieldTypeSnapshot;
    private String normalizedValue;
    private String displayValueSnapshot;
    private Integer sortOrderSnapshot;
    private Date createTime;

    public Long getValueId() { return valueId; }
    public void setValueId(Long valueId) { this.valueId = valueId; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public Long getFieldId() { return fieldId; }
    public void setFieldId(Long fieldId) { this.fieldId = fieldId; }
    public String getFieldKeySnapshot() { return fieldKeySnapshot; }
    public void setFieldKeySnapshot(String fieldKeySnapshot) { this.fieldKeySnapshot = fieldKeySnapshot; }
    public String getFieldNameSnapshot() { return fieldNameSnapshot; }
    public void setFieldNameSnapshot(String fieldNameSnapshot) { this.fieldNameSnapshot = fieldNameSnapshot; }
    public String getFieldTypeSnapshot() { return fieldTypeSnapshot; }
    public void setFieldTypeSnapshot(String fieldTypeSnapshot) { this.fieldTypeSnapshot = fieldTypeSnapshot; }
    public String getNormalizedValue() { return normalizedValue; }
    public void setNormalizedValue(String normalizedValue) { this.normalizedValue = normalizedValue; }
    public String getDisplayValueSnapshot() { return displayValueSnapshot; }
    public void setDisplayValueSnapshot(String displayValueSnapshot) { this.displayValueSnapshot = displayValueSnapshot; }
    public Integer getSortOrderSnapshot() { return sortOrderSnapshot; }
    public void setSortOrderSnapshot(Integer sortOrderSnapshot) { this.sortOrderSnapshot = sortOrderSnapshot; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
