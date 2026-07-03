package com.ruoyi.ticket.mapper;

/**
 * 工单流程处理人查询 Mapper
 *
 * @author ticket
 */
public interface TicketWorkflowAssigneeMapper {

    Long selectDepartmentLeaderUserId(Long departmentId);

    int countEnabledRoleById(Long roleId);

    int countUserEnabledRole(@org.apache.ibatis.annotations.Param("userId") Long userId,
                             @org.apache.ibatis.annotations.Param("roleId") Long roleId);
}
