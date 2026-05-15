-- Add paidAt column to orders table
ALTER TABLE orders ADD COLUMN paid_at TIMESTAMP;
