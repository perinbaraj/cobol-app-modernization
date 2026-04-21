# Copilot Configuration: Project Instructions

> Template for `.github/copilot-instructions.md` — the project-level instruction file that customizes Copilot's behavior for the the client migration project.

---

## How to Use

Copy the content below into `.github/copilot-instructions.md` at the root of your migration repository. Copilot will automatically read these instructions for every interaction in the repo.

---

### `.github/copilot-instructions.md`

```markdown
# the client Mainframe Modernization — Copilot Instructions

## Project Context
This repository contains code being migrated from IBM z/OS mainframe (COBOL) to modern Java 17 + React 18.2. The migration involves 937 COBOL programs (3.38M lines of code), 15,250 JCL jobs, and associated VSAM/DB2 data stores.

## Technology Stack

### Source (Mainframe)
- COBOL (Enterprise COBOL for z/OS V2R5)
- JCL (z/OS Job Control Language)
- CICS (Customer Information Control System)
- BMS (Basic Mapping Support) screens
- VSAM (Virtual Storage Access Method) files
- DB2 for z/OS

### Target (Modern)
- Java 17 (LTS)
- Spring Boot 3.2
- Spring Data JPA
- React 18.2 with TypeScript
- Tailwind CSS
- Azure SQL Database
- MongoDB (document store)
- GitHub Actions (CI/CD and batch orchestration)
- Flyway (database migrations)

## Coding Standards

### Java
- Use Java 17 features: records, sealed classes, switch expressions, pattern matching
- All decimal/financial fields: `BigDecimal` — NEVER use `double` or `float`
- Use Spring Boot conventions: @Service, @Repository, @RestController
- Use constructor injection (not field injection)
- SLF4J for logging (INFO for business events, DEBUG for data)
- All REST endpoints must have OpenAPI/Swagger annotations
- All database methods must be @Transactional where appropriate
- Follow package structure: `com.acme.{domain}.{layer}`

### React / TypeScript
- React 18.2 with TypeScript (strict mode)
- Tailwind CSS for styling (no CSS modules, no styled-components)
- React Query (TanStack Query) for server state
- React Hook Form + Zod for forms and validation
- React Router v6 for navigation
- Functional components only (no class components)
- Custom hooks for business logic (prefix with `use`)

### Database
- Azure SQL: Use Flyway for all schema changes (never manual DDL)
- MongoDB: Use JSON Schema validation
- Naming: snake_case for SQL columns, camelCase for MongoDB fields
- Always include created_at and updated_at audit columns

### Testing
- JUnit 5 + Mockito for unit tests
- TestContainers for integration tests
- React Testing Library + Vitest for frontend tests
- Playwright for E2E tests
- Target: 90%+ code coverage
- Parity tests tagged with `@Tag("parity")`

## COBOL Conversion Rules

When converting COBOL to Java, always follow these rules:

### Data Type Mapping (MANDATORY)
| COBOL | Java | SQL |
|-------|------|-----|
| PIC X(n) | String @Size(max=n) | VARCHAR(n) |
| PIC 9(n) n≤9 | int | INT |
| PIC 9(n)V9(m) | BigDecimal @Digits(n,m) | DECIMAL(n+m,m) |
| PIC S9(n) COMP-3 | BigDecimal | DECIMAL |
| OCCURS n TIMES | List<T> @Size(max=n) | Child table |
| 88-level | enum | CHECK constraint |

### Statement Mapping
| COBOL | Java |
|-------|------|
| PERFORM paragraph | private method call |
| EVALUATE | switch expression |
| CALL 'PROGRAM' | @Autowired service call |
| READ file | repository.findById() |
| WRITE file | repository.save() |
| DISPLAY | logger.info() |

### Common Pitfalls to Avoid
1. NEVER use `double`/`float` for COBOL decimal fields → BigDecimal only
2. Handle COBOL spaces (equivalent to null in Java)
3. Preserve COMP-3 precision (packed decimal)
4. Convert COBOL paragraph names to camelCase method names
5. Replace GO TO with structured control flow
6. COBOL is single-threaded — ensure Java code is thread-safe

## Git Conventions
- Branch naming: `migration/{domain}/{program-name}` (e.g., `migration/customer/custmgmt`)
- PR title: `[Migration] Convert {PROGRAM} to {ServiceName}`
- PR labels: `migration`, `{domain}`, `{phase}`
- Commit messages: Reference original COBOL program (e.g., "Convert CUSTMGMT.cbl to CustomerService")
- Every PR must include tests

## Custom Agents Available
- `@cobol-analyzer` — Analyze COBOL programs
- `@cobol-to-java-converter` — Convert COBOL to Java
- `@jcl-migrator` — Convert JCL to GitHub Actions/Spring Batch
- `@react-scaffolder` — Convert BMS screens to React
- `@migration-reviewer` — Review migration PRs

## Custom Skills Available
- `cobol-parser` — Parse COBOL structure
- `copybook-mapper` — Map copybooks to Java/SQL
- `vsam-to-sql` — Convert VSAM definitions to SQL
- `test-parity` — Generate parity tests
```
