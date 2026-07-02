package com.ruoyi.ticket.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单 SLA 策略请求参数
 *
 * @author ticket
 */
public class TicketSlaPolicyDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 工单优先级 */
    @NotBlank(message = "优先级不能为空")
    @Pattern(regexp = "LOW|MEDIUM|HIGH|URGENT", message = "优先级无效")
    private String priority;

    /** 首次响应时限，单位为分钟 */
    @NotNull(message = "首次响应时限不能为空")
    @Min(value = 1, message = "首次响应时限必须大于 0")
    private Integer responseMinutes;

    /** 解决时限，单位为分钟 */
    @NotNull(message = "解决时限不能为空")
    @Min(value = 1, message = "解决时限必须大于 0")
    private Integer resolveMinutes;

    /** 状态：0 启用，1 停用 */
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "0|1", message = "状态无效")
    private String status;

    /** 备注 */
    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Integer getResponseMinutes() {
        return responseMinutes;
    }

    public void setResponseMinutes(Integer responseMinutes) {
        this.responseMinutes = responseMinutes;
    }

    public Integer getResolveMinutes() {
        return resolveMinutes;
    }

    public void setResolveMinutes(Integer resolveMinutes) {
        this.resolveMinutes = resolveMinutes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
