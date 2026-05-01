@echo off
setlocal EnableExtensions
REM =============================================================================
REM Pharmacy Management System - Start Both Frontend and Backend
REM Double-click this file or create a desktop shortcut to it
REM =============================================================================

title Pharmacy Management System - Launcher
color 0A

echo.
echo  ============================================
echo     PHARMACY MANAGEMENT SYSTEM LAUNCHER
echo  ============================================
echo.

REM Resolve directories
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..\") do set "BACKEND_DIR=%%~fI"
for %%I in ("%SCRIPT_DIR%..\..\Pharmacy-Management") do set "FRONTEND_DIR=%%~fI"

REM Check prerequisites
where java >nul 2>&1
if errorlevel 1 goto NO_JAVA

where node >nul 2>&1
if errorlevel 1 goto NO_NODE

REM Optional: switch Node version if nvm exists
where nvm >nul 2>&1
if errorlevel 1 goto SKIP_NVM
call nvm use 21 >nul 2>&1
:SKIP_NVM

echo [INFO] Starting Backend Server...
if exist "%BACKEND_DIR%build\libs\Pharmacy-Management-Backend.jar" (
	echo [INFO] Starting from JAR file...
) else (
	echo [INFO] JAR not found, starting with Gradle...
)
start "PharmacyBackend" "%SCRIPT_DIR%run-backend.bat"

:AFTER_BACKEND

echo [INFO] Waiting for backend to initialize (10 seconds)...
timeout /t 10 /nobreak >nul

echo.
echo [INFO] Starting Frontend Server...
echo [INFO] Frontend directory: %FRONTEND_DIR%

if not exist "%FRONTEND_DIR%\package.json" goto FRONTEND_INVALID
if not exist "%FRONTEND_DIR%\node_modules" echo [INFO] Installing frontend dependencies (first time setup)...
start "PharmacyFrontend" "%SCRIPT_DIR%run-frontend.bat"

echo.
echo  ============================================
echo     PHARMACY SYSTEM IS STARTING!
echo  ============================================
echo.
echo  Backend:  http://localhost:8080
echo  Frontend: http://localhost:3000
echo.
echo  Keep both windows open while using the app.
echo  Close them to stop the servers.
echo.
echo  Launcher will close in 5 seconds...
timeout /t 5 /nobreak >nul
exit /b 0

:NO_JAVA
echo [ERROR] Java is not installed or not in PATH
echo Please install Java 17 or higher
pause
exit /b 1

:NO_NODE
echo [ERROR] Node.js is not installed or not in PATH
echo Please install Node.js from https://nodejs.org
pause
exit /b 1

:FRONTEND_INVALID
echo [ERROR] Frontend directory is invalid: %FRONTEND_DIR%
pause
exit /b 1
