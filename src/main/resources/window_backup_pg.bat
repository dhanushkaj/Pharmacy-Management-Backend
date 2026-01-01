@echo off
setlocal enabledelayedexpansion

REM ========== CONFIGURATION (EDIT THESE) ==========
set PGHOST=localhost
set PGPORT=5432
set PGDATABASE=pharmacy
set PGUSER=pharmacy
set PGPASSWORD=pharmacy

set BACKUP_DIR=%USERPROFILE%\pg_backups
set ROOT_DRIVE=gdrive:
set BACKUP_PATH=PG_Backups\pharmacy
set RETENTION_DAYS=15
REM ================================================

REM Create backup dir
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"
cd /d "%BACKUP_DIR%"

REM CREATE FULL FOLDER STRUCTURE
rclone mkdir "%ROOT_DRIVE%%BACKUP_PATH%"

REM Timestamp (Windows format)
for /f "tokens=2 delims==" %%i in ('wmic OS Get localdatetime /value') do set TIMESTAMP=%%i
set TIMESTAMP=%TIMESTAMP:~0,8%_%TIMESTAMP:~8,6%
set DUMP_FILE=pg_%PGDATABASE%_%TIMESTAMP%.sql.gz

echo %DATE% %TIME%: Starting backup for %PGDATABASE%...

REM pg_dump (adjust path to your PostgreSQL install)
REM Option 1: If in PATH
pg_dump -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %PGDATABASE% -Fc | powershell -c "gzip -c > '%DUMP_FILE%'"

REM Option 2: Full path (uncomment if needed)
REM "C:\Program Files\PostgreSQL\16\bin\pg_dump.exe" -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %PGDATABASE% -Fc | powershell -c "gzip -c > '%DUMP_FILE%'"

set DRIVE_FULLPATH=%ROOT_DRIVE%%BACKUP_PATH%\

REM UPLOAD
rclone copy "%DUMP_FILE%" "%DRIVE_FULLPATH%latest.dump.gz"
rclone move "%DUMP_FILE%" "%DRIVE_FULLPATH%"

REM LOCAL CLEANUP (older than 15 days)
powershell -command "Get-ChildItem '%BACKUP_DIR%\pg_%PGDATABASE%_*.sql.gz' | Where { $_.LastWriteTime -lt (Get-Date).AddDays(-%RETENTION_DAYS%) } | Remove-Item -Force"

REM DRIVE CLEANUP (ignore new folder errors)
rclone deletefile "%DRIVE_FULLPATH%" --min-age %RETENTION_DAYS%d --exclude "latest.dump.gz"

echo %DATE% %TIME%: Backup complete! Check: rclone ls "%DRIVE_FULLPATH%"
pause
