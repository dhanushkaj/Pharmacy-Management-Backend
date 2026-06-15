# Pharmacy Database Backup - Configurable Periodic Setup (Windows)

Automatically backup your PostgreSQL database to Google Drive at configurable intervals.

## Quick Start - 3 Steps

### Step 1: Edit Configuration
Open `backup_config.json` and set your backup interval:

```json
{
  "backup": {
    "enabled": true,
    "interval_hours": 6
  }
}
```

**Common intervals:**
- `1` - Every 1 hour
- `2` - Every 2 hours
- `4` - Every 4 hours
- `6` - Every 6 hours (recommended)
- `12` - Every 12 hours
- `24` - Daily

Update database credentials if needed in the same file.

### Step 2: Configure rclone for Google Drive

If you haven't set up rclone yet:

```cmd
rclone config
```

Follow the prompts to:
1. Create a new remote called `gdrive`
2. Choose Google Drive as the storage
3. Authenticate with your Google account
4. Choose PharmacyBackups as the folder

### Step 3: Install Automatic Scheduler

**Option A: Using the installer script (Recommended)**
```cmd
cd scripts
install_backup_task.bat
```

This will create a Windows Task Scheduler job that runs every 6 hours (or your configured interval).

**Option B: Manual setup in Task Scheduler**

1. Open **Task Scheduler** (Win + R → `taskschd.msc`)
2. Right-click **Task Scheduler Library** → **Create Task**
3. **General tab:**
   - Name: `PharmacyDatabaseBackup`
   - Check: "Run with highest privileges"
   - Check: "Run whether user is logged in or not"

4. **Triggers tab:**
   - Click **New**
   - Choose: "On a schedule"
   - Repeat task every: `6 hours` (or your configured interval)
   - Duration: "Indefinitely"

5. **Actions tab:**
   - Action: "Start a program"
   - Program: `PowerShell.exe`
   - Arguments: `-NoProfile -ExecutionPolicy Bypass -File "C:\path\to\backup_to_gdrive.ps1"`

6. **Conditions tab:**
   - Uncheck "Start the task only if the computer is on AC power"
   - Check: "Stop if the computer switches to battery power" (optional)

7. Click **OK** and enter admin password

## Verify It's Working

Check the backup log:
```cmd
notepad %USERPROFILE%\pharmacy_backups\backup.log
```

You should see entries like:
```
[2026-06-08 14:30:45] ==========================================
[2026-06-08 14:30:45] Starting database backup...
[2026-06-08 14:30:48] Creating PostgreSQL dump...
[2026-06-08 14:31:05] Compressing backup...
[2026-06-08 14:31:12] Uploading to Google Drive...
[2026-06-08 14:31:25] Backup completed successfully
```

## Changing the Backup Interval

Edit `backup_config.json` and change `interval_hours`. The next scheduled task will use the new interval.

To update an existing scheduled task:
1. Open Task Scheduler
2. Right-click **PharmacyDatabaseBackup**
3. Edit triggers and change the repeat interval

## Uninstall

To remove automatic backups:
```cmd
cd scripts
uninstall_backup_task.bat
```

Or manually delete the task in Task Scheduler.

## Troubleshooting

**"pg_dump not found"**
- Update `postgresql.bin_path` in `backup_config.json`
- Or add PostgreSQL bin to Windows PATH

**"rclone not configured"**
- Run `rclone config` and set up the `gdrive` remote

**"Google Drive authentication failed"**
- Verify your Google account has access to rclone
- Re-run `rclone config` to refresh the token

**"Task never runs"**
- Check Task Scheduler > View > Show All Tasks
- Right-click task > Properties > History tab for errors
