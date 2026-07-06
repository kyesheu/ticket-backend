$ErrorActionPreference = "Stop"
$BaseUrl = "http://localhost:8080"

function Login-Admin {
    $captcha = Invoke-RestMethod -Uri "$BaseUrl/captchaImage"
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

function Invoke-ProjectSql([string]$Sql) {
    $variables = @{}
    Get-Content (Join-Path $PSScriptRoot "..\..\..\.env") | ForEach-Object {
        if ($_ -match '^([^#=]+)=(.*)$') { $variables[$matches[1]] = $matches[2] }
    }
    return docker exec mysql mysql "-u$($variables['DB_USERNAME'])" `
        "-p$($variables['DB_PASSWORD'])" -N -B -D ticket_backend -e $Sql
}

$token = Login-Admin
if (-not $token) { throw "Admin login failed" }
$headers = @{ Authorization = "Bearer $token" }

$started = Invoke-RestMethod -Uri "$BaseUrl/ticket/search/rebuild" -Method Post -Headers $headers
if ($started.code -ne 200) { throw "Search rebuild failed to start" }

$status = $null
foreach ($attempt in 1..60) {
    Start-Sleep -Milliseconds 500
    $status = Invoke-RestMethod -Uri "$BaseUrl/ticket/search/rebuild" -Headers $headers
    if ($status.data.rebuildStatus -in @("SUCCEEDED", "FAILED")) { break }
}
if ($status.data.rebuildStatus -ne "SUCCEEDED") {
    throw "Search rebuild did not succeed: $($status.data.rebuildStatus)"
}
if ($status.data.processedCount -ne $status.data.totalCount) {
    throw "Search rebuild progress count is inconsistent"
}

$alias = Invoke-RestMethod -Uri "http://localhost:9200/_alias/ticket-search"
$backingIndices = @($alias.PSObject.Properties.Name)
if ($backingIndices.Count -ne 1 -or $backingIndices[0] -notlike "ticket-search-v*") {
    throw "Search alias does not point to one versioned index"
}
$indexCount = (Invoke-RestMethod -Uri "http://localhost:9200/ticket-search/_count").count
if ($indexCount -ne $status.data.totalCount) { throw "Search alias document count is inconsistent" }

$eventId = (Invoke-ProjectSql @"
INSERT INTO ticket_search_event
    (ticket_id, event_type, event_status, retry_count, error_message, create_time, update_time)
SELECT MIN(ticket_id), 'UPSERT', 'FAILED', 3, 'smoke failure', NOW(), NOW() FROM ticket;
SELECT LAST_INSERT_ID();
"@ | Select-Object -Last 1).Trim()
$retried = Invoke-RestMethod -Uri "$BaseUrl/ticket/search/events/retry" -Method Post -Headers $headers
if ($retried.code -ne 200 -or $retried.data -lt 1) { throw "Failed search events were not retried" }
$eventStatus = (Invoke-ProjectSql "SELECT event_status FROM ticket_search_event WHERE event_id = $eventId;").Trim()
if ($eventStatus -ne "PENDING") { throw "Retried search event was not reset to PENDING" }

Write-Output "SEARCH_REBUILD_SMOKE_PASS index=$($backingIndices[0]) count=$indexCount retried=$($retried.data)"
