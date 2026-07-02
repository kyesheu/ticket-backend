package com.ruoyi.ticket.mapper;

import com.ruoyi.ticket.domain.TicketSlaPolicy;

import java.util.List;

/**
 * 工单 SLA 策略 Mapper 接口
 *
 * @author ticket
 */
public interface TicketSlaPolicyMapper {

    /**
     * 查询全部 SLA 策略
     *
     * @return SLA 策略列表
     */
    List<TicketSlaPolicy> selectPolicyList();

    /**
     * 根据策略 ID 查询 SLA 策略
     *
     * @param policyId 策略 ID
     * @return SLA 策略，不存在时返回 null
     */
    TicketSlaPolicy selectPolicyById(Long policyId);

    /**
     * 根据优先级查询 SLA 策略，包含停用策略
     *
     * @param priority 工单优先级
     * @return SLA 策略，不存在时返回 null
     */
    TicketSlaPolicy selectPolicyByPriority(String priority);

    /**
     * 根据优先级查询启用的 SLA 策略
     *
     * @param priority 工单优先级
     * @return 启用的 SLA 策略，不存在时返回 null
     */
    TicketSlaPolicy selectEnabledPolicyByPriority(String priority);

    /**
     * 新增 SLA 策略
     *
     * @param policy SLA 策略
     * @return 影响行数
     */
    int insertPolicy(TicketSlaPolicy policy);

    /**
     * 修改 SLA 策略
     *
     * @param policy SLA 策略
     * @return 影响行数
     */
    int updatePolicy(TicketSlaPolicy policy);
}
