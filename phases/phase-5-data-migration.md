# Phase 5: Data Migration — VSAM/DB2 → Azure SQL & MongoDB

## Objective
Convert mainframe data structures (VSAM files, DB2 tables, COBOL copybooks) into modern database schemas (Azure SQL, MongoDB) with migration scripts, JPA entity mappings, and data validation — all accelerated by GitHub Copilot.

---

## Inputs
| Input | Source | Description |
|-------|--------|-------------|
| VSAM file definitions | Source repo | KSDS, ESDS, RRDS definitions |
| DB2 DDL scripts | Source repo | Table definitions, indexes, constraints |
| COBOL copybooks | Source repo | Record layouts (→ column definitions) |
| Data volume estimates | Phase 1 | Row counts, growth patterns |
| Java entities | Phase 2 | Target JPA entities to align |

## Outputs
| Output | Format | Description |
|--------|--------|-------------|
| Azure SQL schemas | DDL scripts | Table definitions with constraints |
| MongoDB models | JSON Schema | Document structure definitions |
| JPA entities | Java 17 classes | Spring Data JPA annotated entities |
| Migration scripts | Flyway SQL | Versioned schema migrations |
| Data mapping docs | Markdown | COBOL field → SQL column mappings |
| ETL scripts | Spring Batch / SQL | Data movement and transformation |

---

## GitHub Copilot Features Used

### 1. `vsam-to-sql` Custom Skill
Automated conversion of VSAM file definitions and copybook layouts to SQL schemas.

**What the skill does:**
- Parses VSAM cluster definitions (KSDS key → PRIMARY KEY)
- Reads COBOL copybook record layouts
- Maps COBOL PIC clauses to SQL data types
- Generates CREATE TABLE statements with constraints
- Generates Flyway migration scripts

### 2. `copybook-mapper` Custom Skill
Converts COBOL data structures to database columns.

**Data type mapping applied:**
| COBOL Type | Azure SQL | MongoDB | Java |
|------------|-----------|---------|------|
| `PIC X(n)` | `VARCHAR(n)` | `String` | `String` |
| `PIC 9(n)` | `INT` or `BIGINT` | `Number (int)` | `int` / `long` |
| `PIC 9(n)V9(m)` | `DECIMAL(n+m,m)` | `Number (double)` | `BigDecimal` |
| `PIC S9(n) COMP` | `INT` | `Number (int)` | `int` |
| `PIC S9(n) COMP-3` | `DECIMAL` | `Number` | `BigDecimal` |
| `OCCURS n TIMES` | Separate table (1:N) | Array | `List<T>` |
| `REDEFINES` | Nullable columns | Variant field | Union type |
| Date (`PIC 9(8)`) | `DATE` | `ISODate` | `LocalDate` |
| Timestamp | `DATETIME2` | `ISODate` | `Instant` |

### 3. Copilot Chat (Schema Design)
For complex data modeling decisions:
```
Given this COBOL copybook with nested OCCURS and REDEFINES, 
recommend whether to use:
1. A single denormalized table
2. Normalized parent/child tables
3. A MongoDB document

Consider: query patterns, data volume, and the Java JPA entities 
already generated in Phase 2.
```

### 4. Copilot Coding Agent
Create Issues for batch schema generation:
```markdown
## Generate Azure SQL Schema: Customer Domain

Source copybooks: CUST-REC.cpy, ADDR-REC.cpy, PHONE-REC.cpy
Target: Azure SQL with Flyway migrations

Requirements:
- Generate CREATE TABLE statements
- Add appropriate indexes
- Generate JPA entity classes
- Generate Flyway V1__create_customer_tables.sql
- Include rollback scripts
```

---

## Conversion Patterns

### Pattern 1: VSAM KSDS → Azure SQL Table

**VSAM definition:**
```
DEFINE CLUSTER (NAME(PROD.CUSTOMER.MASTER) -
  KEYS(10 0) -
  RECORDSIZE(250 500) -
  SHAREOPTIONS(2 3))
```

**Copybook:**
```cobol
01 CUSTOMER-RECORD.
   05 CUST-ID           PIC X(10).
   05 CUST-NAME          PIC X(30).
   05 CUST-ADDR.
      10 ADDR-LINE1      PIC X(40).
      10 ADDR-CITY        PIC X(20).
      10 ADDR-STATE       PIC X(2).
      10 ADDR-ZIP         PIC X(10).
   05 CUST-BALANCE       PIC S9(7)V99 COMP-3.
   05 CUST-STATUS        PIC X(1).
      88 ACTIVE           VALUE 'A'.
      88 INACTIVE         VALUE 'I'.
   05 CUST-LAST-UPDATE   PIC 9(8).
```

