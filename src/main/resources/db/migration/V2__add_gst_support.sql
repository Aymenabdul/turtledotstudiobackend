-- =========================================================
-- V2__add_gst_support.sql
-- Add GSTIN and Tax support for Studio, Clients, and Payments
-- Compatible with MySQL 8 and H2 MySQL mode
-- =========================================================

-- Studios Table: Add GST and Tax fields
ALTER TABLE studios ADD COLUMN gstin VARCHAR(20);
ALTER TABLE studios ADD COLUMN pan_number VARCHAR(20);
ALTER TABLE studios ADD COLUMN state_name VARCHAR(100);
ALTER TABLE studios ADD COLUMN state_code VARCHAR(10);
ALTER TABLE studios ADD COLUMN gst_rate_percent DECIMAL(5,2) DEFAULT 18.00;

-- Clients Table: Add Client GSTIN for B2B billing
ALTER TABLE clients ADD COLUMN gstin VARCHAR(20);

-- Payments Table: Add Invoice Number for GST tax invoice tracking
ALTER TABLE payments ADD COLUMN invoice_number VARCHAR(50);
