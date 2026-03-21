# Pharmacy Database Backup to Google Drive - Setup Guide

This guide will help you set up automatic database backups to Google Drive every 6 hours.

## Prerequisites

- PostgreSQL installed with `pg_dump` available
- macOS (this guide uses launchd for scheduling)

## Step 1: Install rclone

rclone is used to upload files to Google Drive.

```bash
# Install using Homebrew
brew install rclone
```

## Step 2: Configure Google Drive with rclone

```bash
rclone config
```

Follow these steps:
1. Select `n` for new remote
2. Name it `gdrive` (or update the script if using a different name)
3. Select `drive` for Google Drive
4. Leave client_id and client_secret blank (press Enter)
5. Select scope `1` (full access)
6. Leave root_folder_id blank
7. Leave service_account_file blank
8. Select `n` for advanced config
9. Select `y` to auto config (this will open your browser)
10. Sign in to your Google account and grant access
11. Select `n` for team drive
12. Confirm with `y`

### Test the connection:
```bash
rclone ls gdrive:
```

## Step 3: Update the Backup Script

Edit `/Users/cmbklap0208/Documents/pharma_workspace/pharma/new_project/Phamarcy-Management-Backend/scripts/backup_to_gdrive.sh`

Update these values with your actual database credentials:
```bash
DB_NAME="pharmacy"        # Your database name
DB_USER="pharmacy"        # Your database username
DB_PASSWORD="pharmacy"    # Your database password
DB_HOST="localhost"       # Database host
DB_PORT="5432"           # Database port
```

## Step 4: Make the Script Executable

```bash
chmod +x /Users/cmbklap0208/Documents/pharma_workspace/pharma/new_project/Phamarcy-Management-Backend/scripts/backup_to_gdrive.sh
```

## Step 5: Test the Backup Script

Run manually to ensure it works:
```bash
/Users/cmbklap0208/Documents/pharma_workspace/pharma/new_project/Phamarcy-Management-Backend/scripts/backup_to_gdrive.sh
```

Check the logs:
```bash
cat ~/pharmacy_backups/backup.log
```

## Step 6: Set Up Automatic Scheduling (Every 6 Hours)

### Create the backup directory:
```bash
mkdir -p ~/pharmacy_backups
```

### Install the launchd service:
```bash
# Copy the plist file to LaunchAgents
cp /Users/cmbklap0208/Documents/pharma_workspace/pharma/new_project/Phamarcy-Management-Backend/scripts/com.pharmacy.backup.plist ~/Library/LaunchAgents/

# Load the service
launchctl load ~/Library/LaunchAgents/com.pharmacy.backup.plist
```

### Verify it's running:
```bash
launchctl list | grep com.pharmacy.backup
```

## Managing the Backup Service

### Stop the service:
```bash
launchctl unload ~/Library/LaunchAgents/com.pharmacy.backup.plist
```

### Start the service:
```bash
launchctl load ~/Library/LaunchAgents/com.pharmacy.backup.plist
```

### Run backup manually:
```bash
launchctl start com.pharmacy.backup
```

### Check logs:
```bash
# Backup script logs
tail -f ~/pharmacy_backups/backup.log

# launchd logs
tail -f ~/pharmacy_backups/launchd_stdout.log
tail -f ~/pharmacy_backups/launchd_stderr.log
```

### Remove the service completely:
```bash
launchctl unload ~/Library/LaunchAgents/com.pharmacy.backup.plist
rm ~/Library/LaunchAgents/com.pharmacy.backup.plist
```

## Backup Schedule

- **Frequency**: Every 6 hours
- **Local Retention**: Last 5 backups
- **Google Drive Retention**: Last 30 backups

## Backup Location

- **Local**: `~/pharmacy_backups/`
- **Google Drive**: `PharmacyBackups/` folder

## Troubleshooting

### rclone not found
Make sure rclone is installed and the PATH includes Homebrew:
```bash
which rclone
# Should show: /opt/homebrew/bin/rclone or /usr/local/bin/rclone
```

### pg_dump not found
Install PostgreSQL or ensure it's in your PATH:
```bash
brew install postgresql
```

### Permission denied
Make sure the script is executable:
```bash
chmod +x /path/to/backup_to_gdrive.sh
```

### Google Drive authentication expired
Re-authenticate with:
```bash
rclone config reconnect gdrive:
```

## Quick Setup Commands (Copy & Run)

```bash
# 1. Install rclone
brew install rclone

# 2. Configure Google Drive
rclone config

# 3. Make script executable
chmod +x /Users/cmbklap0208/Documents/pharma_workspace/pharma/new_project/Phamarcy-Management-Backend/scripts/backup_to_gdrive.sh

# 4. Create backup directory
mkdir -p ~/pharmacy_backups

# 5. Test the backup
/Users/cmbklap0208/Documents/pharma_workspace/pharma/new_project/Phamarcy-Management-Backend/scripts/backup_to_gdrive.sh

# 6. Install the scheduled task
cp /Users/cmbklap0208/Documents/pharma_workspace/pharma/new_project/Phamarcy-Management-Backend/scripts/com.pharmacy.backup.plist ~/Library/LaunchAgents/
launchctl load ~/Library/LaunchAgents/com.pharmacy.backup.plist

# 7. Verify it's running
launchctl list | grep com.pharmacy.backup
```
