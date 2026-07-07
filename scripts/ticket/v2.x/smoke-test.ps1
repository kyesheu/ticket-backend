# =============================================
# Ticket API v2.x 冒烟测试 (PowerShell)
# 用法: .\scripts\ticket\v2.x\smoke-test.ps1
# =============================================

$ErrorActionPreference = "Continue"
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
. (Join-Path $RepoRoot "scripts\ticket\_lib\Smoke-Bootstrap.ps1")
$BaseUrl = "http://localhost:8080"
$StartedProcesses = @()
$Pass = 0
$Fail = 0
$Skip = 0

trap {
    Stop-SmokeStartedProcesses
    throw
}

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

# ============ 启动项目 ============
Write-Host ""
Write-Host "[S] Ensure Java backend" -ForegroundColor Cyan
if (-not (Ensure-SmokeJavaBackend -RepoRoot $RepoRoot -BaseUrl $BaseUrl)) {
    Write-Host "  Logs: $RepoRoot\logs\smoke-java-backend.log" -ForegroundColor Yellow
    Stop-SmokeStartedProcesses
    exit 1
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

# ============ v2.0 ~ v2.3 ============
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


# ============ 结果 ============
Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  Result: $Pass passed, $Fail failed, $Skip skipped" -ForegroundColor $(if ($Fail -eq 0) { "Green" } else { "Red" })
Write-Host "==========================================" -ForegroundColor Cyan

Stop-SmokeStartedProcesses
if ($Fail -gt 0) { exit 1 }
