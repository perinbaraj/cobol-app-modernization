-- ============================================================================
-- Flyway Undo Migration: U1__drop_customer_tables.sql
-- Rollback for: V1__create_customer_tables.sql
-- WARNING: This will permanently delete all customer data!
-- ============================================================================

-- Drop triggers first
IF OBJECT_ID('trg_customer_phones_updated_at', 'TR') IS NOT NULL
    DROP TRIGGER trg_customer_phones_updated_at;
GO

IF OBJECT_ID('trg_customers_updated_at', 'TR') IS NOT NULL
    DROP TRIGGER trg_customers_updated_at;
GO

-- Drop child table first (foreign key dependency)
IF OBJECT_ID('customer_phones', 'U') IS NOT NULL
    DROP TABLE customer_phones;

-- Drop parent table
IF OBJECT_ID('customers', 'U') IS NOT NULL
    DROP TABLE customers;
