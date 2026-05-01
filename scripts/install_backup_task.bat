@echo off
REM =============================================================================
REM Install Windows Task Scheduler task for automatic backup every 6 hours
REM Run this script as Administrator
REM =============================================================================

echo.
echo ============================================
echo Pharmacy Database Backup - Task Installation
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

REM Get the script directory
set SCRIPT_DIR=%~dp0
set PS_SCRIPT=%SCRIPT_DIR%backup_to_gdrive.ps1

REM Check if PowerShell script exists
if not exist "%PS_SCRIPT%" (
    echo ERROR: backup_to_gdrive.ps1 not found in %SCRIPT_DIR%
    pause
    exit /b 1
)

echo Creating scheduled task: PharmacyDatabaseBackup
echo Schedule: Every 6 hours
echo Script: %PS_SCRIPT%
echo.

REM Delete existing task if it exists
schtasks /delete /tn "PharmacyDatabaseBackup" /f >nul 2>&1

REM Create the scheduled task to run every 6 hours
REM Using MINUTE with 360 minutes = 6 hours
schtasks /create ^
    /tn "PharmacyDatabaseBackup" ^
    /tr "PowerShell.exe -NoProfile -ExecutionPolicy Bypass -File \"%PS_SCRIPT%\"" ^
    /sc MINUTE ^
    /mo 360 ^
    /ru "%USERNAME%" ^
    /rl HIGHEST ^
    /f

if %errorLevel% equ 0 (
    echo.
    echo ============================================
    echo SUCCESS! Task created successfully.
    echo ============================================
    echo.
    echo Task Name: PharmacyDatabaseBackup
    echo Schedule: Every 6 hours (360 minutes)
    echo.
    echo To manage the task:
    echo   - View: schtasks /query /tn "PharmacyDatabaseBackup" /v
    echo   - Run now: schtasks /run /tn "PharmacyDatabaseBackup"
    echo   - Delete: schtasks /delete /tn "PharmacyDatabaseBackup" /f
    echo.
    echo Or open Task Scheduler GUI: taskschd.msc
    echo.
) else (
    echo.
    echo ERROR: Failed to create scheduled task.
    echo.
)

pause
