@echo off
rem Double-click to run, or pass installer arguments: install.cmd --dry-run "C:\path\to\instance"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0install.ps1" %*
set rc=%ERRORLEVEL%
if "%~1"=="" pause
exit /b %rc%
