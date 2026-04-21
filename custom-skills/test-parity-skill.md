# Custom Skill: Test Parity

> A VS Code Agent Skill for generating and running parity tests that compare COBOL program output with Java service output.

---

## Skill Definition

Create this skill in your project as `.github/skills/test-parity/SKILL.md`:

### `.github/skills/test-parity/SKILL.md`

```markdown
---
name: test-parity
description: Generates parity tests comparing original COBOL program output with converted Java service output to ensure migration correctness.
---

# Test Parity Skill

## When to Use This Skill
Activate when:
- Validating that a converted Java service produces the same results as the original COBOL program
- Generating test data for migration validation
- Creating regression test suites for migrated code
- Producing parity reports for migration sign-off

## Parity Testing Approach

### Concept
```
                    ┌─────────────┐
                    │  Test Input  │
                    │  (CSV/JSON)  │
                    └──────┬──────┘
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
    ┌──────────────────┐     ┌──────────────────┐
    │  COBOL Program   │     │   Java Service    │
    │  (reference run) │     │  (under test)     │
    └────────┬─────────┘     └────────┬──────────┘
             │                        │
             ▼                        ▼
    ┌──────────────────┐     ┌──────────────────┐
    │ Expected Output  │     │  Actual Output   │
    │  (from mainframe)│     │  (from Java)     │
    └────────┬─────────┘     └────────┬──────────┘
             │                        │
             └───────────┬────────────┘
                         ▼
               ┌──────────────────┐
               │  COMPARE         │
               │  Field-by-field  │
               │  with tolerance  │
               └────────┬─────────┘
                        │
                        ▼
               ┌──────────────────┐
               │  Parity Report   │
               │  PASS / FAIL     │
               └──────────────────┘
```

## Test Data Preparation

### Step 1: Export COBOL Reference Data
Run the original COBOL program on the mainframe with known inputs and capture outputs.

Export format (CSV):
```
# input fields | delimiter | expected output fields
input_cust_id,input_amount,...|expected_balance,expected_status,...
```

### Step 2: Store in Test Resources
```
src/test/resources/parity-data/
├── customer-calc/
│   ├── input.csv           # Input records
│   └── expected-output.csv # COBOL outputs (reference)
├── order-process/
│   ├── input.csv
│   └── expected-output.csv
└── README.md               # Data format documentation
```

## Comparison Rules

### String Comparison
```java
// COBOL pads strings with spaces; Java does not
private boolean stringsMatch(String cobolValue, String javaValue) {
    if (cobolValue == null && javaValue == null) return true;
    if (cobolValue == null || javaValue == null) return false;
    return cobolValue.trim().equals(javaValue.trim());
}
```

### Numeric Comparison (with tolerance)
```java
// COMP-3 rounding may differ from BigDecimal
private boolean numbersMatch(BigDecimal cobolValue, BigDecimal javaValue,
                              BigDecimal tolerance) {
    if (cobolValue == null && javaValue == null) return true;
    if (cobolValue == null || javaValue == null) return false;
    return cobolValue.subtract(javaValue).abs().compareTo(tolerance) <= 0;
}
```

### Date Comparison
```java
// COBOL dates may be PIC 9(8) YYYYMMDD; Java uses LocalDate
private boolean datesMatch(String cobolDate, LocalDate javaDate) {
    if ("00000000".equals(cobolDate) && javaDate == null) return true;
    LocalDate cobolParsed = LocalDate.parse(cobolDate,
        DateTimeFormatter.ofPattern("yyyyMMdd"));
    return cobolParsed.equals(javaDate);
}
```

### Status Code Comparison
```java
// COBOL uses single-char codes; Java may use enums
private boolean statusMatch(String cobolStatus, Enum<?> javaStatus) {
    return cobolStatus.trim().equals(javaStatus.getCode());
}
```

## Generated Test Structure

### Base Parity Test Class
```java
@Tag("parity")
public abstract class AbstractParityTest {

    protected static final BigDecimal DECIMAL_TOLERANCE =
        new BigDecimal("0.01");

    protected void assertFieldParity(String fieldName,
                                      Object cobolValue,
                                      Object javaValue) {
        if (cobolValue instanceof BigDecimal cv && javaValue instanceof BigDecimal jv) {
            assertThat(jv)
                .as("Parity check: %s (COBOL=%s, Java=%s)", fieldName, cv, jv)
                .isCloseTo(cv, Offset.offset(DECIMAL_TOLERANCE));
        } else if (cobolValue instanceof String cv && javaValue instanceof String jv) {
            assertThat(jv.trim())
                .as("Parity check: %s", fieldName)
                .isEqualTo(cv.trim());
        } else {
            assertThat(javaValue)
                .as("Parity check: %s", fieldName)
                .isEqualTo(cobolValue);
        }
    }

    protected ParityReport generateReport(List<ParityResult> results) {
        long passed = results.stream().filter(ParityResult::passed).count();
        long failed = results.size() - passed;
        return new ParityReport(results.size(), passed, failed,
            (double) passed / results.size() * 100);
    }
}
```

### Concrete Parity Test Example
```java
@Tag("parity")
class CustomerCalculationParityTest extends AbstractParityTest {

    @Autowired
    private CustomerCalculationService service;

    @ParameterizedTest(name = "Parity: custId={0}")
    @CsvFileSource(
        resources = "/parity-data/customer-calc/test-data.csv",
        numLinesToSkip = 1
    )
    void testCalculationParity(
            String custId,
            String inputAmount,
            String expectedBalance,
            String expectedStatus,
            String expectedMessage) {

        // Act: Run Java service
        var result = service.calculate(custId, new BigDecimal(inputAmount));

        // Assert: Compare with COBOL reference output
        assertFieldParity("balance", new BigDecimal(expectedBalance), result.balance());
        assertFieldParity("status", expectedStatus, result.status().getCode());
        assertFieldParity("message", expectedMessage, result.message());
    }
}
```

### Parity Report
```java
public record ParityReport(
    long totalRecords,
    long passed,
    long failed,
    double passRate
) {
    public String toMarkdown() {
        return """
            ## Parity Test Report
            | Metric | Value |
            |--------|-------|
            | Total Records | %d |
            | Passed | %d |
            | Failed | %d |
            | Pass Rate | %.2f%% |
            | Status | %s |
            """.formatted(totalRecords, passed, failed, passRate,
                passRate >= 100.0 ? "✅ PASS" : "❌ FAIL");
    }
}
```

## Running Parity Tests

### Run all parity tests:
```bash
mvn test -Dgroups=parity
```

### Run parity tests for a specific module:
```bash
mvn test -Dgroups=parity -Dtest="*CustomerCalculation*"
```

### Generate parity report:
```bash
mvn test -Dgroups=parity -Dsurefire.reportFormat=plain 2>&1 | \
  grep -E "(PASS|FAIL|Parity)" > parity-report.txt
```

## Integration with CI/CD
```yaml
# .github/workflows/parity-tests.yml
name: Parity Tests
on:
  pull_request:
    labels: [migration]

jobs:
  parity:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
      - name: Run Parity Tests
        run: mvn test -Dgroups=parity
      - name: Upload Parity Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: parity-report
          path: target/surefire-reports/
```
```

---

## How to Invoke

```
Use the test-parity skill to generate parity tests for CustomerCalculationService.java
COBOL reference: CUSTCALC.cbl
Test data: src/test/resources/parity-data/customer-calc/test-data.csv
Tolerance: ±0.01 for decimal fields
```
