package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;

/**
 * Python AI 服务健康状态。
 */
public class TicketAiHealthVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String status;
    private String contractVersion;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getContractVersion() { return contractVersion; }
    public void setContractVersion(String contractVersion) { this.contractVersion = contractVersion; }
}
