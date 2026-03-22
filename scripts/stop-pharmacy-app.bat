@echo off
setlocal EnableDelayedExpansion
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

echo [INFO] Stopping Backend window...
taskkill /F /FI "WINDOWTITLE eq PharmacyBackend*" >nul 2>&1

echo [INFO] Stopping Frontend window...
taskkill /F /FI "WINDOWTITLE eq PharmacyFrontend*" >nul 2>&1

echo [INFO] Stopping Backend server (port 8080)...
set "BACKEND_FOUND=0"
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8080" ^| findstr "LISTENING"') do (
    set "BACKEND_FOUND=1"
    taskkill /F /PID %%a >nul 2>&1
    echo [OK] Killed backend PID %%a
)
if "!BACKEND_FOUND!"=="0" (
    echo [INFO] No backend process found on port 8080
)

echo [INFO] Stopping Frontend server (port 3000)...
set "FRONTEND_FOUND=0"
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":3000" ^| findstr "LISTENING"') do (
    set "FRONTEND_FOUND=1"
    taskkill /F /PID %%a >nul 2>&1
    echo [OK] Killed frontend PID %%a
)
if "!FRONTEND_FOUND!"=="0" (
    echo [INFO] No frontend process found on port 3000
)

echo.
echo  ============================================
echo     ALL SERVERS STOPPED
echo  ============================================
echo.

timeout /t 3 /nobreak >nul
exit /b 0
