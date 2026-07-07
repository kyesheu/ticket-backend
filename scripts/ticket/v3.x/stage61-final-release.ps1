# =============================================
# v3.3 stage 61 full regression, release/rollback drill and project sign-off
# Usage:
#   powershell scripts/ticket/v3.x/stage61-final-release.ps1
#   powershell scripts/ticket/v3.x/stage61-final-release.ps1 -SkipSmoke
#   powershell scripts/ticket/v3.x/stage61-final-release.ps1 -QuickCheck
# =============================================

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$AiBaseUrl = "http://127.0.0.1:8090",
    [string]$ElasticsearchUrl = "http://localhost:9200",
    [switch]$SkipSmoke,
    [switch]$QuickCheck
)

$ErrorActionPreference = "Stop"
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
. (Join-Path $RepoRoot "scripts\ticket\_lib\Smoke-Bootstrap.ps1")

$StartedProcesses = @()
$Pass = 0
$Fail = 0
$Skip = 0
$ReportDir = Join-Path $RepoRoot "reports\stage61"
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

function Write-Skip($Desc, $Reason) {
    Write-Host "  [SKIP] $Desc - $Reason" -ForegroundColor Yellow
    $script:Skip++
}

# =============================================
# [1] Maven full test
# =============================================
function Invoke-JavaFullTest {
    Write-Host ""
    Write-Host "[1] Maven full test (mvn test)" -ForegroundColor Cyan
    $logFile = Join-Path $ReportDir "maven-test-$Timestamp.log"
    Push-Location $RepoRoot
    try {
        $proc = Start-Process -FilePath "mvn" `
            -ArgumentList @("test") `
            -NoNewWindow -Wait -PassThru `
            -RedirectStandardOutput $logFile -RedirectStandardError "$logFile.err"
        if ($proc.ExitCode -ne 0) {
            throw "mvn test failed, see $logFile"
        }
        Write-Pass "Maven full test passed"
    } finally {
        Pop-Location
    }
}

# =============================================
# [2] Maven compile
# =============================================
function Invoke-JavaCompile {
    Write-Host ""
    Write-Host "[2] Maven compile (mvn clean compile)" -ForegroundColor Cyan
    $logFile = Join-Path $ReportDir "maven-compile-$Timestamp.log"
    Push-Location $RepoRoot
    try {
        $proc = Start-Process -FilePath "mvn" `
            -ArgumentList @("clean","compile") `
            -NoNewWindow -Wait -PassThru `
            -RedirectStandardOutput $logFile -RedirectStandardError "$logFile.err"
        if ($proc.ExitCode -ne 0) {
            throw "mvn compile failed, see $logFile"
        }
        Write-Pass "Maven compile passed"
    } finally {
        Pop-Location
    }
}

