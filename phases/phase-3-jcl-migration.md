# Phase 3: JCL Migration — Jobs to Modern Orchestration

## Objective
Convert 15,250 JCL job streams into modern orchestration using GitHub Actions workflows and Spring Batch jobs, powered by GitHub Copilot.

---

## Inputs
| Input | Source | Description |
|-------|--------|-------------|
| JCL job streams | Source repo | EXEC, DD, PROC statements |
| JCL procedures (PROCs) | Source repo | Reusable job templates |
| Dependency map | Phase 1 | Job scheduling dependencies |
| Converted Java services | Phase 2 | Target services to orchestrate |

## Outputs
| Output | Format | Description |
|--------|--------|-------------|
| GitHub Actions workflows | `.github/workflows/*.yml` | CI/CD + scheduled jobs |
| Spring Batch jobs | Java configuration classes | Batch processing |
| Job documentation | Markdown | Schedule, SLA, dependencies |
| Migration scripts | Shell/PowerShell | Data movement helpers |

---

## GitHub Copilot Features Used

### 1. `jcl-migrator` Custom Agent
Specialized for JCL → modern orchestration conversion.

**Example invocation:**
```
@jcl-migrator Convert this JCL job to a GitHub Actions workflow:

//CUSTBATCH JOB (ACCT),'CUSTOMER BATCH',CLASS=A,MSGCLASS=X
//STEP1    EXEC PGM=CUSTLOAD
//INPUT    DD DSN=PROD.CUSTOMER.DAILY,DISP=SHR
//OUTPUT   DD DSN=PROD.CUSTOMER.MASTER,DISP=OLD
//SYSOUT   DD SYSOUT=*
//STEP2    EXEC PGM=CUSTREPORT,COND=(0,NE,STEP1)
//INPUT    DD DSN=PROD.CUSTOMER.MASTER,DISP=SHR
//REPORT   DD SYSOUT=*

Requirements:
- Map PGM= to Java service invocations
- Convert COND= to GitHub Actions conditional steps
- Map DD statements to environment variables or secrets
- Add error handling and notifications
```

### 2. Copilot Chat (JCL Understanding)
For complex JCL with nested PROCs:
```
Explain this JCL procedure step by step. For each STEP:
1. What program does it execute?
2. What files does it read/write?
3. What are the condition codes?
4. What is the dependency chain?
```

### 3. Copilot Coding Agent
Create Issues for batch JCL conversion:
```markdown
## Convert JCL Job: CUSTBATCH

Convert the JCL job CUSTBATCH to:
1. A GitHub Actions workflow for the orchestration
2. A Spring Batch job for the data processing steps

Include: error handling, retry logic, notifications on failure.
```

---

## JCL → Modern Stack Mapping

| JCL Concept | Modern Equivalent | Copilot Generates |
|-------------|------------------|-------------------|
| `JOB` statement | GitHub Actions workflow | Workflow YAML |
| `EXEC PGM=` | Spring Boot service call | REST API invocation step |
| `EXEC PROC=` | Reusable workflow (composite action) | Action YAML |
| `DD DSN=` input | Environment variable / S3 path | env: config |
| `DD DSN=` output | Artifact upload / DB write | Upload step |
| `DD SYSOUT=*` | Logging (stdout) | Logging config |
| `COND=(0,NE)` | `if: steps.X.outcome == 'success'` | Conditional step |
| `CLASS=A` | `runs-on: ubuntu-latest` | Runner config |
| `TIME=` | `timeout-minutes:` | Timeout config |
| `RESTART=` | Retry / rerun workflow | Retry step |
| Job scheduling (CA7/TWS) | `schedule: cron` | Cron expression |

---

## Conversion Patterns

### Pattern 1: Simple Sequential Job → GitHub Actions

**JCL:**
```jcl
//DAILYJOB JOB (ACCT),'DAILY PROCESS',CLASS=A
//STEP1   EXEC PGM=EXTRACT
//STEP2   EXEC PGM=TRANSFORM,COND=(0,NE,STEP1)
//STEP3   EXEC PGM=LOAD,COND=(0,NE,STEP2)
```

**GitHub Actions (Copilot output):**
```yaml
name: Daily ETL Process
on:
  schedule:
    - cron: '0 6 * * *'  # Daily at 6 AM
  workflow_dispatch: {}

jobs:
  daily-etl:
    runs-on: ubuntu-latest
    steps:
      - name: Extract
        run: curl -X POST ${{ vars.API_BASE }}/extract
        id: extract

      - name: Transform
        if: steps.extract.outcome == 'success'
        run: curl -X POST ${{ vars.API_BASE }}/transform

      - name: Load
        if: steps.transform.outcome == 'success'
        run: curl -X POST ${{ vars.API_BASE }}/load
```

### Pattern 2: Batch Processing → Spring Batch

**JCL:**
```jcl
//BATCHJOB JOB (ACCT),'BATCH UPDATE'
//STEP1   EXEC PGM=CUSTUPD
//INPUT   DD DSN=PROD.CUST.UPDATES,DISP=SHR
//MASTER  DD DSN=PROD.CUST.MASTER,DISP=OLD
//REPORT  DD SYSOUT=*
```

**Spring Batch (Copilot output):**
```java
@Configuration
@EnableBatchProcessing
public class CustomerUpdateBatchConfig {

    @Bean
    public Job customerUpdateJob(JobRepository jobRepository,
                                  Step processUpdatesStep) {
        return new JobBuilder("customerUpdateJob", jobRepository)
            .start(processUpdatesStep)
            .build();
    }

    @Bean
    public Step processUpdatesStep(JobRepository jobRepository,
                                    PlatformTransactionManager txManager) {
        return new StepBuilder("processUpdates", jobRepository)
            .<CustomerUpdate, Customer>chunk(100, txManager)
            .reader(updateReader())
            .processor(updateProcessor())
            .writer(masterWriter())
            .build();
    }
}
```

---

## Step-by-Step Workflow

### Step 1: Categorize JCL Jobs
Use Copilot Chat to categorize all 15,250 JCL jobs:
- **Online triggers** → GitHub Actions `workflow_dispatch`
- **Scheduled batch** → GitHub Actions `schedule` (cron)
- **Data processing** → Spring Batch
- **File transfers** → GitHub Actions + cloud storage
- **Reports** → Spring Batch + reporting service

### Step 2: Map Job Dependencies
- Convert mainframe scheduler (CA7/TWS) dependencies to workflow triggers
- Use `workflow_run` event for cross-workflow dependencies

### Step 3: Convert PROCs to Composite Actions
- Reusable JCL procedures → GitHub composite actions
- Store in `.github/actions/` directory

### Step 4: Convert Job Steps
- Use `jcl-migrator` agent for each job
- Generate GitHub Actions YAML or Spring Batch config

### Step 5: Add Monitoring
- Add notification steps (Slack/Teams) for failures
- Add job duration tracking
- Set SLA-based timeouts

---

## Tips
- **Don't convert 1:1**: Some JCL jobs exist only because of mainframe limitations — consolidate where possible
- **Use `workflow_call`**: For JCL PROCs that are called from multiple jobs
- **Parameterize**: Convert JCL symbolic parameters to GitHub Actions inputs
- **Add idempotency**: Mainframe batch jobs often assume single execution — add duplicate detection
