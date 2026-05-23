# Migration KPIs

> Key Performance Indicators for tracking COBOL modernization progress and quality.

## Overview

These KPIs provide visibility into migration progress, quality, and velocity. They should be tracked weekly and reported to stakeholders monthly.

---

## Primary KPIs

### 1. Programs Converted

**Definition:** Percentage of COBOL programs successfully converted to Java services.

**Formula:**
```
Programs Converted % = (Programs in Phase 6 / Total Programs) × 100
```

**Targets:**

| Milestone | Target | Date |
|-----------|--------|------|
| Q1 End | 15% | 2026-03-31 |
| Q2 End | 40% | 2026-06-30 |
| Q3 End | 70% | 2026-09-30 |
| Q4 End | 95% | 2026-12-31 |
| Final | 100% | 2027-01-31 |

**Data Source:** `program-inventory.json` → count where `migrationStatus = "completed"`

---

### 2. Test Coverage

**Definition:** Average code coverage across all converted Java services.

**Formula:**
```
Test Coverage % = (Covered Lines / Total Lines) × 100
```

**Target:** ≥ 90% (mandatory per Definition of Done)

**Thresholds:**

| Coverage | Status | Action |
|----------|--------|--------|
| ≥ 90% | ✅ Green | Meets standard |
| 80-89% | ⚠️ Yellow | Improvement needed |
| < 80% | ❌ Red | Blocks deployment |

**Data Source:** JaCoCo / SonarQube aggregate report

---

### 3. Parity Pass Rate

**Definition:** Percentage of parity tests passing across all converted programs.

**Formula:**
```
Parity Pass Rate % = (Passing Parity Tests / Total Parity Tests) × 100
```

**Target:** 100% (mandatory for production deployment)

**Thresholds:**

| Pass Rate | Status | Action |
|-----------|--------|--------|
| 100% | ✅ Green | Ready for production |
| 95-99% | ⚠️ Yellow | Investigate failures |
| < 95% | ❌ Red | Blocks deployment |

**Data Source:** CI pipeline parity test results

---

### 4. Cycle Time Per Program

**Definition:** Average elapsed time from Phase 2 start to Phase 6 completion per program.

**Formula:**
```
Cycle Time = (Phase 6 Completion Date - Phase 2 Start Date) in business days
```

**Targets by Complexity:**

| Complexity | Target Cycle Time |
|------------|-------------------|
| Low (1-3) | ≤ 5 days |
| Medium (4-6) | ≤ 15 days |
| High (7-8) | ≤ 30 days |
| Very High (9-10) | ≤ 45 days |

**Data Source:** Azure DevOps / Jira work item dates

---

### 5. Defect Escape Rate

**Definition:** Percentage of defects found in production vs. total defects found.

**Formula:**
```
Defect Escape Rate % = (Production Defects / Total Defects) × 100
```

**Target:** < 5%

**Thresholds:**

| Escape Rate | Status | Action |
|-------------|--------|--------|
| < 5% | ✅ Green | Testing effective |
| 5-10% | ⚠️ Yellow | Review test coverage |
| > 10% | ❌ Red | Testing process review required |

**Data Source:** Defect tracking system, categorized by discovery phase

---

## Secondary KPIs

### 6. Lines of Code Converted

**Definition:** Total COBOL lines converted vs. total COBOL lines.

**Formula:**
```
LOC Converted % = (Sum of LOC where status="completed" / Total LOC) × 100
```

**Note:** Tracks effort more accurately than program count (some programs are much larger).

**Data Source:** `program-inventory.json` → sum `linesOfCode` by status

---

### 7. JCL Jobs Migrated

**Definition:** Percentage of JCL jobs converted to GitHub Actions / Spring Batch.

**Formula:**
```
JCL Migrated % = (Jobs Migrated / Total Jobs) × 100
```

**Target:** Track alongside program conversion (jobs depend on services)

**Data Source:** `jcl-catalog.json` → count where `migrationStatus = "completed"`

---

### 8. UI Screens Converted

**Definition:** Percentage of BMS screens converted to React components.

**Formula:**
```
UI Converted % = (Screens Converted / Total Screens) × 100
```

**Data Source:** BMS screen inventory tracking

---

### 9. Data Migration Progress

**Definition:** Percentage of VSAM/DB2 datasets migrated to target databases.

**Formula:**
```
Data Migrated % = (Datasets Migrated / Total Datasets) × 100
```

**Data Source:** `data-dictionary.json` → count where `migrationStatus = "completed"`

---

### 10. Team Velocity

**Definition:** Story points completed per sprint per team.

**Formula:**
```
Velocity = Sum of Story Points in "Done" column per sprint
```

**Use:** Forecasting completion dates, capacity planning.

**Data Source:** Azure DevOps / Jira sprint reports

---

## Tracking Template

### Weekly Status Report