# =============================================
# [3] Maven package
# =============================================
function Invoke-JavaPackage {
    Write-Host ""
    Write-Host "[3] Maven package" -ForegroundColor Cyan
    $logFile = Join-Path $ReportDir "maven-package-$Timestamp.log"
    Push-Location $RepoRoot
    try {
        $proc = Start-Process -FilePath "mvn" `
            -ArgumentList @("-pl","ruoyi-admin","-am","package","-DskipTests") `
            -NoNewWindow -Wait -PassThru `
            -RedirectStandardOutput $logFile -RedirectStandardError "$logFile.err"
        if ($proc.ExitCode -ne 0) {
            throw "mvn package failed, see $logFile"
        }
        $jarPath = Join-Path $RepoRoot "ruoyi-admin\target\ruoyi-admin.jar"
        if (-not (Test-Path -LiteralPath $jarPath)) {
            throw "jar not found: $jarPath"
        }
        Write-Pass "Maven package produced ruoyi-admin.jar"
    } finally {
        Pop-Location
    }
}

# =============================================
# [4] Python pytest full test
# =============================================
function Invoke-PythonFullTest {
    Write-Host ""
    Write-Host "[4] Python pytest full test" -ForegroundColor Cyan
    $pythonExe = Join-Path $RepoRoot "ai-service\.venv\Scripts\python.exe"
    if (-not (Test-Path -LiteralPath $pythonExe)) {
        Write-Fail "Python pytest" "ai-service virtualenv not found"
        return
    }
    $logFile = Join-Path $ReportDir "python-pytest-$Timestamp.log"
    Push-Location $RepoRoot
    try {
        & $pythonExe -m pytest ai-service\tests -q *>> $logFile
        if ($LASTEXITCODE -ne 0) {
            throw "pytest failed, see $logFile"
        }
        $testSummary = Select-String -Path $logFile -Pattern "passed" | Select-Object -Last 1
        Write-Pass "Python pytest full test passed: $($testSummary.Line.Trim())"
    } catch {
        Write-Fail "Python pytest" $_.Exception.Message
    } finally {
        Pop-Location
    }
}

# =============================================
# [5] Full smoke tests (v1.x, v2.x, v3.x)
# =============================================
function Invoke-FullSmoke {
    Write-Host ""
    Write-Host "[5] Full smoke tests (v1.x, v2.x, v3.x)" -ForegroundColor Cyan

    if ($SkipSmoke) {
        Write-Skip "Full smoke tests" "-SkipSmoke was specified"
        return
    }

    . (Join-Path $RepoRoot "scripts\ticket\_lib\Smoke-Bootstrap.ps1")

    if (-not (Ensure-SmokeJavaBackend -RepoRoot $RepoRoot -BaseUrl $BaseUrl)) {
        Write-Fail "Java backend startup" "see logs\smoke-java-backend.log"
        return
    }

    $pythonReady = Ensure-SmokePythonAiService -RepoRoot $RepoRoot -AiBaseUrl $AiBaseUrl

    # --- v1.x smoke ---
    Write-Host "  [5.1] v1.x smoke test" -ForegroundColor DarkCyan
    $v1Script = Join-Path $RepoRoot "scripts\ticket\v1.x\smoke-test.ps1"
    if (Test-Path -LiteralPath $v1Script) {
        $logFile = Join-Path $ReportDir "smoke-v1.x-$Timestamp.log"
        $proc = Start-Process -FilePath "pwsh" `
            -ArgumentList @("-NoProfile","-ExecutionPolicy","Bypass","-File",$v1Script) `
            -NoNewWindow -Wait -PassThru `
            -RedirectStandardOutput $logFile -RedirectStandardError "$logFile.err"
        if ($proc.ExitCode -eq 0) {
            Write-Pass "v1.x smoke passed"
        } else {
            Write-Fail "v1.x smoke" "exit code $($proc.ExitCode), see $logFile"
        }
    } else {
        Write-Fail "v1.x smoke" "script not found"
    }

    # --- v2.x smoke ---
    Write-Host "  [5.2] v2.x smoke test" -ForegroundColor DarkCyan
    $v2Script = Join-Path $RepoRoot "scripts\ticket\v2.x\smoke-test.ps1"
    if (Test-Path -LiteralPath $v2Script) {
        $logFile = Join-Path $ReportDir "smoke-v2.x-$Timestamp.log"
        $proc = Start-Process -FilePath "pwsh" `
            -ArgumentList @("-NoProfile","-ExecutionPolicy","Bypass","-File",$v2Script) `
            -NoNewWindow -Wait -PassThru `
            -RedirectStandardOutput $logFile -RedirectStandardError "$logFile.err"
        if ($proc.ExitCode -eq 0) {
            Write-Pass "v2.x smoke passed"
        } else {
            Write-Fail "v2.x smoke" "exit code $($proc.ExitCode), see $logFile"
        }
    } else {
        Write-Fail "v2.x smoke" "script not found"
    }

    # --- v3.x smoke ---
    Write-Host "  [5.3] v3.x smoke test" -ForegroundColor DarkCyan
    $v3Script = Join-Path $RepoRoot "scripts\ticket\v3.x\smoke-test.ps1"
    if (-not (Test-Path -LiteralPath $v3Script)) {
        Write-Fail "v3.x smoke" "script not found"
    } elseif (-not $pythonReady) {
        Write-Skip "v3.x smoke" "Python AI service not running"
    } else {
        $logFile = Join-Path $ReportDir "smoke-v3.x-$Timestamp.log"
        $proc = Start-Process -FilePath "pwsh" `
            -ArgumentList @("-NoProfile","-ExecutionPolicy","Bypass","-File",$v3Script) `
            -NoNewWindow -Wait -PassThru `
            -RedirectStandardOutput $logFile -RedirectStandardError "$logFile.err"
        if ($proc.ExitCode -eq 0) {
            Write-Pass "v3.x smoke passed"
        } else {
            Write-Fail "v3.x smoke" "exit code $($proc.ExitCode), see $logFile"
        }
    }
}

