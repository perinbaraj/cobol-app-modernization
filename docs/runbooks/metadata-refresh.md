# Metadata Refresh Runbook

> Operational guide for maintaining accurate mainframe metadata catalogs.

## Overview

The metadata catalogs (`program-inventory.json`, `copybook-catalog.json`, `jcl-catalog.json`, `data-dictionary.json`) are the foundation for all migration tooling. Stale metadata leads to incorrect dependency analysis, missed programs, and failed conversions.

---

## When to Refresh Metadata

### Scheduled Refresh

| Frequency | Trigger | Scope |
|-----------|---------|-------|
| **Weekly** | Every Monday 6 AM | Full scan — all COBOL, JCL, BMS |
| **Daily** | 11 PM (optional) | Incremental — changed files only |

### Event-Driven Refresh

Refresh metadata **immediately** when:

- [ ] New COBOL programs added to source repository
- [ ] Copybooks modified (affects all dependent programs)
- [ ] JCL jobs added or restructured
- [ ] BMS maps added or field layouts changed
- [ ] Migration phase transition (e.g., Phase 1 → Phase 2)
- [ ] Post-deployment validation required

---

## Running the Scanner

### Full Scan (Recommended Weekly)

```bash
# Navigate to scripts directory
cd scripts/

# Run full scan with all source directories
python scan-codebase.py \
  --cobol-dir ../mainframe-source/cobol \
  --jcl-dir ../mainframe-source/jcl \
  --bms-dir ../mainframe-source/bms \
  --output-dir ../data \
  --verbose

# Expected output: 4 JSON files in data/
```

### Incremental Scan (Changed Files Only)

```bash
# Scan only files modified since last run
python scan-codebase.py \
  --cobol-dir ../mainframe-source/cobol \
  --jcl-dir ../mainframe-source/jcl \
  --output-dir ../data \
  --incremental \
  --since "2026-05-16"
```

### Scan Specific Programs

```bash
# Targeted scan for specific programs (useful during active migration)
python scan-codebase.py \
  --cobol-dir ../mainframe-source/cobol \
  --programs CUSTMGMT,ORDPROC,INVMAINT \
  --output-dir ../data
```

---

## Validation After Refresh

Always validate metadata after refresh to catch parsing errors or schema violations.

### Run Validator

```bash
cd mcp-servers/mainframe-context/

# Validate all catalogs
node validate.js --data-dir ../../data

# Expected output on success:
# ✅ program-inventory.json: 937 programs validated
# ✅ copybook-catalog.json: 412 copybooks validated
# ✅ jcl-catalog.json: 15250 jobs validated
# ✅ data-dictionary.json: 2847 definitions validated
# ✅ Cross-references validated
```

### Validation Checklist

After refresh, verify:

- [ ] No `❌ [ERROR]` messages in validator output
- [ ] Program count matches expected (≈937 programs)
- [ ] Copybook count matches expected (≈412 copybooks)
- [ ] No orphaned references (programs calling non-existent programs)
- [ ] `migrationStatus` field present on all programs

---

## Troubleshooting Stale Data Issues

### Symptom: Missing Programs

**Indicators:**
- Agent reports "program not found" for known programs
- Dependency graph has gaps
- Migration status shows 0% for domains with completed work

**Resolution:**
```bash
# 1. Verify source files exist
ls -la ../mainframe-source/cobol/*.cbl | wc -l

# 2. Check scanner logs for parse errors
python scan-codebase.py --cobol-dir ../mainframe-source/cobol --verbose 2>&1 | grep ERROR

# 3. Re-run full scan with debug logging
python scan-codebase.py --cobol-dir ../mainframe-source/cobol --debug
```

### Symptom: Outdated Dependencies

**Indicators:**
- Converted service missing calls to other services
- Parity tests fail due to missing downstream calls
- Call graph doesn't match source COBOL

**Resolution:**
```bash
# 1. Force regeneration of dependency graph
python scan-codebase.py --cobol-dir ../mainframe-source/cobol --force-deps

# 2. Validate cross-references
node validate.js --data-dir ../../data --check-refs

# 3. Compare with mainframe documentation
diff <(grep "CALL " ../mainframe-source/cobol/CUSTMGMT.cbl) <(jq '.calls' data/program-inventory.json | grep CUSTMGMT)
```

### Symptom: Stale Migration Status

**Indicators:**
- Dashboard shows programs as "not-started" that are already in production
- Duplicate conversion work being assigned
- Phase completion metrics incorrect

**Resolution:**
```bash
# 1. Sync migration status from tracking system
python scripts/sync-migration-status.py --source azure-devops --target data/program-inventory.json

# 2. Manually update status (if needed)
jq '.programs[] | select(.programId=="CUSTMGMT") | .migrationStatus = "completed"' data/program-inventory.json > tmp.json && mv tmp.json data/program-inventory.json

# 3. Validate and commit
node validate.js --data-dir ../../data
git add data/
git commit -m "chore: sync migration status from ADO"
```

### Symptom: MCP Server Returns Old Data

**Indicators:**
- Copilot agents reference outdated information
- "Last refreshed" timestamp is days old
- MCP server logs show cache hits for stale data

**Resolution:**
```bash
# 1. Restart MCP server to clear cache
pm2 restart mainframe-context

# 2. Or kill and restart manually
pkill -f "node.*mainframe-context"
cd mcp-servers/mainframe-context && npm start

# 3. Verify fresh data is loaded
curl http://localhost:3001/health | jq '.lastRefresh'
```

---

## Automation

### GitHub Actions Workflow (Weekly Refresh)

```yaml
# .github/workflows/metadata-refresh.yml
name: Metadata Refresh

on:
  schedule:
    - cron: '0 6 * * 1'  # Every Monday at 6 AM UTC
  workflow_dispatch:      # Manual trigger

jobs:
  refresh:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.11'
      
      - name: Run Scanner
        run: |
          cd scripts
          python scan-codebase.py \
            --cobol-dir ../mainframe-source/cobol \
            --jcl-dir ../mainframe-source/jcl \
            --bms-dir ../mainframe-source/bms \
            --output-dir ../data
      
      - name: Validate Metadata
        run: |
          cd mcp-servers/mainframe-context
          npm ci
          node validate.js --data-dir ../../data
      
      - name: Commit Changes
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git add data/
          git diff --cached --quiet || git commit -m "chore: weekly metadata refresh"
          git push
```

---

## Contacts

| Role | Contact | Escalation |
|------|---------|------------|
| Scanner Issues | migration-tooling@acme.com | Within 4 hours |
| MCP Server Issues | platform-team@acme.com | Within 2 hours |
| Data Quality | data-governance@acme.com | Within 24 hours |
