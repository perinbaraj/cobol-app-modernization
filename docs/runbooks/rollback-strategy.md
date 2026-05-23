# Rollback Strategy Runbook

> Procedures for reverting database, service, and UI changes when issues are detected post-deployment.

## Overview

Rollbacks may be necessary when:
- Critical bugs discovered in production
- Performance degradation beyond acceptable thresholds
- Data integrity issues detected
- Parity test failures found after deployment
- Business stakeholder requests reversion

**Guiding Principle:** All rollbacks should return the system to a known good state with minimal data loss.

---

## Rollback Decision Matrix

| Issue Severity | Response Time | Rollback Scope | Approval Required |
|----------------|---------------|----------------|-------------------|
| **P1 - Critical** | < 15 min | Full rollback | On-call lead |
| **P2 - High** | < 1 hour | Component rollback | Team lead |
| **P3 - Medium** | < 4 hours | Targeted fix preferred | Developer |
| **P4 - Low** | Next sprint | Forward fix | No rollback |

---

## Database Rollback (Flyway)

### Prerequisites

- Flyway migrations must have corresponding undo scripts
- Undo scripts located in `src/main/resources/db/migration/undo/`
- Naming convention: `U{version}__{description}.sql`

### Rollback Procedure

#### Step 1: Identify Target Version

```bash
# Check current version
flyway info -url=jdbc:sqlserver://your-server -user=admin -password=***

# Output shows applied migrations:
# | Version | Description          | State   |
# |---------|----------------------|---------|
# | 1       | Create customer table| Success |
# | 2       | Add phone table      | Success |
# | 3       | Add indexes          | Success |  ← Current
```

#### Step 2: Execute Undo Migration

```bash
# Rollback last migration
flyway undo -url=jdbc:sqlserver://your-server -user=admin -password=***

# Rollback to specific version (e.g., version 1)
flyway undo -target=1 -url=jdbc:sqlserver://your-server -user=admin -password=***
```

#### Step 3: Verify Rollback

```bash
# Confirm version changed
flyway info

# Validate schema matches expected state
flyway validate
```

### Undo Script Example

```sql
-- V3__Add_indexes.sql (forward migration)
CREATE INDEX idx_customer_email ON customer(email);
CREATE INDEX idx_customer_phone ON customer_phone(customer_id);

-- U3__Add_indexes.sql (undo migration)
DROP INDEX IF EXISTS idx_customer_email ON customer;
DROP INDEX IF EXISTS idx_customer_phone ON customer_phone;
```

### Data Migration Rollback

For migrations that moved data (not just schema):

```sql
-- U5__Migrate_legacy_status.sql
-- Restore original status values from backup table
UPDATE customer c
SET c.status = b.original_status
FROM customer_status_backup b
WHERE c.id = b.customer_id;

-- Remove backup table (optional - keep for audit)
-- DROP TABLE customer_status_backup;
```

### Emergency: Point-in-Time Recovery

If Flyway undo is insufficient:

```bash
# Azure SQL point-in-time restore (up to 35 days)
az sql db restore \
  --dest-name customer-db-restored \
  --name customer-db \
  --resource-group acme-rg \
  --server acme-sql-server \
  --time "2026-05-22T14:30:00Z"
```

---

## Service Rollback (Git Revert)

### Standard Revert (Single Commit)

```bash
# Find the problematic commit
git log --oneline -10

# Revert the commit (creates new commit)
git revert <commit-hash>

# Push to trigger deployment
git push origin main
```

### Revert Merge Commit (PR Revert)

```bash
# For merge commits, specify parent
git revert -m 1 <merge-commit-hash>

# -m 1 keeps the main branch history, reverts PR changes
git push origin main
```

### Revert Multiple Commits

```bash
# Revert a range of commits (oldest to newest)
git revert --no-commit <oldest-hash>^..<newest-hash>

# Review changes before committing
git status
git diff --staged

# Commit the combined revert
git commit -m "Revert: Roll back CUSTMGMT changes (commits abc123..def456)"
git push origin main
```

### Deploy Previous Container Image

If reverting code isn't fast enough:

```bash
# List available images
az acr repository show-tags \
  --name acmecontainerreg \
  --repository customer-service \
  --orderby time_desc

# Deploy previous version
kubectl set image deployment/customer-service \
  customer-service=acmecontainerreg.azurecr.io/customer-service:v1.2.3

# Or using Helm
helm rollback customer-service 2  # Rollback to revision 2
```

### Service Rollback Checklist

- [ ] Identify problematic commit(s) or deployment
- [ ] Create revert commit(s)
- [ ] Update changelog with rollback note
- [ ] Run smoke tests against reverted version
- [ ] Verify health checks pass
- [ ] Monitor error rates for 30 minutes
- [ ] Notify stakeholders of rollback

---

## UI Rollback Procedures

### Revert React Build

```bash
# Revert the UI commit
git revert <ui-commit-hash>

# Rebuild and deploy
npm ci
npm run build
npm run deploy
```

### Deploy Previous UI Bundle

