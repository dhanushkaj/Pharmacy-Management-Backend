-- ============================================================================
-- Pharmacy Database - Reset Transactions and Inventory Script
-- ============================================================================
-- This script:
-- 1. Deletes all transaction data (audit, billings, GRNs, POs, etc.)
-- 2. Keeps categories and products
-- 3. Sets inventory to 200 for all products
-- ============================================================================

-- Safety: Start transaction
BEGIN TRANSACTION;

-- ============================================================================
-- Step 1: Disable Foreign Key Constraints (if needed)
-- ============================================================================

-- For PostgreSQL, constraints are usually handled with CASCADE
-- But we can set this for extra safety:
SET CONSTRAINTS ALL DEFERRED;

-- ============================================================================
-- Step 2: Delete Transaction Data
-- ============================================================================

-- 2.1: Delete Audit Logs
DELETE FROM audit_log;

-- 2.2: Delete Billing Related Data (order matters due to FKs)
DELETE FROM billing_item WHERE billing_id IN (SELECT billing_id FROM billing);
DELETE FROM billing;

-- 2.3: Delete GRN (Goods Received Notes) Related Data
DELETE FROM grn_item WHERE grn_id IN (SELECT grn_id FROM grn);
DELETE FROM grn;

-- 2.4: Delete Purchase Orders Related Data
DELETE FROM purchase_order_item WHERE purchase_order_id IN (SELECT purchase_order_id FROM purchase_order);
DELETE FROM purchase_order;

-- 2.5: Delete Customer Data
DELETE FROM customer;

-- 2.6: Delete Product Bin Data (Inventory Bins/Stock Location)
DELETE FROM product_bin;

-- 2.7: Delete Alert Configurations
DELETE FROM alert_configuration;

-- 2.8: Delete Sales Targets
DELETE FROM sales_target;

-- 2.9: Delete Day-end Records (if exists)
DELETE FROM day_end_manual_bill_record;
DELETE FROM day_end_record;

-- 2.10: Delete Stock Movement History (if exists)
DELETE FROM stock_movement;

-- 2.11: Delete Return Records (if exists)
DELETE FROM return_record;

-- ============================================================================
-- Step 3: Update Product Inventory to 200
-- ============================================================================

-- Set all product total stock to 200
-- This assumes there's a column for total stock (adjust column name if different)
UPDATE product SET total_stock = 200;

-- If inventory is managed in product_stock table instead:
-- UPDATE product_stock SET quantity = 200 WHERE quantity != 200;

-- ============================================================================
-- Step 4: Reset Auto-Increment Sequences (Optional, for clean IDs)
-- ============================================================================

-- Reset sequences to start fresh
ALTER SEQUENCE billing_billing_id_seq RESTART WITH 1;
ALTER SEQUENCE grn_grn_id_seq RESTART WITH 1;
ALTER SEQUENCE purchase_order_purchase_order_id_seq RESTART WITH 1;
ALTER SEQUENCE customer_customer_id_seq RESTART WITH 1;
ALTER SEQUENCE audit_log_id_seq RESTART WITH 1;

-- If other sequences exist, uncomment and adjust:
-- ALTER SEQUENCE sales_target_id_seq RESTART WITH 1;
-- ALTER SEQUENCE alert_configuration_id_seq SEQ RESTART WITH 1;

-- ============================================================================
-- Step 5: Verification Queries (Uncomment to verify before commit)
-- ============================================================================

-- SELECT COUNT(*) as billing_count FROM billing;
-- SELECT COUNT(*) as grn_count FROM grn;
-- SELECT COUNT(*) as customer_count FROM customer;
-- SELECT COUNT(*) as product_count FROM product;
-- SELECT product_id, product_name, total_stock FROM product LIMIT 5;

-- ============================================================================
-- Commit or Rollback
-- ============================================================================

-- If everything looks good, commit:
COMMIT;

-- If you want to undo, use:
-- ROLLBACK;

-- ============================================================================
-- Summary
-- ============================================================================
-- ✓ All audit logs deleted
-- ✓ All billings and billing items deleted
-- ✓ All GRNs and GRN items deleted
-- ✓ All purchase orders deleted
-- ✓ All customers deleted
-- ✓ All product bins (inventory) cleared
-- ✓ All alert configurations deleted
-- ✓ All sales targets deleted
-- ✓ Product inventory set to 200 for all products
-- ✓ Categories and products retained
-- ✓ Sequences reset for clean IDs
-- ============================================================================
