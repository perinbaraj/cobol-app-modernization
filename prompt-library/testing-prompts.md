# Prompt Library: Phase 6 — Testing & Validation

> Prompts for generating parity tests, unit tests, integration tests, and QA validation.

---

## Prompt 1: Unit Test Generation

```
Generate JUnit 5 unit tests for this Java service class that was converted from COBOL program [NAME].cbl.

Test requirements:
1. Test every public method
2. For each method, test:
   - Happy path with typical values
   - Boundary values (min, max, zero)
   - Null/empty input handling
   - COBOL-specific edge cases:
     - Maximum PIC precision values (e.g., 9(7)V99 → 9999999.99)
     - Negative values for signed fields
     - Space-filled strings
     - Zero-filled numeric strings
3. Use Mockito for mocking dependencies (repositories, other services)
4. Use AssertJ for fluent assertions
5. Follow the Arrange-Act-Assert pattern
6. Each test method: descriptive name using should_[expected]_when_[condition]
7. Target: 90%+ line coverage
```

---

## Prompt 2: Integration Test Generation

```
Generate integration tests for the [SERVICE] module using TestContainers.

Setup:
- Azure SQL container for database tests
- WireMock for external service dependencies
- Spring Boot test context

Test scenarios:
1. Full CRUD operations
2. Batch processing with realistic data volume
3. Concurrent access (COBOL is single-threaded, Java is multi-threaded)
4. Transaction rollback on error
5. Data integrity after batch operations
6. Connection pool exhaustion recovery

Generate:
- @SpringBootTest configuration
- TestContainers setup class
- Test data factories
- Database state assertions
- Response body assertions
```

---

## Prompt 3: Parity Test Generation

```
Generate parity tests comparing COBOL output with Java output.

Context:
- COBOL program: [NAME].cbl
- Java service: [CLASS_NAME].java
- Test data: CSV file at test/data/parity/[name].csv
  Format: input_field1,input_field2,...,expected_output1,expected_output2,...
  (expected outputs are from running the original COBOL program)

Generate:
1. @ParameterizedTest with @CsvFileSource
2. For each row: call Java service with inputs, compare with expected outputs
3. Comparison rules:
   - Strings: trim trailing spaces (COBOL pads with spaces)
   - Decimals: allow ±0.01 tolerance (COMP-3 rounding differences)
   - Dates: normalize format before comparison
   - Nulls: COBOL spaces/zeros should match Java nulls
4. On failure: log COBOL value, Java value, difference, and field name
5. Summary: total records, passed, failed, failure rate

Add a Gradle/Maven task to run only parity tests:
  mvn test -Dgroups=parity
```

---

## Prompt 4: React Component Test Generation

```
Generate React Testing Library tests for this component that was converted from BMS screen [MAP_NAME].

Test:
1. Rendering: Component renders without errors
2. Form fields: All input fields present and editable
3. Display fields: Read-only fields show data correctly
4. Validation: Zod schema errors display correctly
5. Form submission: Data sent to API correctly
6. Loading state: Spinner shown during API calls
7. Error state: Error message displayed on API failure
8. Keyboard navigation: Tab order matches original BMS field order
9. PF key equivalents: Keyboard shortcuts trigger correct actions
10. Accessibility: No axe-core violations

Use:
- @testing-library/react for component testing
- @testing-library/user-event for interactions
- msw (Mock Service Worker) for API mocking
- vitest for test runner
```

---

## Prompt 5: E2E Test Generation

```
Generate Playwright end-to-end tests for the [DOMAIN] user workflow.

The workflow was originally a CICS transaction flow:
[Paste CICS transaction sequence]

Test the complete user journey:
1. Navigate to the entry screen
2. Enter search criteria (equivalent to CICS SEND/RECEIVE)
3. View results
4. Select a record
5. Modify data
6. Save changes
7. Verify the update was persisted

Generate:
- Page Object Model for each screen/component
- Test data setup and teardown
- Screenshot on failure
- Network request interception for assertions
- Visual regression test (optional)
```

---

## Prompt 6: Test Data Generation

```
Generate realistic test data for the [DOMAIN] module.

Based on this COBOL copybook:
[Paste copybook]

Generate:
1. 100 sample records as CSV
2. Include edge cases:
   - Maximum length strings
   - Maximum precision decimals
   - Minimum and maximum dates
   - All possible status values (88-level)
   - Empty/space-filled optional fields
   - Records with OCCURS at 0, 1, and max count
3. A Java TestDataFactory class with builder methods
4. A SQL INSERT script for seeding the test database
5. A JSON fixture file for API testing
```

---

## Prompt 7: Migration Sign-Off Checklist

```
Generate a migration sign-off checklist for the [DOMAIN] module.

Review all aspects of the migration:

## Code Migration
- [ ] All COBOL programs in the cluster converted to Java
- [ ] All copybooks converted to Java records/DTOs
- [ ] All CALL dependencies resolved as Spring injections
- [ ] All file I/O converted to repository pattern
- [ ] COBOL comments preserved as Javadoc

## Data Migration
- [ ] Database schema created and versioned (Flyway)
- [ ] Data migration ETL tested with production-scale data
- [ ] Record counts match between source and target
- [ ] Data checksums validated for key fields

## API Layer
- [ ] REST APIs documented with OpenAPI
- [ ] All endpoints tested (unit + integration)
- [ ] Error responses follow standard format

## UI Migration
- [ ] All BMS screens converted to React components
- [ ] Navigation flow matches CICS transaction flow
- [ ] Form validations match BMS field constraints
- [ ] Accessibility audit passed (WCAG 2.1 AA)

## Testing
- [ ] Unit test coverage ≥ 90%
- [ ] Integration tests passing
- [ ] Parity tests: 100% pass rate (with approved tolerances)
- [ ] E2E tests: All critical paths passing
- [ ] Performance: Response time ≤ mainframe baseline

## Operations
- [ ] JCL jobs converted to GitHub Actions / Spring Batch
- [ ] Monitoring and alerting configured
- [ ] Rollback plan documented
- [ ] Runbook created for operations team
```
