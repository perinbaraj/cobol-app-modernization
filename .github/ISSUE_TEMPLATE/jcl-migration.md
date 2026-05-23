---
name: JCL Migration
about: Request conversion of a JCL job to GitHub Actions workflow and/or Spring Batch
title: '[JCL→GHA] Convert {JOB_NAME}'
labels: ['migration', 'jcl-migration', 'phase-3']
assignees: ''
---

## JCL Job Details

**Job Name:** <!-- e.g., CUSTBAT -->
**Source File:** <!-- e.g., samples/jcl/CUSTBAT.jcl -->

### Job Type
- [ ] Batch data processing
- [ ] Report generation
- [ ] Data extraction
- [ ] Data transformation
- [ ] File transfer
- [ ] Database maintenance
- [ ] Other: <!-- specify -->

### Schedule
**Current Schedule:** <!-- e.g., Daily at 06:00 EST -->
**Target Schedule (cron):** <!-- e.g., 0 11 * * * (06:00 EST = 11:00 UTC) -->

---

## Dependencies

### Predecessor Jobs
<!-- Jobs that must complete before this one runs -->
- <!-- e.g., CUSTEXTRACT must complete successfully -->

### Successor Jobs
<!-- Jobs that depend on this one -->
- <!-- e.g., CUSTRPT runs after this completes -->

### Required Services
<!-- Java services this job will call -->
- <!-- e.g., CustomerService (/api/customers) -->

---

## Target Configuration

**Orchestration:** <!-- GitHub Actions / Spring Batch / Both -->
**Processing Type:** 
- [ ] Simple API calls (GitHub Actions only)
- [ ] Complex data processing (Spring Batch)
- [ ] Hybrid (GHA orchestrates Spring Batch jobs)

---

## Requirements

### Functional Requirements
<!-- What does this job accomplish? -->
1. 
2. 

### Data Requirements
- **Input:** <!-- e.g., Customer extract file from predecessor job -->
- **Output:** <!-- e.g., Updated customer records in Azure SQL -->
- **Volume:** <!-- e.g., ~50,000 records daily -->

---

## Acceptance Criteria

### Workflow/Batch
- [ ] Workflow has both schedule AND workflow_dispatch triggers
- [ ] Timeout configured appropriately
- [ ] Error handling and notifications implemented
- [ ] Job summary step included
- [ ] No hardcoded credentials (uses GitHub secrets)

### For Spring Batch (if applicable)
- [ ] Job definition with proper Step chain
- [ ] ItemReader/Processor/Writer implemented
- [ ] Retry and skip policies configured
- [ ] Job parameters externalized

### Testing
- [ ] Workflow tested with manual trigger
- [ ] Error scenarios tested
- [ ] Performance validated against requirements

### Documentation
- [ ] Comments reference original JCL job name
- [ ] README.md updated with job documentation
- [ ] Runbook for manual operations

---

## Reference Materials
<!-- Optional: Link to any additional context -->
- JCL documentation: 
- Predecessor job migration: 
- Related Java services: 

---

## Agent Instructions

@jcl-migrator Please convert this JCL to:
1. GitHub Actions workflow for orchestration
2. Spring Batch config for data processing steps (if needed)
