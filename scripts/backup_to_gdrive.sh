#!/bin/bash
# =============================================================================
# Pharmacy Database Backup to Google Drive
# Runs every 6 hours via launchd
# =============================================================================

# Configuration - Update these values
DB_NAME="pharmacy"
DB_USER="pharmacy"
DB_PASSWORD="pharmacy"
DB_HOST="localhost"
DB_PORT="5432"

# Google Drive remote name (configured in rclone)
GDRIVE_REMOTE="gdrive"
GDRIVE_FOLDER="PharmacyBackups"

# Local backup directory
BACKUP_DIR="$HOME/pharmacy_backups"
LOG_FILE="$BACKUP_DIR/backup.log"

# Retention settings
KEEP_LOCAL_BACKUPS=5    # Keep last 5 local backups
KEEP_GDRIVE_BACKUPS=30  # Keep last 30 backups on Google Drive

# =============================================================================
# Functions
# =============================================================================

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

error_exit() {
    log "ERROR: $1"
    exit 1
}

# =============================================================================
# Main Script
# =============================================================================

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"

log "=========================================="
log "Starting database backup..."

# Generate backup filename with timestamp
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
BACKUP_FILE="pharmacy_backup_${TIMESTAMP}.sql"
BACKUP_PATH="$BACKUP_DIR/$BACKUP_FILE"
COMPRESSED_FILE="${BACKUP_FILE}.gz"
COMPRESSED_PATH="${BACKUP_PATH}.gz"

# Set PostgreSQL password
export PGPASSWORD="$DB_PASSWORD"

# Create database dump
log "Creating PostgreSQL dump..."
pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -F p -f "$BACKUP_PATH"

if [ $? -ne 0 ]; then
    error_exit "Database dump failed!"
fi

# Compress the backup
log "Compressing backup..."
gzip -f "$BACKUP_PATH"

if [ $? -ne 0 ]; then
    error_exit "Compression failed!"
fi

BACKUP_SIZE=$(ls -lh "$COMPRESSED_PATH" | awk '{print $5}')
log "Backup created: $COMPRESSED_FILE (Size: $BACKUP_SIZE)"

# Check if rclone is installed
if ! command -v rclone &> /dev/null; then
    log "WARNING: rclone is not installed. Skipping Google Drive upload."
    log "Install rclone with: brew install rclone"
    log "Then configure with: rclone config"
    exit 0
fi

# Check if rclone remote is configured
if ! rclone listremotes | grep -q "^${GDRIVE_REMOTE}:"; then
    log "WARNING: rclone remote '$GDRIVE_REMOTE' is not configured."
    log "Run 'rclone config' to set up Google Drive."
    log "Backup saved locally at: $COMPRESSED_PATH"
    exit 0
fi

# Upload to Google Drive
log "Uploading to Google Drive..."
rclone copy "$COMPRESSED_PATH" "${GDRIVE_REMOTE}:${GDRIVE_FOLDER}/" --progress

if [ $? -eq 0 ]; then
    log "Successfully uploaded to Google Drive: ${GDRIVE_FOLDER}/${COMPRESSED_FILE}"
else
    log "WARNING: Google Drive upload failed. Backup saved locally."
fi

# Clean up old local backups (keep last N)
log "Cleaning up old local backups..."
cd "$BACKUP_DIR"
ls -t pharmacy_backup_*.sql.gz 2>/dev/null | tail -n +$((KEEP_LOCAL_BACKUPS + 1)) | xargs -r rm -f

# Clean up old Google Drive backups (keep last N)
log "Cleaning up old Google Drive backups..."
OLD_GDRIVE_FILES=$(rclone ls "${GDRIVE_REMOTE}:${GDRIVE_FOLDER}/" 2>/dev/null | awk '{print $2}' | sort -r | tail -n +$((KEEP_GDRIVE_BACKUPS + 1)))
for file in $OLD_GDRIVE_FILES; do
    rclone delete "${GDRIVE_REMOTE}:${GDRIVE_FOLDER}/${file}"
    log "Deleted old backup from Google Drive: $file"
done

log "Backup completed successfully!"
log "=========================================="

# Unset password
unset PGPASSWORD

exit 0
