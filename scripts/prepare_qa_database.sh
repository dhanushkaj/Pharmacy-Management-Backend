#!/bin/bash
# ============================================================================
# QA Environment Database Preparation Script
# ============================================================================
# Purpose: Clean transactional data and create backup for QA deployment
# Date: 2026-03-02
# ============================================================================

# Configuration - Update these values for your environment
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="pharmacy"
DB_USER="pharmacy"
DB_SCHEMA="pharmacy"
BACKUP_DIR="$(pwd)/qa_backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/qa_clean_${TIMESTAMP}.backup"
SQL_BACKUP_FILE="${BACKUP_DIR}/qa_clean_${TIMESTAMP}.sql"

# Use password from environment or prompt
if [ -z "$DB_PASSWORD" ]; then
    echo -e "${YELLOW}Enter database password for user '$DB_USER':${NC}"
    read -s DB_PASSWORD
    export PGPASSWORD="$DB_PASSWORD"
else
    export PGPASSWORD="$DB_PASSWORD"
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}QA Environment Database Preparation${NC}"
echo -e "${GREEN}============================================${NC}"

# Create backup directory if not exists
mkdir -p "$BACKUP_DIR"

# Step 1: Run cleanup script
echo -e "\n${YELLOW}Step 1: Running data cleanup script...${NC}"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "migration_qa_cleanup_transactional_data.sql"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Data cleanup completed successfully${NC}"
else
    echo -e "${RED}✗ Data cleanup failed. Aborting.${NC}"
    exit 1
fi

# Step 2: Create pg_dump (Custom format - recommended for restore flexibility)
echo -e "\n${YELLOW}Step 2: Creating database backup (custom format)...${NC}"
pg_dump \
    -h "$DB_HOST" \
    -p "$DB_PORT" \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    -n "$DB_SCHEMA" \
    -F c \
    -b \
    -v \
    -f "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Backup created: $BACKUP_FILE${NC}"
else
    echo -e "${RED}✗ Backup creation failed${NC}"
    exit 1
fi

# Step 3: Also create plain SQL backup (for easy viewing/manual restore)
echo -e "\n${YELLOW}Step 3: Creating SQL backup (plain text)...${NC}"
pg_dump \
    -h "$DB_HOST" \
    -p "$DB_PORT" \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    -n "$DB_SCHEMA" \
    -F p \
    -b \
    -v \
    -f "$SQL_BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ SQL backup created: $SQL_BACKUP_FILE${NC}"
else
    echo -e "${YELLOW}⚠ SQL backup creation failed (non-critical)${NC}"
fi

# Step 4: Display summary
echo -e "\n${GREEN}============================================${NC}"
echo -e "${GREEN}Preparation Complete!${NC}"
echo -e "${GREEN}============================================${NC}"
echo -e "Backup files created in: ${BACKUP_DIR}"
echo -e "  - Custom format: $(basename $BACKUP_FILE)"
echo -e "  - SQL format: $(basename $SQL_BACKUP_FILE)"
echo -e "\n${YELLOW}To restore on QA server:${NC}"
echo -e "  pg_restore -h <qa_host> -U pharmacy -d postgres -c -v $BACKUP_FILE"
echo -e "\nOr for SQL file:"
echo -e "  psql -h <qa_host> -U pharmacy -d postgres -f $SQL_BACKUP_FILE"

# List preserved data counts
echo -e "\n${YELLOW}Preserved Master Data Summary:${NC}"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" <<EOF
SET search_path TO pharmacy, public;
SELECT 'Categories' as "Table", COUNT(*) as "Count" FROM rdp_categories
UNION ALL SELECT 'Products', COUNT(*) FROM rdp_products
UNION ALL SELECT 'Suppliers', COUNT(*) FROM rdp_suppliers
UNION ALL SELECT 'Users', COUNT(*) FROM rdp_users
UNION ALL SELECT 'Roles', COUNT(*) FROM rdp_roles
UNION ALL SELECT 'User-Roles', COUNT(*) FROM rdp_user_roles
UNION ALL SELECT 'Store Settings', COUNT(*) FROM rdp_store_settings;
EOF

echo -e "\n${GREEN}Done!${NC}"
