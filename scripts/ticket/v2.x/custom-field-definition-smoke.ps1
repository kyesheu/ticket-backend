$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8080"
$captcha = Invoke-RestMethod -Uri "$baseUrl/captchaImage" -Method Get
$loginData = @{ username = "admin"; password = "admin123" }
if ($captcha.captchaEnabled) {
    $rawCode = (docker exec redis redis-cli --raw GET "captcha_codes:$($captcha.uuid)").Trim()
    try { $captchaCode = $rawCode | ConvertFrom-Json } catch { $captchaCode = $rawCode.Trim('"') }
    $loginData.code = [string]$captchaCode; $loginData.uuid = $captcha.uuid
}
$login = Invoke-RestMethod -Uri "$baseUrl/login" -Method Post -Body ($loginData | ConvertTo-Json) -ContentType "application/json"
$headers = @{ Authorization = "Bearer $($login.token)" }
$suffix = [DateTimeOffset]::Now.ToUnixTimeSeconds()
$fieldKey = "SMOKE_SELECT_$suffix"
$bodyObject = @{
    categoryId = 6; fieldKey = $fieldKey; fieldName = "Smoke select"; fieldType = "SINGLE_SELECT"
    requiredFlag = "1"; sortOrder = 10; status = "0"
    options = @(
        @{ optionValue = "A"; optionLabel = "Alpha"; sortOrder = 1; status = "0" },
        @{ optionValue = "B"; optionLabel = "Beta"; sortOrder = 2; status = "0" }
    )
}
$created = Invoke-RestMethod -Uri "$baseUrl/ticket/custom-field" -Headers $headers -Method Post `
    -Body ($bodyObject | ConvertTo-Json -Depth 10) -ContentType "application/json"
if ($created.code -ne 200 -or -not $created.data) { throw "create field failed: $($created.msg)" }
$fieldId = [long]$created.data
$detail = Invoke-RestMethod -Uri "$baseUrl/ticket/custom-field/$fieldId" -Headers $headers -Method Get
if ($detail.code -ne 200 -or $detail.data.options.Count -ne 2) { throw "field detail failed" }
$list = Invoke-RestMethod -Uri "$baseUrl/ticket/custom-field/list/6" -Headers $headers -Method Get
if ($list.code -ne 200 -or -not ($list.data.fieldId -contains $fieldId)) { throw "field list failed" }

$bodyObject.options[0].optionId = $detail.data.options[0].optionId
$bodyObject.options[0].optionLabel = "Alpha updated"
$bodyObject.options[1].optionId = $detail.data.options[1].optionId
$updated = Invoke-RestMethod -Uri "$baseUrl/ticket/custom-field/$fieldId" -Headers $headers -Method Put `
    -Body ($bodyObject | ConvertTo-Json -Depth 10) -ContentType "application/json"
if ($updated.code -ne 200) { throw "update field failed: $($updated.msg)" }

$duplicate = Invoke-RestMethod -Uri "$baseUrl/ticket/custom-field" -Headers $headers -Method Post `
    -Body ($bodyObject | ConvertTo-Json -Depth 10) -ContentType "application/json"
if ($duplicate.code -eq 200) { throw "duplicate field key should fail" }

$bodyObject.fieldType = "TEXT"
$immutable = Invoke-RestMethod -Uri "$baseUrl/ticket/custom-field/$fieldId" -Headers $headers -Method Put `
    -Body ($bodyObject | ConvertTo-Json -Depth 10) -ContentType "application/json"
if ($immutable.code -eq 200) { throw "field type mutation should fail" }

Write-Output "custom field definition smoke passed: fieldId=$fieldId fieldKey=$fieldKey"
