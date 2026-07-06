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
    private Boolean elasticsearchAvailable;
    private Boolean embeddingConfigured;
    private Boolean llmConfigured;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getContractVersion() { return contractVersion; }
    public void setContractVersion(String contractVersion) { this.contractVersion = contractVersion; }
    public Boolean getElasticsearchAvailable() { return elasticsearchAvailable; }
    public void setElasticsearchAvailable(Boolean elasticsearchAvailable) {
        this.elasticsearchAvailable = elasticsearchAvailable;
    }
    public Boolean getEmbeddingConfigured() { return embeddingConfigured; }
    public void setEmbeddingConfigured(Boolean embeddingConfigured) { this.embeddingConfigured = embeddingConfigured; }
    public Boolean getLlmConfigured() { return llmConfigured; }
    public void setLlmConfigured(Boolean llmConfigured) { this.llmConfigured = llmConfigured; }
}
