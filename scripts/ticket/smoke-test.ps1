# =============================================
# Ticket API v1.0 冒烟测试 (PowerShell)
# 用法: .\test-api.ps1
# =============================================

$ErrorActionPreference = "Continue"
$BaseUrl = "http://localhost:8080"
$Pass = 0
$Fail = 0

function Assert-Success($Desc, $Response) {
    if ($Response -match '"code":200') {
        Write-Host "  [PASS] $Desc" -ForegroundColor Green
        $script:Pass++
    } else {
        Write-Host "  [FAIL] $Desc" -ForegroundColor Red
        Write-Host "    Response: $($Response.Substring(0, [Math]::Min(200, $Response.Length)))"
        $script:Fail++
    }
}

function Assert-Error($Desc, $Response) {
    if ($Response -match '"code":500') {
        Write-Host "  [PASS] $Desc" -ForegroundColor Green
        $script:Pass++
    } else {
        Write-Host "  [FAIL] $Desc" -ForegroundColor Red
        Write-Host "    Response: $($Response.Substring(0, [Math]::Min(200, $Response.Length)))"
        $script:Fail++
    }
}

function Assert-Contains($Desc, $Expected, $Response) {
    if ($Response -match $Expected) {
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
$Body = '{"username":"admin","password":"admin123"}'
$LoginResp = Invoke-RestMethod -Uri "$BaseUrl/login" -Method Post -Body $Body -ContentType "application/json"
$Token = $LoginResp.token
$Headers = @{ "Authorization" = "Bearer $Token" }
Write-Host "  Token: $($Token.Substring(0, 30))..."

# ============ 分类测试 ============
Write-Host ""
Write-Host "[1] Category API" -ForegroundColor Cyan

$R = Invoke-RestMethod -Uri "$BaseUrl/ticket/category/tree" -Headers $Headers -Method Get
Assert-Success "GET /ticket/category/tree" $R
Assert-Contains "  tree has children" "children" $R

$R = Invoke-RestMethod -Uri "$BaseUrl/ticket/category/list" -Headers $Headers -Method Get
Assert-Success "GET /ticket/category/list" $R

# 新增
$Body = '{"parentId":1,"categoryName":"PS-TEST","orderNum":99}'
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
