# Custom Skill: Copybook Mapper

> A VS Code Agent Skill that maps COBOL copybooks to Java DTOs, JPA entities, and database schemas.

---

## Skill Definition

Create this skill in your project as `.github/skills/copybook-mapper/SKILL.md`:

### `.github/skills/copybook-mapper/SKILL.md`

```markdown
---
name: copybook-mapper
description: Converts COBOL copybook data structures to Java records/DTOs, JPA entities, Azure SQL DDL, and MongoDB schemas. The single source of truth for data type mapping.
---

# Copybook Mapper Skill

## When to Use This Skill
Activate when:
- Converting COBOL copybooks (.cpy) to Java types
- Generating database schemas from copybook layouts
- Creating data migration mappings
- Validating type conversions in migrated code

## Master Type Mapping Table

### COBOL → Java
| COBOL PIC/USAGE | Java Type | Annotation | Notes |
|-----------------|-----------|------------|-------|
| `PIC X(n)` | `String` | `@Size(max=n)` | Trim trailing spaces |
| `PIC A(n)` | `String` | `@Size(max=n), @Pattern("[A-Za-z ]*")` | Alpha only |
| `PIC 9(n)` n≤9 | `int` | `@Max(10^n - 1)` | |
| `PIC 9(n)` n>9 | `long` | `@Max(10^n - 1)` | |
| `PIC 9(n)V9(m)` | `BigDecimal` | `@Digits(integer=n, fraction=m)` | Scale = m |
| `PIC S9(n)` | `int` (signed) | | Preserve sign |
| `PIC S9(n) COMP` | `int` | | Binary format |
| `PIC S9(n) COMP-3` | `BigDecimal` | `@Digits(...)` | Packed decimal |
| `PIC S9(n)V9(m) COMP-3` | `BigDecimal` | `@Digits(integer=n, fraction=m)` | Packed with decimal |
| `OCCURS n TIMES` | `List<T>` | `@Size(max=n)` | Fixed array |
| `OCCURS n TO m DEPENDING ON x` | `List<T>` | `@Size(min=n, max=m)` | Variable array |
| `88 value-name VALUE 'X'` | `enum` | | Condition name → enum value |
| `REDEFINES other-field` | Sealed interface | | Java 17 sealed types |
| `FILLER` | (skip) | | No Java field |
| Level 66 RENAMES | (skip) | | No Java equivalent |
| Level 77 standalone | Field | | Independent item |

### COBOL → Azure SQL
| COBOL PIC/USAGE | SQL Type | Constraint |
|-----------------|----------|------------|
| `PIC X(n)` | `VARCHAR(n)` | |
| `PIC X(1)` with 88-levels | `CHAR(1)` | `CHECK (col IN (...))` |
| `PIC 9(n)` n≤9 | `INT` | |
| `PIC 9(n)` n>9 | `BIGINT` | |
| `PIC 9(n)V9(m)` | `DECIMAL(n+m, m)` | |
| `PIC S9(n) COMP-3` | `DECIMAL(n, scale)` | |
| `PIC 9(8)` (date) | `DATE` | Format: YYYYMMDD |
| `PIC 9(6)` (time) | `TIME` | Format: HHMMSS |
| `PIC 9(14)` (timestamp) | `DATETIME2` | Format: YYYYMMDDHHMMSS |
| `OCCURS` (simple) | `JSON` column | Or child table |
| `OCCURS` (complex) | Child table with FK | 1:N relationship |

### COBOL → MongoDB
| COBOL PIC/USAGE | BSON Type | Notes |
|-----------------|-----------|-------|
| `PIC X(n)` | `string` | `maxLength: n` |
| `PIC 9(n)` | `int` or `long` | |
| `PIC 9(n)V9(m)` | `decimal` | NumberDecimal |
| Group item | `object` | Nested document |
| `OCCURS` | `array` | Embedded array |
| `PIC 9(8)` date | `date` | ISODate |

## Conversion Process

### Step 1: Parse Copybook
Read the copybook and identify:
- Level hierarchy (01 → 05 → 10 → ...)
- Group items (have sub-items) vs. elementary items (have PIC)
- OCCURS clauses (arrays)
- REDEFINES (overlays)
- 88-level condition names

### Step 2: Build Type Tree
```
01 RECORD-NAME
   05 FIELD-1          → Top-level field
   05 GROUP-1           → Nested object
      10 SUB-FIELD-1   → Nested field
      10 SUB-FIELD-2   → Nested field
   05 ARRAY-1 OCCURS 5 → List<>
      10 ELEM-1        → List element field
```

### Step 3: Generate Outputs

#### Java Record:
```java
public record CustomerRecord(
    @Size(max = 10) String custId,
    @Size(max = 30) String custName,
    CustomerAddress address,      // from group item
    @Digits(integer = 7, fraction = 2) BigDecimal balance,
    CustomerStatus status         // from 88-levels
) {
    public record CustomerAddress(
        @Size(max = 40) String line1,
        @Size(max = 20) String city,
        @Size(max = 2) String state,
        @Size(max = 10) String zip
    ) {}

    public enum CustomerStatus {
        ACTIVE("A"), INACTIVE("I");
        // ...
    }
}
```

#### SQL DDL:
```sql
CREATE TABLE customers (
    cust_id VARCHAR(10) PRIMARY KEY,
    cust_name VARCHAR(30) NOT NULL,
    addr_line1 VARCHAR(40),
    addr_city VARCHAR(20),
    addr_state CHAR(2),
    addr_zip VARCHAR(10),
    balance DECIMAL(9,2) NOT NULL DEFAULT 0,
    status CHAR(1) CHECK (status IN ('A','I'))
);
```

## Special Handling

### REDEFINES
When field A REDEFINES field B, they share the same memory. In Java:
- If they represent different interpretations of the same data → use a sealed interface
- If they represent optional views → use nullable fields with a discriminator

### OCCURS DEPENDING ON
Variable-length arrays need:
- Java: `List<T>` with `@Size(min, max)` + a count field
- SQL: Child table with FK (no fixed array size)
- MongoDB: Array with validation

### COMP-3 Packed Decimal
ALWAYS map to BigDecimal. The precision is:
- `PIC S9(n)V9(m) COMP-3` → BigDecimal with scale = m
- Storage size = (n+m+1)/2 bytes (but that's a mainframe concern)
- Java just needs correct scale: `new BigDecimal(value).setScale(m, RoundingMode.HALF_UP)`
```

---

## How to Install

1. Create the directory: `.github/skills/copybook-mapper/`
2. Add the `SKILL.md` file above
3. Auto-loads when working with `.cpy` files or data conversion tasks

## How to Invoke

```
Use the copybook-mapper skill to convert CUST-REC.cpy to:
1. A Java record
2. An Azure SQL CREATE TABLE
3. A MongoDB JSON schema
```
