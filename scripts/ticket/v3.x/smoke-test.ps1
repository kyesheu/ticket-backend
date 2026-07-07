# =============================================
# Ticket API v3.x 冒烟测试 (PowerShell)
# 用法: .\scripts\ticket\v3.x\smoke-test.ps1
# =============================================

$ErrorActionPreference = "Continue"
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
. (Join-Path $RepoRoot "scripts\ticket\_lib\Smoke-Bootstrap.ps1")
$SmokeEnvFile = Join-Path $PSScriptRoot "smoke-env.local.ps1"
if (Test-Path -LiteralPath $SmokeEnvFile) {
    . $SmokeEnvFile
    Write-Host "Loaded smoke env: $SmokeEnvFile" -ForegroundColor DarkGray
}

function Set-DefaultEnv($Name, $Value) {
    if (-not [Environment]::GetEnvironmentVariable($Name, "Process")) {
        [Environment]::SetEnvironmentVariable($Name, $Value, "Process")
    }
}

Set-DefaultEnv "TICKET_AI_SMOKE_ENABLED" "true"
Set-DefaultEnv "TICKET_AI_ENABLED" "true"
Set-DefaultEnv "TICKET_AI_BASE_URL" "http://127.0.0.1:8090"
Set-DefaultEnv "TICKET_AI_SERVICE_TOKEN" "local-smoke-token-12345"
Set-DefaultEnv "TICKET_AI_SMOKE_MODE" "true"
Set-DefaultEnv "TICKET_AI_KNOWLEDGE_INDEX" "ticket-knowledge-smoke"
Set-DefaultEnv "TICKET_AI_TICKET_HISTORY_INDEX" "ticket-history-smoke"
Set-DefaultEnv "TICKET_AI_ELASTICSEARCH_URL" "http://127.0.0.1:9200"
Set-DefaultEnv "TICKET_AI_EMBEDDING_API_KEY" "local-smoke-embedding-key"
Set-DefaultEnv "TICKET_AI_EMBEDDING_MODEL" "local-smoke-embedding-model"

$BaseUrl = "http://localhost:8080"
$AiBaseUrl = $env:TICKET_AI_BASE_URL
$StartedProcesses = @()
$Pass = 0
$Fail = 0
$Skip = 0

function Test-HttpReady($Url) {
    try {
        Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec 3 | Out-Null
        return $true
    } catch {
        return $false
    }
}

function Wait-HttpReady($Desc, $Url, $TimeoutSeconds) {
    $Deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $Deadline) {
        if (Test-HttpReady $Url) {
            Write-Host "  [PASS] $Desc ready" -ForegroundColor Green
            return $true
        }
        Start-Sleep -Seconds 2
    }
    Write-Host "  [FAIL] $Desc not ready: $Url" -ForegroundColor Red
    $script:Fail++
    return $false
}

