# Parity Test Triage Runbook

> Guide for investigating and resolving parity test failures between COBOL and Java implementations.

## Overview

Parity tests compare the output of original COBOL programs against converted Java services to ensure functional equivalence. Failures indicate potential bugs in the conversion that must be resolved before production deployment.

---

## Quick Triage Flowchart

```
Parity Test Failure
        │
        ▼
┌───────────────────┐
│ Check error type  │
└─────────┬─────────┘
          │
    ┌─────┴─────┬─────────────┬──────────────┐
    ▼           ▼             ▼              ▼
┌───────┐  ┌────────┐   ┌──────────┐   ┌───────────┐
│Decimal│  │  Null  │   │ Spacing/ │   │ Business  │
│ Diff  │  │Handling│   │ Padding  │   │  Logic    │
└───┬───┘  └───┬────┘   └────┬─────┘   └─────┬─────┘
    │          │             │               │
    ▼          ▼             ▼               ▼
  Adjust    Handle       Trim/Pad        Escalate
 BigDecimal  nulls        output         to SME
```

---

## Investigation Steps

### Step 1: Identify the Failing Test

```bash
# Run parity tests with detailed output
./gradlew test --tests "*ParityTest*" --info

# Or with Maven
mvn test -Dtest="*ParityTest*" -X

# Find the specific assertion that failed
grep -A 10 "AssertionError" build/reports/tests/test/index.html
```

### Step 2: Locate Test Input and Expected Output

Parity tests use test fixtures from the `test/resources/parity/` directory:

```
test/resources/parity/
├── CUSTMGMT/
│   ├── input/
│   │   ├── scenario-001.json      # Input data
│   │   └── scenario-002.json
│   ├── expected/
│   │   ├── scenario-001.json      # Expected COBOL output
│   │   └── scenario-002.json
│   └── actual/                    # Generated during test run
│       ├── scenario-001.json
│       └── scenario-002.json
```

### Step 3: Compare Expected vs Actual

```bash
# Visual diff of expected vs actual
diff -u test/resources/parity/CUSTMGMT/expected/scenario-001.json \
        test/resources/parity/CUSTMGMT/actual/scenario-001.json

# Or use a diff tool
code --diff expected/scenario-001.json actual/scenario-001.json
```

---

## Common Failure Causes

### 1. Decimal Precision Differences

**Symptom:** Values differ by small amounts (e.g., `100.10` vs `100.1` or `100.099999`)

**COBOL Context:**
- COBOL uses fixed-point decimal (`PIC 9(7)V99` = 2 decimal places, always)
- Java `double` introduces floating-point errors

**Resolution:**
```java
// ❌ WRONG - using double
double amount = 100.10;

// ✅ CORRECT - using BigDecimal with explicit scale
BigDecimal amount = new BigDecimal("100.10").setScale(2, RoundingMode.HALF_UP);
```

**Test Fix:**
```java
// Use BigDecimal comparison with scale
assertThat(actual.getAmount())
    .usingComparator(BigDecimal::compareTo)
    .isEqualTo(expected.getAmount());
```

---

### 2. Null Handling Differences

**Symptom:** Java returns `null`, COBOL returned spaces or zeros

**COBOL Context:**
- COBOL has no concept of `null`
- Uninitialized alphanumeric fields contain spaces
- Uninitialized numeric fields contain zeros

**Resolution:**
```java
// ❌ WRONG - returning null for empty
if (cobolField.isEmpty()) {
    return null;
}

// ✅ CORRECT - preserving COBOL semantics
if (cobolField.isBlank()) {
    return "";  // or "          " for fixed-width
}

// For numeric fields
if (cobolField == null) {
    return BigDecimal.ZERO;
}
```

**Test Fix:**
```java
// Normalize nulls before comparison
String normalizedActual = actual == null ? "" : actual;
String normalizedExpected = expected == null ? "" : expected;
assertThat(normalizedActual).isEqualTo(normalizedExpected);
```

---

### 3. Spacing and Padding Differences

**Symptom:** Strings differ only in trailing spaces (e.g., `"SMITH"` vs `"SMITH     "`)

**COBOL Context:**
- COBOL `PIC X(10)` always stores 10 characters, right-padded with spaces
- Java `String` does not preserve trailing spaces

**Resolution:**
```java
// ❌ WRONG - trimming unconditionally
return cobolField.trim();

// ✅ CORRECT - preserving original length when needed
return StringUtils.rightPad(value, FIELD_LENGTH);

// Or for comparison purposes, trim both sides
public static String normalizeForComparison(String value) {
    return value == null ? "" : value.trim();
}
```

**Test Fix:**
```java
// Normalize whitespace in assertions
assertThat(actual.trim()).isEqualTo(expected.trim());

// Or use custom comparator
assertThat(actual)
    .usingComparator((a, b) -> a.trim().compareTo(b.trim()))
    .isEqualTo(expected);
```

---

### 4. Date/Time Format Differences

**Symptom:** Dates in different formats (e.g., `2026-05-23` vs `20260523` vs `05/23/26`)

**COBOL Context:**
- COBOL stores dates as numeric or alphanumeric strings
- Common formats: `YYYYMMDD`, `MMDDYYYY`, `YYMMDD`
- No timezone concept in most COBOL programs

