@echo off
setlocal EnableDelayedExpansion

set "BACKEND_PORT=8080"
set "AI_PORT=8090"
set "DRY_RUN=false"

if /I "%~1"=="--dry-run" set "DRY_RUN=true"

echo Stopping Ticket backend services...
echo   Java backend port: %BACKEND_PORT%
echo   Python AI port:    %AI_PORT%
echo.

call :stop_port "%BACKEND_PORT%" "Java backend"
call :stop_port "%AI_PORT%" "Python AI service"

echo.
echo Stop command finished.
exit /b 0

:stop_port
set "PORT=%~1"
set "SERVICE_NAME=%~2"
set "FOUND=false"
set "SEEN_PIDS= "

for /f "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":%PORT% .*LISTENING"') do (
  echo !SEEN_PIDS! | findstr /C:" %%P " >nul
  if errorlevel 1 (
    set "FOUND=true"
    set "SEEN_PIDS=!SEEN_PIDS!%%P "
    if "%DRY_RUN%"=="true" (
      echo [DRY-RUN] Would stop %SERVICE_NAME% on port %PORT%, PID %%P
    ) else (
      echo Stopping %SERVICE_NAME% on port %PORT%, PID %%P...
      taskkill /PID %%P /F >nul 2>nul
      if errorlevel 1 (
        echo [WARN] Failed to stop PID %%P
      ) else (
        echo [PASS] Stopped PID %%P
      )
    )
  )
)

if "!FOUND!"=="false" (
  echo [PASS] %SERVICE_NAME% is not running on port %PORT%
)

exit /b 0