# =============================================
# [6] Key documentation existence check
# =============================================
function Invoke-DocumentExistenceCheck {
    Write-Host ""
    Write-Host "[6] Key documentation existence check" -ForegroundColor Cyan

    $versions = @(
        "1.x\1.0", "1.x\1.1", "1.x\1.2", "1.x\1.3",
        "2.x\2.0", "2.x\2.1", "2.x\2.2", "2.x\2.3",
        "3.x\3.0", "3.x\3.1", "3.x\3.2", "3.x\3.3"
    )
    $docFiles = @(
        "01-project-spec.md", "02-architecture-design.md",
        "03-database-design.md", "04-implementation-plan.md",
        "05-test-release.md"
    )

    $missing = @()
    foreach ($version in $versions) {
        foreach ($file in $docFiles) {
            $path = Join-Path $RepoRoot "docs\$version\$file"
            if (-not (Test-Path -LiteralPath $path)) {
                $missing += "docs\$version\$file"
            }
        }
    }

    if ($missing.Count -gt 0) {
        Write-Fail "Documentation completeness" ($missing -join ", ")
    } else {
        Write-Pass "All 60 doc files (12 versions x 5 docs) present"
    }
}

# =============================================
# [7] Release and rollback documentation check
# =============================================
function Invoke-ReleaseRollbackDocCheck {
    Write-Host ""
    Write-Host "[7] Release and rollback documentation check" -ForegroundColor Cyan

    $checks = @()

    # README check
    $readmePath = Join-Path $RepoRoot "docs\README.md"
    if (Test-Path -LiteralPath $readmePath) {
        $readmeContent = Get-Content $readmePath -Raw
        if ($readmeContent -notmatch "3\.3") {
            $checks += "README missing v3.3 reference"
        }
    } else {
        $checks += "README missing"
    }

    # SQL migration check
    $sqlDir = Join-Path $RepoRoot "sql"
    $sqlFiles = @(Get-ChildItem -Path $sqlDir -Filter "*.sql" -ErrorAction SilentlyContinue | ForEach-Object { $_.Name })
    $expectedSql = @("ry_20260417.sql", "quartz.sql")
    foreach ($v in @("v1.0","v1.1","v1.2","v1.3","v2.0","v2.1","v2.2","v2.3","v3.0","v3.1","v3.2")) {
        $expectedSql += "ticket-$v.sql"
    }
    $missingSql = @($expectedSql | Where-Object { $_ -notin $sqlFiles })
    if ($missingSql.Count -gt 0) {
        $checks += "Missing SQL: $($missingSql -join ', ')"
    }

    # Backups directory
    $backupDir = Join-Path $RepoRoot "backups"
    if (-not (Test-Path -LiteralPath $backupDir)) {
        $checks += "backups dir missing"
    }

    # Architecture doc rollback check
    $archPath = Join-Path $RepoRoot "docs\3.x\3.3\02-architecture-design.md"
    if (Test-Path -LiteralPath $archPath) {
        $archContent = Get-Content $archPath -Raw
        if (($archContent -notmatch "rollback") -and ($archContent -notmatch "revert") -and ($archContent -notmatch "backward")) {
            $checks += "arch doc missing rollback guidance"
        }
    }

    # Implementation plan release check
    $implPath = Join-Path $RepoRoot "docs\3.x\3.3\04-implementation-plan.md"
    if (Test-Path -LiteralPath $implPath) {
        $implContent = Get-Content $implPath -Raw
        if (($implContent -notmatch "release") -and ($implContent -notmatch "deploy") -and ($implContent -notmatch "61")) {
            $checks += "impl plan missing release reference"
        }
    }

    if ($checks.Count -gt 0) {
        Write-Fail "Release and rollback doc" ($checks -join "; ")
    } else {
        Write-Pass "Release and rollback documentation is sufficient"
    }
}

