# Prompt Library: Phase 2 — COBOL to Java Conversion

> Production-ready prompts for converting COBOL programs to Java 17 Spring Boot services.
> Use with `cobol-to-java-converter` agent or Copilot Chat with the COBOL source file open.

---

## Prompt 1: Full Program Conversion

```
Convert this COBOL program to a Java 17 Spring Boot service.

Conversion rules:
- **IDENTIFICATION DIVISION** → Class-level Javadoc
- **WORKING-STORAGE** → Private fields (use final where possible)
- **LINKAGE SECTION** → Method parameters
- **PROCEDURE DIVISION** → Public methods
- **PERFORM paragraph** → Private method call
- **EVALUATE** → Switch expression (Java 17)
- **IF/ELSE** → Standard conditionals
- **COMPUTE** → BigDecimal arithmetic
- **MOVE** → Assignment (with type conversion)
- **STRING/UNSTRING** → String operations
- **INSPECT** → String.replace() / regex
- **READ/WRITE** → Spring Data JPA repository calls
- **CALL** → Injected service method call
- **DISPLAY** → SLF4J logger.info()
- **STOP RUN** → Return from method

Additional requirements:
- Use Spring Boot 3.2 with Java 17 features (records, sealed classes, pattern matching)
- All numeric fields with decimals → BigDecimal (never double/float)
- Add @Service, @Repository, @RestController annotations as appropriate
- Include proper exception handling (replace COBOL status codes)
- Add SLF4J logging at key decision points
- Generate OpenAPI annotations for REST endpoints
```

---

## Prompt 2: Copybook to Java Record

```
Convert this COBOL copybook to a Java 17 record.

Mapping rules:
| COBOL | Java |
|-------|------|
| PIC X(n) | String (with @Size(max=n)) |
| PIC 9(n) where n<=9 | int |
| PIC 9(n) where n>9 | long |
| PIC 9(n)V9(m) | BigDecimal (with @Digits(integer=n, fraction=m)) |
| PIC S9(n) COMP | int |
| PIC S9(n) COMP-3 | BigDecimal |
| OCCURS n TIMES | List<T> (with @Size(max=n)) |
| 88-level | enum |
| REDEFINES | @JsonTypeInfo union (or nullable fields) |
| Group item (01/05) | Nested record |

Additional requirements:
- Add Jakarta Validation annotations
- Add Jackson JSON annotations
- Add JPA annotations if this maps to a database table
- Include a static factory method `fromCobolString(String raw)` for migration
- Include a `toCobolString()` method for parity testing
- Generate Javadoc explaining the original COBOL copybook
```

---

## Prompt 3: COBOL Paragraph to Java Method

```
Convert this COBOL paragraph to a Java method.

Context about the program:
- [paste WORKING-STORAGE relevant variables here]
- [paste any related copybook fields here]

Rules:
1. Method name: Convert COBOL paragraph name to camelCase
   (e.g., CALCULATE-TOTAL → calculateTotal)
2. Extract input parameters from fields READ before this paragraph
3. Return the most meaningful result (not void unless truly side-effect only)
4. Replace COBOL status codes with exceptions
5. Use BigDecimal for all monetary/decimal operations
6. Add null checks where COBOL uses space/zero checks
7. Add @Transactional if this paragraph contains database operations
8. Log entry/exit and key decisions with SLF4J

Example conversion:
COBOL: MOVE SPACES TO WS-ERROR-MSG → Java: errorMsg = null;
COBOL: IF WS-STATUS = '00' → Java: if (status == Status.SUCCESS)
COBOL: ADD 1 TO WS-COUNT → Java: count++;
```

---

## Prompt 4: File I/O to Repository Pattern

```
This COBOL program reads/writes files using these statements:
[paste all READ, WRITE, REWRITE, DELETE, START statements]

File definitions:
[paste FILE SECTION and SELECT/ASSIGN clauses]

Convert to Spring Data JPA:
1. Generate a @Repository interface for each file
2. Map file operations:
   - READ → findById / findBy*
   - WRITE → save
   - REWRITE → save (update)
   - DELETE → deleteById
   - START → findFirstBy* (for VSAM positioning)
   - READ NEXT → Use pagination or streaming
3. Generate @Entity class from the file's record layout
4. Add custom query methods for any complex access patterns
5. Include @Query annotations for SQL that matches the original COBOL logic
```

---

## Prompt 5: Error Handling Conversion

```
Convert COBOL error handling to Java exception handling.

COBOL patterns to convert:
| COBOL Pattern | Java Equivalent |
|---------------|----------------|
| STATUS CODE '00' | Success (no exception) |
| STATUS CODE '10' | EndOfFileException or empty Optional |
| STATUS CODE '23' | RecordNotFoundException |
| STATUS CODE '22' | DuplicateKeyException |
| STATUS CODE '35' | FileNotFoundException |
| ABEND with code | RuntimeException with error code |
| ON SIZE ERROR | ArithmeticException |
| ON OVERFLOW | BufferOverflowException |
| INVALID KEY | InvalidKeyException |

Generate:
1. A custom exception hierarchy under com.acme.migration.exception
2. An exception mapper that translates between COBOL status codes and Java exceptions
3. A @ControllerAdvice for REST API error responses
4. Proper logging of all error conditions
```

---

## Prompt 6: Batch Conversion via Coding Agent Issue

```markdown
## Convert COBOL Module: [MODULE_NAME]

### Source Files
- Main program: `[PROGRAM].cbl`
- Copybooks: `[LIST].cpy`
- Related programs: `[CALLED_PROGRAMS]`

### Target
- Java 17 with Spring Boot 3.2
- Package: `com.acme.[domain]`
- Database: Azure SQL (Spring Data JPA)
- API: REST with OpenAPI 3.0

### Business Logic Summary
[Paste from Phase 1 discovery docs]

### Conversion Checklist
- [ ] Convert copybooks to Java records
- [ ] Convert main program to @Service class
- [ ] Convert file I/O to JPA repositories
- [ ] Convert CALL statements to service injections
- [ ] Add REST controller with OpenAPI annotations
- [ ] Add exception handling
- [ ] Add SLF4J logging
- [ ] Generate JUnit 5 unit tests (90%+ coverage)
- [ ] Generate integration tests
- [ ] Add Javadoc (reference original COBOL)
```

---

## Prompt 7: Code Review Prompt (for migration-reviewer agent)

```
Review this Java code that was converted from COBOL program [NAME].cbl.

Check for these common COBOL-to-Java conversion bugs:

1. **Precision loss**: Any use of double/float for financial data? Must be BigDecimal.
2. **Null handling**: COBOL uses spaces for empty strings. Are null checks correct?
3. **Sign handling**: COMP fields are signed. Is sign preserved in Java?
4. **Truncation**: COBOL PIC X(10) truncates at 10. Does Java enforce maxLength?
5. **Zero-fill**: COBOL PIC 9(5) zero-fills. Is this handled in Java?
6. **Array bounds**: COBOL OCCURS has fixed bounds. Are List sizes validated?
7. **Thread safety**: COBOL is single-threaded. Is the Java code thread-safe for concurrent use?
8. **Transaction boundaries**: Where should @Transactional begin and end?
9. **Error codes**: Are all COBOL status codes mapped to appropriate exceptions?
10. **Business logic parity**: Does the Java code produce identical results?

For each issue found, provide:
- Severity: 🔴 Critical / 🟡 Warning / 🟢 Info
- Location: File and line number
- Problem: What's wrong
- Fix: Suggested code change
```
