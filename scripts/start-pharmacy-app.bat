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
REM Go up two levels: scripts -> Phamarcy-Management-Backend -> new_project
set PROJECT_ROOT=%SCRIPT_DIR%..\..\
set BACKEND_DIR=%SCRIPT_DIR%..\
set FRONTEND_DIR=%PROJECT_ROOT%front_end

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

echo [INFO] Starting Backend Server...
echo.

REM Start Backend in a new window
cd /d "%BACKEND_DIR%"

REM Check if JAR file exists, otherwise use Gradle
if exist "build\libs\*.jar" (
    echo [INFO] Starting from JAR file...
    start "Pharmacy Backend" cmd /k "cd /d "%BACKEND_DIR%" && java -jar build\libs\Phamarcy-Management-Backend-0.0.1-SNAPSHOT.jar"
) else (
    echo [INFO] JAR not found, starting with Gradle...
    start "Pharmacy Backend" cmd /k "cd /d "%BACKEND_DIR%" && gradlew.bat bootRun"
)

echo [INFO] Waiting for backend to initialize (10 seconds)...
timeout /t 10 /nobreak >nul

echo.
echo [INFO] Starting Frontend Server...
echo.

REM Start Frontend in a new window
cd /d "%FRONTEND_DIR%"

REM Check if node_modules exists
if not exist "node_modules" (
    echo [INFO] Installing frontend dependencies (first time setup)...
    start "Pharmacy Frontend Setup" cmd /k "cd /d "%FRONTEND_DIR%" && npm install && npm start"
) else (
    start "Pharmacy Frontend" cmd /k "cd /d "%FRONTEND_DIR%" && npm start"
)

echo.
echo  ============================================
echo     PHARMACY SYSTEM IS STARTING!
echo  ============================================
echo.
echo  Backend:  http://localhost:8080
echo  Frontend: http://localhost:3000
echo.
echo  Two new windows have opened:
echo    - Backend Server (Java Spring Boot)
echo    - Frontend Server (React)
echo.
echo  Keep those windows open while using the app.
echo  Close them to stop the servers.
echo.
echo  This window will close in 5 seconds...
timeout /t 5 /nobreak >nul
exit
