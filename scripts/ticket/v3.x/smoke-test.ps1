# =============================================
# Ticket API v1.3 冒烟测试 (PowerShell)
# 用法: .\scripts\ticket\v3.x\smoke-test.ps1
# =============================================

$ErrorActionPreference = "Continue"
$BaseUrl = "http://localhost:8080"
$Pass = 0
$Fail = 0
$Skip = 0

function Get-ResponseText($Response) {
    if ($Response -is [string]) { return $Response }
    return ($Response | ConvertTo-Json -Depth 10 -Compress)
}

function Assert-Success($Desc, $Response) {
    $Text = Get-ResponseText $Response
    if ($Response.code -eq 200 -or $Text -match '"code":200') {
        Write-Host "  [PASS] $Desc" -ForegroundColor Green
        $script:Pass++
    } else {
        Write-Host "  [FAIL] $Desc" -ForegroundColor Red
        Write-Host "    Response: $($Text.Substring(0, [Math]::Min(200, $Text.Length)))"
        $script:Fail++
    }
}

function Write-Skip($Desc, $Reason) {
    Write-Host "  [SKIP] $Desc — $Reason" -ForegroundColor Yellow
    $script:Skip++
}

function Test-ModuleActive($Method, $Path) {
    try {
        $Uri = "$BaseUrl$Path"
        if ($Method -eq "GET") {
            $R = Invoke-RestMethod -Uri $Uri -Headers $Headers -Method Get
        } else {
            $R = Invoke-RestMethod -Uri $Uri -Method $Method -Headers $Headers `
                -Body '{}' -ContentType "application/json"
        }
        $Text = Get-ResponseText $R
        if ($Text -match "No static resource") { return $false }
        if ($Text -match "Request method.*not supported") { return $false }
        return $true
    } catch {
        if ($_.Exception.Message -match "No static resource") { return $false }
        if ($_.Exception.Message -match "Request method.*not supported") { return $false }
        return $true
    }
}

function Assert-Error($Desc, $Response) {
    $Text = Get-ResponseText $Response
    if ($Response.code -eq 500 -or $Text -match '500') {
        Write-Host "  [PASS] $Desc" -ForegroundColor Green
        $script:Pass++
    } else {
        Write-Host "  [FAIL] $Desc" -ForegroundColor Red
        Write-Host "    Response: $($Text.Substring(0, [Math]::Min(200, $Text.Length)))"
        $script:Fail++
    }
}

function Assert-Contains($Desc, $Expected, $Response) {
    $Text = Get-ResponseText $Response
    if ($Text -match $Expected) {
        Write-Host "  [PASS] $Desc" -ForegroundColor Green
        $script:Pass++
    } else {
        Write-Host "  [FAIL] $Desc (expected: $Expected)" -ForegroundColor Red
        $script:Fail++
    }
}

# ============ 登录 ============
Write-Host ""
Write-Host "[0] Login" -ForegroundColor Cyan
$Captcha = Invoke-RestMethod -Uri "$BaseUrl/captchaImage" -Method Get
$LoginData = @{ username = "admin"; password = "admin123" }
if ($Captcha.captchaEnabled) {
    $RawCode = (docker exec redis redis-cli --raw GET "captcha_codes:$($Captcha.uuid)").Trim()
    try { $CaptchaCode = $RawCode | ConvertFrom-Json } catch { $CaptchaCode = $RawCode.Trim('"') }
    $LoginData.code = [string]$CaptchaCode
    $LoginData.uuid = $Captcha.uuid
}
$Body = $LoginData | ConvertTo-Json
$LoginResp = Invoke-RestMethod -Uri "$BaseUrl/login" -Method Post -Body $Body -ContentType "application/json"
$Token = $LoginResp.token
$Headers = @{ "Authorization" = "Bearer $Token" }
if (-not $Token) { throw "Login failed: $($LoginResp.msg)" }
Write-Host "  [PASS] Login" -ForegroundColor Green

function Get-RequiredCustomFieldInputs($CategoryId) {
    $Form = Invoke-RestMethod -Uri "$BaseUrl/ticket/custom-field/form/$CategoryId" -Headers $Headers -Method Get
    $Inputs = @()
    foreach ($Field in $Form.data) {
        if ($Field.requiredFlag -ne "1" -or $Field.defaultValue) { continue }
        $Value = switch ($Field.fieldType) {
            "TEXT" { "smoke" }; "NUMBER" { if ($Field.minNumber) { $Field.minNumber } else { 1 } }
            "DATE" { "2026-07-03" }; "DATETIME" { "2026-07-03 10:20:30" }; "BOOLEAN" { $true }
            "SINGLE_SELECT" { $Field.options[0].optionValue }; "MULTI_SELECT" { @($Field.options[0].optionValue) }
        }
        $Inputs += @{ fieldKey = $Field.fieldKey; value = $Value }
    }
    return $Inputs
}

# ============ 分类测试 ============
Write-Host ""
Write-Host "[1] Category API" -ForegroundColor Cyan

$R = Invoke-RestMethod -Uri "$BaseUrl/ticket/category/tree" -Headers $Headers -Method Get
Assert-Success "GET /ticket/category/tree" $R
Assert-Contains "  tree has children" "children" $R

$R = Invoke-RestMethod -Uri "$BaseUrl/ticket/category/list" -Headers $Headers -Method Get
Assert-Success "GET /ticket/category/list" $R

# 新增
$CategoryName = "PS-TEST-$([DateTimeOffset]::Now.ToUnixTimeSeconds())"
$Body = @{ parentId = 1; categoryName = $CategoryName; orderNum = 99 } | ConvertTo-Json
try { $R = Invoke-RestMethod -Uri "$BaseUrl/ticket/category" -Method Post -Body $Body -ContentType "application/json" -Headers $Headers } catch { $R = $_.Exception.Message }
Assert-Success "POST /ticket/category (create)" $R

# 删除 (有子节点应失败)
try { $R = Invoke-RestMethod -Uri "$BaseUrl/ticket/category/2" -Method Delete -Headers $Headers } catch { $R = $_.Exception.Message }
Assert-Error "DELETE /ticket/category/2 (has children - should fail)" $R

# ============ 工单创建 ============
Write-Host ""
Write-Host "[2] Ticket Creation" -ForegroundColor Cyan

$Body1 = @{ title = "PS-Smoke-1 WiFi Issue"; content = "Office WiFi down"; categoryId = 6;
    priority = "HIGH"; customFields = @(Get-RequiredCustomFieldInputs 6) } | ConvertTo-Json -Depth 10
$R1 = Invoke-RestMethod -Uri "$BaseUrl/ticket" -Method Post -Body $Body1 -ContentType "application/json" -Headers $Headers
$T1 = $R1.data
Assert-Success "POST /ticket (create ticket 1)" $R1
Write-Host "  Ticket 1 ID: $T1"

$Body2 = '{"title":"PS-Smoke-2 Office Supply","content":"Need A4 paper","categoryId":7}'
$R2 = Invoke-RestMethod -Uri "$BaseUrl/ticket" -Method Post -Body $Body2 -ContentType "application/json" -Headers $Headers
$T2 = $R2.data
Assert-Success "POST /ticket (create ticket 2, default priority)" $R2
Write-Host "  Ticket 2 ID: $T2"

# 列表
$R = Invoke-RestMethod -Uri "$BaseUrl/ticket/list?pageNum=1&pageSize=10" -Headers $Headers -Method Get
Assert-Success "GET /ticket/list" $R
Assert-Contains "  list has total" "total" $R

# 详情
$R = Invoke-RestMethod -Uri "$BaseUrl/ticket/$T1" -Headers $Headers -Method Get
Assert-Success "GET /ticket/$T1 (detail)" $R
Assert-Contains "  detail has ticketNo" "ticketNo" $R
Assert-Contains "  detail has responseDueAt" "responseDueAt" $R
Assert-Contains "  detail has resolveDueAt" "resolveDueAt" $R

# ============ SLA ============
Write-Host ""
Write-Host "[2.1] SLA API" -ForegroundColor Cyan

$R = Invoke-RestMethod -Uri "$BaseUrl/ticket/sla/list" -Headers $Headers -Method Get
Assert-Success "GET /ticket/sla/list" $R
Assert-Contains "  policy has responseMinutes" "responseMinutes" $R

# 将本次 smoke 工单调整为响应超时，验证 SLA 通知链路
if (-not (Test-ModuleActive "GET" "/ticket/sla-alert/list")) {
    Write-Skip "POST /ticket/sla-alert/scan" "SLA Alert 模块未激活 (No static resource)"
    Write-Skip "GET /ticket/sla-alert/list" "SLA Alert 模块未激活 (No static resource)"
    Write-Skip "GET /ticket/sla-alert/{id}" "SLA Alert 模块未激活 (No static resource)"
} else {
    $Sql = "UPDATE ticket SET response_due_at=DATE_SUB(NOW(), INTERVAL 10 MINUTE), response_overdue='0' WHERE ticket_id=$T2;"
    $Sql | docker exec -i mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" ticket_backend'
    $R = Invoke-RestMethod -Uri "$BaseUrl/ticket/sla-alert/scan" -Headers $Headers -Method Post
    Assert-Success "POST /ticket/sla-alert/scan" $R

    $AlertPage = Invoke-RestMethod -Uri "$BaseUrl/ticket/sla-alert/list?pageNum=1&pageSize=20" -Headers $Headers -Method Get
    Assert-Success "GET /ticket/sla-alert/list" $AlertPage
    if ($AlertPage.rows.Count -gt 0) {
        $AlertId = $AlertPage.rows[0].alertId
        $R = Invoke-RestMethod -Uri "$BaseUrl/ticket/sla-alert/$AlertId" -Headers $Headers -Method Get
        Assert-Success "GET /ticket/sla-alert/{id}" $R
        Assert-Contains "  alert has ticketNo" "ticketNo" $R
    }
}

function Assert-Equal($Desc, $Expected, $Actual) {
    if ($Expected -eq $Actual) {
        Write-Host "  [PASS] $Desc" -ForegroundColor Green
        $script:Pass++
    } else {
        Write-Host "  [FAIL] $Desc (expected: $Expected, actual: $Actual)" -ForegroundColor Red
        $script:Fail++
    }
}

function Invoke-SearchDispatch {
    $Jobs = Invoke-RestMethod -Uri "$BaseUrl/monitor/job/list?jobGroup=TICKET&pageNum=1&pageSize=100" `
        -Headers $Headers -Method Get
    $Job = @($Jobs.rows | Where-Object { $_.invokeTarget -eq "ticketSearchTask.dispatch" }) | Select-Object -First 1
    if (-not $Job) { throw "Search dispatcher job ticketSearchTask.dispatch was not found" }
    return Invoke-RestMethod -Uri "$BaseUrl/monitor/job/run" -Method Put -Headers $Headers `
        -Body (@{ jobId = $Job.jobId; jobGroup = $Job.jobGroup } | ConvertTo-Json) -ContentType "application/json"
}

function Invoke-TicketLogin($Username, $Password) {
    $Captcha = Invoke-RestMethod -Uri "$BaseUrl/captchaImage" -Method Get
    $LoginData = @{ username = $Username; password = $Password }
    if ($Captcha.captchaEnabled) {
        $RawCode = (docker exec redis redis-cli --raw GET "captcha_codes:$($Captcha.uuid)").Trim()
        try { $CaptchaCode = $RawCode | ConvertFrom-Json } catch { $CaptchaCode = $RawCode.Trim('"') }
        $LoginData.code = [string]$CaptchaCode
        $LoginData.uuid = $Captcha.uuid
    }
    $Response = Invoke-RestMethod -Uri "$BaseUrl/login" -Method Post `
        -Body ($LoginData | ConvertTo-Json) -ContentType "application/json"
    if (-not $Response.token) { throw "Login failed for $Username`: $($Response.msg)" }
    return $Response.token
}

function New-SmokeRole($RoleName, $RoleKey, $DataScope, $MenuIds, $DeptIds) {
    $Body = @{
        roleName = $RoleName; roleKey = $RoleKey; roleSort = 99; dataScope = $DataScope
        menuCheckStrictly = $false; deptCheckStrictly = $false
        menuIds = $MenuIds; status = "0"
    } | ConvertTo-Json
    $Response = Invoke-RestMethod -Uri "$BaseUrl/system/role" -Method Post -Headers $Headers `
        -Body $Body -ContentType "application/json"
    if ($Response.code -ne 200) { throw "Create role failed: $RoleKey" }
    $Role = (Invoke-RestMethod -Uri "$BaseUrl/system/role/list?roleKey=$RoleKey" -Headers $Headers).rows[0]
    $ScopeBody = @{
        roleId = $Role.roleId; dataScope = $DataScope
        deptCheckStrictly = $false; deptIds = $DeptIds
    } | ConvertTo-Json
    $Response = Invoke-RestMethod -Uri "$BaseUrl/system/role/dataScope" -Method Put `
        -Headers $Headers -Body $ScopeBody -ContentType "application/json"
    if ($Response.code -ne 200) { throw "Configure role scope failed: $RoleKey" }
    return [long]$Role.roleId
}

function New-SmokeUser($Username, $Password, $DeptId, $RoleIds) {
    $Body = @{
        deptId = $DeptId; userName = $Username; nickName = "V13 Smoke"
        password = $Password; roleIds = $RoleIds; postIds = @(); status = "0"
    } | ConvertTo-Json
    $Response = Invoke-RestMethod -Uri "$BaseUrl/system/user" -Method Post -Headers $Headers `
        -Body $Body -ContentType "application/json"
    if ($Response.code -ne 200) { throw "Create user failed: $Username" }
    $User = (Invoke-RestMethod -Uri "$BaseUrl/system/user/list?userName=$Username" -Headers $Headers).rows[0]
    return [long]$User.userId
}

if (-not (Test-ModuleActive "GET" "/ticket/notification/list")) {
    Write-Skip "GET /ticket/notification/list" "Notification 模块未激活 (No static resource)"
    Write-Skip "GET /ticket/notification/unread-count" "Notification 模块未激活 (No static resource)"
    Write-Skip "PUT /ticket/notification/read-all" "Notification 模块未激活 (No static resource)"
    Write-Skip "  notification has SLA_OVERDUE" "Notification 模块未激活"
} else {
    $NotificationPage = Invoke-RestMethod -Uri "$BaseUrl/ticket/notification/list?pageNum=1&pageSize=20" -Headers $Headers -Method Get
    Assert-Success "GET /ticket/notification/list" $NotificationPage
    Assert-Contains "  notification has SLA_OVERDUE" "SLA_OVERDUE" $NotificationPage

    $Unread = Invoke-RestMethod -Uri "$BaseUrl/ticket/notification/unread-count" -Headers $Headers -Method Get
    Assert-Success "GET /ticket/notification/unread-count" $Unread
    if ($NotificationPage.rows.Count -gt 0) {
        $NotificationId = $NotificationPage.rows[0].notificationId
        $R = Invoke-RestMethod -Uri "$BaseUrl/ticket/notification/$NotificationId/read" -Headers $Headers -Method Put
        Assert-Success "PUT /ticket/notification/{id}/read" $R
    }
    $R = Invoke-RestMethod -Uri "$BaseUrl/ticket/notification/read-all" -Headers $Headers -Method Put
    Assert-Success "PUT /ticket/notification/read-all" $R
}

# ============ 合法状态流转 ============
Write-Host ""
Write-Host "[3] Valid Transitions" -ForegroundColor Cyan

$Body = '{"assigneeId":1,"comment":"Assign to admin"}'
$R = Invoke-RestMethod -Uri "$BaseUrl/ticket/$T1/assign" -Method Put -Body $Body -ContentType "application/json" -Headers $Headers
Assert-Success "PUT /ticket/$T1/assign (NEW->PROCESSING)" $R

$Body = '{"comment":"Fixed the WiFi router"}'
$R = Invoke-RestMethod -Uri "$BaseUrl/ticket/$T1/process" -Method Put -Body $Body -ContentType "application/json" -Headers $Headers
Assert-Success "PUT /ticket/$T1/process (PROCESSING->WAIT_CONFIRM)" $R

$Body = '{"comment":"Confirmed resolved"}'
$R = Invoke-RestMethod -Uri "$BaseUrl/ticket/$T1/confirm" -Method Put -Body $Body -ContentType "application/json" -Headers $Headers
Assert-Success "PUT /ticket/$T1/confirm (WAIT_CONFIRM->CLOSED)" $R

$Body = '{"comment":"No longer needed"}'
$R = Invoke-RestMethod -Uri "$BaseUrl/ticket/$T2/cancel" -Method Put -Body $Body -ContentType "application/json" -Headers $Headers
Assert-Success "PUT /ticket/$T2/cancel (NEW->CANCELLED)" $R

# ============ 满意度评价 ============
Write-Host ""
Write-Host "[3.1] Satisfaction" -ForegroundColor Cyan

if (-not (Test-ModuleActive "GET" "/ticket/satisfaction/list")) {
    Write-Skip "POST /ticket/satisfaction/$T1" "Satisfaction 模块未激活 (No static resource)"
    Write-Skip "GET /ticket/satisfaction/ticket/$T1" "Satisfaction 模块未激活 (No static resource)"
    Write-Skip "GET /ticket/satisfaction/list" "Satisfaction 模块未激活 (No static resource)"
    Write-Skip "GET /ticket/satisfaction/statistics" "Satisfaction 模块未激活 (No static resource)"
    Write-Skip "  satisfaction score is 5" "Satisfaction 模块未激活"
    Write-Skip "  statistics has averageScore" "Satisfaction 模块未激活"
} else {
    $Body = '{"score":5,"content":"Smoke test satisfied"}'
    $R = Invoke-RestMethod -Uri "$BaseUrl/ticket/satisfaction/$T1" -Method Post -Body $Body -ContentType "application/json" -Headers $Headers
    Assert-Success "POST /ticket/satisfaction/$T1" $R
    $R = Invoke-RestMethod -Uri "$BaseUrl/ticket/satisfaction/ticket/$T1" -Headers $Headers -Method Get
    Assert-Success "GET /ticket/satisfaction/ticket/$T1" $R
    Assert-Contains "  satisfaction score is 5" '"score":5' $R
    $R = Invoke-RestMethod -Uri "$BaseUrl/ticket/satisfaction/list?pageNum=1&pageSize=20" -Headers $Headers -Method Get
    Assert-Success "GET /ticket/satisfaction/list" $R
    $R = Invoke-RestMethod -Uri "$BaseUrl/ticket/satisfaction/statistics" -Headers $Headers -Method Get
    Assert-Success "GET /ticket/satisfaction/statistics" $R
    Assert-Contains "  statistics has averageScore" "averageScore" $R
}

# ============ 非法状态流转 ============
Write-Host ""
Write-Host "[4] Invalid Transitions" -ForegroundColor Cyan

$Body = '{"assigneeId":1,"comment":"Try re-assign"}'
try { $R = Invoke-RestMethod -Uri "$BaseUrl/ticket/$T1/assign" -Method Put -Body $Body -ContentType "application/json" -Headers $Headers } catch { $R = $_.Exception.Message }
Assert-Error "PUT /ticket/$T1/assign (CLOSED - should fail)" $R

$Body = '{"comment":"Try re-process"}'
try { $R = Invoke-RestMethod -Uri "$BaseUrl/ticket/$T1/process" -Method Put -Body $Body -ContentType "application/json" -Headers $Headers } catch { $R = $_.Exception.Message }
Assert-Error "PUT /ticket/$T1/process (CLOSED - should fail)" $R

$Body = '{"comment":"Try cancel closed"}'
try { $R = Invoke-RestMethod -Uri "$BaseUrl/ticket/$T1/cancel" -Method Put -Body $Body -ContentType "application/json" -Headers $Headers } catch { $R = $_.Exception.Message }
Assert-Error "PUT /ticket/$T1/cancel (CLOSED - should fail)" $R

$Body = '{"comment":""}'
try { $R = Invoke-RestMethod -Uri "$BaseUrl/ticket/$T1/cancel" -Method Put -Body $Body -ContentType "application/json" -Headers $Headers } catch { $R = $_.Exception.Message }
Assert-Error "PUT cancel (empty comment - should fail)" $R

# ============ 评论 ============
Write-Host ""
Write-Host "[5] Comments" -ForegroundColor Cyan

$Body = '{"content":"Please fix ASAP","commentType":"EXTERNAL"}'
$R = Invoke-RestMethod -Uri "$BaseUrl/ticket/$T1/comment" -Method Post -Body $Body -ContentType "application/json" -Headers $Headers
Assert-Success "POST /ticket/$T1/comment (add comment)" $R

$R = Invoke-RestMethod -Uri "$BaseUrl/ticket/$T1/comment" -Headers $Headers -Method Get
Assert-Success "GET /ticket/$T1/comment (list comments)" $R

# ============ 操作日志 ============
Write-Host ""
Write-Host "[6] Operation Logs" -ForegroundColor Cyan

$R = Invoke-RestMethod -Uri "$BaseUrl/ticket/$T1/logs" -Headers $Headers -Method Get
Assert-Success "GET /ticket/$T1/logs" $R
Assert-Contains "  logs contain CREATE" "CREATE" $R
Assert-Contains "  logs contain ASSIGN" "ASSIGN" $R
Assert-Contains "  logs contain PROCESS" "PROCESS" $R
Assert-Contains "  logs contain CONFIRM" "CONFIRM" $R

# ============ 权限 ============
Write-Host ""
Write-Host "[7] Auth" -ForegroundColor Cyan

try { $R = Invoke-RestMethod -Uri "$BaseUrl/ticket/list" -Method Get } catch { $R = $_.Exception.Message }
Assert-Contains "GET /ticket/list (no token)" "401" $R

# ============ v1.3 部门数据权限 ============
Write-Host ""
Write-Host "[8] v1.3 Department Data Scope" -ForegroundColor Cyan

$ScopeSuffix = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
$UserSuffix = $ScopeSuffix.ToString().Substring($ScopeSuffix.ToString().Length - 6)
$ScopePassword = "Tmp!V13$ScopeSuffix"
$ScopeRoleIds = New-Object System.Collections.Generic.List[long]
$ScopeUserIds = New-Object System.Collections.Generic.List[long]

try {
    $TicketMenus = Invoke-RestMethod -Uri "$BaseUrl/system/menu/list" -Headers $Headers
    $TicketMenuIds = @($TicketMenus.data | Where-Object {
        $_.perms -and $_.perms.StartsWith("ticket:")
    } | ForEach-Object { [long]$_.menuId })
    if ($TicketMenuIds.Count -eq 0) { throw "Ticket permission menus not found" }

    $AllRole = New-SmokeRole "V13 All $ScopeSuffix" "v13_all_$ScopeSuffix" "1" $TicketMenuIds @()
    $CustomRole = New-SmokeRole "V13 Custom $ScopeSuffix" "v13_custom_$ScopeSuffix" "2" $TicketMenuIds @(103)
    $DeptRole = New-SmokeRole "V13 Dept $ScopeSuffix" "v13_dept_$ScopeSuffix" "3" $TicketMenuIds @()
    $ChildRole = New-SmokeRole "V13 Child $ScopeSuffix" "v13_child_$ScopeSuffix" "4" $TicketMenuIds @()
    $SelfRole = New-SmokeRole "V13 Self $ScopeSuffix" "v13_self_$ScopeSuffix" "5" $TicketMenuIds @()
    $OtherRole = New-SmokeRole "V13 Other $ScopeSuffix" "v13_other_$ScopeSuffix" "3" $TicketMenuIds @()
    @($AllRole, $CustomRole, $DeptRole, $ChildRole, $SelfRole, $OtherRole) |
        ForEach-Object { $ScopeRoleIds.Add($_) }

    $AllUser = New-SmokeUser "v13all_$UserSuffix" $ScopePassword 105 @($AllRole)
    $CustomUser = New-SmokeUser "v13custom_$UserSuffix" $ScopePassword 105 @($CustomRole)
    $DeptUser = New-SmokeUser "v13dept_$UserSuffix" $ScopePassword 103 @($DeptRole)
    $ChildUser = New-SmokeUser "v13child_$UserSuffix" $ScopePassword 101 @($ChildRole)
    $SelfUser = New-SmokeUser "v13self_$UserSuffix" $ScopePassword 105 @($SelfRole)
    $MultiUser = New-SmokeUser "v13multi_$UserSuffix" $ScopePassword 105 @($OtherRole, $CustomRole)
    @($AllUser, $CustomUser, $DeptUser, $ChildUser, $SelfUser, $MultiUser) |
        ForEach-Object { $ScopeUserIds.Add($_) }

    $ScopeTitle = "V13-SCOPE-$ScopeSuffix"
    $Body = @{
        title = $ScopeTitle; content = "Department data scope smoke"
        categoryId = 6; priority = "MEDIUM"; customFields = @(Get-RequiredCustomFieldInputs 6)
    } | ConvertTo-Json -Depth 10
    $ScopeTicket = (Invoke-RestMethod -Uri "$BaseUrl/ticket" -Method Post -Headers $Headers `
        -Body $Body -ContentType "application/json").data

    $ScopeCases = @(
        @{ Name = "ALL"; Username = "v13all_$UserSuffix"; Expected = 1 },
        @{ Name = "CUSTOM"; Username = "v13custom_$UserSuffix"; Expected = 1 },
        @{ Name = "DEPT"; Username = "v13dept_$UserSuffix"; Expected = 1 },
        @{ Name = "DEPT_AND_CHILD"; Username = "v13child_$UserSuffix"; Expected = 1 },
        @{ Name = "SELF"; Username = "v13self_$UserSuffix"; Expected = 0 },
        @{ Name = "MULTI_ROLE"; Username = "v13multi_$UserSuffix"; Expected = 1 }
    )
    foreach ($Case in $ScopeCases) {
        $UserToken = Invoke-TicketLogin $Case.Username $ScopePassword
        $UserHeaders = @{ Authorization = "Bearer $UserToken" }
        $Page = Invoke-RestMethod -Uri "$BaseUrl/ticket/list?keyword=$ScopeTitle&pageNum=1&pageSize=10" `
            -Headers $UserHeaders
        Assert-Equal "$($Case.Name) list scope" $Case.Expected $Page.total
    }

    $Run = Invoke-SearchDispatch
    Assert-Success "dispatch search events for data scope" $Run
    foreach ($Attempt in 1..20) {
        Start-Sleep -Milliseconds 500
        $AllSearch = Invoke-RestMethod `
            -Uri "$BaseUrl/ticket/search?keyword=$ScopeSuffix&pageSize=10" `
            -Headers @{ Authorization = "Bearer $(Invoke-TicketLogin "v13all_$UserSuffix" $ScopePassword)" }
        if ($AllSearch.data.items.Count -eq 1) { break }
    }
    Assert-Equal "ALL search scope" 1 $AllSearch.data.items.Count
    $SelfSearch = Invoke-RestMethod -Uri "$BaseUrl/ticket/search?keyword=$ScopeSuffix&pageSize=10" `
        -Headers @{ Authorization = "Bearer $(Invoke-TicketLogin "v13self_$UserSuffix" $ScopePassword)" }
    Assert-Equal "SELF search scope" 0 $SelfSearch.data.items.Count

    $SelfToken = Invoke-TicketLogin "v13self_$UserSuffix" $ScopePassword
    $SelfHeaders = @{ Authorization = "Bearer $SelfToken" }
    $InjectedPage = Invoke-RestMethod `
        -Uri "$BaseUrl/ticket/list?keyword=$ScopeTitle&params%5BdataScope%5D=1%20%3D%201&pageNum=1&pageSize=10" `
        -Headers $SelfHeaders
    Assert-Equal "request dataScope injection is ignored" 0 $InjectedPage.total

    $ObjectUris = @(
        "$BaseUrl/ticket/$ScopeTicket",
        "$BaseUrl/ticket/$ScopeTicket/comment",
        "$BaseUrl/ticket/$ScopeTicket/logs",
        "$BaseUrl/ticket/satisfaction/ticket/$ScopeTicket",
        "$BaseUrl/ticket/attachment/ticket/$ScopeTicket"
    )
    foreach ($Uri in $ObjectUris) {
        $Response = Invoke-RestMethod -Uri $Uri -Headers $SelfHeaders
        Assert-Contains "out-of-scope object returns not found" "工单不存在" $Response
    }
    $Body = @{ content = "unauthorized" } | ConvertTo-Json
    $Response = Invoke-RestMethod -Uri "$BaseUrl/ticket/$ScopeTicket/comment" -Method Post `
        -Headers $SelfHeaders -Body $Body -ContentType "application/json"
    Assert-Contains "out-of-scope comment is rejected" "工单不存在" $Response

    $Body = @{ assigneeId = $SelfUser; comment = "cross-department assignment" } | ConvertTo-Json
    $Response = Invoke-RestMethod -Uri "$BaseUrl/ticket/$ScopeTicket/assign" -Method Put `
        -Headers $Headers -Body $Body -ContentType "application/json"
    Assert-Success "assign cross-department user" $Response
    $Page = Invoke-RestMethod -Uri "$BaseUrl/ticket/list?keyword=$ScopeTitle&pageNum=1&pageSize=10" `
        -Headers $SelfHeaders
    Assert-Equal "assignee can see cross-department ticket" 1 $Page.total
    $Response = Invoke-RestMethod -Uri "$BaseUrl/ticket/$ScopeTicket" -Headers $SelfHeaders
    Assert-Success "assignee can access cross-department ticket detail" $Response
}
finally {
    foreach ($UserId in $ScopeUserIds) {
        try { Invoke-RestMethod -Uri "$BaseUrl/system/user/$UserId" -Method Delete -Headers $Headers | Out-Null } catch {}
    }
    foreach ($RoleId in $ScopeRoleIds) {
        try { Invoke-RestMethod -Uri "$BaseUrl/system/role/$RoleId" -Method Delete -Headers $Headers | Out-Null } catch {}
    }
}

# ============ v2.0 动态流程 ============
Write-Host ""
Write-Host "[9] v2.0 Workflow" -ForegroundColor Cyan
if (-not (Test-ModuleActive "GET" "/ticket/workflow/list")) {
    Write-Skip "workflow-definition-smoke.ps1" "Workflow 模块未激活 (No static resource)"
    Write-Skip "workflow-engine-smoke.ps1" "Workflow 模块未激活 (No static resource)"
    Write-Skip "workflow-task-smoke.ps1" "Workflow 模块未激活 (No static resource)"
} else {
    $WorkflowSmokeScripts = @(
        "workflow-definition-smoke.ps1",
        "workflow-engine-smoke.ps1",
        "workflow-task-smoke.ps1"
    )
    foreach ($ScriptName in $WorkflowSmokeScripts) {
        try {
            & (Join-Path $PSScriptRoot "..\v2.x\$ScriptName") | ForEach-Object { Write-Host "  $_" }
            Write-Host "  [PASS] $ScriptName" -ForegroundColor Green
            $Pass++
        }
        catch {
            Write-Host "  [FAIL] $ScriptName - $($_.Exception.Message)" -ForegroundColor Red
            $Fail++
        }
    }
}

# ============ v2.1 自定义字段 ============
Write-Host ""
Write-Host "[10] v2.1 Custom Fields" -ForegroundColor Cyan
if (-not (Test-ModuleActive "POST" "/ticket/custom-field")) {
    Write-Skip "custom-field-definition-smoke.ps1" "Custom Fields 模块未激活 (No static resource)"
    Write-Skip "custom-field-value-smoke.ps1" "Custom Fields 模块未激活 (No static resource)"
    Write-Skip "custom-field-workflow-smoke.ps1" "Custom Fields 模块未激活 (No static resource)"
} else {
    $CustomFieldSmokeScripts = @(
        "custom-field-definition-smoke.ps1",
        "custom-field-value-smoke.ps1",
        "custom-field-workflow-smoke.ps1"
    )
    foreach ($ScriptName in $CustomFieldSmokeScripts) {
        try {
            & (Join-Path $PSScriptRoot "..\v2.x\$ScriptName") | ForEach-Object { Write-Host "  $_" }
            Write-Host "  [PASS] $ScriptName" -ForegroundColor Green
            $Pass++
        }
        catch {
            Write-Host "  [FAIL] $ScriptName - $($_.Exception.Message)" -ForegroundColor Red
            $Fail++
        }
    }
}

# ============ v2.2 附件 ============
Write-Host ""
Write-Host "[11] v2.2 Attachments" -ForegroundColor Cyan
if (-not (Test-ModuleActive "GET" "/ticket/attachment/ticket/1")) {
    Write-Skip "attachment-smoke.ps1" "Attachments 模块未激活 (No static resource)"
} else {
    try {
        & (Join-Path $PSScriptRoot "..\v2.x\attachment-smoke.ps1") | ForEach-Object { Write-Host "  $_" }
        Write-Host "  [PASS] attachment-smoke.ps1" -ForegroundColor Green
        $Pass++
    }
    catch {
        Write-Host "  [FAIL] attachment-smoke.ps1 - $($_.Exception.Message)" -ForegroundColor Red
        $Fail++
    }
}

# ============ v2.3 Elasticsearch 检索 ============
Write-Host ""
Write-Host "[12] v2.3 Elasticsearch Search" -ForegroundColor Cyan
$SearchSmokeScripts = @(
    "search-smoke.ps1",
    "search-rebuild-smoke.ps1"
)
foreach ($ScriptName in $SearchSmokeScripts) {
    try {
        & (Join-Path $PSScriptRoot "..\v2.x\$ScriptName") | ForEach-Object { Write-Host "  $_" }
        Write-Host "  [PASS] $ScriptName" -ForegroundColor Green
        $Pass++
    }
    catch {
        Write-Host "  [FAIL] $ScriptName - $($_.Exception.Message)" -ForegroundColor Red
        $Fail++
    }
}

# ============ v3.0 AI 知识与工单辅助 ============
Write-Host ""
Write-Host "[13] v3.0 AI Knowledge and Assist" -ForegroundColor Cyan

# 诊断：输出所有 AI 相关环境变量
$AiEnvVars = @(
    "TICKET_AI_SMOKE_ENABLED",
    "TICKET_AI_KNOWLEDGE_INDEX",
    "TICKET_AI_TICKET_HISTORY_INDEX",
    "TICKET_AI_SMOKE_MODE",
    "TICKET_AI_ENABLED",
    "TICKET_AI_SERVICE_TOKEN"
)
Write-Host "  --- AI smoke 环境变量诊断 ---" -ForegroundColor DarkGray
foreach ($VarName in $AiEnvVars) {
    $Val = [Environment]::GetEnvironmentVariable($VarName, "Process")
    if ($null -eq $Val -or $Val -eq "") {
        Write-Host "  $VarName = (未设置)" -ForegroundColor DarkGray
    } elseif ($VarName -eq "TICKET_AI_SERVICE_TOKEN") {
        $Masked = if ($Val.Length -gt 8) { $Val.Substring(0, 4) + "****" + $Val.Substring($Val.Length - 4) } else { "****" }
        Write-Host "  $VarName = $Masked" -ForegroundColor DarkGray
    } else {
        Write-Host "  $VarName = $Val" -ForegroundColor DarkGray
    }
}
Write-Host "  ---------------------------------" -ForegroundColor DarkGray

$AiSmokeEnabled = $env:TICKET_AI_SMOKE_ENABLED -eq "true"
if (-not $AiSmokeEnabled) {
    Write-Host "  [SKIP] Set TICKET_AI_SMOKE_ENABLED=true and use smoke/test AI indexes" -ForegroundColor Yellow
} elseif ($env:TICKET_AI_KNOWLEDGE_INDEX -notmatch "(smoke|test)" -or
          $env:TICKET_AI_TICKET_HISTORY_INDEX -notmatch "(smoke|test)") {
    Write-Host "  [FAIL] AI smoke requires knowledge/history index names containing smoke or test" -ForegroundColor Red
    $Fail++
} elseif ($env:TICKET_AI_ENABLED -ne "true" -or -not $env:TICKET_AI_SERVICE_TOKEN) {
    Write-Host "  [FAIL] Set TICKET_AI_ENABLED=true and TICKET_AI_SERVICE_TOKEN before starting Java" `
        -ForegroundColor Red
    $Fail++
} elseif ($env:TICKET_AI_SMOKE_MODE -ne "true") {
    Write-Host "  [FAIL] Set TICKET_AI_SMOKE_MODE=true before starting Python" -ForegroundColor Red
    $Fail++
} else {
    try {
        $Before = Invoke-RestMethod -Uri "$BaseUrl/ticket/$T1" -Headers $Headers -Method Get
        $BeforeStatus = $Before.data.status
        $BeforeComments = @($Before.data.comments).Count

        $TempFile = Join-Path $env:TEMP "ticket-ai-smoke-$([Guid]::NewGuid().ToString('N')).md"
        "# SMOKE Redis`n缓存穿透可使用参数校验、空值缓存和布隆过滤器。" | Set-Content $TempFile -Encoding utf8
        try {
            $Multipart = [System.Net.Http.MultipartFormDataContent]::new()
            $Multipart.Add([System.Net.Http.StringContent]::new(
                "smoke-$([DateTimeOffset]::Now.ToUnixTimeSeconds())"), "sourceId")
            $FileContent = [System.Net.Http.ByteArrayContent]::new([System.IO.File]::ReadAllBytes($TempFile))
            $FileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("text/markdown")
            $Multipart.Add($FileContent, "file", [System.IO.Path]::GetFileName($TempFile))
            $Client = [System.Net.Http.HttpClient]::new()
            $Client.DefaultRequestHeaders.Authorization =
                [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $Token)
            $HttpResponse = $Client.PostAsync("$BaseUrl/ticket/ai/document/import", $Multipart).GetAwaiter().GetResult()
            $R = $HttpResponse.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
            Assert-Success "v3 document import" $R
        } finally {
            if ($Client) { $Client.Dispose() }
            if ($Multipart) { $Multipart.Dispose() }
            Remove-Item -LiteralPath $TempFile -Force -ErrorAction SilentlyContinue
        }

        $R = Invoke-RestMethod -Uri "$BaseUrl/ticket/ai/history/sync?lastTicketId=0&limit=100" `
            -Method Post -Headers $Headers
        Assert-Success "v3 closed ticket sync" $R

        $R = Invoke-RestMethod -Uri "$BaseUrl/ticket/ai/ticket/similar?ticketId=$T1" `
            -Method Post -Headers $Headers
        Assert-Success "v3 similar knowledge search" $R
        if (-not $R.data.degraded) { Assert-Contains "v3 sources are traceable" "sourceId" $R }

        $Assist = Invoke-RestMethod -Uri "$BaseUrl/ticket/ai/ticket/assist?ticketId=$T1&topK=5" `
            -Method Post -Headers $Headers
        Assert-Success "v3 suggestion and reply draft" $Assist
        if (-not $Assist.data.degraded) {
            Assert-Contains "v3 suggestion returned" "suggestion" $Assist
            Assert-Contains "v3 replyDraft returned" "replyDraft" $Assist
            Assert-Contains "v3 assist sources traceable" "sourceId" $Assist
        } else {
            Assert-Contains "v3 degraded response has reason" "reason" $Assist
        }

        $After = Invoke-RestMethod -Uri "$BaseUrl/ticket/$T1" -Headers $Headers -Method Get
        Assert-Equal "AI does not change ticket status" $BeforeStatus $After.data.status
        Assert-Equal "AI does not add ticket comments" $BeforeComments @($After.data.comments).Count

        $TriageBody = @{
            title = "PS-Smoke-Triage Apply $([DateTimeOffset]::Now.ToUnixTimeSeconds())"
            content = "Office WiFi is unstable for multiple users and needs network support"
            categoryId = 6
            priority = "MEDIUM"
            customFields = @(Get-RequiredCustomFieldInputs 6)
        } | ConvertTo-Json -Depth 10
        $TriageTicketId = (Invoke-RestMethod -Uri "$BaseUrl/ticket" -Method Post -Headers $Headers `
            -Body $TriageBody -ContentType "application/json").data

        $Triage = Invoke-RestMethod -Uri "$BaseUrl/ticket/ai/ticket/triage?ticketId=$TriageTicketId" `
            -Method Post -Headers $Headers
        Assert-Success "v3.1 triage suggestion" $Triage
        if ($Triage.data.degraded) {
            Assert-Contains "v3.1 degraded triage has reason" "reason" $Triage
            Write-Skip "v3.1 apply triage suggestion" "triage degraded: $($Triage.data.reason)"
        } else {
            Assert-Contains "v3.1 triage has suggestion id" "suggestionId" $Triage
            Assert-Contains "v3.1 triage has suggested assignee" "suggestedAssigneeId" $Triage
            $SuggestionId = $Triage.data.suggestionId
            if ($SuggestionId) {
                $ApplyBody = @{ comment = "v3.1 smoke apply" } | ConvertTo-Json
                $Apply = Invoke-RestMethod -Uri "$BaseUrl/ticket/ai/triage/$SuggestionId/apply" `
                    -Method Post -Body $ApplyBody -ContentType "application/json" -Headers $Headers
                Assert-Success "v3.1 apply triage suggestion" $Apply
                try {
                    $RepeatApply = Invoke-RestMethod -Uri "$BaseUrl/ticket/ai/triage/$SuggestionId/apply" `
                        -Method Post -Body $ApplyBody -ContentType "application/json" -Headers $Headers
                } catch { $RepeatApply = $_.Exception.Message }
                Assert-Error "v3.1 repeated triage apply should fail" $RepeatApply
            }
        }

        $RejectBody = @{
            title = "PS-Smoke-Triage Reject $([DateTimeOffset]::Now.ToUnixTimeSeconds())"
            content = "Printer access request should be reviewed by service desk"
            categoryId = 6
            priority = "LOW"
            customFields = @(Get-RequiredCustomFieldInputs 6)
        } | ConvertTo-Json -Depth 10
        $RejectTicketId = (Invoke-RestMethod -Uri "$BaseUrl/ticket" -Method Post -Headers $Headers `
            -Body $RejectBody -ContentType "application/json").data
        $RejectTriage = Invoke-RestMethod -Uri "$BaseUrl/ticket/ai/ticket/triage?ticketId=$RejectTicketId" `
            -Method Post -Headers $Headers
        Assert-Success "v3.1 triage suggestion for reject" $RejectTriage
        if ($RejectTriage.data.degraded) {
            Write-Skip "v3.1 reject triage suggestion" "triage degraded: $($RejectTriage.data.reason)"
        } else {
            $RejectSuggestionId = $RejectTriage.data.suggestionId
            Assert-Contains "v3.1 reject path has suggestion id" "suggestionId" $RejectTriage
            if ($RejectSuggestionId) {
                $Reject = Invoke-RestMethod -Uri "$BaseUrl/ticket/ai/triage/$RejectSuggestionId/reject" `
                    -Method Post -Headers $Headers
                Assert-Success "v3.1 reject triage suggestion" $Reject
                try {
                    $RepeatReject = Invoke-RestMethod -Uri "$BaseUrl/ticket/ai/triage/$RejectSuggestionId/reject" `
                        -Method Post -Headers $Headers
                } catch { $RepeatReject = $_.Exception.Message }
                Assert-Error "v3.1 repeated triage reject should fail" $RepeatReject
            }
        }
    } catch {
        Write-Host "  [FAIL] v3 AI dependencies unavailable: $($_.Exception.Message)" -ForegroundColor Red
        $Fail++
    }
}

# ============ 结果 ============
Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  Result: $Pass passed, $Fail failed, $Skip skipped" -ForegroundColor $(if ($Fail -eq 0) { "Green" } else { "Red" })
Write-Host "==========================================" -ForegroundColor Cyan

if ($Fail -gt 0) { exit 1 }
