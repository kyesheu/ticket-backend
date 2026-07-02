# =============================================
# Ticket API v1.1 冒烟测试 (PowerShell)
# 用法: .\test-api.ps1
# =============================================

$ErrorActionPreference = "Continue"
$BaseUrl = "http://localhost:8080"
$Pass = 0
$Fail = 0

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

$Body1 = '{"title":"PS-Smoke-1 WiFi Issue","content":"Office WiFi down","categoryId":6,"priority":"HIGH"}'
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

# ============ 结果 ============
Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  Result: $Pass passed, $Fail failed" -ForegroundColor $(if ($Fail -eq 0) { "Green" } else { "Red" })
Write-Host "==========================================" -ForegroundColor Cyan

if ($Fail -gt 0) { exit 1 }
