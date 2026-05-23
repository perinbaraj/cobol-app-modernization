# Parity Tests — CUSTMGMT Migration

This directory contains parity test assets for validating that the migrated Java
service (`CustomerService`) produces identical results to the original COBOL
program (`CUSTMGMT.cbl`).

## Contents

| File | Description |
|------|-------------|
| `input-data.csv` | Test input records (35+ scenarios) |
| `expected-output.csv` | Expected COBOL-style outputs |
| `CustomerParityTest.java` | JUnit 5 parameterized test |
| `parity-report-template.md` | Report template for results |
| `README.md` | This file |

---

## Quick Start

### Run Parity Tests

```bash
# Run all parity tests
mvn test -Dgroups=parity

# Run with verbose output
mvn test -Dgroups=parity -Dtest=CustomerParityTest

# Run from project root
cd samples/expected-output/java
mvn test -Dgroups=parity
```

### View Results

Reports are generated in `target/parity-reports/`:
```bash
ls target/parity-reports/
# parity-report-20260523-143256.md
```

---

## Test Categories

### 1. Normal Operations (TC001–TC004)
Basic CRUD operations: Add, Update, Delete, Inquiry.

### 2. Discount Boundary Tests (TC005–TC008)
The COBOL program applies a 5% discount for customers with balance > 10,000:
- `TC005`: 9999.99 — no discount
- `TC006`: 10000.00 — no discount (threshold is `>`, not `>=`)
- `TC007`: 10000.01 — discount applies
- `TC008`: 50000.00 — full discount calculation

### 3. Maximum Precision (TC009–TC012)
Tests COBOL `PIC S9(7)V99` precision (max 9,999,999.99).

### 4. Maximum Length Strings (TC013–TC014)
Tests field length limits:
- Customer ID: 10 characters
- Name: 30 characters
- Address: 40 characters
- City: 20 characters
- State: 2 characters
- Zip: 10 characters

### 5. Empty/Space Fields (TC015–TC018)
COBOL pads fields with spaces. Tests ensure Java handles empty/space-filled
fields correctly.

### 6. Status Codes (TC019–TC021)
All 88-level condition values:
- `A` — Active
- `I` — Inactive
- `S` — Suspended

### 7. Error Scenarios (TC022–TC027)
COBOL error codes mapped to Java exceptions:

| Error Code | COBOL Message | Java Exception |
|------------|---------------|----------------|
| 1001 | INVALID TRANSACTION TYPE | `IllegalArgumentException` |
| 1002 | CUSTOMER ALREADY EXISTS | `CustomerAlreadyExistsException` |
| 2001 | CUSTOMER NOT FOUND | `CustomerNotFoundException` |
| 3001 | CUSTOMER NOT FOUND | `CustomerNotFoundException` |
| 4001 | CUSTOMER NOT FOUND | `CustomerNotFoundException` |

### 8. Complex Calculations (TC028–TC030)
Discount calculations with various precision scenarios.

### 9. State Transitions (TC031–TC034)
Update operations crossing discount threshold.

---

## Adding New Test Cases

### 1. Add Input Record

Edit `input-data.csv` and add a new row:

```csv
TC036,A,CUST000099,New Test Customer,123 Test St,Test City,TC,12345,12000.00,A,Description of test case
```

**Format:**
```
testId,operation,customerId,customerName,customerAddress,customerCity,customerState,customerZip,balance,status,description
```

### 2. Add Expected Output

Edit `expected-output.csv` and add matching row:

```csv
TC036,0,,11400.00,A,Y,12000 > 10000 - 5% discount: 600 -> 11400.00
```

**Format:**
```
testId,expectedReturnCode,expectedErrorMsg,expectedBalance,expectedStatus,expectedDiscountApplied,notes
```

### 3. Run Tests

```bash
mvn test -Dgroups=parity -Dtest=CustomerParityTest
```

---

## Interpreting Results

### Console Output

```
[INFO] Running com.example.customer.parity.CustomerParityTest
[INFO] ========================================
[INFO] PARITY TEST SUITE STARTING
[INFO] Running parity test: TC001 - Normal add - below discount threshold
[INFO] PASSED: TC001
...
[INFO] ========================================
[INFO] PARITY TEST SUITE COMPLETED
[INFO] Total: 35 | Passed: 35 | Failed: 0
[INFO] Pass Rate: 100.00%
[INFO] ========================================
```

### Report Sections

1. **Summary** — Total/passed/failed counts, pass rate
2. **Test Results** — Individual test outcomes with details
3. **Approved Tolerances** — Documented acceptable differences
4. **Known Deviations** — Tracked differences with business approval

---

## Approved Tolerances

### Decimal Rounding (±0.01)

COBOL `COMP-3` (packed decimal) and Java `BigDecimal` have minor rounding
differences. The test allows ±0.01 tolerance.

**Example:**
```
COBOL: 10000.51 * 0.05 = 500.0255 → truncates to 500.02
Java:  10000.51 * 0.05 = 500.0255 → rounds to 500.03 (HALF_UP)
```

### String Padding

COBOL pads all fields with spaces (`PIC X(n)`). Java strings are variable-length.
Comparison uses `trim()` to normalize.

---

## Troubleshooting

### Test fails with "Customer not found"

Ensure tests run in order (`@TestMethodOrder`). Some tests depend on prior
records existing (e.g., TC003 Inquiry depends on TC001 Add).

### Balance mismatch

Check if the difference is within tolerance (±0.01). If so, it's an approved
rounding difference. If larger, investigate the discount calculation.

### CSV parsing errors

- Ensure no trailing commas
- Empty fields are represented as `,,` not `, ,`
- Comments start with `#`

---

## COBOL Reference

### Discount Logic

```cobol
6000-APPLY-DISCOUNT.
    IF CUST-BALANCE > 10000.00
        COMPUTE WS-DISCOUNT-AMOUNT =
            CUST-BALANCE * WS-DISCOUNT-RATE
        SUBTRACT WS-DISCOUNT-AMOUNT FROM CUST-BALANCE
            ON SIZE ERROR
                MOVE 'DISCOUNT CALC OVERFLOW' TO WS-ERROR-MSG
                MOVE 6001 TO WS-RETURN-CODE
        END-SUBTRACT
    END-IF.
```

- `WS-DISCOUNT-RATE` = 0.0500 (5%)
- Threshold: `> 10000.00` (strictly greater than)
- `CUST-BALANCE` = `PIC S9(7)V99 COMP-3`

---

## CI/CD Integration

### GitHub Actions

```yaml
- name: Run Parity Tests
  run: mvn test -Dgroups=parity
  
- name: Upload Parity Report
  uses: actions/upload-artifact@v3
  with:
    name: parity-report
    path: target/parity-reports/*.md
```

### Fail Build on Parity Failure

Parity tests use standard JUnit assertions. Any failure will fail the build.

---

## Contact

For questions about parity tests or approved deviations, contact the Migration
Team lead.
