$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8080"

$captcha = Invoke-RestMethod -Uri "$baseUrl/captchaImage" -Method Get
$loginData = @{ username = "admin"; password = "admin123" }
if ($captcha.captchaEnabled) {
    $rawCode = (docker exec redis redis-cli --raw GET "captcha_codes:$($captcha.uuid)").Trim()
    try { $captchaCode = $rawCode | ConvertFrom-Json } catch { $captchaCode = $rawCode.Trim('"') }
    $loginData.code = [string]$captchaCode
    $loginData.uuid = $captcha.uuid
}
$login = Invoke-RestMethod -Uri "$baseUrl/login" -Method Post `
    -Body ($loginData | ConvertTo-Json) -ContentType "application/json"
$headers = @{ Authorization = "Bearer $($login.token)" }

$list = Invoke-RestMethod -Uri "$baseUrl/ticket/workflow/list" -Headers $headers -Method Get
if ($list.code -ne 200) { throw "workflow list failed: $($list.msg)" }

$suffix = [DateTimeOffset]::Now.ToUnixTimeSeconds()
$workflowKey = "SMOKE_$suffix"
$definition = @{
    workflowKey = $workflowKey
    workflowName = "Smoke workflow $suffix"
    nodes = @(
        @{ nodeKey = "START"; nodeName = "Start"; nodeType = "START"; sortOrder = 1 },
        @{ nodeKey = "PROCESS"; nodeName = "Process"; nodeType = "PROCESS";
           assigneeType = "ROLE"; assigneeValue = 1; sortOrder = 2 },
        @{ nodeKey = "END"; nodeName = "End"; nodeType = "END"; sortOrder = 3 }
    )
    transitions = @(
        @{ sourceNodeKey = "START"; targetNodeKey = "PROCESS"; defaultTransition = "1"; sortOrder = 1 },
        @{ sourceNodeKey = "PROCESS"; targetNodeKey = "END"; defaultTransition = "1"; sortOrder = 1 }
    )
}
$created = Invoke-RestMethod -Uri "$baseUrl/ticket/workflow" -Headers $headers -Method Post `
    -Body ($definition | ConvertTo-Json -Depth 10) -ContentType "application/json"
if ($created.code -ne 200 -or -not $created.data) { throw "create draft failed: $($created.msg)" }
$definitionId = $created.data

$detail = Invoke-RestMethod -Uri "$baseUrl/ticket/workflow/$definitionId" -Headers $headers -Method Get
if ($detail.code -ne 200 -or $detail.data.nodes.Count -ne 3) { throw "definition detail failed" }

$published = Invoke-RestMethod -Uri "$baseUrl/ticket/workflow/$definitionId/publish" `
    -Headers $headers -Method Put
if ($published.code -ne 200) { throw "publish failed: $($published.msg)" }

$repeated = Invoke-RestMethod -Uri "$baseUrl/ticket/workflow/$definitionId/publish" `
    -Headers $headers -Method Put
if ($repeated.code -eq 200) { throw "repeated publish should fail" }

$binding = @{ categoryId = 1; workflowKey = "STANDARD" } | ConvertTo-Json
$bound = Invoke-RestMethod -Uri "$baseUrl/ticket/workflow/bind-category" -Headers $headers -Method Put `
    -Body $binding -ContentType "application/json"
if ($bound.code -ne 200) { throw "bind category failed: $($bound.msg)" }

Write-Output "workflow definition smoke passed: definitionId=$definitionId, workflowKey=$workflowKey"
