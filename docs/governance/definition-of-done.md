# Definition of Done

> Phase-specific completion criteria for the COBOL modernization project.

## Overview

Each migration phase has explicit acceptance criteria that must be met before work is considered complete. This document defines the **mandatory** and **recommended** criteria for each phase.

**Legend:**
- ✅ **Mandatory** — Must be completed; blocks phase completion
- ⭐ **Recommended** — Should be completed; exceptions require approval

---

## Phase 1: Discovery & Inventory

> Goal: Complete understanding of the mainframe codebase and dependencies.

### Mandatory Criteria ✅

| # | Criterion | Evidence |
|---|-----------|----------|
| 1.1 | All COBOL programs scanned and cataloged | `program-inventory.json` contains all programs |
| 1.2 | All copybooks identified and mapped to programs | `copybook-catalog.json` complete |
| 1.3 | All JCL jobs scanned and cataloged | `jcl-catalog.json` contains all jobs |
| 1.4 | Cross-program dependencies mapped | `calls` array populated for each program |
| 1.5 | Database/file access documented | `data-dictionary.json` complete |
| 1.6 | Complexity scores assigned | `complexity` field populated (1-10 scale) |
| 1.7 | Business domains identified and assigned | `businessDomain` field populated |
| 1.8 | Metadata validator passes | `node validate.js` returns exit code 0 |

### Recommended Criteria ⭐

| # | Criterion | Notes |
|---|-----------|-------|
| 1.9 | SME interviews completed for high-complexity programs | Document undocumented business rules |
| 1.10 | Migration priority order established | Based on dependencies, risk, business value |
| 1.11 | Estimated effort per program documented | Story points or days per program |
| 1.12 | Technical debt inventory created | Known issues in COBOL code |

### Deliverables

- [ ] `data/program-inventory.json` — All 937 programs
- [ ] `data/copybook-catalog.json` — All 412 copybooks
- [ ] `data/jcl-catalog.json` — All 15,250 JCL jobs
- [ ] `data/data-dictionary.json` — All file/dataset definitions
- [ ] Migration backlog prioritized in Azure DevOps/Jira
- [ ] Phase 1 sign-off from Project Lead

---

## Phase 2: COBOL to Java Conversion

> Goal: Each COBOL program converted to a tested, production-ready Java service.

### Mandatory Criteria ✅

| # | Criterion | Evidence |
|---|-----------|----------|
| 2.1 | Java service compiles without errors | `./gradlew build` or `mvn compile` succeeds |
| 2.2 | Unit tests written for all public methods | Test files exist in `src/test/` |
| 2.3 | **Test coverage ≥ 90%** | JaCoCo/SonarQube report |
| 2.4 | Integration tests for database operations | TestContainers tests pass |
| 2.5 | **Parity tests pass** | `@Tag("parity")` tests all green |
| 2.6 | No `double`/`float` for financial fields | Static analysis rule passes |
| 2.7 | All data types match copybook mapping | Copybook-mapper output verified |
| 2.8 | REST API documented with OpenAPI | Swagger UI accessible |
| 2.9 | Code review completed | PR approved by 2+ reviewers |
| 2.10 | No critical/high SonarQube issues | SonarQube quality gate passes |

### Recommended Criteria ⭐

| # | Criterion | Notes |
|---|-----------|-------|
| 2.11 | Performance within 10% of COBOL baseline | Load test results |
| 2.12 | Error handling matches COBOL behavior | Exception mapping documented |
| 2.13 | Logging follows standard patterns | SLF4J, structured logging |
| 2.14 | Database indexes optimized | Query plans reviewed |

### Per-Program Checklist

```markdown
## Program: [PROGRAM-ID]
Converted By: [Name]
Review Date: [Date]

### Mandatory
- [ ] 2.1 Compiles successfully
- [ ] 2.2 Unit tests written
- [ ] 2.3 Coverage ≥ 90%
- [ ] 2.4 Integration tests pass
- [ ] 2.5 Parity tests pass
- [ ] 2.6 No double/float for decimals
- [ ] 2.7 Data types verified
- [ ] 2.8 OpenAPI documented
- [ ] 2.9 PR approved
- [ ] 2.10 SonarQube passes

### Recommended
- [ ] 2.11 Performance verified
- [ ] 2.12 Error handling reviewed
- [ ] 2.13 Logging standardized
- [ ] 2.14 Indexes optimized

### Sign-off
- [ ] Developer: _______________
- [ ] Reviewer: _______________
- [ ] QA: _______________
```

