#!/bin/bash

# ========== CONFIGURATION ==========
export PGHOST="localhost"
export PGPORT="5432"
export PGDATABASE="pharmacy"
export PGUSER="pharmacy"
export PGPASSWORD="pharmacy"

BACKUP_DIR="$HOME/pg_backups"
ROOT_DRIVE="gdrive:"
BACKUP_PATH="PG_Backups/pharmacy"
RETENTION_DAYS=15
# =================================

mkdir -p "$BACKUP_DIR"
cd "$BACKUP_DIR" || exit 1

# CREATE FULL FOLDER STRUCTURE
rclone mkdir "$ROOT_DRIVE$BACKUP_PATH"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DUMP_FILE="pg_${PGDATABASE}_${TIMESTAMP}.sql.gz"

echo "$(date): Starting backup for $PGDATABASE..."

# DUMP (working fine)
pg_dump -h $PGHOST -p $PGPORT -U $PGUSER -d $PGDATABASE -Fc | gzip > "$DUMP_FILE"

DRIVE_FULLPATH="$ROOT_DRIVE$BACKUP_PATH/"

# UPLOAD (now to correct nested folder)
rclone copy "$DUMP_FILE" "$DRIVE_FULLPATH/latest.dump.gz"
rclone move "$DUMP_FILE" "$DRIVE_FULLPATH"

# CLEANUP (ignore errors for new folders)
find "$BACKUP_DIR" -name "pg_${PGDATABASE}_*.sql.gz" -mtime +$RETENTION_DAYS -delete 2>/dev/null
rclone deletefile "$DRIVE_FULLPATH" --min-age ${RETENTION_DAYS}d --exclude "latest.dump.gz" 2>/dev/null

echo "$(date): Backup complete! Check: rclone ls '$DRIVE_FULLPATH'"
