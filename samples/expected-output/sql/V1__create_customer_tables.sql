-- ============================================================================
-- Flyway Migration: V1__create_customer_tables.sql
-- Generated from: CUST-REC.cpy copybook
-- Source VSAM: PROD.CUST.MASTER (KSDS)
-- ============================================================================

-- -----------------------------------------------------------------------------
-- Table: customers
-- Maps to: CUST-DATA record layout from CUST-REC.cpy
-- VSAM Key: CUST-ID (Primary Key)
-- -----------------------------------------------------------------------------
CREATE TABLE customers (
    -- Primary key from VSAM KSDS key field
    cust_id             VARCHAR(10)     NOT NULL,
    
    -- Customer demographic fields
    cust_name           VARCHAR(30)     NOT NULL,
    cust_addr           VARCHAR(40)     NULL,
    cust_city           VARCHAR(20)     NULL,
    cust_state          VARCHAR(2)      NULL,
    cust_zip            VARCHAR(10)     NULL,
    
    -- Financial field: PIC S9(7)V99 COMP-3 -> DECIMAL(9,2)
    cust_balance        DECIMAL(9, 2)   NOT NULL DEFAULT 0.00,
    
    -- Status field with 88-level values as CHECK constraint
    cust_status         CHAR(1)         NOT NULL DEFAULT 'A',
    
    -- Date field: PIC 9(8) stored as YYYYMMDD -> DATE type
    cust_last_update    DATE            NULL,
    
    -- Phone count: PIC 9(2) -> SMALLINT
    cust_phone_count    SMALLINT        NOT NULL DEFAULT 0,
    
    -- Audit columns (required by coding standards)
    created_at          DATETIME2       NOT NULL DEFAULT GETUTCDATE(),
    updated_at          DATETIME2       NOT NULL DEFAULT GETUTCDATE(),
    
    -- Constraints
    CONSTRAINT pk_customers PRIMARY KEY (cust_id),
    CONSTRAINT chk_customers_status CHECK (cust_status IN ('A', 'I', 'S')),
    CONSTRAINT chk_customers_phone_count CHECK (cust_phone_count >= 0 AND cust_phone_count <= 3)
);

-- Index for common queries
CREATE INDEX ix_customers_status ON customers (cust_status);
CREATE INDEX ix_customers_last_update ON customers (cust_last_update);

-- -----------------------------------------------------------------------------
-- Table: customer_phones
-- Maps to: CUST-PHONES OCCURS 3 TIMES from CUST-REC.cpy
-- Normalized from repeating group to child table with foreign key
-- -----------------------------------------------------------------------------
CREATE TABLE customer_phones (
    phone_id            INT             IDENTITY(1,1) NOT NULL,
    cust_id             VARCHAR(10)     NOT NULL,
    phone_seq           SMALLINT        NOT NULL,  -- 1-3, maps to OCCURS index
    
    -- Phone type: PIC X(1) with 88-level values
    phone_type          CHAR(1)         NOT NULL,
    
    -- Phone number: PIC X(15)
    phone_number        VARCHAR(15)     NOT NULL,
    
    -- Audit columns
    created_at          DATETIME2       NOT NULL DEFAULT GETUTCDATE(),
    updated_at          DATETIME2       NOT NULL DEFAULT GETUTCDATE(),
    
    -- Constraints
    CONSTRAINT pk_customer_phones PRIMARY KEY (phone_id),
    CONSTRAINT fk_customer_phones_customer FOREIGN KEY (cust_id) 
        REFERENCES customers (cust_id) ON DELETE CASCADE,
    CONSTRAINT uq_customer_phones UNIQUE (cust_id, phone_seq),
    CONSTRAINT chk_customer_phones_type CHECK (phone_type IN ('H', 'W', 'M')),
    CONSTRAINT chk_customer_phones_seq CHECK (phone_seq BETWEEN 1 AND 3)
);

-- Index for lookups by customer
CREATE INDEX ix_customer_phones_cust_id ON customer_phones (cust_id);

-- -----------------------------------------------------------------------------
-- Trigger: Update updated_at on customers modification
-- -----------------------------------------------------------------------------
GO
CREATE TRIGGER trg_customers_updated_at
ON customers
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE customers
    SET updated_at = GETUTCDATE()
    FROM customers c
    INNER JOIN inserted i ON c.cust_id = i.cust_id;
END;
GO

-- -----------------------------------------------------------------------------
-- Trigger: Update updated_at on customer_phones modification
-- -----------------------------------------------------------------------------
CREATE TRIGGER trg_customer_phones_updated_at
ON customer_phones
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE customer_phones
    SET updated_at = GETUTCDATE()
    FROM customer_phones cp
    INNER JOIN inserted i ON cp.phone_id = i.phone_id;
END;
GO
