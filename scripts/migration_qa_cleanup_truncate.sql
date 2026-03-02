-- ============================================================================
-- QA ENVIRONMENT DATA CLEANUP SCRIPT (TRUNCATE VERSION - FASTER)
-- ============================================================================
-- Purpose: Delete all transactional/test data while preserving master data
-- Date: 2026-03-02
-- 
-- This version uses TRUNCATE for faster execution
-- Use DELETE version if you need row-level control or triggers to fire
-- ============================================================================

-- ============================================================================
-- PRESERVED DATA (Master Data) - NOT TOUCHED:
--   - rdp_categories (Product categories)
--   - rdp_products (Products)
--   - rdp_suppliers (Suppliers)
--   - rdp_users (User accounts)
--   - rdp_roles (User roles)
--   - rdp_user_roles (User-role mappings)
--   - rdp_store_settings (Store configuration)
-- ============================================================================

-- Start transaction
BEGIN;

-- ============================================================================
-- TRUNCATE ALL TRANSACTIONAL TABLES (CASCADE handles FK dependencies)
-- ============================================================================

-- Billing/Sales data
TRUNCATE TABLE pharmacy.rdp_billing_items RESTART IDENTITY CASCADE;
TRUNCATE TABLE pharmacy.rdp_billings RESTART IDENTITY CASCADE;

-- GRN data
TRUNCATE TABLE pharmacy.rdp_grn_items RESTART IDENTITY CASCADE;
TRUNCATE TABLE pharmacy.rdp_grns RESTART IDENTITY CASCADE;

-- Purchase order data
TRUNCATE TABLE pharmacy.rdp_purchase_order_items RESTART IDENTITY CASCADE;
TRUNCATE TABLE pharmacy.rdp_purchase_orders RESTART IDENTITY CASCADE;

-- Inventory data
TRUNCATE TABLE pharmacy.rdp_inventory_items RESTART IDENTITY CASCADE;
TRUNCATE TABLE pharmacy.rdp_inventory_returns RESTART IDENTITY CASCADE;
TRUNCATE TABLE pharmacy.stock_movement RESTART IDENTITY CASCADE;

-- Customer data
TRUNCATE TABLE pharmacy.rdp_customers RESTART IDENTITY CASCADE;

-- Alert data
TRUNCATE TABLE pharmacy.rdp_alert_logs RESTART IDENTITY CASCADE;
TRUNCATE TABLE pharmacy.rdp_alert_config RESTART IDENTITY CASCADE;

-- Audit data
TRUNCATE TABLE pharmacy.audit_logs RESTART IDENTITY CASCADE;

-- Day end reports
TRUNCATE TABLE pharmacy.rdp_day_end_manual_bill RESTART IDENTITY CASCADE;
TRUNCATE TABLE pharmacy.rdp_day_end_reports RESTART IDENTITY CASCADE;

COMMIT;

-- ============================================================================
-- VERIFICATION - Check deleted vs preserved tables
-- ============================================================================
DO $$
DECLARE
    deleted_count INTEGER;
    preserved_count INTEGER;
BEGIN
    -- Check transactional tables are empty
    SELECT COUNT(*) INTO deleted_count FROM pharmacy.rdp_billings;
    RAISE NOTICE 'rdp_billings count: % (should be 0)', deleted_count;
    
    SELECT COUNT(*) INTO deleted_count FROM pharmacy.rdp_grns;
    RAISE NOTICE 'rdp_grns count: % (should be 0)', deleted_count;
    
    SELECT COUNT(*) INTO deleted_count FROM pharmacy.rdp_purchase_orders;
    RAISE NOTICE 'rdp_purchase_orders count: % (should be 0)', deleted_count;
    
    SELECT COUNT(*) INTO deleted_count FROM pharmacy.audit_logs;
    RAISE NOTICE 'audit_logs count: % (should be 0)', deleted_count;
    
    -- Check preserved tables still have data
    SELECT COUNT(*) INTO preserved_count FROM pharmacy.rdp_categories;
    RAISE NOTICE 'rdp_categories count: % (should be > 0)', preserved_count;
    
    SELECT COUNT(*) INTO preserved_count FROM pharmacy.rdp_products;
    RAISE NOTICE 'rdp_products count: % (should be > 0)', preserved_count;
    
    SELECT COUNT(*) INTO preserved_count FROM pharmacy.rdp_suppliers;
    RAISE NOTICE 'rdp_suppliers count: % (should be > 0)', preserved_count;
    
    SELECT COUNT(*) INTO preserved_count FROM pharmacy.rdp_users;
    RAISE NOTICE 'rdp_users count: % (should be > 0)', preserved_count;
    
    SELECT COUNT(*) INTO preserved_count FROM pharmacy.rdp_roles;
    RAISE NOTICE 'rdp_roles count: % (should be > 0)', preserved_count;
END $$;
