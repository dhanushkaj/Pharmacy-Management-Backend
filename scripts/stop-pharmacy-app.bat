@echo off
REM =============================================================================
REM Pharmacy Management System - Stop All Servers
REM =============================================================================

title Pharmacy Management System - Shutdown
color 0C

echo.
echo  ============================================
echo     STOPPING PHARMACY MANAGEMENT SYSTEM
echo  ============================================
echo.

echo [INFO] Stopping Backend Server (Java on port 8080)...
for /f "tokens=5" %%a in ('netstat -aon ^| find ":8080" ^| find "LISTENING"') do (
    taskkill /F /PID %%a 2>nul
    if %errorLevel% equ 0 (
        echo [OK] Backend server stopped
    )
)

echo [INFO] Stopping Frontend Server (Node on port 3000)...
for /f "tokens=5" %%a in ('netstat -aon ^| find ":3000" ^| find "LISTENING"') do (
    taskkill /F /PID %%a 2>nul
    if %errorLevel% equ 0 (
        echo [OK] Frontend server stopped
    )
)

echo.
echo  ============================================
echo     ALL SERVERS STOPPED
echo  ============================================
echo.

timeout /t 3 /nobreak >nul
exit
