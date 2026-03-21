# =============================================================================
# Pharmacy Database Backup to Google Drive (Windows PowerShell)
# Runs every 6 hours via Windows Task Scheduler
# =============================================================================

# Configuration - Update these values
$DB_NAME = "pharmacy"
$DB_USER = "pharmacy"
$DB_PASSWORD = "pharmacy"
$DB_HOST = "localhost"
$DB_PORT = "5432"

# Google Drive remote name (configured in rclone)
$GDRIVE_REMOTE = "gdrive"
$GDRIVE_FOLDER = "PharmacyBackups"

# Local backup directory
$BACKUP_DIR = "$env:USERPROFILE\pharmacy_backups"
$LOG_FILE = "$BACKUP_DIR\backup.log"

# Retention settings
$KEEP_LOCAL_BACKUPS = 5     # Keep last 5 local backups
$KEEP_GDRIVE_BACKUPS = 30   # Keep last 30 backups on Google Drive

# PostgreSQL bin path (update if different)
$PG_BIN_PATH = "C:\Program Files\PostgreSQL\16\bin"

# =============================================================================
# Functions
# =============================================================================

function Write-Log {
    param([string]$Message)
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logMessage = "[$timestamp] $Message"
    Write-Host $logMessage
    Add-Content -Path $LOG_FILE -Value $logMessage
}

function Exit-WithError {
    param([string]$Message)
    Write-Log "ERROR: $Message"
    exit 1
}

# =============================================================================
# Main Script
# =============================================================================

# Create backup directory if it doesn't exist
if (-not (Test-Path $BACKUP_DIR)) {
    New-Item -ItemType Directory -Path $BACKUP_DIR -Force | Out-Null
}

Write-Log "=========================================="
Write-Log "Starting database backup..."

# Generate backup filename with timestamp
$TIMESTAMP = Get-Date -Format "yyyyMMdd_HHmmss"
$BACKUP_FILE = "pharmacy_backup_$TIMESTAMP.sql"
$BACKUP_PATH = "$BACKUP_DIR\$BACKUP_FILE"
$COMPRESSED_FILE = "$BACKUP_FILE.gz"
$COMPRESSED_PATH = "$BACKUP_PATH.gz"

# Set PostgreSQL password environment variable
$env:PGPASSWORD = $DB_PASSWORD

# Check if pg_dump exists
$pgDumpPath = "$PG_BIN_PATH\pg_dump.exe"
if (-not (Test-Path $pgDumpPath)) {
    # Try to find pg_dump in PATH
    $pgDumpPath = (Get-Command pg_dump -ErrorAction SilentlyContinue).Path
    if (-not $pgDumpPath) {
        Exit-WithError "pg_dump not found. Please update PG_BIN_PATH in the script or add PostgreSQL to PATH."
    }
}

# Create database dump
Write-Log "Creating PostgreSQL dump..."
$pgDumpArgs = @(
    "-h", $DB_HOST,
    "-p", $DB_PORT,
    "-U", $DB_USER,
    "-d", $DB_NAME,
    "-F", "p",
    "-f", $BACKUP_PATH
)

$process = Start-Process -FilePath $pgDumpPath -ArgumentList $pgDumpArgs -Wait -NoNewWindow -PassThru
if ($process.ExitCode -ne 0) {
    Exit-WithError "Database dump failed!"
}

# Compress the backup using .NET compression
Write-Log "Compressing backup..."
try {
    $sourceBytes = [System.IO.File]::ReadAllBytes($BACKUP_PATH)
    $outputStream = [System.IO.File]::Create($COMPRESSED_PATH)
    $gzipStream = New-Object System.IO.Compression.GZipStream($outputStream, [System.IO.Compression.CompressionMode]::Compress)
    $gzipStream.Write($sourceBytes, 0, $sourceBytes.Length)
    $gzipStream.Close()
    $outputStream.Close()
    
    # Remove uncompressed file
    Remove-Item $BACKUP_PATH -Force
} catch {
    Exit-WithError "Compression failed: $_"
}

$backupSize = (Get-Item $COMPRESSED_PATH).Length / 1MB
$backupSizeFormatted = "{0:N2} MB" -f $backupSize
Write-Log "Backup created: $COMPRESSED_FILE (Size: $backupSizeFormatted)"

# Check if rclone is installed
$rclonePath = (Get-Command rclone -ErrorAction SilentlyContinue).Path
if (-not $rclonePath) {
    Write-Log "WARNING: rclone is not installed. Skipping Google Drive upload."
    Write-Log "Download rclone from: https://rclone.org/downloads/"
    Write-Log "Backup saved locally at: $COMPRESSED_PATH"
    exit 0
}

# Check if rclone remote is configured
$remotes = & rclone listremotes 2>&1
if ($remotes -notmatch "^${GDRIVE_REMOTE}:") {
    Write-Log "WARNING: rclone remote '$GDRIVE_REMOTE' is not configured."
    Write-Log "Run 'rclone config' to set up Google Drive."
    Write-Log "Backup saved locally at: $COMPRESSED_PATH"
    exit 0
}

# Upload to Google Drive
Write-Log "Uploading to Google Drive..."
$uploadResult = & rclone copy $COMPRESSED_PATH "${GDRIVE_REMOTE}:${GDRIVE_FOLDER}/" --progress 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Log "Successfully uploaded to Google Drive: ${GDRIVE_FOLDER}/${COMPRESSED_FILE}"
} else {
    Write-Log "WARNING: Google Drive upload failed. Backup saved locally."
    Write-Log "Error: $uploadResult"
}

# Clean up old local backups (keep last N)
Write-Log "Cleaning up old local backups..."
$localBackups = Get-ChildItem -Path $BACKUP_DIR -Filter "pharmacy_backup_*.sql.gz" | Sort-Object LastWriteTime -Descending
if ($localBackups.Count -gt $KEEP_LOCAL_BACKUPS) {
    $localBackups | Select-Object -Skip $KEEP_LOCAL_BACKUPS | ForEach-Object {
        Remove-Item $_.FullName -Force
        Write-Log "Deleted old local backup: $($_.Name)"
    }
}

# Clean up old Google Drive backups (keep last N)
Write-Log "Cleaning up old Google Drive backups..."
$gdriveFiles = & rclone ls "${GDRIVE_REMOTE}:${GDRIVE_FOLDER}/" 2>&1 | ForEach-Object { ($_ -split '\s+', 2)[1] } | Sort-Object -Descending
if ($gdriveFiles.Count -gt $KEEP_GDRIVE_BACKUPS) {
    $gdriveFiles | Select-Object -Skip $KEEP_GDRIVE_BACKUPS | ForEach-Object {
        & rclone delete "${GDRIVE_REMOTE}:${GDRIVE_FOLDER}/$_"
        Write-Log "Deleted old backup from Google Drive: $_"
    }
}

Write-Log "Backup completed successfully!"
Write-Log "=========================================="

# Clear password
$env:PGPASSWORD = ""

exit 0
