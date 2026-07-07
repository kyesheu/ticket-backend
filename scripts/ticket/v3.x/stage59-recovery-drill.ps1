# =============================================
# v3.3 stage 59 recovery and degradation drill
# Usage:
#   powershell scripts/ticket/v3.x/stage59-recovery-drill.ps1
#   powershell scripts/ticket/v3.x/stage59-recovery-drill.ps1 -CheckAiDegradation
# =============================================

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ElasticsearchUrl = "http://localhost:9200",
    [switch]$CheckAiDegradation
)

$ErrorActionPreference = "Stop"
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
. (Join-Path $RepoRoot "scripts\ticket\_lib\Smoke-Bootstrap.ps1")

$StartedProcesses = @()
$Pass = 0
$Fail = 0
$BackupDir = Join-Path $RepoRoot "backups\smoke"
$CoreTables = @(
    "ticket",
    "ticket_comment",
    "ticket_operation_log",
    "ticket_notification",
    "ticket_search_event",
    "ticket_ai_triage_suggestion",
    "ticket_ai_feedback"
)

trap {
    Stop-SmokeStartedProcesses
    throw
}

function Write-Pass($Desc) {
    Write-Host "  [PASS] $Desc" -ForegroundColor Green
    $script:Pass++
}

function Write-Fail($Desc, $Reason) {
    Write-Host "  [FAIL] $Desc - $Reason" -ForegroundColor Red
    $script:Fail++
}

function Read-SmokeEnv {
    $envFile = Join-Path $RepoRoot ".env"
    if (-not (Test-Path -LiteralPath $envFile)) {
        throw ".env not found: $envFile"
    }
    $variables = @{}
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^([^#=]+)=(.*)$') {
            $variables[$matches[1].Trim()] = $matches[2].Trim().Trim('"')
        }
    }
    return $variables
}

function Invoke-MySql {
    param(
        [hashtable]$Variables,
        [string]$Sql,
        [string]$Database = "ticket_backend"
    )
    return docker exec -e "MYSQL_PWD=$($Variables['DB_PASSWORD'])" mysql mysql "-u$($Variables['DB_USERNAME'])" `
        -N -B -D $Database -e $Sql
}

function Invoke-MySqlServer {
    param(
        [hashtable]$Variables,
        [string]$Sql
    )
    return docker exec -e "MYSQL_PWD=$($Variables['DB_PASSWORD'])" mysql mysql "-u$($Variables['DB_USERNAME'])" -N -B -e $Sql
}

function Login-Admin {
    $captcha = Invoke-RestMethod -Uri "$BaseUrl/captchaImage" -Method Get
    $login = @{ username = "admin"; password = "admin123" }
    if ($captcha.captchaEnabled) {
        $raw = (docker exec redis redis-cli --raw GET "captcha_codes:$($captcha.uuid)").Trim()
        try { $code = $raw | ConvertFrom-Json } catch { $code = $raw.Trim('"') }
        $login.code = [string]$code
        $login.uuid = $captcha.uuid
    }
    return (Invoke-RestMethod -Uri "$BaseUrl/login" -Method Post `
        -Body ($login | ConvertTo-Json) -ContentType "application/json").token
}

