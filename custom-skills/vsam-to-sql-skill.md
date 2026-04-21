# Custom Skill: VSAM to SQL

> A VS Code Agent Skill that converts VSAM file definitions and associated copybooks to Azure SQL schemas with Flyway migrations.

---

## Skill Definition

Create this skill in your project as `.github/skills/vsam-to-sql/SKILL.md`:

### `.github/skills/vsam-to-sql/SKILL.md`

```markdown
---
name: vsam-to-sql
description: Converts VSAM cluster definitions (KSDS, ESDS, RRDS) and their associated copybook record layouts to Azure SQL table schemas, Flyway migrations, and Spring Data JPA entities.
---

# VSAM to SQL Skill

## When to Use This Skill
Activate when:
- Converting VSAM file definitions to relational database schemas
- Generating Flyway migration scripts from mainframe data definitions
- Creating JPA entities that match VSAM record layouts
- Designing the data migration strategy for VSAM → Azure SQL

## VSAM Concept Mapping

### VSAM Organization → SQL Strategy
| VSAM Type | Description | SQL Strategy |
|-----------|-------------|-------------|
| **KSDS** | Key-Sequenced | Table with PRIMARY KEY from KEYS parameter |
| **ESDS** | Entry-Sequenced | Table with IDENTITY column (auto-increment) |
| **RRDS** | Relative-Record | Table with INT PRIMARY KEY (slot number) |
| **KSDS with AIX** | Alternate Index | Additional UNIQUE or non-unique indexes |
| **PATH** | Alternate index path | (handled by SQL indexes) |

### VSAM Parameters → SQL Features
| VSAM Parameter | SQL Equivalent | Notes |
|----------------|---------------|-------|
| `KEYS(length offset)` | PRIMARY KEY columns | Map to copybook fields at that offset |
| `RECORDSIZE(avg max)` | Total column sizes | Verify columns fit |
| `SHAREOPTIONS(cr,cs)` | Isolation level | (2,3) = READ COMMITTED typical |
| `FREESPACE(ci ca)` | Fill factor | `CREATE INDEX ... WITH (FILLFACTOR=n)` |
| `UNIQUE` | UNIQUE constraint | On primary key columns |
| `NONUNIQUEKEY` (AIX) | Non-unique INDEX | Secondary index |
| `UPGRADE` (AIX) | (automatic in SQL) | Index maintained on write |

## Conversion Process

### Input 1: VSAM Cluster Definition
```
DEFINE CLUSTER (                              -
    NAME(PROD.CUSTOMER.MASTER)                -
    INDEXED                                    -
    KEYS(10 0)                                -
    RECORDSIZE(250 500)                        -
    SHAREOPTIONS(2 3)                          -
    FREESPACE(20 10)                           -
  ) DATA (                                     -
    NAME(PROD.CUSTOMER.MASTER.DATA)           -
  ) INDEX (                                    -
    NAME(PROD.CUSTOMER.MASTER.INDEX)          -
  )

DEFINE ALTERNATEINDEX (                        -
    NAME(PROD.CUSTOMER.MASTER.NAMEAIX)        -
    RELATE(PROD.CUSTOMER.MASTER)              -
    KEYS(30 10)                               -
    NONUNIQUEKEY                               -
    UPGRADE                                    -
  )
```

### Input 2: Associated Copybook
```cobol
01 CUSTOMER-RECORD.
   05 CUST-KEY.
      10 CUST-ID        PIC X(10).
   05 CUST-NAME          PIC X(30).
   05 CUST-DETAILS.
      10 ADDR-LINE1      PIC X(40).
      10 ADDR-CITY        PIC X(20).
      10 ADDR-STATE       PIC X(2).
      10 ADDR-ZIP         PIC X(10).
   05 CUST-BALANCE       PIC S9(7)V99 COMP-3.
   05 CUST-STATUS        PIC X(1).
      88 CUST-ACTIVE     VALUE 'A'.
      88 CUST-INACTIVE   VALUE 'I'.
      88 CUST-SUSPENDED  VALUE 'S'.
   05 CUST-LAST-UPDATE   PIC 9(8).
   05 CUST-PHONE-COUNT   PIC 9(2).
   05 CUST-PHONES OCCURS 5 TIMES.
      10 PHONE-TYPE      PIC X(1).
      10 PHONE-NUMBER    PIC X(15).
   05 FILLER             PIC X(47).
