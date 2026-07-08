package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * AI 反馈统计。
 */
public class TicketAiFeedbackStatisticsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long totalCount;
    private Long usefulCount;
    private Long notUsefulCount;
    private Long adoptedCount;
    private BigDecimal usefulRate;
    private BigDecimal adoptedRate;

    public Long getTotalCount() { return totalCount; }
    public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
    public Long getUsefulCount() { return usefulCount; }
    public void setUsefulCount(Long usefulCount) { this.usefulCount = usefulCount; }
    public Long getNotUsefulCount() { return notUsefulCount; }
    public void setNotUsefulCount(Long notUsefulCount) { this.notUsefulCount = notUsefulCount; }
    public Long getAdoptedCount() { return adoptedCount; }
    public void setAdoptedCount(Long adoptedCount) { this.adoptedCount = adoptedCount; }
    public BigDecimal getUsefulRate() { return usefulRate; }
    public void setUsefulRate(BigDecimal usefulRate) { this.usefulRate = usefulRate; }
    public BigDecimal getAdoptedRate() { return adoptedRate; }
    public void setAdoptedRate(BigDecimal adoptedRate) { this.adoptedRate = adoptedRate; }
}
