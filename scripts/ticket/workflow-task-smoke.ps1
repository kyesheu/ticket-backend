$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8080"
$mysqlPassword = (docker exec mysql printenv MYSQL_ROOT_PASSWORD).Trim()

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

function New-SmokeTicket($name) {
    $body = @{ title = "$name $([DateTimeOffset]::Now.ToUnixTimeMilliseconds())";
               content = "workflow task smoke"; categoryId = 6; priority = "HIGH" } | ConvertTo-Json
    $response = Invoke-RestMethod -Uri "$baseUrl/ticket" -Headers $headers -Method Post `
        -Body $body -ContentType "application/json"
    if ($response.code -ne 200) { throw "create ticket failed: $($response.msg)" }
    return [long]$response.data
}

function Get-PendingTask($ticketId) {
    $sql = "SELECT t.task_id FROM ticket_workflow_task t INNER JOIN ticket_workflow_instance i " +
           "ON i.instance_id=t.instance_id WHERE i.ticket_id=$ticketId AND t.task_status='PENDING' LIMIT 1;"
    return [long](docker exec mysql mysql -uroot "-p$mysqlPassword" -N ticket_backend -e $sql).Trim()
}

$ticketId = New-SmokeTicket "Workflow complete"
$assignTask = Get-PendingTask $ticketId
$pending = Invoke-RestMethod -Uri "$baseUrl/ticket/workflow/task/list" -Headers $headers -Method Get
if ($pending.code -ne 200 -or -not ($pending.data.taskId -contains $assignTask)) {
    throw "pending task list does not contain task $assignTask"
}
$detail = Invoke-RestMethod -Uri "$baseUrl/ticket/workflow/task/$assignTask" -Headers $headers -Method Get
if ($detail.code -ne 200 -or $detail.data.ticketId -ne $ticketId) { throw "task detail failed" }
$assignBody = @{ assigneeId = 1; comment = "assign" } | ConvertTo-Json
$response = Invoke-RestMethod -Uri "$baseUrl/ticket/workflow/task/$assignTask/complete" `
    -Headers $headers -Method Put -Body $assignBody -ContentType "application/json"
if ($response.code -ne 200) { throw "assign task failed: $($response.msg)" }

$repeated = Invoke-RestMethod -Uri "$baseUrl/ticket/workflow/task/$assignTask/complete" `
    -Headers $headers -Method Put -Body $assignBody -ContentType "application/json"
if ($repeated.code -eq 200) { throw "repeated completion should fail" }

$processTask = Get-PendingTask $ticketId
$actionBody = @{ comment = "process" } | ConvertTo-Json
$response = Invoke-RestMethod -Uri "$baseUrl/ticket/workflow/task/$processTask/complete" `
    -Headers $headers -Method Put -Body $actionBody -ContentType "application/json"
if ($response.code -ne 200) { throw "process task failed: $($response.msg)" }

$confirmTask = Get-PendingTask $ticketId
$response = Invoke-RestMethod -Uri "$baseUrl/ticket/workflow/task/$confirmTask/complete" `
    -Headers $headers -Method Put -Body $actionBody -ContentType "application/json"
if ($response.code -ne 200) { throw "confirm task failed: $($response.msg)" }

$statusSql = "SELECT CONCAT(t.status,',',i.workflow_status,',',COUNT(w.task_id)) FROM ticket t " +
             "INNER JOIN ticket_workflow_instance i ON i.ticket_id=t.ticket_id " +
             "LEFT JOIN ticket_workflow_task w ON w.instance_id=i.instance_id AND w.task_status='PENDING' " +
             "WHERE t.ticket_id=$ticketId GROUP BY t.status,i.workflow_status;"
$finalState = (docker exec mysql mysql -uroot "-p$mysqlPassword" -N ticket_backend -e $statusSql).Trim()
if ($finalState -ne "CLOSED,COMPLETED,0") { throw "unexpected completed state: $finalState" }

$cancelTicketId = New-SmokeTicket "Workflow cancel"
$response = Invoke-RestMethod -Uri "$baseUrl/ticket/workflow/task/ticket/$cancelTicketId/cancel" `
    -Headers $headers -Method Put -Body (@{ comment = "cancel" } | ConvertTo-Json) -ContentType "application/json"
if ($response.code -ne 200) { throw "cancel failed: $($response.msg)" }

$terminateTicketId = New-SmokeTicket "Workflow terminate"
$response = Invoke-RestMethod -Uri "$baseUrl/ticket/workflow/task/ticket/$terminateTicketId/terminate" `
    -Headers $headers -Method Put -Body (@{ comment = "terminate" } | ConvertTo-Json) -ContentType "application/json"
if ($response.code -ne 200) { throw "terminate failed: $($response.msg)" }

Write-Output "workflow task smoke passed: completed=$ticketId cancelled=$cancelTicketId terminated=$terminateTicketId"
