$ErrorActionPreference = "Stop"
$BaseUrl = "http://localhost:8080"

function Login-Admin {
    $Captcha = Invoke-RestMethod -Uri "$BaseUrl/captchaImage" -Method Get
    $LoginData = @{ username = "admin"; password = "admin123" }
    if ($Captcha.captchaEnabled) {
        $RawCode = (docker exec redis redis-cli --raw GET "captcha_codes:$($Captcha.uuid)").Trim()
        try { $CaptchaCode = $RawCode | ConvertFrom-Json } catch { $CaptchaCode = $RawCode.Trim('"') }
        $LoginData.code = [string]$CaptchaCode
        $LoginData.uuid = $Captcha.uuid
    }
    $Response = Invoke-RestMethod -Uri "$BaseUrl/login" -Method Post `
        -Body ($LoginData | ConvertTo-Json) -ContentType "application/json"
    if (-not $Response.token) { throw "Attachment smoke login failed: $($Response.msg)" }
    return @{ Authorization = "Bearer $($Response.token)" }
}

function Assert-Code($Description, $Expected, $Response) {
    if ($Response.code -ne $Expected) {
        throw "$Description failed: expected code $Expected, actual $($Response.code), message $($Response.msg)"
    }
}

$Headers = Login-Admin
$TextFile = Get-Item (Join-Path $PSScriptRoot "../../ruoyi-admin/src/main/resources/banner.txt")
$ImageFile = Get-Item (Join-Path $PSScriptRoot "../../ruoyi-ui/src/assets/logo/logo.png")
$InvalidFile = Get-Item (Join-Path $PSScriptRoot "../../pom.xml")

$Upload = Invoke-RestMethod -Uri "$BaseUrl/ticket/attachment/upload" -Method Post `
    -Headers $Headers -Form @{ file = $TextFile }
Assert-Code "upload ticket attachment" 200 $Upload
$TicketAttachmentId = [long]$Upload.data.attachmentId

$Suffix = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
$CreateBody = @{
    title = "V22-ATTACHMENT-$Suffix"
    content = "Attachment smoke"
    categoryId = 7
    priority = "MEDIUM"
    attachmentIds = @($TicketAttachmentId)
} | ConvertTo-Json -Depth 5
$Create = Invoke-RestMethod -Uri "$BaseUrl/ticket" -Method Post -Headers $Headers `
    -Body $CreateBody -ContentType "application/json"
Assert-Code "create ticket with attachment" 200 $Create
$TicketId = [long]$Create.data

$List = Invoke-RestMethod -Uri "$BaseUrl/ticket/attachment/ticket/$TicketId" -Headers $Headers
Assert-Code "list ticket attachments" 200 $List
if ($List.data.Count -ne 1 -or $List.data[0].originalName -ne "banner.txt" `
        -or $List.data[0].businessType -ne "TICKET") {
    throw "ticket attachment binding snapshot is incorrect"
}

$Download = Invoke-WebRequest -Uri "$BaseUrl/ticket/attachment/$TicketAttachmentId/download" -Headers $Headers
if ($Download.StatusCode -ne 200 -or -not $Download.Headers["Content-Disposition"]) {
    throw "download ticket attachment failed"
}

$CommentUpload = Invoke-RestMethod -Uri "$BaseUrl/ticket/attachment/upload" -Method Post `
    -Headers $Headers -Form @{ file = $ImageFile }
Assert-Code "upload comment attachment" 200 $CommentUpload
$CommentAttachmentId = [long]$CommentUpload.data.attachmentId
$CommentBody = @{
    content = "Comment attachment smoke"
    commentType = "EXTERNAL"
    attachmentIds = @($CommentAttachmentId)
} | ConvertTo-Json -Depth 5
$Comment = Invoke-RestMethod -Uri "$BaseUrl/ticket/$TicketId/comment" -Method Post -Headers $Headers `
    -Body $CommentBody -ContentType "application/json"
Assert-Code "bind comment attachment" 200 $Comment

$List = Invoke-RestMethod -Uri "$BaseUrl/ticket/attachment/ticket/$TicketId" -Headers $Headers
Assert-Code "list ticket and comment attachments" 200 $List
if ($List.data.Count -ne 2 -or "COMMENT" -notin @($List.data.businessType)) {
    throw "comment attachment binding is incorrect"
}

$Delete = Invoke-RestMethod -Uri "$BaseUrl/ticket/attachment/$CommentAttachmentId" `
    -Method Delete -Headers $Headers
Assert-Code "delete active ticket attachment" 200 $Delete
$RepeatedDelete = Invoke-RestMethod -Uri "$BaseUrl/ticket/attachment/$CommentAttachmentId" `
    -Method Delete -Headers $Headers
Assert-Code "reject repeated attachment delete" 500 $RepeatedDelete

$InvalidUpload = Invoke-RestMethod -Uri "$BaseUrl/ticket/attachment/upload" -Method Post `
    -Headers $Headers -Form @{ file = $InvalidFile }
Assert-Code "reject invalid attachment extension" 500 $InvalidUpload

Write-Output "attachment smoke passed: ticket_id=$TicketId"
