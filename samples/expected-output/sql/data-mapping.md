# COBOL to SQL Data Mapping

## Source: CUST-REC.cpy → Target: Azure SQL

This document details the field-by-field mapping from the COBOL copybook `CUST-REC.cpy` to Azure SQL tables.

---

## Summary

| Source | Target Table | Key | Notes |
|--------|-------------|-----|-------|
| CUST-DATA (01 level) | `customers` | cust_id | Main customer record |
| CUST-PHONES (OCCURS 3) | `customer_phones` | phone_id | Normalized child table |
| N/A | `customer_updates_staging` | staging_id | ETL staging table |

---

## Field Mappings

### Table: `customers`

| COBOL Field | PIC Clause | SQL Column | SQL Type | Notes |
|-------------|-----------|------------|----------|-------|
| CUST-ID | PIC X(10) | cust_id | VARCHAR(10) | Primary key (VSAM KSDS key) |
| CUST-NAME | PIC X(30) | cust_name | VARCHAR(30) | NOT NULL |
| CUST-ADDR | PIC X(40) | cust_addr | VARCHAR(40) | Nullable |
| CUST-CITY | PIC X(20) | cust_city | VARCHAR(20) | Nullable |
| CUST-STATE | PIC X(2) | cust_state | VARCHAR(2) | Nullable |
| CUST-ZIP | PIC X(10) | cust_zip | VARCHAR(10) | Nullable |
| CUST-BALANCE | PIC S9(7)V99 COMP-3 | cust_balance | DECIMAL(9,2) | **See precision note** |
| CUST-STATUS | PIC X(1) | cust_status | CHAR(1) | CHECK (A/I/S) |
| CUST-LAST-UPDATE | PIC 9(8) | cust_last_update | DATE | YYYYMMDD → DATE |
| CUST-PHONE-COUNT | PIC 9(2) | cust_phone_count | SMALLINT | Range 0-3 |
| FILLER | PIC X(23) | — | — | Not mapped |
| — | — | created_at | DATETIME2 | Audit column |
| — | — | updated_at | DATETIME2 | Audit column |

### Table: `customer_phones`

| COBOL Field | PIC Clause | SQL Column | SQL Type | Notes |
|-------------|-----------|------------|----------|-------|
| — | OCCURS index | phone_seq | SMALLINT | 1, 2, or 3 |
| PHONE-TYPE | PIC X(1) | phone_type | CHAR(1) | CHECK (H/W/M) |
| PHONE-NUMBER | PIC X(15) | phone_number | VARCHAR(15) | NOT NULL |
| — | — | phone_id | INT IDENTITY | Surrogate PK |
| — | — | cust_id | VARCHAR(10) | FK to customers |
| — | — | created_at | DATETIME2 | Audit column |
| — | — | updated_at | DATETIME2 | Audit column |

---

## Type Conversion Rules

### PIC X(n) → VARCHAR(n)
- Direct character-to-character mapping
- COBOL spaces are preserved; application layer converts to NULL if needed
- Trailing spaces may be trimmed by application

### PIC 9(n) → INT or SMALLINT
- `PIC 9(2)` → SMALLINT (0-99)
- `PIC 9(n)` where n ≤ 9 → INT
- `PIC 9(n)` where n > 9 → BIGINT

### PIC S9(n)V9(m) COMP-3 → DECIMAL(n+m, m)

**Critical: COMP-3 Packed Decimal Precision**

| COBOL | Precision | SQL Equivalent |
|-------|-----------|----------------|
| `PIC S9(7)V99 COMP-3` | 9 digits, 2 decimal | DECIMAL(9,2) |
| `PIC S9(5)V9(4) COMP-3` | 9 digits, 4 decimal | DECIMAL(9,4) |
| `PIC S9(15)V99 COMP-3` | 17 digits, 2 decimal | DECIMAL(17,2) |

**⚠️ IMPORTANT:** 
- NEVER use FLOAT or DOUBLE for financial/decimal COBOL fields
- Always use DECIMAL/NUMERIC for exact precision
- COMP-3 stores 2 digits per byte; ensure SQL precision matches