function Get-TableCounts {
    param(
        [hashtable]$Variables,
        [string]$Database
    )
    $counts = @{}
    foreach ($table in $CoreTables) {
        $exists = Invoke-MySqlServer $Variables `
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$Database' AND table_name='$table';"
        if ([int]$exists -eq 1) {
            $counts[$table] = [long](Invoke-MySql $Variables "SELECT COUNT(*) FROM $table;" $Database)
        }
    }
    return $counts
}

function Assert-CountsEqual {
    param(
        [hashtable]$Expected,
        [hashtable]$Actual
    )
    foreach ($table in $Expected.Keys) {
        if (-not $Actual.ContainsKey($table)) {
            throw "Restored database missing table: $table"
        }
        if ($Expected[$table] -ne $Actual[$table]) {
            throw "Table count mismatch: $table expected=$($Expected[$table]) actual=$($Actual[$table])"
        }
    }
}

function Invoke-BackupRestoreDrill {
    param([hashtable]$Variables)

    New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null
    $timestamp = Get-Date -Format "yyyyMMddHHmmss"
    $backupName = "ticket_backend_$timestamp.sql"
    $backupPath = Join-Path $BackupDir $backupName
    $containerBackupPath = "/tmp/$backupName"
    $restoreDatabase = "ticket_backend_restore_$timestamp"

    $beforeCounts = Get-TableCounts $Variables "ticket_backend"
    docker exec -e "MYSQL_PWD=$($Variables['DB_PASSWORD'])" mysql sh -c `
        "mysqldump -u'$($Variables['DB_USERNAME'])' --single-transaction --routines --triggers ticket_backend > '$containerBackupPath'"
    if ($LASTEXITCODE -ne 0) {
        throw "mysqldump failed"
    }
    docker cp "mysql:$containerBackupPath" $backupPath | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "backup file copy failed"
    }
    if (-not (Test-Path -LiteralPath $backupPath)) {
        throw "backup file was not created: $backupPath"
    }

    Invoke-MySqlServer $Variables "CREATE DATABASE $restoreDatabase DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" | Out-Null
    docker exec -e "MYSQL_PWD=$($Variables['DB_PASSWORD'])" mysql sh -c `
        "mysql -u'$($Variables['DB_USERNAME'])' '$restoreDatabase' < '$containerBackupPath'"
    if ($LASTEXITCODE -ne 0) {
        throw "restore import failed"
    }
    $afterCounts = Get-TableCounts $Variables $restoreDatabase
    Assert-CountsEqual $beforeCounts $afterCounts
    Write-Pass "MySQL backup restored into $restoreDatabase and core table counts match"
    Write-Host "  Backup file: $backupPath" -ForegroundColor DarkGray
}

function Invoke-SearchRebuildDrill {
    param([hashtable]$Headers)

    $started = Invoke-RestMethod -Uri "$BaseUrl/ticket/search/rebuild" -Method Post -Headers $Headers
    if ($started.code -ne 200) {
        throw "Search rebuild failed to start: $($started.msg)"
    }
    $status = $null
    foreach ($attempt in 1..90) {
        Start-Sleep -Milliseconds 500
        $status = Invoke-RestMethod -Uri "$BaseUrl/ticket/search/rebuild" -Headers $Headers
        if ($status.data.rebuildStatus -in @("SUCCEEDED", "FAILED")) { break }
    }
    if ($status.data.rebuildStatus -ne "SUCCEEDED") {
        throw "Search rebuild did not succeed: $($status.data.rebuildStatus)"
    }
    if ($status.data.processedCount -ne $status.data.totalCount) {
        throw "Search rebuild progress count is inconsistent"
    }
    $alias = Invoke-RestMethod -Uri "$ElasticsearchUrl/_alias/ticket-search"
    $backingIndices = @($alias.PSObject.Properties.Name)
    if ($backingIndices.Count -ne 1 -or $backingIndices[0] -notlike "ticket-search-v*") {
        throw "Search alias does not point to one versioned index"
    }
    $indexCount = (Invoke-RestMethod -Uri "$ElasticsearchUrl/ticket-search/_count").count
    if ($indexCount -ne $status.data.totalCount) {
        throw "Search alias document count mismatch: index=$indexCount expected=$($status.data.totalCount)"
    }
    Write-Pass "Elasticsearch rebuild succeeded: index=$($backingIndices[0]) count=$indexCount"
}

function Invoke-EventCompensationDrill {
    param(
        [hashtable]$Variables,
        [hashtable]$Headers
    )

    $eventId = (Invoke-MySql $Variables @"
INSERT INTO ticket_search_event
    (ticket_id, event_type, event_status, retry_count, error_message, create_time, update_time)
SELECT MIN(ticket_id), 'UPSERT', 'FAILED', 3, 'stage59 drill', NOW(), NOW() FROM ticket;
SELECT LAST_INSERT_ID();
"@ | Select-Object -Last 1).Trim()
    if (-not $eventId) {
        throw "failed event was not inserted"
    }
    $retried = Invoke-RestMethod -Uri "$BaseUrl/ticket/search/events/retry" -Method Post -Headers $Headers
    if ($retried.code -ne 200 -or $retried.data -lt 1) {
        throw "failed search event was not retried"
    }
    $eventStatus = (Invoke-MySql $Variables "SELECT event_status FROM ticket_search_event WHERE event_id = $eventId;").Trim()
    if ($eventStatus -ne "PENDING") {
        throw "retried event status mismatch: $eventStatus"
    }
    Write-Pass "Failed search event compensation reset eventId=$eventId to PENDING"
}

function Invoke-AiDegradationCheck {
    $health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health/readiness" -Method Get
    $json = $health | ConvertTo-Json -Depth 20 -Compress
    if ($json -notmatch "ticketAi") {
        throw "readiness response does not include ticketAi details; set MANAGEMENT_HEALTH_SHOW_DETAILS=always for this drill"
    }
    if ($json -notmatch "DEGRADED|DISABLED|UP") {
        throw "ticketAi health status was not reported"
    }
    Write-Pass "AI dependency health is visible in readiness response"
}

Write-Host ""
Write-Host "[S] Ensure Java backend" -ForegroundColor Cyan
if (-not (Ensure-SmokeJavaBackend -RepoRoot $RepoRoot -BaseUrl $BaseUrl)) {
    Write-Fail "Java backend startup" "$RepoRoot\logs\smoke-java-backend.log"
    Stop-SmokeStartedProcesses
    exit 1
}

$Variables = Read-SmokeEnv
$Token = Login-Admin
if (-not $Token) {
    throw "Admin login failed"
}
$Headers = @{ Authorization = "Bearer $Token" }

Write-Host ""
Write-Host "[1] MySQL backup and restore drill" -ForegroundColor Cyan
try { Invoke-BackupRestoreDrill $Variables } catch { Write-Fail "MySQL backup and restore drill" $_.Exception.Message }

Write-Host ""
Write-Host "[2] Elasticsearch rebuild drill" -ForegroundColor Cyan
try { Invoke-SearchRebuildDrill $Headers } catch { Write-Fail "Elasticsearch rebuild drill" $_.Exception.Message }

Write-Host ""
Write-Host "[3] Failed event compensation drill" -ForegroundColor Cyan
try { Invoke-EventCompensationDrill $Variables $Headers } catch { Write-Fail "Failed event compensation drill" $_.Exception.Message }

if ($CheckAiDegradation) {
    Write-Host ""
    Write-Host "[4] AI degradation visibility check" -ForegroundColor Cyan
    try { Invoke-AiDegradationCheck } catch { Write-Fail "AI degradation visibility check" $_.Exception.Message }
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  Result: $Pass passed, $Fail failed" -ForegroundColor $(if ($Fail -eq 0) { "Green" } else { "Red" })
Write-Host "==========================================" -ForegroundColor Cyan

Stop-SmokeStartedProcesses
if ($Fail -gt 0) { exit 1 }
