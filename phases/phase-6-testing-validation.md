# Phase 6: Testing & Validation

## Objective
Ensure migrated Java/React applications produce identical results to original COBOL programs through automated parity testing, regression suites, and quality gates — all powered by GitHub Copilot.

---

## Inputs
| Input | Source | Description |
|-------|--------|-------------|
| Original COBOL programs | Source repo | Reference implementations |
| Converted Java services | Phase 2 | Migrated code under test |
| React components | Phase 4 | UI under test |
| Database schemas | Phase 5 | Data layer under test |
| Business logic docs | Phase 1 | Expected behavior specs |
| Production test data | Mainframe exports | Realistic test datasets |

## Outputs
| Output | Format | Description |
|--------|--------|-------------|
| Unit tests | JUnit 5 + Mockito | Per-method coverage |
| Integration tests | TestContainers | End-to-end service tests |
| Parity tests | Custom framework | COBOL output vs. Java output comparison |
| UI tests | Playwright / Cypress | React component + E2E tests |
| Test reports | JaCoCo + Allure | Coverage and results dashboards |
| Sign-off checklist | Markdown | Migration acceptance criteria |

---

## GitHub Copilot Features Used

### 1. `test-parity` Custom Skill
The core testing tool. Generates tests that compare COBOL output with Java output.

**What the skill does:**
- Takes a COBOL program and its Java equivalent as input
- Identifies all input/output data paths
- Generates test cases that feed identical inputs to both
- Compares outputs field-by-field with tolerance for known differences
- Reports mismatches with root cause hints

**Example invocation:**
```
Load skill: test-parity

Compare COBOL program CUSTCALC.cbl with Java service CustomerCalculationService.java
Test data: Use the sample records in test/data/customer-calc-samples.csv
Tolerance: Allow ±0.01 for decimal fields (COMP-3 rounding differences)
```

### 2. Copilot Coding Agent
Autonomous test generation via Issues:
```markdown
## Generate Test Suite: CustomerService

Write comprehensive tests for `CustomerService.java` (converted from CUSTMGMT.cbl):

1. Unit tests (JUnit 5 + Mockito)
   - Test every public method
   - Edge cases: null inputs, max values, boundary conditions
   - Verify COBOL numeric precision is preserved

2. Integration tests (TestContainers + Azure SQL)
   - Test full CRUD operations
   - Test batch processing
   - Test concurrent access

3. Parity tests
   - Compare output with COBOL reference data in test/data/
   - Field-by-field comparison
   - Report any mismatches

Target: 90%+ code coverage
```

### 3. `migration-reviewer` Custom Agent
Final quality gate before sign-off:
```
@migration-reviewer Perform final review of the Customer domain migration:

Checklist:
1. ✅ All COBOL programs converted?
2. ✅ All copybooks mapped to DTOs?
3. ✅ All VSAM files migrated to Azure SQL?
4. ✅ Unit test coverage > 90%?
5. ✅ Integration tests passing?
6. ✅ Parity tests passing (COBOL vs Java)?
7. ✅ UI screens converted to React?
8. ✅ API contracts documented (OpenAPI)?
9. ✅ JCL jobs converted to GitHub Actions?
10. ✅ Performance benchmarks acceptable?
```

### 4. Copilot Chat (Test Strategy)
For complex testing scenarios:
```
This COBOL program uses COMP-3 packed decimal arithmetic.
The Java conversion uses BigDecimal.

Generate test cases that verify precision is preserved for:
1. Multiplication with > 2 decimal places
2. Division with remainder
3. Rounding at maximum precision (9(7)V99)
4. Negative values with sign handling
```

### 5. Copilot CLI (Test Execution)
Run and analyze test results from the terminal:
```bash
# Run parity tests and ask Copilot to analyze failures
mvn test -Dtest=ParityTest* 2>&1 | gh copilot explain "Analyze these test failures"
```

---

## Testing Strategy

### Layer 1: Unit Tests (JUnit 5 + Mockito)
| What to Test | How | Copilot Helps |
|-------------|-----|---------------|
| Individual methods | Mock dependencies | Generate test cases from COBOL logic |
| Edge cases | Boundary values | Identify edge cases from PIC clauses |
| Error handling | Exception scenarios | Map COBOL status codes to exceptions |
| Data mapping | DTO conversions | Verify copybook → Java mapping |

