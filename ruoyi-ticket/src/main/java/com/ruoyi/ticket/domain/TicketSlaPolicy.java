package com.ruoyi.ticket.domain;

import com.ruoyi.common.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 工单 SLA 策略实体
 *
 * @author ticket
 */
public class TicketSlaPolicy extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 策略 ID */
    private Long policyId;

    /** 工单优先级 */
    private String priority;

    /** 首次响应时限，单位为分钟 */
    private Integer responseMinutes;

    /** 解决时限，单位为分钟 */
    private Integer resolveMinutes;

    /** 状态：0 启用，1 停用 */
    private String status;

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

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
}
