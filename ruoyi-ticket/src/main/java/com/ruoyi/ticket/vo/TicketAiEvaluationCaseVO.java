package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 固定评测用例。
 */
public class TicketAiEvaluationCaseVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String caseId;
    private String title;
    private String expectedCapability;

    public TicketAiEvaluationCaseVO() {}

    public TicketAiEvaluationCaseVO(String caseId, String title, String expectedCapability) {
        this.caseId = caseId;
        this.title = title;
        this.expectedCapability = expectedCapability;
    }

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getExpectedCapability() { return expectedCapability; }
    public void setExpectedCapability(String expectedCapability) { this.expectedCapability = expectedCapability; }
}
