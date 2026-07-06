package com.ruoyi.ticket.exception;

import java.io.Serial;

/**
 * Python AI 服务调用异常。
 */
public class TicketAiServiceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TicketAiServiceException(String message) {
        super(message);
    }

    public TicketAiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