function Start-SmokeProcess($Desc, $WorkingDirectory, $Command, $LogFile) {
    Write-Host "  Starting $Desc" -ForegroundColor DarkGray
    $EscapedLogFile = $LogFile.Replace("'", "''")
    $WrappedCommand = "& { $Command } *>> '$EscapedLogFile'"
    $Process = Start-Process -FilePath "pwsh" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $WrappedCommand) `
        -WorkingDirectory $WorkingDirectory `
        -WindowStyle Hidden `
        -PassThru
    $script:StartedProcesses += [pscustomobject]@{
        Desc = $Desc
        Process = $Process
        LogFile = $LogFile
    }
}

function Ensure-PythonAiService() {
    if (Test-HttpReady "$AiBaseUrl/api/v1/health") {
        Write-Host "  [PASS] Python AI service already running" -ForegroundColor Green
        return $true
    }

    $PythonExe = Join-Path $RepoRoot "ai-service\.venv\Scripts\python.exe"
    if (-not (Test-Path -LiteralPath $PythonExe)) {
        Write-Host "  Installing ai-service virtualenv" -ForegroundColor DarkGray
        Push-Location (Join-Path $RepoRoot "ai-service")
        try {
            python -m venv .venv
            & $PythonExe -m pip install -e ".[test]"
        } finally {
            Pop-Location
        }
    }

    $AiLog = Join-Path $RepoRoot "logs\smoke-ai-service.log"
    New-Item -ItemType Directory -Force -Path (Split-Path $AiLog) | Out-Null
    Start-SmokeProcess "Python AI service" (Join-Path $RepoRoot "ai-service") `
        ".\.venv\Scripts\python.exe -m uvicorn ticket_ai.main:app --host 127.0.0.1 --port 8090" `
        $AiLog
    return (Wait-HttpReady "Python AI service" "$AiBaseUrl/api/v1/health" 60)
}

function Ensure-JavaService() {
    if (Test-HttpReady "$BaseUrl/captchaImage") {
        Write-Host "  [PASS] Java backend already running" -ForegroundColor Green
        return $true
    }

    $JavaLog = Join-Path $RepoRoot "logs\smoke-java-backend.log"
    New-Item -ItemType Directory -Force -Path (Split-Path $JavaLog) | Out-Null
    Write-Host "  Packaging Java backend" -ForegroundColor DarkGray
    Push-Location $RepoRoot
    try {
        mvn -pl ruoyi-admin -am package -DskipTests *>> $JavaLog
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  [FAIL] Java backend package failed, see $JavaLog" -ForegroundColor Red
            $script:Fail++
            return $false
        }
    } finally {
        Pop-Location
    }

    $JarPath = Join-Path $RepoRoot "ruoyi-admin\target\ruoyi-admin.jar"
    Start-SmokeProcess "Java backend" $RepoRoot "java -jar '$JarPath'" $JavaLog
    return (Wait-HttpReady "Java backend" "$BaseUrl/captchaImage" 120)
}

function Test-AiEndpointActive() {
    try {
        Invoke-RestMethod -Uri "$BaseUrl/ticket/ai/documents?pageNum=1&pageSize=1" -Headers $Headers -Method Get `
            -TimeoutSec 5 | Out-Null
        return $true
    } catch {
        $Text = $_.Exception.Message
        if ($Text -match "No static resource" -or $Text -match "404") {
            return $false
        }
        return $true
    }
}

function Stop-SmokeStartedProcesses() {
    foreach ($Item in $script:StartedProcesses) {
        if ($Item.Process -and -not $Item.Process.HasExited) {
            Write-Host "  Stopping $($Item.Desc)" -ForegroundColor DarkGray
            taskkill /PID $Item.Process.Id /T /F | Out-Null
        }
    }
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

# ============ 启动依赖服务 ============
Write-Host ""
Write-Host "[S] Ensure smoke services" -ForegroundColor Cyan
$ServicesReady = (Ensure-PythonAiService) -and (Ensure-JavaService)
if (-not $ServicesReady) {
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "  Result: $Pass passed, $Fail failed, $Skip skipped" -ForegroundColor Red
    Write-Host "  Logs: $RepoRoot\logs\smoke-ai-service.log, $RepoRoot\logs\smoke-java-backend.log" -ForegroundColor Yellow
    Write-Host "==========================================" -ForegroundColor Cyan
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
if (-not (Test-AiEndpointActive)) {
    Write-Host "  [FAIL] Java backend is running without AI smoke endpoints. Stop the current Java process and rerun this script so it can start Java with smoke defaults." -ForegroundColor Red
    $Fail++
    Stop-SmokeStartedProcesses
    exit 1
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

# 创建测试工单
Write-Host ""
Write-Host "[T] Create test ticket for AI smoke" -ForegroundColor Cyan
$TestBody = @{
    title = "PS-AI-Smoke-$([DateTimeOffset]::Now.ToUnixTimeSeconds())"
    content = "AI smoke test ticket"
    categoryId = 7
    priority = "MEDIUM"
} | ConvertTo-Json -Depth 10
try {
    $TestResp = Invoke-RestMethod -Uri "$BaseUrl/ticket" -Method Post -Body $TestBody -ContentType "application/json" -Headers $Headers
    $T1 = $TestResp.data
    if (-not $T1) { throw "Create ticket returned null: $($TestResp | ConvertTo-Json)" }
    Write-Host "  Ticket ID: $T1"
} catch {
    Write-Host "  [FAIL] Create test ticket: $($_.Exception.Message)" -ForegroundColor Red
    $Fail++
    $T1 = $null
}

# ============ v3.0 ~ v3.1 ============
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
    Write-Skip "v3 AI smoke" "Set TICKET_AI_SMOKE_ENABLED=true and use smoke/test AI indexes"
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
            categoryId = 7
            priority = "MEDIUM"
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
            categoryId = 7
            priority = "LOW"
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

Stop-SmokeStartedProcesses
if ($Fail -gt 0) { exit 1 }
