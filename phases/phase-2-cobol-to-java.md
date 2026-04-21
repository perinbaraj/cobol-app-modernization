# Phase 2: COBOL → Java 17 Conversion

## Objective
Convert 937 COBOL programs to idiomatic Java 17 Spring Boot microservices, leveraging GitHub Copilot's coding agent, custom agents, and skills for automated conversion with human-in-the-loop review.

---

## Inputs
| Input | Source | Description |
|-------|--------|-------------|
| COBOL programs | Phase 1 inventory | Prioritized list of programs to convert |
| Copybooks | Source repo | Shared data structures (→ Java DTOs) |
| Dependency map | Phase 1 | CALL/COPY graphs to determine conversion order |
| Business logic docs | Phase 1 | Plain-English summaries for validation |
| MCP context | Mainframe server | Program metadata, file assignments |

## Outputs
| Output | Format | Description |
|--------|--------|-------------|
| Java services | Spring Boot 3.x / Java 17 | One service per logical program group |
| Java DTOs | POJOs with records | One DTO per copybook |
| REST APIs | OpenAPI 3.0 specs | API contracts for each service |
| Repository layer | Spring Data JPA | Data access classes |
| Migration PR | GitHub Pull Request | Each conversion = one PR for review |

---

## GitHub Copilot Features Used

### 1. `cobol-to-java-converter` Custom Agent
The primary conversion engine. Configured with COBOL-to-Java mapping rules.

**Invoke for single program conversion:**
```
@cobol-to-java-converter Convert the COBOL program CUSTMGMT.cbl to a Java 17 Spring Boot service.

Context:
- Target: Spring Boot 3.2, Java 17
- Use Java records for copybook data structures
- Map WORKING-STORAGE to class fields
- Convert PERFORM to method calls
- Convert EVALUATE to switch expressions
- Map file I/O to Spring Data JPA repositories
- Expose main functionality as REST endpoints
```

### 2. `copybook-mapper` Custom Skill
Automatically converts COBOL copybooks to Java types.

**Mapping rules applied by the skill:**
| COBOL | Java |
|-------|------|
| `PIC X(n)` | `String` |
| `PIC 9(n)` | `int` or `long` |
| `PIC 9(n)V9(m)` | `BigDecimal` |
| `PIC S9(n) COMP` | `int` |
| `PIC S9(n) COMP-3` | `BigDecimal` |
| `OCCURS n TIMES` | `List<T>` |
| `REDEFINES` | Inheritance or union type |
| `88-level` | `enum` |

### 3. Copilot Coding Agent
For autonomous conversion at scale:
1. Create a GitHub Issue: "Convert CUSTMGMT.cbl to Java 17 Spring Boot service"
2. Assign to Copilot Coding Agent
3. Agent reads the COBOL source, applies conversion patterns, creates a draft PR
4. Developer reviews and merges

**Issue template:**
```markdown
## COBOL to Java Conversion

**Source program:** `CUSTMGMT.cbl`
**Related copybooks:** `CUST-REC.cpy`, `ADDR-REC.cpy`
**Business domain:** Customer Management
**Priority:** High
**Complexity score:** Medium (from Phase 1)

### Conversion requirements:
- Target: Java 17, Spring Boot 3.2
- Data access: Spring Data JPA → Azure SQL
- API style: REST (OpenAPI 3.0)
- Use Java records for DTOs
- Include unit tests (JUnit 5 + Mockito)

### Business logic summary:
(paste from Phase 1 documentation)
```

### 4. Copilot Edits (Multi-File)
After initial conversion, use Edits mode for batch refinements:
```
Select all generated Java files in src/main/java/com/the client/customer/
Edit: Replace all raw string SQL with Spring Data JPA @Query annotations
```

