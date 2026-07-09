@echo off
setlocal

set "BACKEND_DIR=%~dp0"
if "%BACKEND_DIR:~-1%"=="\" set "BACKEND_DIR=%BACKEND_DIR:~0,-1%"

set "AI_DIR=%BACKEND_DIR%\ai-service"
set "AI_PYTHON=%AI_DIR%\.venv\Scripts\python.exe"
set "JAVA_JAR=%BACKEND_DIR%\ruoyi-admin\target\ruoyi-admin.jar"
set "LOG_DIR=%BACKEND_DIR%\logs"
set "START_FAILED=0"

echo Starting Ticket backend...
echo   Backend:  http://localhost:8080
echo   Python:   http://127.0.0.1:8090
echo   Logs:     %LOG_DIR%
echo.

if /I "%~1"=="--dry-run" (
  echo [DRY-RUN] load "%BACKEND_DIR%\.env"
  echo [DRY-RUN] background start Python AI service, log "%LOG_DIR%\ai-service.log"
  echo [DRY-RUN] wait for Python AI health check
  echo [DRY-RUN] background package and start Java backend, log "%LOG_DIR%\java-backend.log"
  echo [DRY-RUN] wait for Java backend health check
  exit /b 0
)

if not exist "%LOG_DIR%\" mkdir "%LOG_DIR%"

if exist "%BACKEND_DIR%\.env" (
  for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%BACKEND_DIR%\.env") do (
    if not "%%A"=="" if not defined %%A set "%%A=%%B"
  )
) else (
  echo [WARN] .env not found: %BACKEND_DIR%\.env
)

if not defined TICKET_AI_ENABLED set "TICKET_AI_ENABLED=true"
if not defined TICKET_AI_BASE_URL set "TICKET_AI_BASE_URL=http://127.0.0.1:8090"
if not defined TICKET_AI_SERVICE_TOKEN set "TICKET_AI_SERVICE_TOKEN=local-smoke-token-12345"
if not defined TICKET_AI_ELASTICSEARCH_URL set "TICKET_AI_ELASTICSEARCH_URL=http://127.0.0.1:9200"

powershell -NoProfile -Command "try { Invoke-RestMethod -Uri '%TICKET_AI_BASE_URL%/api/v1/health' -TimeoutSec 2 | Out-Null; exit 0 } catch { exit 1 }"
if errorlevel 1 (
  if not exist "%AI_PYTHON%" (
    echo Installing Python AI virtualenv...
    pushd "%AI_DIR%"
    python -m venv .venv
    "%AI_PYTHON%" -m pip install -e ".[test]"
    if errorlevel 1 (
      popd
      echo [FAIL] Failed to install Python AI dependencies
      echo        Check: %LOG_DIR%\ai-service.err.log
      exit /b 1
    )
    popd
  )
  echo Starting Python AI service in background...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath '%AI_PYTHON%' -ArgumentList @('-m','uvicorn','ticket_ai.main:app','--app-dir','src','--host','127.0.0.1','--port','8090') -WorkingDirectory '%AI_DIR%' -WindowStyle Hidden -RedirectStandardOutput '%LOG_DIR%\ai-service.log' -RedirectStandardError '%LOG_DIR%\ai-service.err.log'"
  echo Waiting for Python AI service health check...
  call :wait_health "%TICKET_AI_BASE_URL%/api/v1/health" 30
  if errorlevel 1 (
    echo [FAIL] Python AI service did not become ready within 30 seconds
    echo        Check: %LOG_DIR%\ai-service.log
    echo        Check: %LOG_DIR%\ai-service.err.log
    set "START_FAILED=1"
  ) else (
    echo [PASS] Python AI service is ready
  )
) else (
  echo [PASS] Python AI service already running
)

powershell -NoProfile -Command "try { Invoke-RestMethod -Uri 'http://localhost:8080/captchaImage' -TimeoutSec 2 | Out-Null; exit 0 } catch { exit 1 }"
if errorlevel 1 (
  echo Packaging and starting Java backend in background...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$cmd = 'mvn -pl ruoyi-admin -am package -DskipTests *> \"%LOG_DIR%\java-package.log\"; if ($LASTEXITCODE -eq 0) { java -jar \"%JAVA_JAR%\" *> \"%LOG_DIR%\java-backend.log\" } else { exit $LASTEXITCODE }'; Start-Process -FilePath 'powershell' -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-Command',$cmd) -WorkingDirectory '%BACKEND_DIR%' -WindowStyle Hidden"
  echo Waiting for Java backend health check...
  call :wait_health "http://localhost:8080/captchaImage" 120
  if errorlevel 1 (
    echo [FAIL] Java backend did not become ready within 120 seconds
    echo        Check: %LOG_DIR%\java-package.log
    echo        Check: %LOG_DIR%\java-backend.log
    set "START_FAILED=1"
  ) else (
    echo [PASS] Java backend is ready
  )
) else (
  echo [PASS] Java backend already running
)

if "%START_FAILED%"=="1" (
  echo.
  echo Startup finished with errors.
  exit /b 1
)

echo.
echo Backend services are ready.
exit /b 0

:wait_health
set "HEALTH_URL=%~1"
set "WAIT_SECONDS=%~2"
powershell -NoProfile -Command "$deadline=(Get-Date).AddSeconds([int]'%WAIT_SECONDS%'); do { try { Invoke-RestMethod -Uri '%HEALTH_URL%' -TimeoutSec 2 | Out-Null; exit 0 } catch { Start-Sleep -Seconds 2 } } while ((Get-Date) -lt $deadline); exit 1"
exit /b %ERRORLEVEL%
