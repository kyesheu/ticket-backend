# Shared smoke-test bootstrap helpers.

function Get-SmokeRepoRoot {
    param([string]$ScriptRoot)
    return (Resolve-Path (Join-Path $ScriptRoot "..\..\..")).Path
}

function Set-SmokeDefaultEnv {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Value
    )
    if (-not [Environment]::GetEnvironmentVariable($Name, "Process")) {
        [Environment]::SetEnvironmentVariable($Name, $Value, "Process")
    }
}

function Test-SmokeHttpReady {
    param([Parameter(Mandatory = $true)][string]$Url)
    try {
        Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec 3 | Out-Null
        return $true
    } catch {
        return $false
    }
}

function Wait-SmokeHttpReady {
    param(
        [Parameter(Mandatory = $true)][string]$Desc,
        [Parameter(Mandatory = $true)][string]$Url,
        [int]$TimeoutSeconds = 120
    )
    $Deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $Deadline) {
        if (Test-SmokeHttpReady $Url) {
            Write-Host "  [PASS] $Desc ready" -ForegroundColor Green
            return $true
        }
        Start-Sleep -Seconds 2
    }
    Write-Host "  [FAIL] $Desc not ready: $Url" -ForegroundColor Red
    return $false
}

function Start-SmokeProcess {
    param(
        [Parameter(Mandatory = $true)][string]$Desc,
        [Parameter(Mandatory = $true)][string]$WorkingDirectory,
        [Parameter(Mandatory = $true)][string]$Command,
        [Parameter(Mandatory = $true)][string]$LogFile
    )
    if (-not $script:StartedProcesses) {
        $script:StartedProcesses = @()
    }
    Write-Host "  Starting $Desc" -ForegroundColor DarkGray
    $EscapedLogFile = $LogFile.Replace("'", "''")
    $WrappedCommand = "& { $Command } *>> '$EscapedLogFile'"
    $Process = Start-Process -FilePath "pwsh" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $WrappedCommand) `
        -WorkingDirectory $WorkingDirectory `
        -WindowStyle Hidden `
        -PassThru
    $script:StartedProcesses += [pscustomobject]@{
        Desc = $Desc
        Process = $Process
        LogFile = $LogFile
    }
}

function Ensure-SmokeJavaBackend {
    param(
        [Parameter(Mandatory = $true)][string]$RepoRoot,
        [string]$BaseUrl = "http://localhost:8080",
        [switch]$SkipPackage
    )
    if (Test-SmokeHttpReady "$BaseUrl/captchaImage") {
        Write-Host "  [PASS] Java backend already running" -ForegroundColor Green
        return $true
    }

    $JavaLog = Join-Path $RepoRoot "logs\smoke-java-backend.log"
    New-Item -ItemType Directory -Force -Path (Split-Path $JavaLog) | Out-Null
    if (-not $SkipPackage) {
        Write-Host "  Packaging Java backend" -ForegroundColor DarkGray
        Push-Location $RepoRoot
        try {
            mvn -pl ruoyi-admin -am package -DskipTests *>> $JavaLog
            if ($LASTEXITCODE -ne 0) {
                Write-Host "  [FAIL] Java backend package failed, see $JavaLog" -ForegroundColor Red
                return $false
            }
        } finally {
            Pop-Location
        }
    }

    $JarPath = Join-Path $RepoRoot "ruoyi-admin\target\ruoyi-admin.jar"
    if (-not (Test-Path -LiteralPath $JarPath)) {
        Write-Host "  [FAIL] Java backend jar not found: $JarPath" -ForegroundColor Red
        return $false
    }
    Start-SmokeProcess "Java backend" $RepoRoot "java -jar '$JarPath'" $JavaLog
    return (Wait-SmokeHttpReady "Java backend" "$BaseUrl/captchaImage" 120)
}

function Ensure-SmokePythonAiService {
    param(
        [Parameter(Mandatory = $true)][string]$RepoRoot,
        [string]$AiBaseUrl = "http://127.0.0.1:8090"
    )
    if (Test-SmokeHttpReady "$AiBaseUrl/api/v1/health") {
        Write-Host "  [PASS] Python AI service already running" -ForegroundColor Green
        return $true
    }

    $PythonExe = Join-Path $RepoRoot "ai-service\.venv\Scripts\python.exe"
    if (-not (Test-Path -LiteralPath $PythonExe)) {
        Write-Host "  Installing ai-service virtualenv" -ForegroundColor DarkGray
        Push-Location (Join-Path $RepoRoot "ai-service")
        try {
            python -m venv .venv
            & $PythonExe -m pip install -e ".[test]"
        } finally {
            Pop-Location
        }
    }

    $AiLog = Join-Path $RepoRoot "logs\smoke-ai-service.log"
    New-Item -ItemType Directory -Force -Path (Split-Path $AiLog) | Out-Null
    Start-SmokeProcess "Python AI service" (Join-Path $RepoRoot "ai-service") `
        ".\.venv\Scripts\python.exe -m uvicorn ticket_ai.main:app --app-dir src --host 127.0.0.1 --port 8090" `
        $AiLog
    return (Wait-SmokeHttpReady "Python AI service" "$AiBaseUrl/api/v1/health" 60)
}

function Stop-SmokeStartedProcesses {
    if (-not $script:StartedProcesses) {
        return
    }
    foreach ($Item in $script:StartedProcesses) {
        if ($Item.Process -and -not $Item.Process.HasExited) {
            Write-Host "  Stopping $($Item.Desc)" -ForegroundColor DarkGray
            taskkill /PID $Item.Process.Id /T /F | Out-Null
        }
    }
}