# =============================================
# [8] TODO, stub and release gate audit
# =============================================
function Invoke-CodeAudit {
    Write-Host ""
    Write-Host "[8] TODO, stub and release gate audit" -ForegroundColor Cyan

    $findings = @()
    $reportFile = Join-Path $ReportDir "code-audit-$Timestamp.txt"

    $javaDirs = @("ruoyi-ticket", "ruoyi-common", "ruoyi-framework", "ruoyi-admin", "ruoyi-system")
    $seenPaths = @{}
    foreach ($dir in $javaDirs) {
        $fullDir = Join-Path $RepoRoot $dir
        if (-not (Test-Path -LiteralPath $fullDir)) { continue }
        Get-ChildItem -Path $fullDir -Recurse -Filter "*.java" -ErrorAction SilentlyContinue | ForEach-Object {
            $relative = $_.FullName.Substring($RepoRoot.Length + 1)
            if ($seenPaths.ContainsKey($relative)) { return }
            $seenPaths[$relative] = $true
            $lineNo = 0
            Get-Content -LiteralPath $_.FullName -ErrorAction SilentlyContinue | ForEach-Object {
                $lineNo++
                if ($_ -match "//\s*(TODO|FIXME|STUB|HACK|XXX)\b") {
                    if ($relative -match "UUID\.java") { return }
                    $findings += "$relative`:$lineNo $_"
                }
            }
        }
    }

    $pySrcDir = Join-Path $RepoRoot "ai-service\src"
    if (Test-Path -LiteralPath $pySrcDir) {
        Get-ChildItem -Path $pySrcDir -Recurse -Filter "*.py" -ErrorAction SilentlyContinue | ForEach-Object {
            $relative = $_.FullName.Substring($RepoRoot.Length + 1)
            $lineNo = 0
            Get-Content -LiteralPath $_.FullName -ErrorAction SilentlyContinue | ForEach-Object {
                $lineNo++
                if ($_ -match "#\s*(TODO|FIXME|STUB|HACK|XXX)\b") {
                    $findings += "$relative`:$lineNo $_"
                }
            }
        }
    }

    $findings | Set-Content -LiteralPath $reportFile -Encoding utf8

    $criticalFindings = @($findings | Where-Object {
        ($_ -notmatch "ruoyi-ui") -and ($_ -notmatch "generator") -and ($_ -notmatch "ui.src.utils.generator")
    })
    if ($criticalFindings.Count -gt 0) {
        Write-Fail "TODO/stub/gate audit" ("$($criticalFindings.Count) critical issues, see $reportFile")
        Write-Host "  Critical findings:" -ForegroundColor DarkGray
        $criticalFindings | ForEach-Object { Write-Host "    $_" -ForegroundColor DarkGray }
    } else {
        Write-Pass "TODO/stub/gate audit: clean, no critical issues"
    }
}

# =============================================
# [9] Document vs reality consistency check
# =============================================
function Invoke-DocumentConsistencyCheck {
    Write-Host ""
    Write-Host "[9] Document vs reality consistency check" -ForegroundColor Cyan

    $issues = @()

    $specPath = Join-Path $RepoRoot "docs\3.x\3.3\01-project-spec.md"
    if (Test-Path -LiteralPath $specPath) {
        $specContent = Get-Content $specPath -Raw
        if ($specContent -notmatch "v3\.3") {
            $issues += "spec missing v3.3"
        }
    }

    $trPath = Join-Path $RepoRoot "docs\3.x\3.3\05-test-release.md"
    if (Test-Path -LiteralPath $trPath) {
        $trContent = Get-Content $trPath -Raw
        if ($trContent -notmatch "58") { $issues += "TR missing stage 58" }
        if ($trContent -notmatch "59") { $issues += "TR missing stage 59" }
        if ($trContent -notmatch "60") { $issues += "TR missing stage 60" }
    }

    $readmePath = Join-Path $RepoRoot "docs\README.md"
    if (Test-Path -LiteralPath $readmePath) {
        $readmeContent = Get-Content $readmePath -Raw
        $allVersions = @("1.0","1.1","1.2","1.3","2.0","2.1","2.2","2.3","3.0","3.1","3.2","3.3")
        foreach ($v in $allVersions) {
            if ($readmeContent -notmatch [regex]::Escape($v)) {
                $issues += "README missing version $v"
            }
        }
    }

    if ($issues.Count -gt 0) {
        Write-Fail "Document consistency" ($issues -join "; ")
    } else {
        Write-Pass "Document consistency verified"
    }
}

