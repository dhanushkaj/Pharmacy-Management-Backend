@echo off
REM =============================================================================
REM Uninstall Windows Task Scheduler task for database backup
REM Run this script as Administrator
REM =============================================================================

echo.
echo ============================================
echo Pharmacy Database Backup - Task Removal
echo ============================================
echo.

REM Check for admin rights
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo ERROR: This script requires Administrator privileges.
    echo Please right-click and select "Run as administrator"
    pause
    exit /b 1
)

echo Removing scheduled task: PharmacyDatabaseBackup
echo.

schtasks /delete /tn "PharmacyDatabaseBackup" /f

if %errorLevel% equ 0 (
    echo.
    echo SUCCESS! Task removed successfully.
    echo.
) else (
    echo.
    echo Task may not exist or removal failed.
    echo.
)

pause