### 88-Level Conditions → CHECK Constraints

COBOL 88-level condition names map to SQL CHECK constraints:

```cobol
05 CUST-STATUS PIC X(1).
   88 CUST-ACTIVE    VALUE 'A'.
   88 CUST-INACTIVE  VALUE 'I'.
   88 CUST-SUSPENDED VALUE 'S'.
```

Maps to:
```sql
CONSTRAINT chk_customers_status CHECK (cust_status IN ('A', 'I', 'S'))
```

### OCCURS n TIMES → Child Table

The COBOL `OCCURS` clause creates a repeating group that must be normalized:

```cobol
05 CUST-PHONES OCCURS 3 TIMES.
   10 PHONE-TYPE   PIC X(1).
   10 PHONE-NUMBER PIC X(15).
```

**Normalized to separate table:**
- Parent: `customers` (one row per customer)
- Child: `customer_phones` (up to 3 rows per customer)
- Relationship: Foreign key with ON DELETE CASCADE

### PIC 9(8) Date → DATE

COBOL numeric dates stored as `YYYYMMDD`:

```cobol
05 CUST-LAST-UPDATE PIC 9(8).
```

**Conversion options:**
1. Store as DATE type (recommended)
2. Convert during ETL: `CONVERT(DATE, STUFF(STUFF(cust_last_update, 5, 0, '-'), 8, 0, '-'))`

---

## VSAM Key Mapping

| VSAM File | Type | Key Field(s) | SQL Equivalent |
|-----------|------|--------------|----------------|
| PROD.CUST.MASTER | KSDS | CUST-ID | PRIMARY KEY (cust_id) |
| PROD.CUST.DAILY.UPDATES | Sequential | — | Staging table |
| PROD.CUST.ERRORS | Sequential | — | batch_execution_log |

---

## Precision Considerations

### CUST-BALANCE Field

The `CUST-BALANCE` field is defined as `PIC S9(7)V99 COMP-3`:

- **Total digits:** 9 (7 integer + 2 decimal)
- **Range:** -9,999,999.99 to +9,999,999.99
- **Storage:** 5 bytes in COMP-3 (packed decimal)
- **SQL mapping:** `DECIMAL(9,2)`

**Validation rules:**
- Application must validate range before insert/update
- Consider adding CHECK constraint: `CHECK (cust_balance BETWEEN -9999999.99 AND 9999999.99)`

### Phone Number Field

The `PHONE-NUMBER` field is `PIC X(15)`:
- Stores formatted phone numbers (e.g., "1-800-555-1234")
- No format validation in COBOL; add application-layer validation
- Consider: VARCHAR(15) allows flexibility for international formats

---

## Record Size Analysis

### COBOL Record Size
| Component | Size (bytes) |
|-----------|-------------|
| CUST-ID | 10 |
| CUST-NAME | 30 |
| CUST-ADDR | 40 |
| CUST-CITY | 20 |
| CUST-STATE | 2 |
| CUST-ZIP | 10 |
| CUST-BALANCE (COMP-3) | 5 |
| CUST-STATUS | 1 |
| CUST-LAST-UPDATE | 8 |
| CUST-PHONE-COUNT | 2 |
| CUST-PHONES (3 × 16) | 48 |
| FILLER | 23 |
| **Total** | **199 bytes** |

### SQL Row Size (Estimated)
- `customers` table: ~150 bytes per row (variable due to VARCHAR)
- `customer_phones` table: ~50 bytes per row
- Combined: ~300 bytes per customer (with 3 phones)

---

## Migration Notes

1. **FILLER fields** are not migrated; they exist for record alignment only
2. **Audit columns** (`created_at`, `updated_at`) are added for modern data governance
3. **Surrogate keys** (`phone_id`) are added to normalized tables
4. **Indexes** are added based on common query patterns from COBOL programs
5. **Triggers** maintain `updated_at` timestamp automatically
