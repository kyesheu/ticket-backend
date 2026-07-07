package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 知识文档分页列表。
 */
public class TicketAiDocumentListVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<TicketAiDocumentDetailVO> rows;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;

    public List<TicketAiDocumentDetailVO> getRows() { return rows; }
    public void setRows(List<TicketAiDocumentDetailVO> rows) { this.rows = rows; }
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
