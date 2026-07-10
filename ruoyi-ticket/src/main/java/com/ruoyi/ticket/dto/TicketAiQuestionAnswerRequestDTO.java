package com.ruoyi.ticket.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * Python AI 问答契约请求。
 */
public class TicketAiQuestionAnswerRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String contractVersion = "v1";
    private String question;
    private String category;
    private Integer topK = 5;

    public String getContractVersion() { return contractVersion; }
    public void setContractVersion(String contractVersion) { this.contractVersion = contractVersion; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
}
