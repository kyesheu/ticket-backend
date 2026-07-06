$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8080"
$captcha = Invoke-RestMethod -Uri "$baseUrl/captchaImage" -Method Get
$loginData = @{ username = "admin"; password = "admin123" }
if ($captcha.captchaEnabled) {
    $rawCode = (docker exec redis redis-cli --raw GET "captcha_codes:$($captcha.uuid)").Trim()
    try { $captchaCode = $rawCode | ConvertFrom-Json } catch { $captchaCode = $rawCode.Trim('"') }
    $loginData.code = [string]$captchaCode; $loginData.uuid = $captcha.uuid
}
$login = Invoke-RestMethod -Uri "$baseUrl/login" -Method Post `
    -Body ($loginData | ConvertTo-Json) -ContentType "application/json"
$headers = @{ Authorization = "Bearer $($login.token)" }
$suffix = [DateTimeOffset]::Now.ToUnixTimeSeconds()
$fieldKey = "ROUTE_FIELD_$suffix"
$field = @{
    categoryId = 6; fieldKey = $fieldKey; fieldName = "Route field"; fieldType = "TEXT"
    requiredFlag = "0"; maxLength = 20; sortOrder = 100; status = "0"; options = @()
}
$fieldResult = Invoke-RestMethod -Uri "$baseUrl/ticket/custom-field" -Headers $headers -Method Post `
    -Body ($field | ConvertTo-Json -Depth 10) -ContentType "application/json"
if ($fieldResult.code -ne 200) { throw "create route field failed: $($fieldResult.msg)" }

$workflowKey = "CUSTOM_ROUTE_$suffix"
$definition = @{
    workflowKey = $workflowKey; workflowName = "Custom route smoke $suffix"
    nodes = @(
        @{ nodeKey = "START"; nodeName = "Start"; nodeType = "START"; sortOrder = 1 },
        @{ nodeKey = "MATCH"; nodeName = "Match"; nodeType = "PROCESS"; assigneeType = "ROLE"; assigneeValue = 1; sortOrder = 2 },
        @{ nodeKey = "DEFAULT"; nodeName = "Default"; nodeType = "PROCESS"; assigneeType = "ROLE"; assigneeValue = 1; sortOrder = 3 },
        @{ nodeKey = "END"; nodeName = "End"; nodeType = "END"; sortOrder = 4 }
    )
    transitions = @(
        @{ sourceNodeKey = "START"; targetNodeKey = "MATCH"; conditionField = "CUSTOM_FIELD";
           conditionKey = $fieldKey; conditionOperator = "EQ"; conditionValue = "ROUTE"; defaultTransition = "0"; sortOrder = 1 },
        @{ sourceNodeKey = "START"; targetNodeKey = "DEFAULT"; defaultTransition = "1"; sortOrder = 2 },
        @{ sourceNodeKey = "MATCH"; targetNodeKey = "END"; defaultTransition = "1"; sortOrder = 1 },
        @{ sourceNodeKey = "DEFAULT"; targetNodeKey = "END"; defaultTransition = "1"; sortOrder = 1 }
    )
}
$created = Invoke-RestMethod -Uri "$baseUrl/ticket/workflow" -Headers $headers -Method Post `
    -Body ($definition | ConvertTo-Json -Depth 10) -ContentType "application/json"
if ($created.code -ne 200) { throw "create workflow failed: $($created.msg)" }
$published = Invoke-RestMethod -Uri "$baseUrl/ticket/workflow/$($created.data)/publish" -Headers $headers -Method Put
if ($published.code -ne 200) { throw "publish workflow failed: $($published.msg)" }

$mysqlPassword = (docker exec mysql printenv MYSQL_ROOT_PASSWORD).Trim()
$oldWorkflowKey = (docker exec mysql mysql -uroot "-p$mysqlPassword" -N ticket_backend `
    -e "SELECT COALESCE(NULLIF(workflow_key,''),'STANDARD') FROM ticket_category WHERE category_id=6").Trim()
try {
    $bind = @{ categoryId = 6; workflowKey = $workflowKey }
    $bound = Invoke-RestMethod -Uri "$baseUrl/ticket/workflow/bind-category" -Headers $headers -Method Put `
        -Body ($bind | ConvertTo-Json) -ContentType "application/json"
    if ($bound.code -ne 200) { throw "bind workflow failed: $($bound.msg)" }

    $form = Invoke-RestMethod -Uri "$baseUrl/ticket/custom-field/form/6" -Headers $headers -Method Get
    $inputs = @(@{ fieldKey = $fieldKey; value = "ROUTE" })
    foreach ($item in $form.data) {
        if ($item.fieldKey -eq $fieldKey -or $item.requiredFlag -ne "1" -or $item.defaultValue) { continue }
        $sample = switch ($item.fieldType) {
            "TEXT" { "smoke" }; "NUMBER" { 1 }; "DATE" { "2026-07-03" }
            "DATETIME" { "2026-07-03 10:20:30" }; "BOOLEAN" { $true }
            "SINGLE_SELECT" { $item.options[0].optionValue }; "MULTI_SELECT" { @($item.options[0].optionValue) }
        }
        $inputs += @{ fieldKey = $item.fieldKey; value = $sample }
    }
    $ticket = @{ title = "Custom route smoke $suffix"; categoryId = 6; priority = "MEDIUM"; customFields = $inputs }
    $ticketResult = Invoke-RestMethod -Uri "$baseUrl/ticket" -Headers $headers -Method Post `
        -Body ($ticket | ConvertTo-Json -Depth 10) -ContentType "application/json"
    if ($ticketResult.code -ne 200) { throw "create routed ticket failed: $($ticketResult.msg)" }
    $node = (docker exec mysql mysql -uroot "-p$mysqlPassword" -N ticket_backend `
        -e "SELECT current_node_key FROM ticket_workflow_instance WHERE ticket_id=$($ticketResult.data)").Trim()
    if ($node -ne "MATCH") { throw "custom field route expected MATCH but was $node" }
    Write-Output "custom field workflow smoke passed: ticketId=$($ticketResult.data) workflowKey=$workflowKey"
} finally {
    $restore = @{ categoryId = 6; workflowKey = $oldWorkflowKey }
    Invoke-RestMethod -Uri "$baseUrl/ticket/workflow/bind-category" -Headers $headers -Method Put `
        -Body ($restore | ConvertTo-Json) -ContentType "application/json" | Out-Null
}
