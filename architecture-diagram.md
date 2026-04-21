# Architecture Diagrams — GitHub Copilot in Mainframe Modernization

> All diagrams use [Mermaid](https://mermaid.js.org/) syntax. Render in VS Code (Mermaid extension), GitHub markdown preview, or any Mermaid-compatible viewer.

---

## Master Architecture: End-to-End Copilot-Powered Migration

This diagram shows the complete modernization pipeline with every GitHub Copilot touchpoint.

```mermaid
graph TB
    subgraph SOURCE["🖥️ SOURCE: IBM Mainframe z/OS 2.5"]
        COBOL["937 COBOL Programs<br/>3.38M Lines of Code"]
        JCL["15,250 JCL Jobs"]
        BMS["BMS/CICS Screens"]
        VSAM["VSAM Files & DB2"]
        COPYBOOK["COBOL Copybooks"]
    end

    subgraph COPILOT["🤖 GITHUB COPILOT ECOSYSTEM"]
        direction TB
        subgraph AGENTS["Custom Agents"]
            A1["🔍 cobol-analyzer"]
            A2["🔄 cobol-to-java-converter"]
            A3["📋 jcl-migrator"]
            A4["🎨 react-scaffolder"]
            A5["✅ migration-reviewer"]
        end
        subgraph SKILLS["Custom Skills"]
            S1["📖 cobol-parser"]
            S2["🗺️ copybook-mapper"]
            S3["💾 vsam-to-sql"]
            S4["🧪 test-parity"]
        end
        subgraph TOOLS["Copilot Tools"]
            T1["💬 Copilot Chat"]
            T2["✏️ Copilot Edits"]
            T3["🤖 Coding Agent"]
            T4["📟 Copilot CLI"]
            T5["📝 Code Review"]
        end
        subgraph MCP["MCP Server"]
            M1["📊 Mainframe<br/>Context Server"]
        end
        PL["📚 Prompt Library"]
    end

    subgraph TARGET["🎯 TARGET: Modern Stack"]
        JAVA["Java 17<br/>Spring Boot Services"]
        REACT["React 18.2<br/>TypeScript Components"]
        SQL["Azure SQL<br/>Application DB"]
        MONGO["MongoDB<br/>Document Store"]
        GHA["GitHub Actions<br/>CI/CD Pipelines"]
        BATCH["Spring Batch<br/>Job Scheduler"]
    end

    COBOL -->|Phase 1: Analyze| A1
    COPYBOOK -->|Feed context| M1
    COBOL -->|Phase 2: Convert| A2
    JCL -->|Phase 3: Migrate| A3
    BMS -->|Phase 4: Modernize| A4
    VSAM -->|Phase 5: Transform| S3

    A1 -->|Inventory & docs| JAVA
    A2 -->|Converted code| JAVA
    A3 -->|Pipeline configs| GHA
    A3 -->|Batch jobs| BATCH
    A4 -->|UI components| REACT
    S3 -->|Schemas| SQL
    S3 -->|Models| MONGO

    M1 -.->|Context| A1
    M1 -.->|Context| A2
    M1 -.->|Context| A3
    PL -.->|Prompts| T1
    PL -.->|Prompts| T3

    S1 -.->|Parse| A1
    S2 -.->|Map| A2
    S4 -.->|Test| A5

    A5 -->|Review PRs| T5

    style SOURCE fill:#ff6b6b,stroke:#c0392b,color:#fff
    style COPILOT fill:#3498db,stroke:#2980b9,color:#fff
    style TARGET fill:#2ecc71,stroke:#27ae60,color:#fff
    style AGENTS fill:#9b59b6,stroke:#8e44ad,color:#fff
    style SKILLS fill:#e67e22,stroke:#d35400,color:#fff
    style TOOLS fill:#1abc9c,stroke:#16a085,color:#fff
    style MCP fill:#f39c12,stroke:#e67e22,color:#fff
```

---

## Phase Flow: How Phases Connect

```mermaid
flowchart LR
    P1["Phase 1<br/>🔍 Discovery"]
    P2["Phase 2<br/>🔄 COBOL→Java"]
    P3["Phase 3<br/>📋 JCL Migration"]
    P4["Phase 4<br/>🎨 UI Modernization"]
    P5["Phase 5<br/>💾 Data Migration"]
    P6["Phase 6<br/>🧪 Testing"]

    P1 -->|Inventory feeds| P2
    P1 -->|Dependency map feeds| P3
    P2 -->|APIs ready| P4
    P2 -->|Data access patterns| P5
    P3 -->|Pipelines ready| P6
    P4 -->|UI components| P6
    P5 -->|Schemas ready| P6

    P1 -.->|Can run in parallel| P5

    style P1 fill:#e74c3c,color:#fff
    style P2 fill:#e67e22,color:#fff
    style P3 fill:#f1c40f,color:#000
    style P4 fill:#2ecc71,color:#fff
    style P5 fill:#3498db,color:#fff
    style P6 fill:#9b59b6,color:#fff
```

---

## Phase 1: Discovery — Copilot Workflow

```mermaid
flowchart TD
    subgraph INPUT["📥 Input"]
        SRC["COBOL Source Files"]
        JCL_IN["JCL Job Definitions"]
        CPY["Copybook Files"]
    end

    subgraph COPILOT_P1["🤖 Copilot Actions"]
        CA["🔍 cobol-analyzer Agent<br/>'Analyze this COBOL program'"]
        CP["📖 cobol-parser Skill<br/>Extract divisions, sections, paragraphs"]
        CC["💬 Copilot Chat<br/>'Explain the business logic in...'"]
        CLI["📟 Copilot CLI<br/>Batch file analysis"]
    end

    subgraph OUTPUT["📤 Output"]
        INV["📊 Program Inventory<br/>(name, type, LOC, complexity)"]
        DEP["🕸️ Dependency Map<br/>(CALL graphs, COPY relations)"]
        BIZ["📝 Business Logic Docs<br/>(per-program summaries)"]
        PRI["🎯 Migration Priority Matrix<br/>(complexity × business value)"]
    end

    SRC --> CA
    JCL_IN --> CA
    CPY --> CP

    CA --> INV
    CA --> DEP
    CP --> CC
    CC --> BIZ
    INV --> PRI
    DEP --> PRI

    style INPUT fill:#ff6b6b,color:#fff
    style COPILOT_P1 fill:#3498db,color:#fff
    style OUTPUT fill:#2ecc71,color:#fff
```

---

## Phase 2: COBOL → Java Conversion — Copilot Workflow

```mermaid
flowchart TD
    subgraph INPUT["📥 Input"]
        COB["COBOL Program"]
        CPY2["Related Copybooks"]
        CTX["MCP: Program Metadata"]
    end

    subgraph STEP1["Step 1: Understand"]
        CHAT["💬 Copilot Chat<br/>'Explain this COBOL program<br/>structure and business rules'"]
    end

    subgraph STEP2["Step 2: Scaffold"]
        AGENT["🔄 cobol-to-java-converter Agent<br/>'Convert COBOL program X to<br/>Spring Boot service'"]
        SKILL["🗺️ copybook-mapper Skill<br/>Copybook → Java DTO"]
    end

    subgraph STEP3["Step 3: Refine"]
        EDIT["✏️ Copilot Edits<br/>Multi-file refinement"]
        CODING["🤖 Coding Agent<br/>Issue → PR automation"]
    end

    subgraph STEP4["Step 4: Review"]
        REV["✅ migration-reviewer Agent<br/>Check conversion fidelity"]
        CR["📝 Code Review<br/>PR review comments"]
    end

    subgraph OUTPUT2["📤 Output"]
        JAVA2["☕ Java 17 Service<br/>(Spring Boot)"]
        DTO["📦 Java DTOs<br/>(from copybooks)"]
        API["🔌 REST API<br/>(OpenAPI spec)"]
    end

    COB --> CHAT
    CPY2 --> SKILL
    CTX -.-> CHAT

    CHAT --> AGENT
    SKILL --> AGENT

    AGENT --> EDIT
    AGENT --> CODING

    EDIT --> REV
    CODING --> REV
    REV --> CR

    CR --> JAVA2
    CR --> DTO
    CR --> API

    style INPUT fill:#ff6b6b,color:#fff
    style STEP1 fill:#f39c12,color:#fff
    style STEP2 fill:#3498db,color:#fff
    style STEP3 fill:#9b59b6,color:#fff
    style STEP4 fill:#1abc9c,color:#fff
    style OUTPUT2 fill:#2ecc71,color:#fff
```

---

## Phase 3: JCL Migration — Copilot Workflow

```mermaid
flowchart TD
    subgraph INPUT3["📥 Input"]
        JCL3["JCL Job Streams"]
        PROC["JCL Procedures (PROCs)"]
    end

    subgraph COPILOT_P3["🤖 Copilot Actions"]
        JM["📋 jcl-migrator Agent<br/>'Convert this JCL to<br/>GitHub Actions workflow'"]
        CHAT3["💬 Copilot Chat<br/>'What does this JCL step do?'"]
        PROMPT3["📚 JCL Prompt Library<br/>Step-by-step conversion patterns"]
    end

    subgraph OUTPUT3["📤 Output"]
        GHA3["⚙️ GitHub Actions Workflows<br/>(.github/workflows/*.yml)"]
        SB3["📅 Spring Batch Jobs<br/>(for batch processing)"]
        DOC3["📝 Job Documentation<br/>(schedule, dependencies, SLAs)"]
    end

    JCL3 --> JM
    PROC --> CHAT3
    CHAT3 --> JM
    PROMPT3 -.-> JM

    JM --> GHA3
    JM --> SB3
    JM --> DOC3

    style INPUT3 fill:#ff6b6b,color:#fff
    style COPILOT_P3 fill:#3498db,color:#fff
    style OUTPUT3 fill:#2ecc71,color:#fff
```

---

## Phase 4: UI Modernization — Copilot Workflow

```mermaid
flowchart TD
    subgraph INPUT4["📥 Input"]
        BMS4["BMS Screen Maps"]
        CICS4["CICS Transaction Defs"]
        API4["Java REST APIs<br/>(from Phase 2)"]
    end

    subgraph COPILOT_P4["🤖 Copilot Actions"]
        RS["🎨 react-scaffolder Agent<br/>'Convert this BMS map to<br/>a React component'"]
        EDIT4["✏️ Copilot Edits<br/>Multi-component generation"]
        CHAT4["💬 Copilot Chat<br/>'Create API integration hooks'"]
    end

    subgraph OUTPUT4["📤 Output"]
        COMP["⚛️ React Components<br/>(TypeScript + Tailwind)"]
        HOOKS["🪝 Custom Hooks<br/>(API integration)"]
        ROUTES["🛤️ Route Definitions<br/>(React Router)"]
        FORMS["📋 Form Validations<br/>(Zod schemas)"]
    end

    BMS4 --> RS
    CICS4 --> RS
    API4 --> CHAT4

    RS --> COMP
    EDIT4 --> HOOKS
    CHAT4 --> HOOKS
    RS --> ROUTES
    EDIT4 --> FORMS

    style INPUT4 fill:#ff6b6b,color:#fff
    style COPILOT_P4 fill:#3498db,color:#fff
    style OUTPUT4 fill:#2ecc71,color:#fff
```

---

## Phase 5: Data Migration — Copilot Workflow

```mermaid
flowchart TD
    subgraph INPUT5["📥 Input"]
        VSAM5["VSAM File Definitions"]
        DB2["DB2 Table DDLs"]
        CPY5["Data Copybooks"]
    end

    subgraph COPILOT_P5["🤖 Copilot Actions"]
        VS["💾 vsam-to-sql Skill<br/>Schema generation"]
        CM["🗺️ copybook-mapper Skill<br/>Field mapping"]
        CHAT5["💬 Copilot Chat<br/>'Generate JPA entities for...'"]
        CODING5["🤖 Coding Agent<br/>Migration script PRs"]
    end

    subgraph OUTPUT5["📤 Output"]
        SCHEMA["🗄️ Azure SQL Schemas"]
        MGSCHEMA["🍃 MongoDB Models"]
        JPA["☕ JPA Entities"]
        MIGRATE["📜 Migration Scripts<br/>(Flyway/Liquibase)"]
    end

    VSAM5 --> VS
    DB2 --> VS
    CPY5 --> CM

    VS --> SCHEMA
    VS --> MGSCHEMA
    CM --> CHAT5
    CHAT5 --> JPA
    CODING5 --> MIGRATE

    style INPUT5 fill:#ff6b6b,color:#fff
    style COPILOT_P5 fill:#3498db,color:#fff
    style OUTPUT5 fill:#2ecc71,color:#fff
```

---

## Phase 6: Testing & Validation — Copilot Workflow

```mermaid
flowchart TD
    subgraph INPUT6["📥 Input"]
        ORIG["Original COBOL<br/>Programs"]
        CONV["Converted Java<br/>Services"]
        TESTDATA["Production Test<br/>Data Samples"]
    end

    subgraph COPILOT_P6["🤖 Copilot Actions"]
        TP["🧪 test-parity Skill<br/>Generate parity assertions"]
        CODING6["🤖 Coding Agent<br/>'Write integration tests for...'"]
        CLI6["📟 Copilot CLI<br/>Run test suites, analyze failures"]
        REV6["✅ migration-reviewer Agent<br/>Final review checklist"]
    end

    subgraph OUTPUT6["📤 Output"]
        UNIT["✅ Unit Tests<br/>(JUnit 5)"]
        INT["✅ Integration Tests<br/>(TestContainers)"]
        PARITY["✅ Parity Reports<br/>(COBOL vs Java output diff)"]
        SIGN["📋 Sign-off Checklist"]
    end

    ORIG --> TP
    CONV --> TP
    TESTDATA --> CODING6

    TP --> PARITY
    CODING6 --> UNIT
    CODING6 --> INT
    CLI6 --> PARITY
    REV6 --> SIGN

    style INPUT6 fill:#ff6b6b,color:#fff
    style COPILOT_P6 fill:#3498db,color:#fff
    style OUTPUT6 fill:#2ecc71,color:#fff
```

---

## Agent & Skill Interaction Map

Shows how custom agents invoke skills and tools:

```mermaid
flowchart LR
    subgraph AGENTS2["🤖 Custom Agents"]
        A1B["cobol-analyzer"]
        A2B["cobol-to-java-converter"]
        A3B["jcl-migrator"]
        A4B["react-scaffolder"]
        A5B["migration-reviewer"]
    end

    subgraph SKILLS2["🛠️ Custom Skills"]
        S1B["cobol-parser"]
        S2B["copybook-mapper"]
        S3B["vsam-to-sql"]
        S4B["test-parity"]
    end

    subgraph MCP2["📡 MCP Server"]
        M1B["mainframe-context"]
    end

    subgraph TOOLS2["🔧 Copilot Tools"]
        T1B["Chat"]
        T2B["Edits"]
        T3B["Coding Agent"]
        T4B["CLI"]
        T5B["Code Review"]
    end

    A1B --> S1B
    A1B --> M1B
    A2B --> S2B
    A2B --> M1B
    A2B --> T2B
    A2B --> T3B
    A3B --> T1B
    A4B --> T2B
    A5B --> S4B
    A5B --> T5B

    style AGENTS2 fill:#9b59b6,color:#fff
    style SKILLS2 fill:#e67e22,color:#fff
    style MCP2 fill:#f39c12,color:#fff
    style TOOLS2 fill:#1abc9c,color:#fff
```
