package com.ruoyi.ticket.dto;

import java.io.Serial;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工单自定义字段输入。
 */
@Schema(description = "工单自定义字段输入")
public class TicketCustomFieldInputDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "字段键")
    private String fieldKey;

    @Schema(description = "字段值；多选类型传字符串数组")
    private Object value;

    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
}
