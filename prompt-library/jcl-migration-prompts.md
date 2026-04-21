# Prompt Library: Phase 3 — JCL Migration

> Production-ready prompts for converting JCL jobs to GitHub Actions workflows and Spring Batch.

---

## Prompt 1: JCL Job Analysis

```
Analyze this JCL job and provide a structured breakdown:

1. **Job Header**: Name, class, message class, accounting
2. **Steps** (in execution order):
   | Step | Program | Input DDs | Output DDs | Condition | Description |
   |------|---------|-----------|------------|-----------|-------------|
3. **Data flow**: Which steps produce data consumed by later steps?
4. **Error handling**: What condition codes trigger step skipping or abends?
5. **Scheduling**: Any clues about when/how often this job runs?
6. **Dependencies**: Does this job reference other JCL procedures?

Based on this analysis, recommend:
- Should this become a GitHub Actions workflow, a Spring Batch job, or both?
- Can any steps be parallelized in the modern version?
- Are there steps that can be eliminated (mainframe-only concerns)?
```

---

## Prompt 2: JCL to GitHub Actions Workflow

```
Convert this JCL job to a GitHub Actions workflow.

Mapping rules:
| JCL | GitHub Actions |
|-----|---------------|
| JOB statement | workflow name + trigger |
| EXEC PGM= | step calling Java service REST API |
| EXEC PROC= | uses: ./.github/actions/[proc-name] |
| DD DSN= (input) | env variable or artifact download |
| DD DSN= (output) | artifact upload |
| DD SYSOUT=* | step output (stdout) |
| COND=(0,NE) | if: steps.prev.outcome == 'success' |
| COND=(4,LT) | if: steps.prev.outputs.rc < 4 |
| TIME= | timeout-minutes: |
| CLASS= | runs-on: |
| RESTART= | continue-on-error + retry |
| JCL SORT | Sort utility step or Java stream sort |

Requirements:
- Add workflow_dispatch trigger for manual runs
- Add schedule trigger if this is a recurring job
- Add error notification step (GitHub Issues or Slack)
- Add job summary output for monitoring
- Use GitHub secrets for any credentials
```

---

## Prompt 3: JCL PROC to GitHub Composite Action

```
Convert this JCL procedure (PROC) to a GitHub Actions composite action.

The PROC is called from multiple JCL jobs, so the action must be reusable.

Generate:
1. `action.yml` with:
   - inputs: Map PROC symbolic parameters to action inputs
   - outputs: Map PROC output datasets to action outputs
   - steps: Convert each EXEC step
2. Usage example showing how to call from a workflow
3. Documentation of what the PROC does

Place the action in: .github/actions/[proc-name]/action.yml
```

---

## Prompt 4: JCL to Spring Batch

```
Convert this JCL batch processing job to a Spring Batch configuration.

Mapping rules:
| JCL Concept | Spring Batch |
|-------------|-------------|
| JOB | @Bean Job |
| STEP with PGM | @Bean Step |
| DD input file | FlatFileItemReader or JdbcCursorItemReader |
| DD output file | FlatFileItemWriter or JdbcBatchItemWriter |
| SORT step | SortingItemReader or pre-sort query |
| COND code | StepExecutionListener + FlowBuilder |
| RESTART | JobRepository (built-in restart) |
| STEP limit | chunk size in StepBuilder |

Generate:
1. @Configuration class with Job and Step beans
2. ItemReader, ItemProcessor, ItemWriter for each step
3. DTO classes for input/output records
4. Step listeners for logging and error handling
5. Job parameters mapping from JCL symbolic parameters
6. Retry and skip policies for error resilience
```

---

## Prompt 5: Job Scheduler Conversion

```
We need to convert the mainframe job scheduler (CA7/TWS) to GitHub Actions scheduling.

Here is the job dependency chain:
[Paste job names and their dependencies]

Generate:
1. A cron schedule for each independent job
2. workflow_run triggers for dependent jobs
3. A dependency diagram (Mermaid) showing the execution order
4. Recommended time windows for each job based on the original schedule

Handle these patterns:
- Jobs that must run in sequence → workflow_run trigger chain
- Jobs that can run in parallel → concurrent workflows
- Jobs with calendar-based schedules → cron expressions
- Jobs triggered by file arrival → repository_dispatch with webhook
```

---

## Prompt 6: IDCAMS/Utility Conversion

```
Convert these mainframe utility steps to modern equivalents:

| Mainframe Utility | Modern Equivalent | Generate |
|-------------------|-------------------|----------|
| IDCAMS DEFINE CLUSTER | Flyway migration script | CREATE TABLE DDL |
| IDCAMS DELETE | Flyway rollback | DROP TABLE DDL |
| IDCAMS REPRO | Spring Batch file copy | Reader → Writer job |
| IEBGENER | File copy step | GitHub Actions artifact |
| IEBCOPY | Library copy | Git operations |
| SORT (DFSORT/SYNCSORT) | Java stream sort / SQL ORDER BY | Sort utility class |
| IEFBR14 | No-op (remove) | Comment only |
| IKJEFT01 (TSO) | Shell script step | GitHub Actions run step |

For each utility step in this JCL, generate the modern equivalent.
```