### Layer 2: Integration Tests (TestContainers)
| What to Test | How | Copilot Helps |
|-------------|-----|---------------|
| Database operations | Real Azure SQL in container | Generate CRUD test data |
| API endpoints | MockMvc or WebTestClient | Generate request/response fixtures |
| Batch processing | Spring Batch test utils | Generate batch test scenarios |
| Cross-service calls | WireMock | Generate mock service responses |

### Layer 3: Parity Tests (COBOL vs. Java)
| What to Test | How | Copilot Helps |
|-------------|-----|---------------|
| Identical input → output | Side-by-side comparison | Generate comparison framework |
| Numeric precision | Field-level diff | Identify precision risks |
| Date handling | Format conversion tests | Map COBOL date formats |
| String handling | Space/null normalization | Handle COBOL space padding |

### Layer 4: UI Tests (Playwright)
| What to Test | How | Copilot Helps |
|-------------|-----|---------------|
| Form submission | E2E test flows | Generate from BMS screen flow |
| Navigation | Route testing | Map CICS transaction flow |
| Validation | Input constraint tests | Generate from Zod schemas |
| Accessibility | axe-core integration | Add a11y assertions |

---

## Parity Test Framework

```java
/**
 * Parity test pattern: Compare COBOL output with Java output
 * Generated by test-parity skill
 */
@ParameterizedTest
@CsvFileSource(resources = "/parity-data/customer-calc.csv")
void testCustomerCalculationParity(
        String custId,
        String expectedBalance,    // from COBOL run
        String expectedStatus) {   // from COBOL run

    // Run the Java equivalent
    var result = customerService.calculateBalance(custId);

    // Compare with COBOL output (with tolerance)
    assertThat(result.balance())
        .isCloseTo(new BigDecimal(expectedBalance),
                   Offset.offset(new BigDecimal("0.01")));

    assertThat(result.status().getCode())
        .isEqualTo(expectedStatus.trim());
}
```

---

## Step-by-Step Workflow

### Step 1: Export COBOL Reference Data
1. Run original COBOL programs with production test data
2. Capture all outputs (files, reports, screen data)
3. Store as CSV/JSON in `test/data/parity/`

### Step 2: Generate Unit Tests
1. Use Coding Agent to generate JUnit tests for each converted service
2. Target: 90%+ code coverage
3. Focus on COBOL-specific edge cases (precision, spaces, status codes)

### Step 3: Generate Integration Tests
1. Use Coding Agent with TestContainers setup
2. Test full request → response cycle
3. Include database state verification

### Step 4: Run Parity Tests
1. Feed identical inputs to Java services
2. Compare outputs with COBOL reference data
3. Investigate and fix mismatches

### Step 5: Generate UI Tests
1. Use Copilot to generate Playwright tests from React components
2. Test form flows matching CICS transaction paths
3. Add accessibility checks

### Step 6: Sign-Off
1. Run `migration-reviewer` agent final checklist
2. Generate coverage reports (JaCoCo)
3. Generate parity reports
4. Document known differences and approved deviations

---

## Quality Gates

| Gate | Criteria | Tool |
|------|----------|------|
| Unit Test Coverage | ≥ 90% line coverage | JaCoCo |
| Integration Tests | 100% pass rate | TestContainers |
| Parity Tests | 100% pass (with approved tolerances) | Custom framework |
| UI Tests | 100% critical path pass | Playwright |
| Code Review | All PRs reviewed by `migration-reviewer` | GitHub |
| Performance | Response time ≤ mainframe baseline | JMeter / k6 |

---

## Tips
- **Export COBOL outputs first**: You need a reference to test against — do this before decommissioning anything
- **Tolerance is expected**: COMP-3 rounding may differ slightly from BigDecimal — define acceptable tolerance
- **Test with production-scale data**: Unit tests miss volume issues — run with realistic datasets
- **Automate in CI/CD**: All tests should run on every PR via GitHub Actions
- **Track parity percentage**: Start at 0%, target 100% — monitor progress as a KPI
