package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 工单处理建议与回复草稿。
 */
public class TicketAiAssistVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String suggestion;
    private String replyDraft;
    private List<TicketAiSourceVO> sources;

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public String getReplyDraft() { return replyDraft; }
    public void setReplyDraft(String replyDraft) { this.replyDraft = replyDraft; }
    public List<TicketAiSourceVO> getSources() { return sources; }
    public void setSources(List<TicketAiSourceVO> sources) { this.sources = sources; }
}