---

## Phase 3: JCL to Workflow Migration

> Goal: All batch jobs converted to GitHub Actions workflows or Spring Batch with equivalent scheduling.

### Mandatory Criteria ✅

| # | Criterion | Evidence |
|---|-----------|----------|
| 3.1 | Workflow YAML valid | GitHub Actions lint passes |
| 3.2 | Schedule matches original JCL | Cron expression documented |
| 3.3 | All job steps converted | Step count matches JCL |
| 3.4 | Dependencies between jobs preserved | `needs:` clauses correct |
| 3.5 | Error handling equivalent | `if: failure()` steps present |
| 3.6 | Output datasets created correctly | File assertions in tests |
| 3.7 | Manual trigger available | `workflow_dispatch` enabled |
| 3.8 | Dry-run mode available | Can run without side effects |

### Recommended Criteria ⭐

| # | Criterion | Notes |
|---|-----------|-------|
| 3.9 | SLA monitoring configured | Alerts for late/failed jobs |
| 3.10 | Retry logic implemented | Exponential backoff |
| 3.11 | Parallel execution where safe | Performance improvement |
| 3.12 | Audit logging for compliance | Job runs logged to SIEM |

### Per-Job Checklist

```markdown
## JCL Job: [JOB-NAME]

### Mandatory
- [ ] 3.1 YAML validates
- [ ] 3.2 Schedule: `[cron]` matches original: `[schedule]`
- [ ] 3.3 Steps: [N] (matches JCL)
- [ ] 3.4 Dependencies preserved
- [ ] 3.5 Error handling complete
- [ ] 3.6 Output files verified
- [ ] 3.7 Manual trigger works
- [ ] 3.8 Dry-run tested

### Sign-off
- [ ] Developer: _______________
- [ ] Operations: _______________
```

---

## Phase 4: UI Modernization

> Goal: BMS screens converted to React components with modern UX.

### Mandatory Criteria ✅

| # | Criterion | Evidence |
|---|-----------|----------|
| 4.1 | All fields from BMS present in React | Field mapping complete |
| 4.2 | Form validation matches COBOL edits | Zod schema validates |
| 4.3 | TypeScript strict mode enabled | No `any` types |
| 4.4 | Component tests pass | React Testing Library |
| 4.5 | **E2E tests pass** | Playwright tests green |
| 4.6 | **WCAG 2.1 AA compliant** | axe-core audit passes |
| 4.7 | Responsive design (mobile/tablet/desktop) | Visual QA complete |
| 4.8 | Error states handled | API errors shown to user |
| 4.9 | Loading states implemented | Skeleton/spinner present |

### Recommended Criteria ⭐

| # | Criterion | Notes |
|---|-----------|-------|
| 4.10 | Lighthouse performance ≥ 90 | Core Web Vitals met |
| 4.11 | Keyboard navigation complete | Tab order logical |
| 4.12 | Internationalization ready | i18n keys used |
| 4.13 | Dark mode supported | Theme toggle works |

### Per-Screen Checklist

```markdown
## BMS Screen: [MAP-NAME]

### Mandatory
- [ ] 4.1 All [N] fields present
- [ ] 4.2 Validation matches COBOL
- [ ] 4.3 No TypeScript `any`
- [ ] 4.4 Component tests pass
- [ ] 4.5 E2E tests pass
- [ ] 4.6 Accessibility audit passes
- [ ] 4.7 Responsive on all breakpoints
- [ ] 4.8 Error states handled
- [ ] 4.9 Loading states implemented

### Sign-off
- [ ] Developer: _______________
- [ ] UX Designer: _______________
- [ ] Accessibility: _______________
```

---

## Phase 5: Data Migration

> Goal: All VSAM/DB2 data migrated to Azure SQL/MongoDB with verified integrity.

### Mandatory Criteria ✅

| # | Criterion | Evidence |
|---|-----------|----------|
| 5.1 | Flyway migrations applied successfully | `flyway info` shows success |
| 5.2 | All records migrated | Row counts match |
| 5.3 | **Data checksums verified** | Hash comparison passes |
| 5.4 | Referential integrity maintained | FK constraints validated |
| 5.5 | Character encoding correct | No mojibake in data |
| 5.6 | Decimal precision preserved | Sample comparison passes |
| 5.7 | Dates converted correctly | Timezone handling verified |
| 5.8 | Null handling documented | COBOL spaces → SQL nulls |
| 5.9 | Rollback tested | Can restore to pre-migration state |

