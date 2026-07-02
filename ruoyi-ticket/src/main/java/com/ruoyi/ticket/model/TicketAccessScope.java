package com.ruoyi.ticket.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 工单访问范围。
 *
 * @author ticket
 */
public class TicketAccessScope implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final Long deptId;
    private final boolean fullAccess;
    private final boolean includeDept;
    private final boolean includeDeptChildren;
    private final List<Long> customRoleIds;

    public TicketAccessScope(Long userId, Long deptId, boolean fullAccess, boolean includeDept,
                             boolean includeDeptChildren, List<Long> customRoleIds) {
        this.userId = userId;
        this.deptId = deptId;
        this.fullAccess = fullAccess;
        this.includeDept = includeDept;
        this.includeDeptChildren = includeDeptChildren;
        this.customRoleIds = customRoleIds == null
                ? Collections.emptyList() : List.copyOf(customRoleIds);
    }

    public Long getUserId() {
        return userId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public boolean isFullAccess() {
        return fullAccess;
    }

    public boolean isIncludeDept() {
        return includeDept;
    }

    public boolean isIncludeDeptChildren() {
        return includeDeptChildren;
    }

    public List<Long> getCustomRoleIds() {
        return customRoleIds;
    }
}
