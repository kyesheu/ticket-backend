package com.ruoyi.ticket.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** 满意度评价查询参数。 */
public class TicketSatisfactionQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer score;
    private Date beginTime;
    private Date endTime;

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Date getBeginTime() { return beginTime; }
    public void setBeginTime(Date beginTime) { this.beginTime = beginTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
}