```bash
# If using CDN/blob storage for static assets
# List previous versions
az storage blob list \
  --container-name ui-builds \
  --account-name acmestorage \
  --prefix "customer-ui/" \
  --query "[].{name:name, lastModified:properties.lastModified}" \
  --output table

# Restore previous version
az storage blob copy start \
  --source-container ui-builds \
  --source-blob "customer-ui/v1.2.3/index.html" \
  --destination-container ui-live \
  --destination-blob "index.html" \
  --account-name acmestorage
```

### Feature Flag Rollback

If the issue is isolated to a feature:

```typescript
// Disable feature via environment variable or config service
// .env or Azure App Configuration
FEATURE_NEW_CUSTOMER_FORM=false

// React code checks flag
const { isEnabled } = useFeatureFlag('NEW_CUSTOMER_FORM');

if (isEnabled) {
  return <NewCustomerForm />;
} else {
  return <LegacyCustomerForm />;  // Rollback to legacy
}
```

### UI Rollback Checklist

- [ ] Identify failing component or feature
- [ ] Determine rollback strategy (revert, redeploy, feature flag)
- [ ] Clear CDN cache if applicable
- [ ] Verify rollback in staging environment
- [ ] Deploy to production
- [ ] Test critical user journeys
- [ ] Monitor error tracking (Sentry, etc.)

---

## Data Reconciliation After Rollback

### When Required

Data reconciliation is required when:
- Database rollback affects tables modified by users post-deployment
- Service rollback leaves data in inconsistent state
- Partial failures during deployment left mixed state

### Reconciliation Process

#### Step 1: Identify Affected Records

```sql
-- Find records modified after deployment
SELECT id, updated_at, created_at
FROM customer
WHERE updated_at > '2026-05-22T14:00:00Z'
  AND updated_at < '2026-05-23T10:30:00Z';  -- Rollback time
```

#### Step 2: Export Affected Data

```bash
# Export to CSV for analysis
sqlcmd -S server -d customer-db -Q "
  SELECT * FROM customer 
  WHERE updated_at BETWEEN '2026-05-22T14:00:00' AND '2026-05-23T10:30:00'
" -o affected_customers.csv -s"," -W
```

#### Step 3: Determine Recovery Action

| Scenario | Action |
|----------|--------|
| Data valid, just wrong schema | Re-apply after forward fix |
| Data corrupted by bug | Restore from backup, replay valid transactions |
| New records created | Keep in holding table, migrate after fix |
| Updates to existing records | Manual review required |

#### Step 4: Re-apply Valid Changes

```sql
-- Example: Re-apply valid customer updates from holding table
INSERT INTO customer (id, name, email, status, created_at, updated_at)
SELECT id, name, email, status, created_at, GETDATE()
FROM customer_recovery_hold
WHERE validation_status = 'VALID'
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  email = EXCLUDED.email,
  status = EXCLUDED.status,
  updated_at = EXCLUDED.updated_at;
```

#### Step 5: Document and Notify

```markdown
## Data Reconciliation Report

**Rollback Date:** 2026-05-23 10:30 UTC
**Affected Table:** customer
**Records Affected:** 47

### Summary
- 42 records re-applied successfully
- 3 records require manual review (conflicting updates)
- 2 records discarded (created by bug)

### Manual Review Required
| Customer ID | Issue | Assigned To |
|-------------|-------|-------------|
| C12345 | Duplicate email conflict | J. Smith |
| C12346 | Invalid status transition | M. Jones |
| C12347 | Missing required field | J. Smith |
```

---

## Post-Rollback Actions

### Immediate (Within 1 Hour)

- [ ] Confirm system stability (error rates, latency)
- [ ] Notify stakeholders (email, Slack, Teams)
- [ ] Create incident ticket if not already exists
- [ ] Document timeline in incident ticket

### Short-Term (Within 24 Hours)

- [ ] Conduct root cause analysis
- [ ] Identify fix for original issue
- [ ] Write regression test for the bug
- [ ] Update runbook if new scenario discovered
- [ ] Schedule post-mortem if P1/P2

### Before Re-deployment

- [ ] Fix verified in development environment
- [ ] Parity tests passing
- [ ] Code review completed
- [ ] Staged deployment successful
- [ ] Rollback plan reviewed and ready
- [ ] Stakeholder sign-off obtained

---

## Rollback Communication Template

```markdown
Subject: [ROLLBACK] Customer Service v2.3.1 → v2.3.0

**Status:** Rollback Complete
**Time:** 2026-05-23 10:30 UTC
**Duration:** 15 minutes
**Impact:** Customer lookup unavailable during rollback

## Summary
Rolled back Customer Service from v2.3.1 to v2.3.0 due to [brief description].

## Affected Systems
- Customer Service API
- Customer UI (cached data may be stale)

## User Impact
- Users may need to refresh browser
- Any customer updates between 09:00-10:30 UTC are being reconciled

## Next Steps
1. Root cause analysis in progress
2. Fix ETA: [date]
3. Re-deployment planned for: [date]

## Contact
Incident Commander: [name]
Slack Channel: #incident-2026-05-23
Ticket: ACME-12345
```

---

## Contacts

| Role | Contact | Availability |
|------|---------|--------------|
| On-Call Lead | oncall@acme.com | 24/7 |
| Database Team | dba-team@acme.com | Business hours + on-call |
| Platform Team | platform@acme.com | Business hours + on-call |
| Business Stakeholders | business-ops@acme.com | Business hours |
