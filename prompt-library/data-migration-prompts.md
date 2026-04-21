# Prompt Library: Phase 5 — Data Migration

> Prompts for converting VSAM/DB2 to Azure SQL and MongoDB schemas.

---

## Prompt 1: Copybook to SQL DDL

```
Convert this COBOL copybook to an Azure SQL CREATE TABLE statement.

Type mapping:
| COBOL | Azure SQL |
|-------|-----------|
| PIC X(n) | VARCHAR(n) |
| PIC 9(n) n<=9 | INT |
| PIC 9(n) n>9 | BIGINT |
| PIC 9(n)V9(m) | DECIMAL(n+m, m) |
| PIC S9(n) COMP | INT |
| PIC S9(n) COMP-3 | DECIMAL |
| PIC 9(8) (date) | DATE |
| OCCURS n TIMES | Separate child table with FK |
| REDEFINES | Nullable columns with discriminator |
| 88-level | CHECK constraint |
| Group item | Embedded or separate table |

Additional requirements:
- Add PRIMARY KEY (from VSAM key or first unique field)
- Add created_at DATETIME2 DEFAULT GETDATE()
- Add updated_at DATETIME2 DEFAULT GETDATE()
- Add appropriate indexes based on access patterns
- Generate as Flyway migration script (V[n]__create_[table].sql)
- Include rollback script (U[n]__drop_[table].sql)
```

---

## Prompt 2: VSAM Cluster to Database Schema

```
Convert this VSAM cluster definition to a database schema:

[Paste IDCAMS DEFINE CLUSTER statement]

Analyze:
1. Key structure (KEYS parameter) → PRIMARY KEY
2. Record size (RECORDSIZE) → Column totals
3. Access pattern (KSDS/ESDS/RRDS) → Index strategy
4. Share options (SHAREOPTIONS) → Concurrency needs
5. Alternate indexes (AIX) → Secondary indexes

Generate:
- CREATE TABLE with all columns from the associated copybook
- CREATE INDEX for key and alternate indexes
- JPA @Entity class
- Spring Data JPA @Repository interface
- Flyway migration script
```

---

## Prompt 3: Copybook to JPA Entity

```
Convert this COBOL copybook to a JPA entity class (Java 17).

Requirements:
- Use @Entity, @Table, @Column annotations
- Use Java records for immutable data or classes for mutable
- Map all COBOL PIC clauses to correct Java types (BigDecimal for decimals!)
- Add Jakarta Validation annotations (@NotNull, @Size, @Digits)
- Add audit fields (createdAt, updatedAt) with @CreationTimestamp, @UpdateTimestamp
- Handle OCCURS with @OneToMany or @ElementCollection
- Handle REDEFINES with @Inheritance or nullable columns
- Handle 88-level with enum + @Enumerated
- Generate equals/hashCode based on business key (not database ID)
- Include builder pattern or static factory method
```

---

## Prompt 4: Copybook to MongoDB Schema

```
Convert this COBOL copybook to a MongoDB document schema:

Guidelines:
- Nested COBOL groups → Embedded documents
- OCCURS → Arrays
- REDEFINES → Union type with discriminator
- Flatten where possible (MongoDB favors denormalization)
- Add indexes for query patterns

Generate:
1. JSON Schema for collection validation
2. Mongoose model (if using Node.js) or Spring Data MongoDB @Document
3. Sample document matching the copybook layout
4. Index definitions
```

---

## Prompt 5: Data Migration ETL Script

```
Generate a data migration ETL pipeline for moving data from mainframe export files to the target database.

Source: Fixed-width flat file exported from VSAM/DB2
Target: Azure SQL table [TABLE_NAME]

Copybook layout for the flat file:
[Paste copybook]

Generate:
1. Spring Batch configuration:
   - FlatFileItemReader with FixedLengthTokenizer matching copybook layout
   - ItemProcessor for data transformation:
     - COBOL date (PIC 9(8)) → LocalDate
     - COBOL packed decimal → BigDecimal
     - COBOL spaces → null
     - COBOL zeros → 0 or null (based on field semantics)
   - JdbcBatchItemWriter for Azure SQL
2. Data validation step (check record counts, checksums)
3. Error handling (skip bad records, log to error table)
4. Progress reporting (% complete, records/sec)
```

---

## Prompt 6: DB2 to Azure SQL Migration

```
Convert these DB2 DDL statements to Azure SQL compatible syntax.

DB2-specific items to convert:
| DB2 | Azure SQL |
|-----|-----------|
| TABLESPACE | Filegroup (or omit) |
| BUFFERPOOL | (remove) |
| LOCKSIZE | (remove) |
| CCSID | COLLATE (if needed) |
| TIMESTAMP WITH TIME ZONE | DATETIMEOFFSET |
| GENERATED ALWAYS AS IDENTITY | IDENTITY(1,1) |
| WITH UR (uncommitted read) | WITH (NOLOCK) |
| FETCH FIRST n ROWS | TOP n |
| CURRENT TIMESTAMP | GETDATE() or SYSDATETIME() |
| CONCAT or || | + or CONCAT() |

Also generate:
1. Index migration (convert DB2 indexes to Azure SQL)
2. View migration (convert DB2 views)
3. Stored procedure migration guidance
4. Flyway migration scripts for all objects
```
