@echo off
setlocal
set "RUNNER=%~dp0run-all.ps1"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%RUNNER%" %*
set "RUNNER_EXIT=%ERRORLEVEL%"
if not "%RUNNER_EXIT%"=="0" (
    echo.
    echo CFC Lab launcher stopped with an error. Read the message above.
    pause
)
exit /b %RUNNER_EXIT%
