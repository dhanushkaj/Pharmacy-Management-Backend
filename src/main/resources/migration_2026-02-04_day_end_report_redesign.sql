-- Migration: Add new day-end report structure for pharmacy
-- Date: 2026-02-04

CREATE TABLE IF NOT EXISTS pharmacy.rdp_day_end_reports (
    day_end_report_id BIGSERIAL PRIMARY KEY,
    date VARCHAR(20) NOT NULL UNIQUE,
    branch VARCHAR(100),
    pos_id VARCHAR(50),
    cashier VARCHAR(100),
    shift VARCHAR(50),
    day_end_no VARCHAR(50),
    card_payments NUMERIC(14,2) DEFAULT 0,
    online_transfers NUMERIC(14,2) DEFAULT 0,
    customer_cheque_payments NUMERIC(14,2) DEFAULT 0,
    total_sales NUMERIC(14,2) DEFAULT 0,
    cash_sales NUMERIC(14,2) DEFAULT 0,
    card_sales NUMERIC(14,2) DEFAULT 0,
    online_transfer_sales NUMERIC(14,2) DEFAULT 0,
    cheque_sales NUMERIC(14,2) DEFAULT 0,
    returns NUMERIC(14,2) DEFAULT 0,
    expected_cash NUMERIC(14,2) DEFAULT 0,
    physical_cash_counted NUMERIC(14,2) DEFAULT 0,
    difference NUMERIC(14,2) DEFAULT 0,
    status VARCHAR(20),
    difference_reason VARCHAR(500),
    cashier_signature VARCHAR(100),
    supervisor_signature VARCHAR(100),
    printed_on VARCHAR(30)
);

CREATE TABLE IF NOT EXISTS pharmacy.rdp_day_end_note_breakdown (
    id BIGSERIAL PRIMARY KEY,
    day_end_report_id BIGINT REFERENCES pharmacy.rdp_day_end_reports(day_end_report_id) ON DELETE CASCADE,
    value INT NOT NULL,
    qty INT NOT NULL,
    total NUMERIC(14,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS pharmacy.rdp_day_end_coin_breakdown (
    id BIGSERIAL PRIMARY KEY,
    day_end_report_id BIGINT REFERENCES pharmacy.rdp_day_end_reports(day_end_report_id) ON DELETE CASCADE,
    value INT NOT NULL,
    qty INT NOT NULL,
    total NUMERIC(14,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS pharmacy.rdp_day_end_supplier_payments (
    id BIGSERIAL PRIMARY KEY,
    day_end_report_id BIGINT REFERENCES pharmacy.rdp_day_end_reports(day_end_report_id) ON DELETE CASCADE,
    supplier_name VARCHAR(200) NOT NULL,
    mode VARCHAR(20) NOT NULL, -- Cash or Cheque
    amount NUMERIC(14,2) NOT NULL
);
