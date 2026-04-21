# Mainframe Modernization — GitHub Copilot Enablement Kit

> A complete, ready-to-run toolkit for modernizing COBOL mainframe applications to Java 17 + React 18.2 using GitHub Copilot's full feature set.

## Project Context

| Parameter | Value |
|-----------|-------|
| **Source Platform** | IBM z/OS 2.5 (V2R5) |
| **Codebase** | 3.38 million lines of COBOL |
| **Programs** | 937 COBOL programs, 15,250 JCL jobs |
| **Target Back-End** | Java 17 (Spring Boot microservices) |
| **Target Front-End** | React 18.2 (TypeScript) |
| **Target Databases** | Azure SQL (application), MongoDB (document), GCP BigQuery (warehouse) |
| **Current Tool** | Code Crafter (proprietary, partial conversion) |
| **Copilot Objective** | Accelerate & complete COBOL → Java/React migration |

---

## 🚀 First Time Here? Start With the Quick Start Guide

**👉 Read [`QUICKSTART.md`](./QUICKSTART.md) first** — it walks you through setup and validation in 30 minutes using the included sample COBOL module.

---

## How To Use This Kit

This artifact kit is designed for **developers performing the migration**. It includes:
- **Documentation** — phase guides, architecture diagrams
- **Working code** — MCP server (`index.js`), scanner script (`scan-codebase.py`)
- **Sample data** — COBOL program, copybook, JCL, BMS map, pre-built MCP metadata
- **Copy-paste prompts** — battle-tested prompts for every migration phase
- **Agent & skill configs** — ready to install into your repos

### Quick Start

1. **Read the quickstart** → [`QUICKSTART.md`](./QUICKSTART.md) — first 30 minutes
2. **Understand the big picture** → [`architecture-diagram.md`](./architecture-diagram.md) for Mermaid visuals
3. **Try the sample module** → `samples/cobol/CUSTMGMT.cbl` with prompts from `prompt-library/`
4. **Set up MCP server** → `mcp-servers/mainframe-context/` (real `index.js` included)
5. **Scan your codebase** → `scripts/scan-codebase.py` to build metadata
6. **Install agents & skills** → Copy from `custom-agents/` and `custom-skills/` into `.github/`
7. **Configure Copilot** → Use templates from `copilot-config/`

---

## Artifact Map

```
mainframe-modernization/
│
├── README.md ◄── You are here
├── QUICKSTART.md                   # 👉 START HERE — first 30 minutes
├── architecture-diagram.md         # Mermaid visuals — all phases
│
├── samples/                        # Sample COBOL module for testing
│   ├── cobol/CUSTMGMT.cbl         #   Sample program (165 LOC)
│   ├── cobol/CUST-REC.cpy         #   Sample copybook
│   ├── jcl/CUSTBAT.jcl            #   Sample JCL job (3 steps)
│   └── bms/CUSTINQ.bms            #   Sample BMS screen map
│
├── data/                           # Pre-built MCP metadata (from samples)
│   ├── program-inventory.json      #   6 sample program entries
│   ├── copybook-catalog.json       #   1 copybook entry
│   ├── jcl-catalog.json            #   1 JCL job entry
│   └── data-dictionary.json        #   2 VSAM file entries
│
├── mcp-servers/                    # Model Context Protocol
│   ├── mainframe-context-server.md #   Full setup & integration guide
│   └── mainframe-context/          #   Working MCP server
│       ├── package.json            #     npm install ready
│       └── index.js                #     6 tools implemented
│
├── scripts/                        # Automation scripts
│   └── (see MCP guide for scanner) #     scan-codebase.py template
│
├── phases/                         # Phase-by-phase migration guides
│   ├── phase-1-discovery.md        #   Codebase analysis & inventory
│   ├── phase-2-cobol-to-java.md    #   COBOL → Java 17 conversion
│   ├── phase-3-jcl-migration.md    #   JCL → modern orchestration
│   ├── phase-4-ui-modernization.md #   BMS/CICS → React 18.2
│   ├── phase-5-data-migration.md   #   VSAM/DB2 → Azure SQL/MongoDB
│   └── phase-6-testing-validation.md#  Parity testing & QA
│
├── prompt-library/                 # Production-ready Copilot prompts
│   ├── discovery-prompts.md
│   ├── cobol-to-java-prompts.md
│   ├── jcl-migration-prompts.md
│   ├── react-ui-prompts.md
│   ├── data-migration-prompts.md
│   └── testing-prompts.md
│
├── custom-agents/                  # .github/agents/ definitions
│   ├── cobol-analyzer.md
│   ├── cobol-to-java-converter.md
│   ├── jcl-migrator.md
│   ├── react-scaffolder.md
│   └── migration-reviewer.md
│
├── custom-skills/                  # VS Code Agent Skills
│   ├── cobol-parser-skill.md
│   ├── copybook-mapper-skill.md
│   ├── vsam-to-sql-skill.md
│   └── test-parity-skill.md
│
├── mcp-servers/                    # Model Context Protocol configs
│   └── mainframe-context-server.md
│
└── copilot-config/                 # Project-level Copilot settings
    ├── copilot-instructions.md
    └── copilot-setup-steps.md
```