**Azure SQL (Copilot output):**
```sql
-- Flyway: V1__create_customer_table.sql
CREATE TABLE customers (
    cust_id         VARCHAR(10)    NOT NULL PRIMARY KEY,
    cust_name       VARCHAR(30)    NOT NULL,
    addr_line1      VARCHAR(40),
    addr_city       VARCHAR(20),
    addr_state      CHAR(2),
    addr_zip        VARCHAR(10),
    cust_balance    DECIMAL(9,2)   NOT NULL DEFAULT 0,
    cust_status     CHAR(1)        NOT NULL DEFAULT 'A'
        CHECK (cust_status IN ('A', 'I')),
    cust_last_update DATE,
    created_at      DATETIME2      DEFAULT GETDATE(),
    updated_at      DATETIME2      DEFAULT GETDATE()
);

CREATE INDEX idx_customers_status ON customers(cust_status);
CREATE INDEX idx_customers_name ON customers(cust_name);
```

**JPA Entity (Copilot output):**
```java
@Entity
@Table(name = "customers")
public record Customer(
    @Id
    @Column(name = "cust_id", length = 10)
    String custId,

    @Column(name = "cust_name", length = 30, nullable = false)
    String custName,

    @Embedded
    Address address,

    @Column(name = "cust_balance", precision = 9, scale = 2)
    BigDecimal custBalance,

    @Column(name = "cust_status", length = 1)
    CustomerStatus custStatus,

    @Column(name = "cust_last_update")
    LocalDate custLastUpdate
) {}
```

### Pattern 2: OCCURS → Child Table

**COBOL:**
```cobol
01 ORDER-RECORD.
   05 ORDER-ID           PIC X(10).
   05 ORDER-DATE          PIC 9(8).
   05 LINE-ITEM-COUNT     PIC 9(3).
   05 LINE-ITEMS OCCURS 50 TIMES.
      10 ITEM-CODE        PIC X(10).
      10 ITEM-QTY         PIC 9(5).
      10 ITEM-PRICE       PIC 9(5)V99.
```

**Azure SQL (Copilot output):**
```sql
CREATE TABLE orders (
    order_id    VARCHAR(10)  NOT NULL PRIMARY KEY,
    order_date  DATE         NOT NULL,
    line_item_count INT      NOT NULL DEFAULT 0
);

CREATE TABLE order_line_items (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    order_id    VARCHAR(10)  NOT NULL REFERENCES orders(order_id),
    item_code   VARCHAR(10)  NOT NULL,
    item_qty    INT          NOT NULL,
    item_price  DECIMAL(7,2) NOT NULL
);
```

### Pattern 3: VSAM → MongoDB Document

**For document-oriented data:**
```json
// MongoDB Schema (Copilot output)
{
  "$jsonSchema": {
    "bsonType": "object",
    "required": ["custId", "custName"],
    "properties": {
      "custId": { "bsonType": "string", "maxLength": 10 },
      "custName": { "bsonType": "string", "maxLength": 30 },
      "address": {
        "bsonType": "object",
        "properties": {
          "line1": { "bsonType": "string" },
          "city": { "bsonType": "string" },
          "state": { "bsonType": "string", "maxLength": 2 },
          "zip": { "bsonType": "string", "maxLength": 10 }
        }
      },
      "balance": { "bsonType": "decimal" },
      "status": { "enum": ["A", "I"] },
      "lastUpdate": { "bsonType": "date" }
    }
  }
}
```

---

## Step-by-Step Workflow

### Step 1: Inventory Data Assets
1. List all VSAM clusters (KSDS, ESDS, RRDS)
2. List all DB2 tables
3. Map each to its COBOL copybook(s)

### Step 2: Decide Target Database
| Data Pattern | Target DB | Reason |
|-------------|-----------|--------|
| Structured, relational | Azure SQL | Strong consistency, joins |
| Document-oriented, nested | MongoDB | Flexible schema, nested data |
| Analytics/warehouse | GCP BigQuery | Large-scale analytics |

### Step 3: Generate Schemas
1. Use `vsam-to-sql` skill for each VSAM/copybook pair
2. Review generated DDL and adjust indexes
3. Generate Flyway migration scripts

### Step 4: Generate JPA Entities
1. Use `copybook-mapper` skill to generate entities
2. Align with Phase 2 Java services
3. Add Spring Data repositories

### Step 5: Create ETL Scripts
1. Use Copilot to generate Spring Batch readers for VSAM export files
2. Generate data transformation processors
3. Generate database writers

### Step 6: Validate Data
1. Compare record counts (source vs. target)
2. Checksum validation on key fields
3. Run data quality checks

---

## Tips
- **COBOL dates are tricky**: `PIC 9(8)` could be YYYYMMDD, MMDDYYYY, or Julian — confirm the format
- **COMP-3 precision**: Always map to DECIMAL, never FLOAT — financial data demands precision
- **OCCURS DEPENDING ON**: Variable-length arrays need special handling — use a count column + child table
- **REDEFINES**: Don't create multiple columns — choose the dominant type and add a discriminator
- **Null handling**: COBOL has no nulls — spaces and zeros are used instead. Define explicit NULL mapping rules
