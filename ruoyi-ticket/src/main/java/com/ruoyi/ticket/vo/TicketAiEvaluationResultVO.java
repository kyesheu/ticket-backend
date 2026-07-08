package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * AI 评测运行结果。
 */
public class TicketAiEvaluationResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String runId;
    private String status;
    private Integer caseCount;
    private Date runTime;
    private String summary;

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getCaseCount() { return caseCount; }
    public void setCaseCount(Integer caseCount) { this.caseCount = caseCount; }
    public Date getRunTime() { return runTime; }
    public void setRunTime(Date runTime) { this.runTime = runTime; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