**Resolution:**
```java
// ❌ WRONG - using Java date formatting
return LocalDate.now().toString();  // "2026-05-23"

// ✅ CORRECT - matching COBOL format
DateTimeFormatter COBOL_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
return LocalDate.now().format(COBOL_DATE);  // "20260523"
```

---

### 5. Numeric Sign Handling

**Symptom:** Negative numbers display differently (e.g., `-100` vs `100-` vs `100CR`)

**COBOL Context:**
- COBOL can represent signs as trailing characters
- `PIC S9(5)` with `SIGN TRAILING` stores `12345-` for -12345

**Resolution:**
```java
// Convert COBOL signed numeric to Java
public BigDecimal parseCobolSignedNumeric(String value) {
    if (value.endsWith("-") || value.endsWith("CR")) {
        String numPart = value.replaceAll("[^0-9.]", "");
        return new BigDecimal(numPart).negate();
    }
    return new BigDecimal(value.replaceAll("[^0-9.]", ""));
}
```

---

### 6. Business Logic Differences (Escalate)

**Symptom:** Output is fundamentally different, not just formatting

**Examples:**
- Different calculation results beyond rounding
- Different records returned from queries
- Different branching paths taken

**This requires escalation** — see next section.

---

## Escalation Path

### When to Escalate

Escalate to business SME when:

- [ ] Logic difference cannot be explained by data type conversion
- [ ] Multiple parity tests fail with same pattern
- [ ] COBOL source appears to have undocumented business rules
- [ ] Behavior depends on mainframe-specific features (CICS, DB2 specifics)

### Escalation Process

1. **Document the Discrepancy**
   ```markdown
   ## Parity Failure Report
   
   **Program:** CUSTMGMT
   **Test Case:** scenario-047
   **Date:** 2026-05-23
   
   ### Input
   ```json
   { "customerId": "C12345", "action": "CALCULATE_FEES" }
   ```
   
   ### Expected (COBOL)
   ```json
   { "feeAmount": 125.50 }
   ```
   
   ### Actual (Java)
   ```json
   { "feeAmount": 127.00 }
   ```
   
   ### Analysis
   The fee calculation differs by $1.50. Reviewed Java implementation
   and it matches documented formula. Suspect COBOL has legacy override.
   ```

2. **Create Escalation Ticket**
   - Title: `[Parity] CUSTMGMT fee calculation discrepancy`
   - Priority: Based on business impact
   - Assign to: Business SME for that domain
   - Label: `parity-escalation`, `needs-sme-review`

3. **Schedule Review Meeting** (if complex)
   - Include: Developer, Business SME, QA Lead
   - Bring: COBOL source, Java implementation, test data

---

## Approved Deviations

Some differences between COBOL and Java may be **intentional and approved**. Document these to prevent re-investigation.

### Deviation Documentation Process

1. **Get Approval**
   - Business SME must approve in writing (email or ticket comment)
   - Architecture Review Board approval for structural changes

2. **Document in Code**
   ```java
   /**
    * DEVIATION: CUSTMGMT-DEV-001
    * 
    * COBOL rounds fees to nearest dollar, Java uses precise cents.
    * Approved by: Jane Smith (Business Lead) on 2026-04-15
    * Ticket: ACME-4521
    * Reason: Modern UI requires cent precision for display
    */
   public BigDecimal calculateFee(BigDecimal amount) {
       // New precise calculation
   }
   ```

3. **Update Parity Test**
   ```java
   @Test
   @Tag("parity")
   @Tag("approved-deviation:CUSTMGMT-DEV-001")
   void testFeeCalculation_approvedDeviation() {
       // Test documents the deviation, does not assert exact match
       BigDecimal cobolResult = new BigDecimal("126.00");  // Rounded
       BigDecimal javaResult = service.calculateFee(amount);
       
       // Assert within acceptable range
       assertThat(javaResult).isBetween(
           cobolResult.subtract(new BigDecimal("0.99")),
           cobolResult.add(new BigDecimal("0.99"))
       );
   }
   ```

4. **Add to Deviation Registry**
   
   Maintain `docs/deviations.md`:
   ```markdown
   | ID | Program | Description | Approved By | Date | Ticket |
   |----|---------|-------------|-------------|------|--------|
   | CUSTMGMT-DEV-001 | CUSTMGMT | Fee rounding precision | J. Smith | 2026-04-15 | ACME-4521 |
   ```

---

## Parity Test Best Practices

### Tagging Strategy

```java
@Tag("parity")           // All parity tests
@Tag("domain:customer")  // Business domain
@Tag("program:CUSTMGMT") // Source program
@Tag("critical")         // High-impact test
```

### Running Parity Tests Only

```bash
# Gradle
./gradlew test --tests "*" -PincludeTags="parity"

# Maven
mvn test -Dgroups="parity"
```

### CI Pipeline Integration

```yaml
parity-tests:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - name: Run Parity Tests
      run: ./gradlew test -PincludeTags="parity"
    - name: Upload Results
      if: failure()
      uses: actions/upload-artifact@v4
      with:
        name: parity-failures
        path: |
          build/reports/tests/
          test/resources/parity/**/actual/
```

---

## Contacts

| Role | Contact | Response Time |
|------|---------|---------------|
| Parity Test Support | migration-qa@acme.com | 4 hours |
| Business SME (Customer) | customer-sme@acme.com | 24 hours |
| Business SME (Orders) | orders-sme@acme.com | 24 hours |
| Architecture Review | arch-review@acme.com | 48 hours |