# =============================================
# [10] Artifact and script audit
# =============================================
function Invoke-ArtifactAudit {
    Write-Host ""
    Write-Host "[10] Artifact and script audit" -ForegroundColor Cyan

    $missing = @()

    $requiredScripts = @(
        "scripts\ticket\_lib\Smoke-Bootstrap.ps1",
        "scripts\ticket\v1.x\smoke-test.ps1",
        "scripts\ticket\v2.x\smoke-test.ps1",
        "scripts\ticket\v3.x\smoke-test.ps1",
        "scripts\ticket\v3.x\stage59-recovery-drill.ps1",
        "scripts\ticket\v3.x\stage60-security-performance.ps1",
        "scripts\ticket\v3.x\stage61-final-release.ps1"
    )

    foreach ($script in $requiredScripts) {
        $path = Join-Path $RepoRoot $script
        if (-not (Test-Path -LiteralPath $path)) {
            $missing += $script
        }
    }

    $v2SpecialistScripts = @(
        "attachment-smoke.ps1", "custom-field-definition-smoke.ps1",
        "custom-field-value-smoke.ps1", "custom-field-workflow-smoke.ps1",
        "search-rebuild-smoke.ps1", "search-smoke.ps1",
        "workflow-definition-smoke.ps1", "workflow-engine-smoke.ps1",
        "workflow-task-smoke.ps1"
    )
    foreach ($s in $v2SpecialistScripts) {
        $path = Join-Path $RepoRoot "scripts\ticket\v2.x\$s"
        if (-not (Test-Path -LiteralPath $path)) {
            $missing += "scripts\ticket\v2.x\$s"
        }
    }

    if ($missing.Count -gt 0) {
        Write-Fail "Artifact audit" ($missing -join ", ")
    } else {
        Write-Pass "All required scripts and artifacts present"
    }
}

# =============================================
# Quick check mode (no build/test/smoke)
# =============================================
if ($QuickCheck) {
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "  STAGE 61 QUICK CHECK (no build/test/smoke)" -ForegroundColor Yellow
    Write-Host "============================================" -ForegroundColor Cyan

    Invoke-DocumentExistenceCheck
    Invoke-ReleaseRollbackDocCheck
    Invoke-CodeAudit
    Invoke-DocumentConsistencyCheck
    Invoke-ArtifactAudit

    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "  Quick Check Result: $Pass passed, $Fail failed, $Skip skipped" -ForegroundColor $(if ($Fail -eq 0) { "Green" } else { "Red" })
    Write-Host "  Reports: $ReportDir" -ForegroundColor DarkGray
    Write-Host "==========================================" -ForegroundColor Cyan

    Stop-SmokeStartedProcesses
    if ($Fail -gt 0) { exit 1 }
    exit 0
}

# =============================================
# Full execution
# =============================================
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  STAGE 61 FULL REGRESSION AND SIGN-OFF" -ForegroundColor Cyan
Write-Host "  Project: ticket-backend v3.3" -ForegroundColor Cyan
Write-Host "  Reports: $ReportDir" -ForegroundColor DarkGray
Write-Host "============================================" -ForegroundColor Cyan

Invoke-JavaFullTest
Invoke-JavaCompile
Invoke-JavaPackage
Invoke-PythonFullTest

if (-not $SkipSmoke) {
    Invoke-FullSmoke
} else {
    Write-Skip "Full smoke tests" "-SkipSmoke was specified"
}

Invoke-DocumentExistenceCheck
Invoke-ReleaseRollbackDocCheck
Invoke-CodeAudit
Invoke-DocumentConsistencyCheck
Invoke-ArtifactAudit

# =============================================
# Summary
# =============================================
Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  Stage 61 Final Result" -ForegroundColor Cyan
Write-Host "  $Pass passed, $Fail failed, $Skip skipped" -ForegroundColor $(if ($Fail -eq 0) { "Green" } else { "Red" })
Write-Host "  Reports: $ReportDir" -ForegroundColor DarkGray
Write-Host "==========================================" -ForegroundColor Cyan

if ($Fail -eq 0) {
    Write-Host ""
    Write-Host "All release gates passed. Ready to mark v3.3 complete." -ForegroundColor Green
}

Stop-SmokeStartedProcesses
if ($Fail -gt 0) { exit 1 }
exit 0
