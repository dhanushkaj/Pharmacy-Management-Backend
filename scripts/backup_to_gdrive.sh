#!/bin/bash
# =============================================================================
# Pharmacy Database Backup to Google Drive
# Runs every 6 hours via launchd
# Reads configuration from backup_config.json
# =============================================================================

# Load configuration from backup_config.json
CONFIG_FILE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/backup_config.json"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: backup_config.json not found at $CONFIG_FILE"
    exit 1
fi

# Parse JSON config (using jq if available, otherwise basic grep)
if command -v jq &> /dev/null; then
    DB_NAME=$(jq -r '.database.name' "$CONFIG_FILE")
    DB_USER=$(jq -r '.database.user' "$CONFIG_FILE")
    DB_PASSWORD=$(jq -r '.database.password' "$CONFIG_FILE")
    DB_HOST=$(jq -r '.database.host' "$CONFIG_FILE")
    DB_PORT=$(jq -r '.database.port' "$CONFIG_FILE")
    GDRIVE_REMOTE=$(jq -r '.gdrive.remote' "$CONFIG_FILE")
    GDRIVE_FOLDER=$(jq -r '.gdrive.folder' "$CONFIG_FILE")
    KEEP_LOCAL_BACKUPS=$(jq -r '.retention.local_backups' "$CONFIG_FILE")
    KEEP_GDRIVE_BACKUPS=$(jq -r '.retention.gdrive_backups' "$CONFIG_FILE")
else
    # Fallback: hardcoded defaults if jq not available
    DB_NAME="pharmacy"
    DB_USER="pharmacy"
    DB_PASSWORD="pharmacy"
    DB_HOST="localhost"
    DB_PORT="5432"
    GDRIVE_REMOTE="gdrive"
    GDRIVE_FOLDER="PharmacyBackups"
    KEEP_LOCAL_BACKUPS=5
    KEEP_GDRIVE_BACKUPS=30
fi

# Local backup directory
BACKUP_DIR="$HOME/pharmacy_backups"
LOG_FILE="$BACKUP_DIR/backup.log"

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