### Recommended Criteria ⭐

| # | Criterion | Notes |
|---|-----------|-------|
| 5.10 | Performance indexes created | Query plan analysis |
| 5.11 | Archival strategy implemented | Old data archived |
| 5.12 | Data masking for non-prod | PII protected in dev/test |
| 5.13 | CDC streaming configured | Real-time sync if needed |

### Per-Dataset Checklist

```markdown
## Dataset: [DATASET-NAME]

### Mandatory
- [ ] 5.1 Migration scripts applied
- [ ] 5.2 Row count: Source [N] = Target [N]
- [ ] 5.3 Checksum: Source [hash] = Target [hash]
- [ ] 5.4 FK constraints validated
- [ ] 5.5 Encoding verified (sample check)
- [ ] 5.6 Decimal precision verified
- [ ] 5.7 Dates verified
- [ ] 5.8 Null handling documented
- [ ] 5.9 Rollback tested

### Sign-off
- [ ] Data Engineer: _______________
- [ ] DBA: _______________
- [ ] Data Owner: _______________
```

---

## Phase 6: Testing & Validation

> Goal: All parity tests passing, system validated, production sign-off obtained.

### Mandatory Criteria ✅

| # | Criterion | Evidence |
|---|-----------|----------|
| 6.1 | **All parity tests passing** | 100% pass rate |
| 6.2 | All approved deviations documented | `docs/deviations.md` complete |
| 6.3 | Integration test suite passing | CI pipeline green |
| 6.4 | E2E test suite passing | Playwright report |
| 6.5 | Performance testing complete | Load test report |
| 6.6 | Security scan complete | No critical/high vulnerabilities |
| 6.7 | UAT sign-off obtained | Business stakeholder approval |
| 6.8 | Runbooks complete | Operations documentation |
| 6.9 | Rollback plan documented and tested | Runbook verified |
| 6.10 | **Go-live checklist complete** | See below |

### Go-Live Checklist

```markdown
## Go-Live Sign-Off

**Program/Module:** _______________
**Go-Live Date:** _______________

### Technical Readiness
- [ ] All Phase 1-5 DoD criteria met
- [ ] All parity tests passing (6.1)
- [ ] Performance meets SLA
- [ ] Security scan clear
- [ ] Monitoring configured
- [ ] Alerts configured
- [ ] Runbooks published

### Business Readiness
- [ ] UAT complete and signed off
- [ ] Training delivered
- [ ] Support team briefed
- [ ] Communication sent to users

### Operational Readiness
- [ ] Rollback plan tested
- [ ] On-call schedule confirmed
- [ ] Escalation path documented
- [ ] Maintenance window scheduled

### Approvals
| Role | Name | Signature | Date |
|------|------|-----------|------|
| Project Lead | | | |
| Technical Lead | | | |
| Business Owner | | | |
| Operations Lead | | | |
| Security Officer | | | |
```

---

## Phase Transition Gates

### Gate Review Process

1. **Self-Assessment** — Developer completes phase checklist
2. **Peer Review** — Team lead verifies criteria
3. **QA Validation** — QA team runs test suites
4. **Gate Review Meeting** — Stakeholders review evidence
5. **Sign-Off** — Approvers sign checklist
6. **Status Update** — Update `migrationStatus` in metadata

### Gate Review Cadence

| Phase | Review Frequency | Attendees |
|-------|------------------|-----------|
| Phase 1 → 2 | Once per domain | Project Lead, Tech Lead, SME |
| Phase 2 → 3 | Per program batch | Tech Lead, QA Lead |
| Phase 3 → 4 | Per job family | Tech Lead, Operations |
| Phase 4 → 5 | Per UI module | Tech Lead, UX Lead |
| Phase 5 → 6 | Per data domain | Tech Lead, DBA, Data Owner |
| Phase 6 → Prod | Per release | Full stakeholder group |

---

## Exceptions Process

If a mandatory criterion cannot be met:

1. **Document Exception Request**
   - Criterion that cannot be met
   - Reason it cannot be met
   - Risk assessment
   - Mitigation plan
   - Proposed timeline to remediate

2. **Obtain Approval**
   - Tech Lead approval for low-risk exceptions
   - Project Lead approval for medium-risk
   - Steering Committee for high-risk

3. **Track Exception**
   - Add to exception register
   - Set remediation deadline
   - Assign owner

4. **Remediate**
   - Close exception before next phase gate
   - Or obtain extended approval
