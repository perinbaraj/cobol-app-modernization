# Phase 1: Discovery & Codebase Analysis

## Objective
Build a complete inventory of the the client mainframe codebase, understand business logic, map dependencies, and create a prioritized migration backlog — all accelerated by GitHub Copilot.

---

## Inputs
| Input | Description | Volume |
|-------|-------------|--------|
| COBOL source files | Application programs (.cbl, .cob) | 937 programs |
| JCL definitions | Job control language files | 15,250 jobs |
| Copybooks | Shared data structures (.cpy) | TBD (inventory needed) |
| BMS maps | Screen definitions for CICS | TBD |
| VSAM catalog | File definitions | TBD |
| DB2 DDL | Database table definitions | TBD |

## Outputs
| Output | Format | Used By |
|--------|--------|---------|
| Program Inventory | Spreadsheet/JSON | All phases |
| Dependency Map | Mermaid diagram + JSON | Phase 2, 3 |
| Business Logic Docs | Markdown per program | Phase 2 |
| Complexity Scores | CSV with metrics | Phase prioritization |
| Migration Priority Matrix | Ranked list | Project management |

---

## GitHub Copilot Features Used

### 1. `cobol-analyzer` Custom Agent
The primary tool for Phase 1. This agent is configured to:
- Parse COBOL program structure (IDENTIFICATION, ENVIRONMENT, DATA, PROCEDURE divisions)
- Identify CALL statements (program dependencies)
- Identify COPY statements (copybook dependencies)
- Extract embedded SQL (DB2 queries)
- Summarize business logic in plain English

**How to invoke:**
```
@cobol-analyzer Analyze the COBOL program CUSTMGMT.cbl and produce:
1. A structural summary (divisions, sections, paragraphs)
2. A list of all CALL dependencies
3. A list of all COPY dependencies
4. Embedded SQL statements
5. A plain-English summary of business logic
```

### 2. `cobol-parser` Custom Skill
Loaded automatically when working with `.cbl` files. Provides:
- Division/section/paragraph extraction
- Data item catalog (WORKING-STORAGE, LINKAGE SECTION)
- PERFORM graph (which paragraphs call which)

### 3. Copilot Chat (Batch Analysis)
Use chat for interactive exploration:
```
Explain the business purpose of this COBOL program.
Focus on: What data does it read? What does it compute? What does it write?
```

### 4. Copilot CLI (Bulk Processing)
For batch analysis across hundreds of files:
```bash
# Use Copilot CLI to summarize all programs in a directory
for file in src/cobol/*.cbl; do
  gh copilot explain "Summarize the business logic in $file"
done
```

### 5. MCP Server (Mainframe Context)
Connect the mainframe metadata server to give Copilot context about:
- Which programs are called by which JCL jobs
- Which copybooks belong to which programs
- VSAM file assignments per program

---

## Step-by-Step Workflow

### Step 1: Bulk Inventory Scan
1. Upload all COBOL sources to a GitHub repository
2. Use `cobol-analyzer` agent to scan each program
3. Collect: program name, LOC, division count, CALL targets, COPY targets

### Step 2: Dependency Mapping
1. Build a CALL graph across all 937 programs
2. Build a COPY graph for all copybooks
3. Identify clusters of tightly-coupled programs (migration units)
4. Generate Mermaid diagrams for each cluster

### Step 3: Business Logic Documentation
1. For each program, use Copilot Chat to generate a plain-English summary
2. Tag programs by business domain (e.g., customer management, inventory, pricing)
3. Document data flows (input files → processing → output files)

### Step 4: Complexity Assessment
Score each program on:
| Metric | Low (1) | Medium (2) | High (3) |
|--------|---------|------------|----------|
| Lines of Code | < 500 | 500–2000 | > 2000 |
| CALL depth | 0–1 | 2–5 | > 5 |
| Embedded SQL | None | SELECT only | INSERT/UPDATE/DELETE |
| CICS transactions | None | 1–3 | > 3 |
| File I/O operations | 1–2 | 3–5 | > 5 |

### Step 5: Prioritize Migration Backlog
Use the complexity score + business value to create a priority matrix:

```
High Value + Low Complexity  → Migrate FIRST (quick wins)
High Value + High Complexity → Migrate SECOND (high impact)
Low Value  + Low Complexity  → Migrate THIRD (easy batch)
Low Value  + High Complexity → Migrate LAST (or retire)
```

---

## Tips
- **Start with a pilot cluster**: Pick 5–10 related programs to validate the entire pipeline
- **Use Copilot's `@workspace` context**: Add all COBOL files to workspace for cross-file analysis
- **Generate machine-readable output**: Ask Copilot to produce JSON inventories for downstream tooling
- **Tag everything**: Use GitHub Issues with labels for each program to track migration status
