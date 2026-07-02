package com.ruoyi.ticket.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import com.ruoyi.ticket.domain.TicketCategory;
import com.ruoyi.ticket.domain.TicketComment;
import com.ruoyi.ticket.domain.TicketOperationLog;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工单接口文档响应模型
 *
 * @author ticket
 */
public final class TicketApiResponseVO {

    private TicketApiResponseVO() {
    }

    /**
     * 通用操作结果
     */
    @Schema(description = "通用操作结果")
    public static class OperationResult implements Serializable {

        @Serial
private static final long serialVersionUID = 1L;

        @Schema(description = "状态码")
        private Integer code;

        @Schema(description = "返回消息")
        private String msg;

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }
    }

    /**
     * 工单ID响应
     */
    @Schema(description = "工单ID响应")
    public static class TicketIdResult extends OperationResult {

        @Serial
private static final long serialVersionUID = 1L;

        @Schema(description = "工单ID")
        private Long data;

        public Long getData() {
            return data;
        }

        public void setData(Long data) {
            this.data = data;
        }
    }

    /**
     * 工单分页响应
     */
    @Schema(description = "工单分页响应")
    public static class TicketPageResult implements Serializable {

        @Serial
private static final long serialVersionUID = 1L;

        @Schema(description = "总记录数")
        private Long total;

        @Schema(description = "列表数据")
        private List<TicketListVO> rows;

        @Schema(description = "状态码")
        private Integer code;

        @Schema(description = "返回消息")
        private String msg;

        public Long getTotal() {
            return total;
        }

        public void setTotal(Long total) {
            this.total = total;
        }

        public List<TicketListVO> getRows() {
            return rows;
        }

        public void setRows(List<TicketListVO> rows) {
            this.rows = rows;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }
    }

    /**
     * 工单详情响应
     */
    @Schema(description = "工单详情响应")
    public static class TicketDetailResult extends OperationResult {

        @Serial
private static final long serialVersionUID = 1L;

        @Schema(description = "返回数据")
        private TicketVO data;

        public TicketVO getData() {
            return data;
        }

        public void setData(TicketVO data) {
            this.data = data;
        }
    }

    /**
     * 工单分类响应
     */
    @Schema(description = "工单分类响应")
    public static class TicketCategoryResult extends OperationResult {

        @Serial
private static final long serialVersionUID = 1L;

        @Schema(description = "返回数据")
        private TicketCategory data;

        public TicketCategory getData() {
            return data;
        }

        public void setData(TicketCategory data) {
            this.data = data;
        }
    }

    /**
     * 工单分类列表响应
     */
    @Schema(description = "工单分类列表响应")
    public static class TicketCategoryListResult extends OperationResult {

        @Serial
private static final long serialVersionUID = 1L;

        @Schema(description = "返回数据")
        private List<TicketCategory> data;

        public List<TicketCategory> getData() {
            return data;
        }

        public void setData(List<TicketCategory> data) {
            this.data = data;
        }
    }

    /**
     * 工单分类树响应
     */
    @Schema(description = "工单分类树响应")
    public static class TicketCategoryTreeResult extends OperationResult {

        @Serial
private static final long serialVersionUID = 1L;

        @Schema(description = "返回数据")
        private List<TicketCategoryTreeVO> data;

        public List<TicketCategoryTreeVO> getData() {
            return data;
        }

        public void setData(List<TicketCategoryTreeVO> data) {
            this.data = data;
        }
    }

    /**
     * 工单评论列表响应
     */
    @Schema(description = "工单评论列表响应")
    public static class TicketCommentListResult extends OperationResult {

        @Serial
private static final long serialVersionUID = 1L;

        @Schema(description = "返回数据")
        private List<TicketComment> data;

        public List<TicketComment> getData() {
            return data;
        }

        public void setData(List<TicketComment> data) {
            this.data = data;
        }
    }

    /**
     * 工单操作日志列表响应
     */
    @Schema(description = "工单操作日志列表响应")
    public static class TicketOperationLogListResult extends OperationResult {

        @Serial
private static final long serialVersionUID = 1L;

        @Schema(description = "返回数据")
        private List<TicketOperationLog> data;

        public List<TicketOperationLog> getData() {
            return data;
        }

        public void setData(List<TicketOperationLog> data) {
            this.data = data;
        }
    }
}
