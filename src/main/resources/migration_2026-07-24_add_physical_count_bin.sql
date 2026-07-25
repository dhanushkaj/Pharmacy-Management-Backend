-- Migration: Add PHYSICAL_COUNT to stock_movement bin type constraints
-- Created: 2026-07-24

-- Drop existing constraints
ALTER TABLE pharmacy.stock_movement DROP CONSTRAINT stock_movement_from_bin_check;
ALTER TABLE pharmacy.stock_movement DROP CONSTRAINT stock_movement_to_bin_check;

-- Add new constraints with PHYSICAL_COUNT
ALTER TABLE pharmacy.stock_movement 
ADD CONSTRAINT stock_movement_from_bin_check CHECK (((from_bin)::text = ANY ((ARRAY['PHYSICAL_COUNT'::character varying, 'GRN'::character varying, 'INVENTORY'::character varying, 'SOLD'::character varying, 'CUSTOMER_RETURN'::character varying, 'SUPPLIER_RETURN'::character varying, 'EXPIRED'::character varying, 'DAMAGED'::character varying])::text[])));

ALTER TABLE pharmacy.stock_movement 
ADD CONSTRAINT stock_movement_to_bin_check CHECK (((to_bin)::text = ANY ((ARRAY['PHYSICAL_COUNT'::character varying, 'GRN'::character varying, 'INVENTORY'::character varying, 'SOLD'::character varying, 'CUSTOMER_RETURN'::character varying, 'SUPPLIER_RETURN'::character varying, 'EXPIRED'::character varying, 'DAMAGED'::character varying])::text[])));
