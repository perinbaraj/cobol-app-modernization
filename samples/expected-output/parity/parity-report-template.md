# Parity Test Report

**Generated:** {{TIMESTAMP}}
**Source Program:** CUSTMGMT.cbl
**Target Service:** com.example.customer.service.CustomerService
**Test Suite Version:** 1.0.0

---

## Executive Summary

| Metric | Value |
|--------|-------|
| Total Test Cases | {{TOTAL_TESTS}} |
| Passed | {{PASSED_TESTS}} |
| Failed | {{FAILED_TESTS}} |
| **Pass Rate** | **{{PASS_RATE}}%** |

### Parity Status

- [ ] **FULL PARITY** — All tests pass, Java matches COBOL exactly
- [ ] **ACCEPTABLE PARITY** — All tests pass within approved tolerances
- [ ] **PARTIAL PARITY** — Some tests fail, deviations documented
- [ ] **FAILED** — Critical differences require remediation

---

## Test Categories Summary

| Category | Total | Passed | Failed | Notes |
|----------|-------|--------|--------|-------|
| Normal Operations (A/U/D/I) | | | | |
| Discount Boundary Tests | | | | |
| Maximum Precision | | | | |
| Maximum Length Strings | | | | |
| Empty/Space Fields | | | | |
| Status Codes (A/I/S) | | | | |
| Error Scenarios | | | | |

---

## Approved Tolerances

The following deviations from exact COBOL output are approved for the Java implementation:

### 1. Decimal Rounding Tolerance (±0.01)

**Approved:** {{DATE}}
**Approver:** {{APPROVER}}

| Tolerance | Reason |
|-----------|--------|
| ±0.01 | COBOL COMP-3 (packed decimal) vs Java BigDecimal rounding differences. COBOL uses truncation; Java uses HALF_UP. Difference is immaterial for financial purposes. |

**Example:**
- COBOL: `10000.51 * 0.05 = 500.025` → truncates to `500.02`
- Java: `10000.51 * 0.05 = 500.0255` → rounds to `500.03`
- Difference: `0.01` (within tolerance)

### 2. String Padding Differences

**Approved:** {{DATE}}
**Approver:** {{APPROVER}}

| Tolerance | Reason |
|-----------|--------|
| Trailing spaces ignored | COBOL uses fixed-length fields padded with spaces (PIC X(n)). Java strings are variable-length. Comparison after trim() is acceptable. |

---

## Detailed Test Results

### Normal Operations

| Test ID | Operation | Expected Code | Actual Code | Expected Balance | Actual Balance | Status |
|---------|-----------|---------------|-------------|------------------|----------------|--------|
| TC001 | Add | 0 | | 5000.00 | | |
| TC002 | Update | 0 | | 8000.00 | | |
| TC003 | Inquiry | 0 | | 8000.00 | | |
| TC004 | Delete | 0 | | - | | |

### Discount Boundary Tests

| Test ID | Input Balance | Discount Applied | Expected Balance | Actual Balance | Diff | Status |
|---------|---------------|------------------|------------------|----------------|------|--------|
| TC005 | 9999.99 | No | 9999.99 | | | |
| TC006 | 10000.00 | No | 10000.00 | | | |
| TC007 | 10000.01 | Yes (5%) | 9500.01 | | | |
| TC008 | 50000.00 | Yes (5%) | 47500.00 | | | |

### Error Code Mapping

| Test ID | Scenario | COBOL Code | Java Exception | Status |
|---------|----------|------------|----------------|--------|
| TC023 | Duplicate | 1002 | CustomerAlreadyExistsException | |
| TC024 | Not Found (Update) | 2001 | CustomerNotFoundException | |
| TC025 | Not Found (Delete) | 3001 | CustomerNotFoundException | |
| TC026 | Not Found (Inquiry) | 4001 | CustomerNotFoundException | |
| TC027 | Invalid Trans Type | 1001 | IllegalArgumentException | |

---

## Known Deviations

### Deviation #1: [Title]

**Test Cases Affected:** TC###, TC###
**Severity:** Low/Medium/High
**Status:** Approved/Pending/Rejected

**COBOL Behavior:**
```cobol
[Relevant COBOL code]
```

**Java Behavior:**
```java
[Relevant Java code]
```

**Business Impact:** [Description]

**Resolution:** [Accepted as-is / To be fixed / Workaround applied]

---

## Validation Sign-Off

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Developer | | | |
| QA Lead | | | |
| Business Analyst | | | |
| Migration Lead | | | |

---

## Appendix A: Test Data Files

- `input-data.csv` — Test input records
- `expected-output.csv` — Expected COBOL outputs
- `CustomerParityTest.java` — JUnit 5 test implementation

## Appendix B: Calculation Reference

### Discount Calculation Formula

```
IF CUST-BALANCE > 10000.00 THEN
    DISCOUNT = CUST-BALANCE * 0.0500
    NEW-BALANCE = CUST-BALANCE - DISCOUNT
END-IF
```

**Java equivalent:**
```java
if (balance.compareTo(new BigDecimal("10000.00")) > 0) {
    BigDecimal discount = balance.multiply(new BigDecimal("0.0500"))
                                  .setScale(2, RoundingMode.HALF_UP);
    balance = balance.subtract(discount);
}
```
