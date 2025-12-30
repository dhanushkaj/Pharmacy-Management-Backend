-- Migration: add stock_movement table and bin_type enum

-- create enum type for bin types
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'bin_type') THEN
        CREATE TYPE bin_type AS ENUM (
            'GRN', 'INVENTORY', 'SOLD', 'CUSTOMER_RETURN', 'SUPPLIER_RETURN', 'EXPIRED', 'DAMAGED'
        );
    END IF;
END$$;

-- create stock_movement table
CREATE TABLE IF NOT EXISTS stock_movement (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    batch_no TEXT NOT NULL,
    from_bin bin_type NOT NULL,
    to_bin bin_type NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    reference_type TEXT,
    reference_id TEXT,
    performed_by TEXT,
    remarks TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    CONSTRAINT fk_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE INDEX IF NOT EXISTS idx_sm_product_batch_date ON stock_movement(product_id, batch_no, created_at);
