# =============================================
# v3.3 stage 60 security, dependency and performance checks
# Usage:
#   powershell scripts/ticket/v3.x/stage60-security-performance.ps1
# =============================================

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$AiBaseUrl = "http://127.0.0.1:8090",
    [int]$Iterations = 20
)

$ErrorActionPreference = "Stop"
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
. (Join-Path $RepoRoot "scripts\ticket\_lib\Smoke-Bootstrap.ps1")

$StartedProcesses = @()
$Pass = 0
$Fail = 0
$ReportDir = Join-Path $RepoRoot "reports\stage60"
$Timestamp = Get-Date -Format "yyyyMMddHHmmss"
New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null

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

function Save-JavaDependencyTree {
    $outFile = Join-Path $ReportDir "java-dependency-tree-$Timestamp.txt"
    Push-Location $RepoRoot
    try {
        mvn dependency:tree "-DoutputFile=$outFile" "-DappendOutput=false" | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "mvn dependency:tree failed"
        }
    } finally {
        Pop-Location
    }
    Write-Pass "Java dependency tree generated: $outFile"
}

function Save-PythonDependencyList {
    $outFile = Join-Path $ReportDir "python-freeze-$Timestamp.txt"
    $pythonExe = Join-Path $RepoRoot "ai-service\.venv\Scripts\python.exe"
    if (-not (Test-Path -LiteralPath $pythonExe)) {
        throw "Python virtualenv not found: $pythonExe"
    }
    & $pythonExe -m pip freeze | Set-Content -LiteralPath $outFile -Encoding utf8
    if ($LASTEXITCODE -ne 0) {
        throw "pip freeze failed"
    }
    Write-Pass "Python dependency list generated: $outFile"
}

function Invoke-SecretPatternScan {
    $outFile = Join-Path $ReportDir "secret-scan-$Timestamp.txt"
    $patterns = @(
        'AKIA[0-9A-Z]{16}',
        'sk-[A-Za-z0-9_-]{20,}',
        'xox[baprs]-[A-Za-z0-9-]{20,}',
        '-----BEGIN (RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----',
        '(?i)(password|passwd|pwd|secret|token|api[_-]?key)\s*[:=]\s*["''][A-Za-z0-9_./+=-]{16,}["'']'
    )
    $findings = New-Object System.Collections.Generic.List[string]
    $files = Get-ChildItem -Path $RepoRoot -Recurse -File -ErrorAction SilentlyContinue
    foreach ($file in $files) {
        $relative = $file.FullName.Substring($RepoRoot.Length + 1)
        if ($relative -match '(^|\\)(target|logs|backups|reports|uploadPath|\.git|\.idea|\.venv|\.codex|\.agents)(\\|$)' `
                -or $relative -match '(^|\\)(src\\test|tests)(\\|$)' `
                -or $relative -eq ".env") {
            continue
        }
        $lineNo = 0
        try {
            Get-Content -LiteralPath $file.FullName -ErrorAction Stop | ForEach-Object {
                $lineNo++
                foreach ($pattern in $patterns) {
                    if ($_ -match $pattern) {
                        if ($_ -match 'replace-with|local-smoke|example|TOKEN_SECRET|DB_PASSWORD|TICKET_AI_SERVICE_TOKEN') {
                            continue
                        }
                        $findings.Add("$relative`:$lineNo`t$($_.Trim())")
                    }
                }
            }
        } catch {
            continue
        }
    }
    $findings | Set-Content -LiteralPath $outFile -Encoding utf8
    if ($findings.Count -gt 0) {
        throw "secret-like patterns found, see $outFile"
    }
    Write-Pass "Secret pattern scan passed: $outFile"
}

