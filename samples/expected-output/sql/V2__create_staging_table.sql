-- ============================================================================
-- Flyway Migration: V2__create_staging_table.sql
-- Purpose: Staging table for PROD.CUST.DAILY.UPDATES sequential file
-- Used by: CustomerBatch job (STEP1 - CUSTLOAD equivalent)
-- ============================================================================

-- -----------------------------------------------------------------------------
-- Table: customer_updates_staging
-- Staging area for daily customer update files
-- Mirrors CUST-DATA structure for bulk loading
-- -----------------------------------------------------------------------------
CREATE TABLE customer_updates_staging (
    staging_id          BIGINT          IDENTITY(1,1) NOT NULL,
    batch_id            UNIQUEIDENTIFIER NOT NULL,
    batch_timestamp     DATETIME2       NOT NULL DEFAULT GETUTCDATE(),
    
    -- Record data (matches CUST-DATA layout)
    cust_id             VARCHAR(10)     NOT NULL,
    cust_name           VARCHAR(30)     NULL,
    cust_addr           VARCHAR(40)     NULL,
    cust_city           VARCHAR(20)     NULL,
    cust_state          VARCHAR(2)      NULL,
    cust_zip            VARCHAR(10)     NULL,
    cust_balance        DECIMAL(9, 2)   NULL,
    cust_status         CHAR(1)         NULL,
    cust_last_update    DATE            NULL,
    cust_phone_count    SMALLINT        NULL,
    
    -- Denormalized phone data for staging (will be normalized during processing)
    phone_1_type        CHAR(1)         NULL,
    phone_1_number      VARCHAR(15)     NULL,
    phone_2_type        CHAR(1)         NULL,
    phone_2_number      VARCHAR(15)     NULL,
    phone_3_type        CHAR(1)         NULL,
    phone_3_number      VARCHAR(15)     NULL,
    
    -- Processing metadata
    record_status       VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    error_message       VARCHAR(500)    NULL,
    processed_at        DATETIME2       NULL,
    
    -- Audit columns
    created_at          DATETIME2       NOT NULL DEFAULT GETUTCDATE(),
    
    -- Constraints
    CONSTRAINT pk_customer_updates_staging PRIMARY KEY (staging_id),
    CONSTRAINT chk_staging_record_status CHECK (
        record_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'ERROR', 'SKIPPED')
    )
);

-- Indexes for batch processing
CREATE INDEX ix_staging_batch_id ON customer_updates_staging (batch_id);
CREATE INDEX ix_staging_record_status ON customer_updates_staging (record_status);
CREATE INDEX ix_staging_cust_id ON customer_updates_staging (cust_id);

-- -----------------------------------------------------------------------------
-- Table: batch_execution_log
-- Tracks batch job executions (replaces JCL job log)
-- -----------------------------------------------------------------------------
CREATE TABLE batch_execution_log (
    execution_id        BIGINT          IDENTITY(1,1) NOT NULL,
    batch_id            UNIQUEIDENTIFIER NOT NULL,
    job_name            VARCHAR(50)     NOT NULL,
    step_name           VARCHAR(50)     NOT NULL,
    
    -- Execution details
    start_time          DATETIME2       NOT NULL,
    end_time            DATETIME2       NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'RUNNING',
    exit_code           INT             NULL,
    
    -- Metrics
    records_read        INT             NULL,
    records_written     INT             NULL,
    records_skipped     INT             NULL,
    records_error       INT             NULL,
    
    -- Error details
    error_message       VARCHAR(2000)   NULL,
    
    -- Constraints
    CONSTRAINT pk_batch_execution_log PRIMARY KEY (execution_id),
    CONSTRAINT chk_batch_status CHECK (
        status IN ('RUNNING', 'COMPLETED', 'FAILED', 'STOPPED', 'ABANDONED')
    )
);

-- Indexes for querying execution history
CREATE INDEX ix_batch_log_batch_id ON batch_execution_log (batch_id);
CREATE INDEX ix_batch_log_job_step ON batch_execution_log (job_name, step_name);
CREATE INDEX ix_batch_log_start_time ON batch_execution_log (start_time DESC);
