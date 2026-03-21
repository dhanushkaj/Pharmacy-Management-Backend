@echo off
REM =============================================================================
REM Pharmacy Database Backup to Google Drive (Windows Batch)
REM Simple wrapper to run the PowerShell script
REM =============================================================================

PowerShell -NoProfile -ExecutionPolicy Bypass -File "%~dp0backup_to_gdrive.ps1"
