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

$suffix = [DateTimeOffset]::Now.ToUnixTimeSeconds()
$body = @{
    title = "Workflow engine smoke $suffix"
    content = "Verify workflow instance startup"
    categoryId = 6
    priority = "HIGH"
} | ConvertTo-Json
$created = Invoke-RestMethod -Uri "$baseUrl/ticket" -Headers $headers -Method Post `
    -Body $body -ContentType "application/json"
if ($created.code -ne 200 -or -not $created.data) { throw "ticket creation failed: $($created.msg)" }
$ticketId = [long]$created.data

$sql = "SELECT CONCAT(i.instance_id,',',i.definition_id,',',i.current_node_key,',',COUNT(t.task_id)) " +
       "FROM ticket_workflow_instance i LEFT JOIN ticket_workflow_task t " +
       "ON t.instance_id=i.instance_id AND t.task_status='PENDING' " +
       "WHERE i.ticket_id=$ticketId GROUP BY i.instance_id,i.definition_id,i.current_node_key;"
$mysqlPassword = (docker exec mysql printenv MYSQL_ROOT_PASSWORD).Trim()
$result = (docker exec mysql mysql -uroot "-p$mysqlPassword" -N ticket_backend -e $sql).Trim()
$parts = $result.Split(',')
if ($parts.Count -ne 4 -or $parts[2] -ne "ASSIGN" -or $parts[3] -ne "1") {
    throw "workflow startup assertion failed: $result"
}

Write-Output "workflow engine smoke passed: ticketId=$ticketId, instanceId=$($parts[0]), definitionId=$($parts[1])"