```

### Output 1: Flyway Migration Script

```sql
-- V1__create_customer_tables.sql
-- Source: VSAM KSDS PROD.CUSTOMER.MASTER
-- Copybook: CUSTOMER-RECORD

CREATE TABLE customers (
    -- Primary key from VSAM KEYS(10 0) → CUST-ID at offset 0, length 10
    cust_id         VARCHAR(10)     NOT NULL,
    cust_name       VARCHAR(30)     NOT NULL,
    addr_line1      VARCHAR(40),
    addr_city       VARCHAR(20),
    addr_state      CHAR(2),
    addr_zip        VARCHAR(10),
    cust_balance    DECIMAL(9,2)    NOT NULL DEFAULT 0.00,
    cust_status     CHAR(1)         NOT NULL DEFAULT 'A',
    cust_last_update DATE,
    -- Audit columns (not in COBOL)
    created_at      DATETIME2       NOT NULL DEFAULT GETDATE(),
    updated_at      DATETIME2       NOT NULL DEFAULT GETDATE(),

    CONSTRAINT pk_customers PRIMARY KEY (cust_id),
    CONSTRAINT chk_cust_status CHECK (cust_status IN ('A', 'I', 'S'))
);

-- Alternate index: NAMEAIX → KEYS(30 10) → CUST-NAME at offset 10, length 30
CREATE INDEX idx_customers_name ON customers (cust_name);

-- OCCURS 5 TIMES → Child table
CREATE TABLE customer_phones (
    id              INT             IDENTITY(1,1) NOT NULL,
    cust_id         VARCHAR(10)     NOT NULL,
    phone_type      CHAR(1)         NOT NULL,
    phone_number    VARCHAR(15)     NOT NULL,

    CONSTRAINT pk_customer_phones PRIMARY KEY (id),
    CONSTRAINT fk_customer_phones_cust FOREIGN KEY (cust_id)
        REFERENCES customers(cust_id) ON DELETE CASCADE
);

CREATE INDEX idx_customer_phones_cust ON customer_phones (cust_id);
```

### Output 2: Rollback Script

```sql
-- U1__drop_customer_tables.sql
DROP TABLE IF EXISTS customer_phones;
DROP TABLE IF EXISTS customers;
```

### Output 3: JPA Entity

```java
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @Column(name = "cust_id", length = 10)
    private String custId;

    @Column(name = "cust_name", length = 30, nullable = false)
    private String custName;

    @Embedded
    private Address address;

    @Column(name = "cust_balance", precision = 9, scale = 2)
    private BigDecimal custBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "cust_status", length = 1)
    private CustomerStatus custStatus;

    @Column(name = "cust_last_update")
    private LocalDate custLastUpdate;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Size(max = 5)
    private List<CustomerPhone> phones = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
```

## Validation Rules
1. Total column sizes must not exceed VSAM RECORDSIZE(max)
2. Primary key columns must match VSAM KEYS(length offset)
3. OCCURS arrays must become child tables (not JSON columns) for >3 fields
4. FILLER fields are skipped (no SQL column)
5. Every table gets created_at + updated_at audit columns
6. Every migration script has a corresponding rollback script
```

---

## How to Invoke

```
Use the vsam-to-sql skill to convert VSAM cluster PROD.CUSTOMER.MASTER 
with copybook CUSTOMER-RECORD to Azure SQL.
Generate: DDL, Flyway migration, rollback, and JPA entity.
```
