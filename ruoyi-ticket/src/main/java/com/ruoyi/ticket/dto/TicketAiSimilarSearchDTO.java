package com.ruoyi.ticket.dto;

import java.io.Serial;
import java.io.Serializable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 历史工单相似检索请求。
 */
public class TicketAiSimilarSearchDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String contractVersion = "v1";

    @NotBlank
    @Size(max = 2000)
    private String query;

    @NotNull
    @Min(1)
    @Max(20)
    private Integer topK;

    public String getContractVersion() { return contractVersion; }
    public void setContractVersion(String contractVersion) { this.contractVersion = contractVersion; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
}