function Invoke-PythonSecurityTests {
    $pythonExe = Join-Path $RepoRoot "ai-service\.venv\Scripts\python.exe"
    & $pythonExe -m pytest `
        "ai-service\tests\test_stage48_safety.py" `
        "ai-service\tests\test_contract.py" `
        -q
    if ($LASTEXITCODE -ne 0) {
        throw "Python security tests failed"
    }
    Write-Pass "Python security and contract tests passed"
}

function Measure-Endpoint {
    param(
        [string]$Name,
        [string]$Uri,
        [hashtable]$Headers = @{}
    )
    $durations = New-Object System.Collections.Generic.List[double]
    $failures = 0
    foreach ($i in 1..$Iterations) {
        $watch = [System.Diagnostics.Stopwatch]::StartNew()
        try {
            Invoke-RestMethod -Uri $Uri -Method Get -Headers $Headers -TimeoutSec 10 | Out-Null
            $watch.Stop()
            $durations.Add($watch.Elapsed.TotalMilliseconds)
        } catch {
            $watch.Stop()
            $failures++
        }
    }
    $sorted = @($durations | Sort-Object)
    $p95 = if ($sorted.Count -gt 0) {
        $index = [Math]::Min($sorted.Count - 1, [Math]::Ceiling($sorted.Count * 0.95) - 1)
        [Math]::Round($sorted[$index], 2)
    } else {
        0
    }
    return [pscustomobject]@{
        Name = $Name
        Count = $Iterations
        Success = $durations.Count
        Failures = $failures
        P95Ms = $p95
    }
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

function Invoke-PerformanceBaseline {
    if (-not (Ensure-SmokeJavaBackend -RepoRoot $RepoRoot -BaseUrl $BaseUrl -SkipPackage)) {
        throw "Java backend is not ready; run smoke scripts or compile/package first"
    }
    $token = Login-Admin
    if (-not $token) {
        throw "Admin login failed"
    }
    $headers = @{ Authorization = "Bearer $token" }
    $results = @()
    $results += Measure-Endpoint "java_liveness" "$BaseUrl/actuator/health/liveness"
    $results += Measure-Endpoint "captcha" "$BaseUrl/captchaImage"
    $results += Measure-Endpoint "ticket_list" "$BaseUrl/ticket/list?pageNum=1&pageSize=10" $headers
    if (Test-SmokeHttpReady "$AiBaseUrl/api/v1/health") {
        $results += Measure-Endpoint "python_ai_health" "$AiBaseUrl/api/v1/health"
    }
    $outFile = Join-Path $ReportDir "performance-baseline-$Timestamp.csv"
    $results | Export-Csv -LiteralPath $outFile -NoTypeInformation -Encoding utf8
    $bad = @($results | Where-Object { $_.Failures -gt 0 -or $_.P95Ms -gt 2000 })
    if ($bad.Count -gt 0) {
        throw "performance baseline exceeded threshold, see $outFile"
    }
    Write-Pass "Performance baseline passed: $outFile"
}

Write-Host ""
Write-Host "[1] Dependency inventory" -ForegroundColor Cyan
try { Save-JavaDependencyTree } catch { Write-Fail "Java dependency tree" $_.Exception.Message }
try { Save-PythonDependencyList } catch { Write-Fail "Python dependency list" $_.Exception.Message }

Write-Host ""
Write-Host "[2] Security checks" -ForegroundColor Cyan
try { Invoke-SecretPatternScan } catch { Write-Fail "Secret pattern scan" $_.Exception.Message }
try { Invoke-PythonSecurityTests } catch { Write-Fail "Python security tests" $_.Exception.Message }

Write-Host ""
Write-Host "[3] Performance baseline" -ForegroundColor Cyan
try { Invoke-PerformanceBaseline } catch { Write-Fail "Performance baseline" $_.Exception.Message }

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  Result: $Pass passed, $Fail failed" -ForegroundColor $(if ($Fail -eq 0) { "Green" } else { "Red" })
Write-Host "  Reports: $ReportDir" -ForegroundColor DarkGray
Write-Host "==========================================" -ForegroundColor Cyan

Stop-SmokeStartedProcesses
if ($Fail -gt 0) { exit 1 }
