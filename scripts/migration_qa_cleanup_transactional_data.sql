-- ============================================================================
-- QA ENVIRONMENT DATA CLEANUP SCRIPT
-- ============================================================================
-- Purpose: Delete all transactional/test data while preserving master data
-- Date: 2026-03-02
-- 
-- PRESERVED DATA (Master Data):
--   - rdp_categories (Product categories)
--   - rdp_products (Products)
--   - rdp_suppliers (Suppliers - master data, not transactional)
--   - rdp_users (User accounts)
--   - rdp_roles (User roles)
--   - rdp_user_roles (User-role mappings)
--   - rdp_store_settings (Store configuration)
--
-- DELETED DATA (Transactional Data):
--   - rdp_billings, rdp_billing_items (Sales transactions)
--   - rdp_grns, rdp_grn_items (Goods received notes)
--   - rdp_purchase_orders, rdp_purchase_order_items (Purchase orders)
--   - rdp_inventory_items (Inventory batches - will be recreated via GRN)
--   - rdp_inventory_returns (Return transactions)
--   - rdp_customers (Customer records - consider if master data)
--   - rdp_alert_config, rdp_alert_logs (Alert system data)
--   - audit_logs (Audit trail)
--   - stock_movement (Stock movement history)
--   - rdp_day_end_reports and related tables (Day end reports)
--   - rdp_day_end_manual_bill (Manual bill entries)
-- ============================================================================

-- Set search path to pharmacy schema
SET search_path TO pharmacy, public;

-- Start transaction
BEGIN;

-- ============================================================================
-- STEP 1: DELETE CHILD/DEPENDENT TABLES FIRST (Foreign Key Order)
-- ============================================================================

-- Delete billing items (child of billings and products)
DELETE FROM rdp_billing_items;

-- Delete billings (depends on customers)
DELETE FROM rdp_billings;

-- Delete GRN items (child of grns and products)
DELETE FROM rdp_grn_items;

-- Delete GRNs (depends on purchase orders)
DELETE FROM rdp_grns;

-- Delete purchase order items (child of purchase orders and products)
DELETE FROM rdp_purchase_order_items;

-- Delete purchase orders (depends on suppliers)
DELETE FROM rdp_purchase_orders;

-- Delete inventory items (depends on products)
DELETE FROM rdp_inventory_items;

-- Delete inventory returns (depends on products and suppliers)
DELETE FROM rdp_inventory_returns;

-- Delete stock movements (depends on products)
DELETE FROM stock_movement;

-- Delete customers (billing references removed already)
DELETE FROM rdp_customers;

-- ============================================================================
-- STEP 2: DELETE ALERT SYSTEM DATA
-- ============================================================================

DELETE FROM rdp_alert_logs;

DELETE FROM rdp_alert_config;

-- ============================================================================
-- STEP 3: DELETE AUDIT TRAIL
-- ============================================================================

DELETE FROM audit_logs;

-- ============================================================================
-- STEP 4: DELETE DAY END REPORT DATA
-- ============================================================================

-- Delete day end child tables first (due to FK constraints)
DELETE FROM rdp_day_end_supplier_payments;
DELETE FROM rdp_day_end_note_breakdown;
DELETE FROM rdp_day_end_coin_breakdown;

-- Delete day end manual bills
DELETE FROM rdp_day_end_manual_bill;

-- Delete day end reports (parent table)
DELETE FROM rdp_day_end_reports;

-- ============================================================================
-- STEP 5: RESET SEQUENCES FOR CLEAN IDs
-- ============================================================================

