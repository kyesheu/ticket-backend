package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/** 满意度统计响应对象。 */
public class TicketSatisfactionStatisticsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long totalCount;
    private BigDecimal averageScore;
    private Long oneStarCount;
    private Long twoStarCount;
    private Long threeStarCount;
    private Long fourStarCount;
    private Long fiveStarCount;

    public Long getTotalCount() { return totalCount; }
    public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
    public BigDecimal getAverageScore() { return averageScore; }
    public void setAverageScore(BigDecimal averageScore) { this.averageScore = averageScore; }
    public Long getOneStarCount() { return oneStarCount; }
    public void setOneStarCount(Long oneStarCount) { this.oneStarCount = oneStarCount; }
    public Long getTwoStarCount() { return twoStarCount; }
    public void setTwoStarCount(Long twoStarCount) { this.twoStarCount = twoStarCount; }
    public Long getThreeStarCount() { return threeStarCount; }
    public void setThreeStarCount(Long threeStarCount) { this.threeStarCount = threeStarCount; }
    public Long getFourStarCount() { return fourStarCount; }
    public void setFourStarCount(Long fourStarCount) { this.fourStarCount = fourStarCount; }
    public Long getFiveStarCount() { return fiveStarCount; }
    public void setFiveStarCount(Long fiveStarCount) { this.fiveStarCount = fiveStarCount; }
}
