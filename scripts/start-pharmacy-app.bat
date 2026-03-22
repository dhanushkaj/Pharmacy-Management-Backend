@echo off
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

REM Get the directory where this script is located
set SCRIPT_DIR=%~dp0
REM Go up two levels: scripts -> Pharmacy-Management-Backend -> pharma
set PROJECT_ROOT=%SCRIPT_DIR%..\..\
set BACKEND_DIR=%SCRIPT_DIR%..\
set FRONTEND_DIR=D:\pharma\Pharmacy-Management

REM Check if Java is installed
where java >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] Java is not installed or not in PATH
    echo Please install Java 17 or higher
    pause
    exit /b 1
)

REM Check if Node.js is installed
where node >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] Node.js is not installed or not in PATH
    echo Please install Node.js from https://nodejs.org
    pause
    exit /b 1
)

REM Switch to Node.js version 21 if nvm is available
nvm use 21 >nul 2>&1
if %errorLevel% neq 0 (
    echo [WARNING] Could not switch to Node.js 21. Make sure nvm is installed and Node 21 is available.
)

echo [INFO] Starting Backend Server...
echo.

REM Start Backend in a new window
cd /d "%BACKEND_DIR%"

REM Check if JAR file exists, otherwise use Gradle
if exist "build\libs\Pharmacy-Management-Backend.jar" (
    echo [INFO] Starting from JAR file...
    start "PharmacyBackend" cmd /k "cd /d "%BACKEND_DIR%" && java -jar build\libs\Pharmacy-Management-Backend.jar"
) else (
    echo [INFO] JAR not found, starting with Gradle...
    start "PharmacyBackend" cmd /k "cd /d "%BACKEND_DIR%" && gradlew.bat bootRun"
)

echo [INFO] Waiting for backend to initialize (10 seconds)...
timeout /t 10 /nobreak >nul

echo.
echo [INFO] Starting Frontend Server...
echo [INFO] Frontend directory: %FRONTEND_DIR%
echo.

REM Check if frontend directory exists
if not exist "%FRONTEND_DIR%" (
    echo [ERROR] Frontend directory not found: %FRONTEND_DIR%
    pause
    exit /b 1
)

REM Start Frontend in a new window
echo [INFO] Installing frontend dependencies (first time setup)...
start "PharmacyFrontend" cmd /k cd /d "D:\pharma\Pharmacy-Management" ^& npm install ^& npm start

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
