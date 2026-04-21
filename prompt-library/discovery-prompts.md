# Prompt Library: Phase 1 — Discovery & Analysis

> Production-ready prompts for GitHub Copilot Chat, Coding Agent, and CLI.
> Copy-paste into your Copilot session with the COBOL source file open.

---

## Prompt 1: Program Structure Analysis

```
Analyze this COBOL program and provide a structured summary:

1. **IDENTIFICATION DIVISION**: Program name, author, date
2. **ENVIRONMENT DIVISION**: File assignments, special names
3. **DATA DIVISION**:
   - FILE SECTION: All file definitions with record layouts
   - WORKING-STORAGE: All variables with PIC clauses and initial values
   - LINKAGE SECTION: Parameters passed from calling programs
4. **PROCEDURE DIVISION**:
   - Main entry point logic
   - List of all paragraphs/sections with a one-line summary each
   - PERFORM hierarchy (which paragraphs call which)

Output format: Markdown with tables for data items and a bullet list for paragraphs.
```

---

## Prompt 2: Dependency Extraction

```
Extract all dependencies from this COBOL program:

1. **CALL dependencies**: List every CALL statement with:
   - Called program name
   - Parameters passed (USING clause)
   - Whether it's a static or dynamic call

2. **COPY dependencies**: List every COPY statement with:
   - Copybook name
   - REPLACING clause (if any)
   - Which division it appears in

3. **File dependencies**: List every file with:
   - SELECT/ASSIGN clause
   - Organization (sequential, indexed, relative)
   - Access mode
   - Record key (if VSAM)

4. **SQL dependencies**: List every embedded SQL statement with:
   - Table names referenced
   - Operation type (SELECT/INSERT/UPDATE/DELETE)
   - Host variables used

Output format: JSON with arrays for each dependency type.
```

---

## Prompt 3: Business Logic Documentation

```
Read this COBOL program and explain its business purpose in plain English.

Structure your explanation as:
1. **Purpose**: What does this program do? (one paragraph)
2. **Inputs**: What data does it receive? (files, parameters, user input)
3. **Processing**: What business rules does it apply? (step by step)
4. **Outputs**: What does it produce? (files, reports, screen updates, database changes)
5. **Error handling**: How does it handle errors? (status codes, ABEND conditions)
6. **Business domain**: Which business area does this belong to? (e.g., customer management, inventory, pricing, billing)

Write for an audience of Java developers who do not know COBOL.
Do not include COBOL syntax in the explanation — translate everything to business terms.
```

---

## Prompt 4: Complexity Scoring

```
Assess the migration complexity of this COBOL program on a scale of 1-5:

Score each dimension:
| Dimension | Score (1-5) | Reasoning |
|-----------|-------------|-----------|
| Lines of Code | | (1: <500, 2: 500-1000, 3: 1000-2000, 4: 2000-5000, 5: >5000) |
| CALL Depth | | (1: none, 2: 1-2, 3: 3-5, 4: 6-10, 5: >10) |
| Embedded SQL | | (1: none, 2: simple SELECT, 3: joins, 4: cursors, 5: dynamic SQL) |
| CICS Transactions | | (1: none, 2: simple, 3: multi-screen, 4: with BMS, 5: conversational) |
| File I/O | | (1: 1-2, 2: 3-5, 3: 6-10, 4: VSAM complex, 5: multi-format) |
| Data Complexity | | (1: simple, 2: OCCURS, 3: REDEFINES, 4: nested, 5: variable length) |
| Business Logic | | (1: linear, 2: branching, 3: loops, 4: complex rules, 5: state machine) |

**Overall Score**: Average of all dimensions, rounded up.
**Migration Estimate Category**: Low (1-2), Medium (3), High (4-5)
**Recommended Approach**: (Agent-automated / Agent-assisted / Manual)
```

---

## Prompt 5: Batch Inventory Generation (CLI)

```bash
# Use with Copilot CLI for bulk processing
# Run in the directory containing COBOL source files

gh copilot explain "List all COBOL programs in this directory.
For each .cbl file, extract:
- Program ID (from IDENTIFICATION DIVISION)
- Number of lines
- Number of CALL statements
- Number of COPY statements
- Number of SQL statements
Output as a CSV with headers: filename,program_id,loc,calls,copies,sql_stmts"
```

---

## Prompt 6: Migration Unit Clustering

```
Given these program dependencies (CALL graph), group programs into migration units:

[Paste your CALL graph JSON here]

Rules for clustering:
1. Programs that CALL each other should be in the same cluster
2. Programs sharing the same copybooks should be in the same cluster
3. Each cluster should be independently deployable
4. Target cluster size: 5-15 programs
5. Identify "bridge" programs that connect clusters

Output:
- List of clusters with program names
- A Mermaid diagram showing clusters and their connections
- Recommended migration order (which cluster first?)
```
