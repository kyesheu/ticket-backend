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

function Get-RequiredFields([long]$CategoryId, $Headers) {
    $form = Invoke-RestMethod -Uri "$BaseUrl/ticket/custom-field/form/$CategoryId" -Headers $Headers
    $inputs = @()
    foreach ($field in $form.data) {
        if ($field.requiredFlag -ne "1" -or $field.defaultValue) { continue }
        $value = switch ($field.fieldType) {
            "TEXT" { "search smoke" }
            "NUMBER" { if ($field.minNumber) { $field.minNumber } else { 1 } }
            "DATE" { "2026-07-03" }
            "DATETIME" { "2026-07-03 10:20:30" }
            "BOOLEAN" { $true }
            "SINGLE_SELECT" { $field.options[0].optionValue }
            "MULTI_SELECT" { @($field.options[0].optionValue) }
        }
        $inputs += @{ fieldKey = $field.fieldKey; value = $value }
    }
    return $inputs
}

function Invoke-SearchDispatch($Headers) {
    $jobs = Invoke-RestMethod -Uri "$BaseUrl/monitor/job/list?jobGroup=TICKET&pageNum=1&pageSize=100" `
        -Headers $Headers
    $job = @($jobs.rows | Where-Object { $_.invokeTarget -eq "ticketSearchTask.dispatch" }) | Select-Object -First 1
    if (-not $job) { throw "Search dispatcher job ticketSearchTask.dispatch was not found" }
    return Invoke-RestMethod -Uri "$BaseUrl/monitor/job/run" -Method Put -Headers $Headers `
        -Body (@{ jobId = $job.jobId; jobGroup = $job.jobGroup } | ConvertTo-Json) -ContentType "application/json"
}

$token = Login-Admin
if (-not $token) { throw "Admin login failed" }
$headers = @{ Authorization = "Bearer $token" }
$suffix = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
$keyword = "V23SEARCH$suffix"
$customFields = @(Get-RequiredFields 6 $headers)

foreach ($number in 1..2) {
    $body = @{
        title = "$keyword result $number"
        content = "<script>alert(1)</script> searchable $keyword"
        categoryId = 6
        priority = "HIGH"
        customFields = $customFields
    } | ConvertTo-Json -Depth 10
    $created = Invoke-RestMethod -Uri "$BaseUrl/ticket" -Method Post -Headers $headers `
        -Body $body -ContentType "application/json"
    if ($created.code -ne 200) { throw "Ticket creation failed" }
}

$run = Invoke-SearchDispatch $headers
if ($run.code -ne 200) { throw "Search dispatcher job failed to start" }

$page = $null
foreach ($attempt in 1..20) {
    Start-Sleep -Milliseconds 500
    try {
        $page = Invoke-RestMethod -Uri "$BaseUrl/ticket/search?keyword=$keyword&status=NEW&pageSize=1" `
            -Headers $headers
        if ($page.data.items.Count -eq 1 -and $page.data.hasMore -and $page.data.nextCursor) { break }
    } catch {}
}
if ($page.data.items.Count -ne 1) { throw "Search result was not indexed" }
if (-not $page.data.hasMore -or -not $page.data.nextCursor) { throw "Search cursor was not returned" }
$json = $page | ConvertTo-Json -Depth 10 -Compress
if ($json -match '<script>') { throw "Unsafe highlight was returned" }
if ($json -notmatch '<em>') { throw "Server highlight marker was not returned" }

$combined = Invoke-RestMethod `
    -Uri "$BaseUrl/ticket/search?keyword=$keyword&status=NEW&priority=HIGH&categoryId=6&creatorId=1&pageSize=10" `
    -Headers $headers
if ($combined.data.items.Count -ne 2) { throw "Combined search filters did not return both tickets" }
$excluded = Invoke-RestMethod `
    -Uri "$BaseUrl/ticket/search?keyword=$keyword&status=NEW&priority=LOW&categoryId=6&pageSize=10" `
    -Headers $headers
if ($excluded.data.items.Count -ne 0) { throw "Combined search filters returned an excluded ticket" }

$cursor = [uri]::EscapeDataString($page.data.nextCursor)
$next = Invoke-RestMethod -Uri "$BaseUrl/ticket/search?keyword=$keyword&status=NEW&pageSize=1&cursor=$cursor" `
    -Headers $headers
if ($next.data.items.Count -ne 1) { throw "Cursor page did not return the second result" }

$invalidPage = Invoke-RestMethod -Uri "$BaseUrl/ticket/search?keyword=$keyword&pageSize=101" -Headers $headers
if ($invalidPage.code -ne 500) { throw "Oversized page was accepted" }

$tampered = Invoke-RestMethod `
    -Uri "$BaseUrl/ticket/search?keyword=$keyword&status=NEW&pageSize=1&cursor=${cursor}x" -Headers $headers
if ($tampered.code -ne 500) { throw "Tampered cursor was accepted" }

$unauthenticated = Invoke-RestMethod -Uri "$BaseUrl/ticket/search?keyword=$keyword"
if ($unauthenticated.code -ne 401) { throw "Unauthenticated search was accepted" }

Write-Output "SEARCH_SMOKE_PASS keyword=$keyword"
