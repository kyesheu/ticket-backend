package com.ruoyi.ticket.service;

import com.ruoyi.ticket.model.TicketAccessScope;
import java.util.List;

/**
 * 工单访问策略接口。
 *
 * @author ticket
 */
public interface ITicketAccessPolicy {

    /**
     * 根据当前登录用户和权限字符解析访问范围。
     *
     * @param permission 权限字符
     * @return 服务端可信的访问范围
     */
    TicketAccessScope resolveScope(String permission);

    /**
     * 校验当前用户能否访问指定工单。
     *
     * @param ticketId 工单 ID
     * @param permission 权限字符
     */
    void assertCanAccess(Long ticketId, String permission);

    /** 批量筛选当前用户可访问的工单 ID。 */
    List<Long> filterAccessibleTicketIds(List<Long> ticketIds, String permission);
}
