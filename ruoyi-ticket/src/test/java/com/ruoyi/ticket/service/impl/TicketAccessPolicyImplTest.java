package com.ruoyi.ticket.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.ticket.mapper.TicketMapper;
import com.ruoyi.ticket.model.TicketAccessScope;

/**
 * 工单访问策略单元测试。
 *
 * @author ticket
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工单访问策略单元测试")
class TicketAccessPolicyImplTest {

    private static final String LIST_PERMISSION = "ticket:ticket:list";

    @Mock
    private TicketMapper ticketMapper;

    @InjectMocks
    private TicketAccessPolicyImpl accessPolicy;

    private MockedStatic<SecurityUtils> securityUtilsMock;
    private SysUser user;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        user = new SysUser();
        user.setUserId(10L);
        user.setDeptId(100L);
        LoginUser loginUser = new LoginUser();
        loginUser.setUser(user);
        securityUtilsMock.when(SecurityUtils::getLoginUser).thenReturn(loginUser);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    @DisplayName("超级管理员应拥有全部数据权限")
    void adminShouldHaveFullAccess() {
        user.setUserId(1L);
        securityUtilsMock.when(() -> SecurityUtils.isAdmin(1L)).thenReturn(true);

        TicketAccessScope scope = accessPolicy.resolveScope(LIST_PERMISSION);

        assertThat(scope.isFullAccess()).isTrue();
    }

    @Test
    @DisplayName("任一角色拥有全部范围时应直接放行")
    void allScopeRoleShouldHaveFullAccess() {
        user.setRoles(List.of(role(1L, Constants.Dept.DATA_SCOPE_DEPT, "0", LIST_PERMISSION),
                role(2L, Constants.Dept.DATA_SCOPE_ALL, "0", LIST_PERMISSION)));

        assertThat(accessPolicy.resolveScope(LIST_PERMISSION).isFullAccess()).isTrue();
    }

    @Test
    @DisplayName("多角色应合并本部门、下级部门和自定义部门范围")
    void multipleRolesShouldMergeScopes() {
        user.setRoles(List.of(role(1L, Constants.Dept.DATA_SCOPE_DEPT, "0", LIST_PERMISSION),
                role(2L, Constants.Dept.DATA_SCOPE_DEPT_AND_CHILD, "0", LIST_PERMISSION),
                role(3L, Constants.Dept.DATA_SCOPE_CUSTOM, "0", LIST_PERMISSION)));

        TicketAccessScope scope = accessPolicy.resolveScope(LIST_PERMISSION);

        assertThat(scope.isIncludeDept()).isTrue();
        assertThat(scope.isIncludeDeptChildren()).isTrue();
        assertThat(scope.getCustomRoleIds()).containsExactly(3L);
    }

    @Test
    @DisplayName("仅本人、停用角色和无当前权限角色不应扩大部门范围")
    void ineffectiveRolesShouldNotExpandScope() {
        user.setRoles(List.of(role(1L, Constants.Dept.DATA_SCOPE_SELF, "0", LIST_PERMISSION),
                role(2L, Constants.Dept.DATA_SCOPE_DEPT, "1", LIST_PERMISSION),
                role(3L, Constants.Dept.DATA_SCOPE_DEPT_AND_CHILD, "0", "ticket:ticket:add")));

        TicketAccessScope scope = accessPolicy.resolveScope(LIST_PERMISSION);

        assertThat(scope.isFullAccess()).isFalse();
        assertThat(scope.isIncludeDept()).isFalse();
        assertThat(scope.isIncludeDeptChildren()).isFalse();
        assertThat(scope.getUserId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("可访问工单应通过 Mapper 范围校验")
    void accessibleTicketShouldPass() {
        user.setRoles(List.of(role(1L, Constants.Dept.DATA_SCOPE_SELF, "0", LIST_PERMISSION)));
        when(ticketMapper.countAccessibleTicket(org.mockito.ArgumentMatchers.eq(8L),
                org.mockito.ArgumentMatchers.any(TicketAccessScope.class))).thenReturn(1);

        accessPolicy.assertCanAccess(8L, LIST_PERMISSION);

        verify(ticketMapper).countAccessibleTicket(org.mockito.ArgumentMatchers.eq(8L),
                org.mockito.ArgumentMatchers.argThat(scope -> scope.getUserId().equals(10L)));
    }

    @Test
    @DisplayName("范围外工单应按不存在拒绝")
    void inaccessibleTicketShouldBeRejected() {
        user.setRoles(List.of());
        when(ticketMapper.countAccessibleTicket(org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.any(TicketAccessScope.class))).thenReturn(0);

        assertThatThrownBy(() -> accessPolicy.assertCanAccess(9L, LIST_PERMISSION))
                .isInstanceOf(ServiceException.class)
                .hasMessage("工单不存在");
    }

    private SysRole role(Long roleId, String dataScope, String status, String permission) {
        SysRole role = new SysRole();
        role.setRoleId(roleId);
        role.setDataScope(dataScope);
        role.setStatus(status);
        role.setPermissions(Set.of(permission));
        return role;
    }
}