```markdown
# Migration Status Report — Week of [DATE]

## Summary

| KPI | Current | Target | Status |
|-----|---------|--------|--------|
| Programs Converted | [X]% ([N]/937) | [T]% | 🟢/🟡/🔴 |
| Test Coverage | [X]% | 90% | 🟢/🟡/🔴 |
| Parity Pass Rate | [X]% | 100% | 🟢/🟡/🔴 |
| Avg Cycle Time | [X] days | [T] days | 🟢/🟡/🔴 |
| Defect Escape Rate | [X]% | <5% | 🟢/🟡/🔴 |

## Progress by Phase

| Phase | Not Started | In Progress | Complete |
|-------|-------------|-------------|----------|
| Phase 1: Discovery | [N] | [N] | [N] |
| Phase 2: COBOL→Java | [N] | [N] | [N] |
| Phase 3: JCL→Workflow | [N] | [N] | [N] |
| Phase 4: UI | [N] | [N] | [N] |
| Phase 5: Data | [N] | [N] | [N] |
| Phase 6: Validation | [N] | [N] | [N] |

## Progress by Domain

| Domain | Programs | Complete | % |
|--------|----------|----------|---|
| Customer | [N] | [N] | [X]% |
| Orders | [N] | [N] | [X]% |
| Inventory | [N] | [N] | [X]% |
| Billing | [N] | [N] | [X]% |
| Reporting | [N] | [N] | [X]% |

## Velocity Trend

| Sprint | Planned | Completed | Variance |
|--------|---------|-----------|----------|
| Sprint [N-2] | [X] pts | [X] pts | [+/-X]% |
| Sprint [N-1] | [X] pts | [X] pts | [+/-X]% |
| Sprint [N] | [X] pts | [X] pts | [+/-X]% |

## Blockers & Risks

| ID | Description | Impact | Mitigation | Owner | Due |
|----|-------------|--------|------------|-------|-----|
| R-001 | [Description] | [H/M/L] | [Action] | [Name] | [Date] |

## Key Accomplishments This Week

- [Accomplishment 1]
- [Accomplishment 2]
- [Accomplishment 3]

## Focus Areas Next Week

- [Focus 1]
- [Focus 2]
- [Focus 3]
```

---

## Dashboard Queries

### Program Status Distribution

```sql
-- For reporting dashboard
SELECT 
    migrationStatus,
    COUNT(*) as program_count,
    SUM(linesOfCode) as total_loc,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM programs), 1) as percentage
FROM programs
GROUP BY migrationStatus
ORDER BY 
    CASE migrationStatus
        WHEN 'not-started' THEN 1
        WHEN 'in-progress' THEN 2
        WHEN 'testing' THEN 3
        WHEN 'completed' THEN 4
    END;
```

### Programs by Complexity

```sql
SELECT 
    CASE 
        WHEN complexity <= 3 THEN 'Low (1-3)'
        WHEN complexity <= 6 THEN 'Medium (4-6)'
        WHEN complexity <= 8 THEN 'High (7-8)'
        ELSE 'Very High (9-10)'
    END as complexity_band,
    COUNT(*) as total,
    SUM(CASE WHEN migrationStatus = 'completed' THEN 1 ELSE 0 END) as completed,
    ROUND(SUM(CASE WHEN migrationStatus = 'completed' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1) as pct_complete
FROM programs
GROUP BY complexity_band
ORDER BY MIN(complexity);
```

### Cycle Time by Program

```sql
SELECT 
    programId,
    businessDomain,
    complexity,
    DATEDIFF(day, phase2_start_date, phase6_complete_date) as cycle_time_days,
    CASE 
        WHEN complexity <= 3 AND cycle_time > 5 THEN 'Over target'
        WHEN complexity <= 6 AND cycle_time > 15 THEN 'Over target'
        WHEN complexity <= 8 AND cycle_time > 30 THEN 'Over target'
        WHEN complexity > 8 AND cycle_time > 45 THEN 'Over target'
        ELSE 'On track'
    END as status
FROM program_tracking
WHERE phase6_complete_date IS NOT NULL
ORDER BY cycle_time_days DESC;
```

---

## Reporting Cadence

| Report | Audience | Frequency | Format |
|--------|----------|-----------|--------|
| KPI Dashboard | All stakeholders | Real-time | Power BI |
| Weekly Status | Project team | Weekly (Monday) | Email + Meeting |
| Executive Summary | Leadership | Bi-weekly | Slide deck |
| Steering Committee | Executives | Monthly | Formal presentation |
| Post-Sprint Review | Development team | Per sprint | Retrospective |

---

## Data Collection Automation

### Update Program Status Script

```python
#!/usr/bin/env python3
"""Update program-inventory.json with latest status from ADO/Jira."""

import json
from azure.devops.connection import Connection
from msrest.authentication import BasicAuthentication

def sync_status():
    # Connect to ADO
    credentials = BasicAuthentication('', os.environ['ADO_PAT'])
    connection = Connection(base_url=os.environ['ADO_ORG_URL'], creds=credentials)
    
    # Query work items
    wit_client = connection.clients.get_work_item_tracking_client()
    
    # Load current inventory
    with open('data/program-inventory.json') as f:
        inventory = json.load(f)
    
    # Update statuses
    for program in inventory['programs']:
        work_item = find_work_item(wit_client, program['programId'])
        if work_item:
            program['migrationStatus'] = map_status(work_item.fields['System.State'])
    
    # Save updated inventory
    with open('data/program-inventory.json', 'w') as f:
        json.dump(inventory, f, indent=2)

if __name__ == '__main__':
    sync_status()
```

### CI Pipeline KPI Collection

```yaml
# .github/workflows/collect-kpis.yml
name: Collect KPIs

on:
  schedule:
    - cron: '0 6 * * *'  # Daily at 6 AM

jobs:
  collect:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Calculate Coverage
        run: |
          # Aggregate coverage from all services
          ./scripts/aggregate-coverage.sh > metrics/coverage.json
      
      - name: Calculate Parity Rate
        run: |
          # Count passing/failing parity tests
          ./scripts/parity-stats.sh > metrics/parity.json
      
      - name: Push to Metrics Store
        run: |
          # Push to Azure Monitor / Datadog / etc.
          ./scripts/push-metrics.sh
```

---

## Contacts

| Role | Contact | Responsibility |
|------|---------|----------------|
| Metrics Owner | metrics@acme.com | KPI definitions, data quality |
| Dashboard Admin | bi-team@acme.com | Power BI maintenance |
| Project Lead | pm@acme.com | Status reporting |
