@echo off
setlocal EnableExtensions
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..\..\Pharmacy-Management") do set "FRONTEND_DIR=%%~fI"

if not exist "%FRONTEND_DIR%\package.json" (
    echo [ERROR] Frontend directory is invalid: %FRONTEND_DIR%
    pause
    exit /b 1
)

cd /d "%FRONTEND_DIR%"
where nvm >nul 2>&1
if not errorlevel 1 (
    call nvm use 21 >nul 2>&1
)

if not exist "node_modules" (
    echo [INFO] Frontend: installing dependencies...
    npm.cmd install
)

echo [INFO] Frontend: starting React app...
npm.cmd start