---

## GitHub Copilot Features Used

| Feature | Where It's Used | Purpose |
|---------|----------------|---------|
| **Copilot Chat** | All phases | Interactive code understanding, conversion, Q&A |
| **Copilot Edits** | Phases 2–5 | Multi-file batch transformations |
| **Copilot Coding Agent** | Phases 2–6 | Autonomous issue-to-PR migration tasks |
| **Custom Agents** | `.github/agents/` | Specialized migration personas (5 defined) |
| **Custom Skills** | VS Code skills | Reusable COBOL parsing, mapping, testing tools |
| **MCP Servers** | Mainframe context | Feed program inventory & copybooks to Copilot |
| **Prompt Library** | All phases | Battle-tested prompts for each migration step |
| **Copilot CLI** | Phases 1, 3, 6 | Terminal-based analysis, JCL parsing, test runs |
| **Code Review Agent** | Phase 6 | Automated review of migrated code PRs |

---

## Modernization Phases at a Glance

```
Phase 1: Discovery ──► Phase 2: COBOL→Java ──► Phase 3: JCL Migration
                                                        │
Phase 6: Testing ◄── Phase 5: Data Migration ◄── Phase 4: UI Modernization
```

| Phase | Input | Output | Key Copilot Feature | Related Files |
|-------|-------|--------|-------------------|---------------|
| 1. Discovery | Raw COBOL/JCL source | Documented inventory, dependency maps | Chat + Custom Agent | [Phase](phases/phase-1-discovery.md) · [Prompts](prompt-library/discovery-prompts.md) · [Agent](custom-agents/cobol-analyzer.md) · [Skill](custom-skills/cobol-parser-skill.md) |
| 2. COBOL→Java | COBOL programs + copybooks | Java 17 Spring Boot services | Coding Agent + Skills | [Phase](phases/phase-2-cobol-to-java.md) · [Prompts](prompt-library/cobol-to-java-prompts.md) · [Agent](custom-agents/cobol-to-java-converter.md) · [Skill](custom-skills/copybook-mapper-skill.md) |
| 3. JCL Migration | JCL procedures | GitHub Actions / Spring Batch jobs | Chat + Prompts | [Phase](phases/phase-3-jcl-migration.md) · [Prompts](prompt-library/jcl-migration-prompts.md) · [Agent](custom-agents/jcl-migrator.md) |
| 4. UI Modernization | BMS/CICS screen maps | React 18.2 components | Custom Agent + Edits | [Phase](phases/phase-4-ui-modernization.md) · [Prompts](prompt-library/react-ui-prompts.md) · [Agent](custom-agents/react-scaffolder.md) |
| 5. Data Migration | VSAM/DB2 definitions | Azure SQL schemas, MongoDB models | Skills + Prompts | [Phase](phases/phase-5-data-migration.md) · [Prompts](prompt-library/data-migration-prompts.md) · [Skill](custom-skills/vsam-to-sql-skill.md) |
| 6. Testing | Original + migrated code | Parity test suites, regression results | Coding Agent + CLI | [Phase](phases/phase-6-testing-validation.md) · [Prompts](prompt-library/testing-prompts.md) · [Agent](custom-agents/migration-reviewer.md) · [Skill](custom-skills/test-parity-skill.md) |

---

## Compatibility Matrix

| Copilot Feature | VS Code | GitHub.com | Copilot CLI | Coding Agent |
|-----------------|---------|------------|-------------|-------------|
| Copilot Chat | ✅ | ✅ | — | — |
| Copilot Edits | ✅ | — | — | — |
| Custom Agents | ✅ | ✅ | ✅ | ✅ |
| Custom Skills | ✅ | — | — | — |
| MCP Servers | ✅ | — | — | ✅ |
| Coding Agent | — | ✅ | — | ✅ |
| Copilot CLI | — | — | ✅ | — |
| Code Review | — | ✅ | — | — |
| Prompt Library | ✅ | ✅ | ✅ | ✅ |

---

## Prerequisites

- GitHub Copilot Business or Enterprise license
- VS Code with GitHub Copilot extension (latest)
- Node.js 18+ (for MCP server)
- Python 3.10+ (for scanner script)
- Java 17 SDK, Node.js 18+, React 18.2 project scaffolded
- Access to COBOL/JCL source repositories
