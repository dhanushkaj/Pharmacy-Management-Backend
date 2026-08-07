-- Migration: Add session_code column to rdp_users for user session switching
-- Created: 2026-08-06
-- Purpose: Enable switching user sessions by entering a unique code

-- Add session_code column to rdp_users table
ALTER TABLE pharmacy.rdp_users
ADD COLUMN session_code VARCHAR(20) UNIQUE;

-- Create index for faster lookups by session code
CREATE INDEX idx_rdp_users_session_code ON pharmacy.rdp_users(session_code);
