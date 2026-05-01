# Pharmacy Database Backup to Google Drive - Windows Setup Guide

Automatically backup your PostgreSQL database to Google Drive every 6 hours.

## Files Included

| File | Description |
|------|-------------|
| `backup_to_gdrive.ps1` | Main PowerShell backup script |
| `backup_to_gdrive.bat` | Batch wrapper for the PowerShell script |
| `install_backup_task.bat` | Installs the Windows scheduled task |
| `uninstall_backup_task.bat` | Removes the scheduled task |

---

## Quick Setup (5 Steps)

### Step 1: Install rclone

1. Download rclone from: https://rclone.org/downloads/
2. Extract to `C:\rclone\` (or any folder)
3. Add to PATH:
   - Press `Win + X` → System → Advanced system settings
   - Click "Environment Variables"
   - Edit "Path" under System variables
   - Add `C:\rclone` (or your rclone folder)
   - Click OK

**Or use winget:**
```cmd
winget install Rclone.Rclone
```

### Step 2: Configure Google Drive

Open Command Prompt and run:
```cmd
rclone config
```

Follow these steps:
1. Type `n` for new remote → Enter
2. Name: `gdrive` → Enter
3. Storage type: Find `drive` (Google Drive), type the number → Enter
4. client_id: Leave blank → Enter
5. client_secret: Leave blank → Enter
6. Scope: Type `1` (full access) → Enter
7. root_folder_id: Leave blank → Enter
8. service_account_file: Leave blank → Enter
9. Advanced config: `n` → Enter
10. Auto config: `y` → Enter
11. Browser will open - sign in to Google and allow access
12. Team drive: `n` → Enter
13. Confirm: `y` → Enter
14. Quit: `q` → Enter

**Test the connection:**
```cmd
rclone ls gdrive:
```

### Step 3: Update Database Credentials

Edit `backup_to_gdrive.ps1` and update these values at the top:

```powershell
$DB_NAME = "pharmacy"           # Your database name
$DB_USER = "pharmacy"           # Your database username  
$DB_PASSWORD = "pharmacy"       # Your database password
$DB_HOST = "localhost"          # Database host
$DB_PORT = "5432"              # Database port

# PostgreSQL bin path - update to match your installation
$PG_BIN_PATH = "C:\Program Files\PostgreSQL\16\bin"
```

### Step 4: Test the Backup

Run a test backup:
```cmd
powershell -ExecutionPolicy Bypass -File backup_to_gdrive.ps1
```

Check the logs:
```cmd
type %USERPROFILE%\pharmacy_backups\backup.log
```

### Step 5: Install Scheduled Task

Right-click `install_backup_task.bat` → **Run as administrator**

This creates a Windows Task Scheduler task that runs every 6 hours.

---

## Managing the Backup Task

### View task status:
```cmd
schtasks /query /tn "PharmacyDatabaseBackup" /v
```

### Run backup manually:
```cmd
schtasks /run /tn "PharmacyDatabaseBackup"
```

### Remove the task:
Run `uninstall_backup_task.bat` as administrator, or:
```cmd
schtasks /delete /tn "PharmacyDatabaseBackup" /f
```

### Open Task Scheduler GUI:
```cmd
taskschd.msc
```
Look for "PharmacyDatabaseBackup" in the task list.

---

## Backup Details

| Setting | Value |
|---------|-------|
| Frequency | Every 6 hours |
| Local backup folder | `%USERPROFILE%\pharmacy_backups\` |
| Google Drive folder | `PharmacyBackups/` |
| Local retention | Last 5 backups |
| Google Drive retention | Last 30 backups |
| Backup format | Compressed SQL (.sql.gz) |

---

## Troubleshooting

### "rclone is not recognized"
Add rclone to your PATH or install with winget:
```cmd
winget install Rclone.Rclone
```

### "pg_dump not found"
Update `$PG_BIN_PATH` in the script to match your PostgreSQL installation:
```powershell
# Common paths:
$PG_BIN_PATH = "C:\Program Files\PostgreSQL\16\bin"   # PostgreSQL 16
$PG_BIN_PATH = "C:\Program Files\PostgreSQL\15\bin"   # PostgreSQL 15
$PG_BIN_PATH = "C:\Program Files\PostgreSQL\14\bin"   # PostgreSQL 14
```

### "Access denied" or "permission error"
- Run `install_backup_task.bat` as Administrator
- Ensure your Windows user has database access

### "Google Drive remote not configured"
Run `rclone config` again and make sure to name the remote `gdrive`

### Google Drive authentication expired
```cmd
rclone config reconnect gdrive:
```

### View backup logs
```cmd
type %USERPROFILE%\pharmacy_backups\backup.log
```

### Script execution policy error
Run PowerShell as Administrator and execute:
```powershell
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
```

---

## Manual Backup Commands

**Run backup immediately:**
```cmd
cd scripts
backup_to_gdrive.bat
```

**Or with PowerShell:**
```powershell
.\backup_to_gdrive.ps1
```

---

## Verifying Backups on Google Drive

1. Go to https://drive.google.com
2. Look for the `PharmacyBackups` folder
3. You should see files like: `pharmacy_backup_20260321_143022.sql.gz`

**Or check via command line:**
```cmd
rclone ls gdrive:PharmacyBackups
```
