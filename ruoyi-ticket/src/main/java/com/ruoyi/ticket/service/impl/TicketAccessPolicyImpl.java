package com.ruoyi.ticket.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.model.TicketAccessScope;
import com.ruoyi.ticket.service.ITicketAccessPolicy;

/**
 * 工单访问策略实现。
 *
 * @author ticket
 */
@Service
public class TicketAccessPolicyImpl implements ITicketAccessPolicy {

    @Autowired
    private TicketMapper ticketMapper;

    @Override
    public TicketAccessScope resolveScope(String permission) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null || loginUser.getUser() == null) {
            throw new ServiceException("用户登录状态无效");
        }

        SysUser user = loginUser.getUser();
        if (user.isAdmin()) {
            return fullAccessScope(user);
        }

        boolean includeDept = false;
        boolean includeDeptChildren = false;
        List<Long> customRoleIds = new ArrayList<>();
        List<SysRole> roles = user.getRoles() == null ? Collections.emptyList() : user.getRoles();
        for (SysRole role : roles) {
            if (!isEffectiveRole(role, permission)) {
                continue;
            }
            String dataScope = role.getDataScope();
            if (Constants.Dept.DATA_SCOPE_ALL.equals(dataScope)) {
                return fullAccessScope(user);
            }
            if (Constants.Dept.DATA_SCOPE_CUSTOM.equals(dataScope)) {
                customRoleIds.add(role.getRoleId());
            } else if (Constants.Dept.DATA_SCOPE_DEPT.equals(dataScope)) {
                includeDept = true;
            } else if (Constants.Dept.DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope)) {
                includeDeptChildren = true;
            }
        }
        return new TicketAccessScope(user.getUserId(), user.getDeptId(), false, includeDept,
                includeDeptChildren, customRoleIds);
    }

    @Override
    public void assertCanAccess(Long ticketId, String permission) {
        if (ticketId == null || ticketMapper.countAccessibleTicket(ticketId, resolveScope(permission)) == 0) {
            throw new ServiceException("工单不存在");
        }
    }

    @Override
    public List<Long> filterAccessibleTicketIds(List<Long> ticketIds, String permission) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return Collections.emptyList();
        }
        return ticketMapper.selectAccessibleTicketIds(ticketIds, resolveScope(permission));
    }

    private boolean isEffectiveRole(SysRole role, String permission) {
        if (role == null || !UserConstants.ROLE_NORMAL.equals(role.getStatus())) {
            return false;
        }
        Set<String> permissions = role.getPermissions();
        return StringUtils.isEmpty(permission) || permissions != null
                && (permissions.contains(Constants.ALL_PERMISSION) || permissions.contains(permission));
    }

    private TicketAccessScope fullAccessScope(SysUser user) {
        return new TicketAccessScope(user.getUserId(), user.getDeptId(), true, false,
                false, Collections.emptyList());
    }
}