-- Reset transactional table sequences
ALTER SEQUENCE rdp_billing_items_billing_item_id_seq RESTART WITH 1;
ALTER SEQUENCE rdp_billings_billing_id_seq RESTART WITH 1;
ALTER SEQUENCE rdp_grn_items_grn_item_id_seq RESTART WITH 1;
ALTER SEQUENCE rdp_grns_grn_id_seq RESTART WITH 1;
ALTER SEQUENCE rdp_purchase_order_items_poi_id_seq RESTART WITH 1;
ALTER SEQUENCE rdp_purchase_orders_po_id_seq RESTART WITH 1;
ALTER SEQUENCE rdp_inventory_items_inv_id_seq RESTART WITH 1;
ALTER SEQUENCE rdp_inventory_returns_return_id_seq RESTART WITH 1;
ALTER SEQUENCE rdp_customers_customer_id_seq RESTART WITH 1;
ALTER SEQUENCE rdp_alert_logs_alert_log_id_seq RESTART WITH 1;
ALTER SEQUENCE rdp_alert_config_alert_config_id_seq RESTART WITH 1;
ALTER SEQUENCE audit_logs_id_seq RESTART WITH 1;
ALTER SEQUENCE stock_movement_id_seq RESTART WITH 1;

-- Commit the transaction
COMMIT;

-- ============================================================================
-- POST-CLEANUP VERIFICATION (Run separately after script)
-- ============================================================================
/*
-- Verify deleted tables are empty:
SELECT 'rdp_billing_items' as table_name, COUNT(*) as row_count FROM pharmacy.rdp_billing_items
UNION ALL SELECT 'rdp_billings', COUNT(*) FROM pharmacy.rdp_billings
UNION ALL SELECT 'rdp_grn_items', COUNT(*) FROM pharmacy.rdp_grn_items
UNION ALL SELECT 'rdp_grns', COUNT(*) FROM pharmacy.rdp_grns
UNION ALL SELECT 'rdp_purchase_order_items', COUNT(*) FROM pharmacy.rdp_purchase_order_items
UNION ALL SELECT 'rdp_purchase_orders', COUNT(*) FROM pharmacy.rdp_purchase_orders
UNION ALL SELECT 'rdp_inventory_items', COUNT(*) FROM pharmacy.rdp_inventory_items
UNION ALL SELECT 'rdp_inventory_returns', COUNT(*) FROM pharmacy.rdp_inventory_returns
UNION ALL SELECT 'rdp_customers', COUNT(*) FROM pharmacy.rdp_customers
UNION ALL SELECT 'rdp_alert_logs', COUNT(*) FROM pharmacy.rdp_alert_logs
UNION ALL SELECT 'rdp_alert_config', COUNT(*) FROM pharmacy.rdp_alert_config
UNION ALL SELECT 'audit_logs', COUNT(*) FROM pharmacy.audit_logs
UNION ALL SELECT 'stock_movement', COUNT(*) FROM pharmacy.stock_movement
UNION ALL SELECT 'rdp_day_end_reports', COUNT(*) FROM pharmacy.rdp_day_end_reports
UNION ALL SELECT 'rdp_day_end_manual_bill', COUNT(*) FROM pharmacy.rdp_day_end_manual_bill;

-- Verify preserved tables have data:
SELECT 'rdp_categories' as table_name, COUNT(*) as row_count FROM pharmacy.rdp_categories
UNION ALL SELECT 'rdp_products', COUNT(*) FROM pharmacy.rdp_products
UNION ALL SELECT 'rdp_suppliers', COUNT(*) FROM pharmacy.rdp_suppliers
UNION ALL SELECT 'rdp_users', COUNT(*) FROM pharmacy.rdp_users
UNION ALL SELECT 'rdp_roles', COUNT(*) FROM pharmacy.rdp_roles
UNION ALL SELECT 'rdp_user_roles', COUNT(*) FROM pharmacy.rdp_user_roles
UNION ALL SELECT 'rdp_store_settings', COUNT(*) FROM pharmacy.rdp_store_settings;
*/

-- ============================================================================
-- NOTES:
-- ============================================================================
-- 1. Run this script BEFORE creating the pg_dump for QA environment
-- 2. Suppliers are preserved as they are master data (supplier catalog)
-- 3. Products reference suppliers, so suppliers must be kept
-- 4. After running this, create dump with:
--    pg_dump -h localhost -U pharmacy -d postgres -n pharmacy -F c -f qa_clean_dump.backup
-- 5. To restore on QA server:
--    pg_restore -h qa_host -U pharmacy -d postgres -c qa_clean_dump.backup
-- ============================================================================
