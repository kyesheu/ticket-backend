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
$definitions = @(
    @{ key = "V21_TEXT_$suffix"; type = "TEXT"; value = "snapshot-value"; maxLength = 20 },
    @{ key = "V21_NUMBER_$suffix"; type = "NUMBER"; value = 10.50; minNumber = 1; maxNumber = 20 },
    @{ key = "V21_DATE_$suffix"; type = "DATE"; value = "2026-07-03" },
    @{ key = "V21_DATETIME_$suffix"; type = "DATETIME"; value = "2026-07-03T10:20:30" },
    @{ key = "V21_BOOLEAN_$suffix"; type = "BOOLEAN"; value = $true },
    @{ key = "V21_SINGLE_$suffix"; type = "SINGLE_SELECT"; value = "A";
       options = @(@{ optionValue = "A"; optionLabel = "Alpha"; sortOrder = 1; status = "0" }) },
    @{ key = "V21_MULTI_$suffix"; type = "MULTI_SELECT"; value = @("A", "B");
       options = @(@{ optionValue = "A"; optionLabel = "Alpha"; sortOrder = 1; status = "0" },
                   @{ optionValue = "B"; optionLabel = "Beta"; sortOrder = 2; status = "0" }) }
)
$sortOrder = 100
$createdIds = @{}
foreach ($item in $definitions) {
    $options = @()
    if ($item.options) { $options = @($item.options) }
    $definition = @{
        categoryId = 6; fieldKey = $item.key; fieldName = "Smoke $($item.type)"; fieldType = $item.type
        requiredFlag = "0"; sortOrder = $sortOrder++; status = "0"
        maxLength = $item.maxLength; minNumber = $item.minNumber; maxNumber = $item.maxNumber
        options = $options
    }
    $createdField = Invoke-RestMethod -Uri "$baseUrl/ticket/custom-field" -Headers $headers -Method Post `
        -Body ($definition | ConvertTo-Json -Depth 10) -ContentType "application/json"
    if ($createdField.code -ne 200) { throw "create $($item.type) field failed: $($createdField.msg)" }
    $createdIds[$item.key] = [long]$createdField.data
}

$form = Invoke-RestMethod -Uri "$baseUrl/ticket/custom-field/form/6" -Headers $headers -Method Get
if ($form.code -ne 200) { throw "form query failed" }
$inputs = @($definitions | ForEach-Object { @{ fieldKey = $_.key; value = $_.value } })
foreach ($field in $form.data) {
    if ($definitions.key -contains $field.fieldKey) { continue }
    if ($field.requiredFlag -eq "1" -and -not $field.defaultValue) {
        $sample = switch ($field.fieldType) {
            "TEXT" { "smoke" }
            "NUMBER" { if ($field.minNumber) { $field.minNumber } else { 1 } }
            "DATE" { "2026-07-03" }
            "DATETIME" { "2026-07-03 10:20:30" }
            "BOOLEAN" { $true }
            "SINGLE_SELECT" { $field.options[0].optionValue }
            "MULTI_SELECT" { @($field.options[0].optionValue) }
        }
        $inputs += @{ fieldKey = $field.fieldKey; value = $sample }
    }
}
$ticketBody = @{ title = "v2.1 custom field smoke $suffix"; categoryId = 6; priority = "MEDIUM"; customFields = $inputs }
$createdTicket = Invoke-RestMethod -Uri "$baseUrl/ticket" -Headers $headers -Method Post `
    -Body ($ticketBody | ConvertTo-Json -Depth 10) -ContentType "application/json"
if ($createdTicket.code -ne 200 -or -not $createdTicket.data) { throw "create ticket failed: $($createdTicket.msg)" }
$detail = Invoke-RestMethod -Uri "$baseUrl/ticket/$($createdTicket.data)" -Headers $headers -Method Get
$snapshots = @($detail.data.customFields | Where-Object { $definitions.key -contains $_.fieldKeySnapshot })
if ($detail.code -ne 200 -or $snapshots.Count -ne 7) { throw "seven-type snapshot detail failed" }
$textSnapshot = $snapshots | Where-Object { $_.fieldKeySnapshot -eq $definitions[0].key }
if ($textSnapshot.normalizedValue -ne "snapshot-value") { throw "text normalization failed" }
$numberSnapshot = $snapshots | Where-Object { $_.fieldKeySnapshot -eq $definitions[1].key }
if ($numberSnapshot.normalizedValue -ne "10.5") { throw "number normalization failed" }

$textDefinition = Invoke-RestMethod -Uri "$baseUrl/ticket/custom-field/$($createdIds[$definitions[0].key])" `
    -Headers $headers -Method Get
$textDefinition.data.status = "1"
$disabled = Invoke-RestMethod -Uri "$baseUrl/ticket/custom-field/$($createdIds[$definitions[0].key])" `
    -Headers $headers -Method Put -Body ($textDefinition.data | ConvertTo-Json -Depth 10) -ContentType "application/json"
if ($disabled.code -ne 200) { throw "disable field failed: $($disabled.msg)" }
$history = Invoke-RestMethod -Uri "$baseUrl/ticket/$($createdTicket.data)" -Headers $headers -Method Get
if (-not ($history.data.customFields.fieldKeySnapshot -contains $definitions[0].key)) {
    throw "disabled field history snapshot missing"
}
$disabledInput = Invoke-RestMethod -Uri "$baseUrl/ticket" -Headers $headers -Method Post `
    -Body ($ticketBody | ConvertTo-Json -Depth 10) -ContentType "application/json"
if ($disabledInput.code -eq 200) { throw "disabled field should reject new input" }

$ticketBody.customFields += @{ fieldKey = "UNKNOWN_FIELD"; value = "x" }
$invalid = Invoke-RestMethod -Uri "$baseUrl/ticket" -Headers $headers -Method Post `
    -Body ($ticketBody | ConvertTo-Json -Depth 10) -ContentType "application/json"
if ($invalid.code -eq 200) { throw "unknown field should fail" }
Write-Output "custom field seven-type smoke passed: ticketId=$($createdTicket.data)"
