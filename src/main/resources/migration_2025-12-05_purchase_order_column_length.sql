-- Migration script for Purchase Order and GRN column length changes
-- Date: 2025-12-05
-- Purpose: Increase order_code and grn_code column lengths to support longer codes

-- Background:
-- - PO format: PO-SUPPLIERNAME-20251126-0001 (requires up to 50 chars)
-- - GRN format: GRN-PO-SUPPLIERNAME-20251126-0001-0001 (requires up to 100 chars)

-- ========================================
-- 1. Purchase Order: order_code (30 -> 50)
-- ========================================

-- Check current column definition
SELECT 'Before PO change:' as status, column_name, data_type, character_maximum_length 
FROM information_schema.columns 
WHERE table_name = 'rdp_purchase_orders' AND column_name = 'order_code';

-- Increase order_code column length from 30 to 50
ALTER TABLE public.rdp_purchase_orders 
ALTER COLUMN order_code TYPE character varying(50);

-- Verify the change
SELECT 'After PO change:' as status, column_name, data_type, character_maximum_length 
FROM information_schema.columns 
WHERE table_name = 'rdp_purchase_orders' AND column_name = 'order_code';

-- ========================================
-- 2. GRN: grn_code (30 -> 100)
-- ========================================

-- Check if GRN table exists and check current column definition
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'rdp_grns') THEN
        -- Show current definition
        RAISE NOTICE 'GRN table exists, checking current column length...';
        
        -- Check current column definition
        PERFORM column_name, data_type, character_maximum_length 
        FROM information_schema.columns 
        WHERE table_name = 'rdp_grns' AND column_name = 'grn_code';
        
        -- Increase grn_code column length to 100
        ALTER TABLE public.rdp_grns 
        ALTER COLUMN grn_code TYPE character varying(100);
        
        RAISE NOTICE 'GRN grn_code column updated to 100 characters';
    ELSE
        RAISE NOTICE 'GRN table does not exist yet - will be created by Hibernate';
    END IF;
END $$;

-- Verify GRN change (if table exists)
SELECT 'After GRN change:' as status, column_name, data_type, character_maximum_length 
FROM information_schema.columns 
WHERE table_name = 'rdp_grns' AND column_name = 'grn_code';

-- ========================================
-- Notes
-- ========================================
-- If using spring.jpa.hibernate.ddl-auto=update, these changes will be applied automatically.
-- This script is provided for:
-- 1. Manual migrations in production environments
-- 2. Environments where DDL auto is disabled
-- 3. Documentation of schema changes