### 5. `migration-reviewer` Custom Agent
Automated review of every conversion PR:
```
@migration-reviewer Review this COBOL-to-Java conversion PR.
Check for:
1. Business logic parity with original COBOL
2. Proper handling of COBOL numeric precision (COMP-3 → BigDecimal)
3. Correct null handling (COBOL spaces vs Java nulls)
4. Thread safety (COBOL is single-threaded, Java may be concurrent)
5. Error handling (COBOL status codes → Java exceptions)
```

---

## Conversion Patterns

### Pattern 1: COBOL PERFORM → Java Method

**COBOL:**
```cobol
PERFORM CALCULATE-TOTAL
    THRU CALCULATE-TOTAL-EXIT.

CALCULATE-TOTAL.
    COMPUTE WS-TOTAL = WS-PRICE * WS-QUANTITY.
    IF WS-TOTAL > 9999.99
        MOVE 9999.99 TO WS-TOTAL
    END-IF.
CALCULATE-TOTAL-EXIT.
    EXIT.
```

**Java (Copilot output):**
```java
private BigDecimal calculateTotal(BigDecimal price, int quantity) {
    BigDecimal total = price.multiply(BigDecimal.valueOf(quantity));
    BigDecimal max = new BigDecimal("9999.99");
    return total.compareTo(max) > 0 ? max : total;
}
```

### Pattern 2: COBOL File I/O → Spring Data JPA

**COBOL:**
```cobol
READ CUSTOMER-FILE INTO WS-CUSTOMER-REC
    AT END SET WS-EOF TO TRUE
END-READ.
```

**Java (Copilot output):**
```java
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerId(String customerId);
}
```

### Pattern 3: COBOL EVALUATE → Java Switch Expression

**COBOL:**
```cobol
EVALUATE WS-TRANS-TYPE
    WHEN 'A' PERFORM ADD-CUSTOMER
    WHEN 'U' PERFORM UPDATE-CUSTOMER
    WHEN 'D' PERFORM DELETE-CUSTOMER
    WHEN OTHER PERFORM ERROR-HANDLER
END-EVALUATE.
```

**Java (Copilot output):**
```java
switch (transactionType) {
    case "A" -> addCustomer(request);
    case "U" -> updateCustomer(request);
    case "D" -> deleteCustomer(request);
    default -> throw new InvalidTransactionException(transactionType);
};
```

---

## Step-by-Step Workflow

### Step 1: Prepare the Target Project
```bash
# Scaffold Spring Boot project
spring init --java-version=17 --dependencies=web,data-jpa,validation \
  --group-id=com.acme --artifact-id=customer-service \
  customer-service
```

### Step 2: Convert Copybooks to DTOs
1. Run `copybook-mapper` skill on all copybooks in the cluster
2. Review generated Java records
3. Add validation annotations (@NotNull, @Size, etc.)

### Step 3: Convert Programs to Services
1. Start with leaf programs (no CALL dependencies)
2. Use `cobol-to-java-converter` agent for each program
3. Use Coding Agent for batch conversions via Issues

### Step 4: Wire Dependencies
1. Convert CALL chains to Spring dependency injection
2. Replace COBOL paragraphs with Java methods
3. Convert WORKING-STORAGE to service fields or method-scoped variables

### Step 5: Add REST API Layer
1. Use Copilot Chat to generate @RestController endpoints
2. Generate OpenAPI specs with Copilot
3. Add request/response DTOs

### Step 6: Review & Merge
1. `migration-reviewer` agent reviews each PR
2. Developer validates business logic against Phase 1 docs
3. Merge approved PRs

---

## Tips
- **Convert bottom-up**: Start with leaf programs (called by others, call nothing) to build a tested foundation
- **One PR per program**: Keep changes reviewable
- **Preserve COBOL comments**: Ask Copilot to transfer COBOL comments as Java Javadoc
- **Watch for precision**: COBOL COMP-3 packed decimal → always use `BigDecimal`, never `double`
- **Handle COBOL spaces**: COBOL uses spaces for "empty", Java uses null — define a mapping strategy
