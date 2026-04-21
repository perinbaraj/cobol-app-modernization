# Quick Start Guide — Your First 30 Minutes

> **Read this first.** This guide walks you through setting up and validating the entire Copilot-powered migration toolkit in 30 minutes using a sample COBOL module included in this repo.

---

## Prerequisites Checklist

Before you start, make sure you have:

- [ ] GitHub Copilot Business or Enterprise license
- [ ] VS Code with GitHub Copilot extension (latest)
- [ ] Node.js 18+ (`node --version`)
- [ ] Python 3.10+ (`python --version`)
- [ ] Java 17 SDK (`java --version`)
- [ ] Access to the COBOL source repository

---

## Minute 0–5: Configure Copilot for the Project

### 1. Copy Copilot instructions into your migration repo
```bash
# In your migration repository root
mkdir -p .github
cp copilot-config/copilot-instructions.md .github/copilot-instructions.md
```
> This makes Copilot follow COBOL-to-Java conversion rules in every chat session.

### 2. Copy custom agents
```bash
mkdir -p .github/agents
# Copy agent instructions (use the content from custom-agents/*.md)
# Each file documents the agent config to place in .github/agents/
```

---

## Minute 5–15: Set Up the MCP Server

### 1. Install the MCP server
```bash
cd mcp-servers/mainframe-context
npm install
```

### 2. Run the scanner on your COBOL codebase
```bash
python scripts/scan-codebase.py \
  --cobol-dir /path/to/your/cobol/source \
  --jcl-dir /path/to/your/jcl/source \
  --output-dir data/
```

> **Don't have access to the real codebase yet?** Use the included samples:
> ```bash
> python scripts/scan-codebase.py \
>   --cobol-dir samples/cobol \
>   --jcl-dir samples/jcl \
>   --output-dir data/
> ```

### 3. Configure VS Code to use the MCP server
Create `.vscode/mcp.json` in your project:
```json
{
  "servers": {
    "mainframe-context": {
      "type": "stdio",
      "command": "node",
      "args": ["./mcp-servers/mainframe-context/index.js"]
    }
  }
}
```

### 4. Restart VS Code
Press `Ctrl+Shift+P` → "Developer: Reload Window"

---

## Minute 15–25: Test with the Sample Module

The `samples/` folder contains a sanitized COBOL module you can use to validate the entire pipeline.

### 1. Open the sample COBOL program
Open `samples/cobol/CUSTMGMT.cbl` in VS Code.

### 2. Try the Discovery prompts
Open Copilot Chat and paste from `prompt-library/discovery-prompts.md`:
```
Analyze this COBOL program and provide a structured summary:
1. Divisions and sections
2. All CALL dependencies
3. All COPY dependencies
4. A plain-English summary of business logic
```

### 3. Try the Conversion prompts
Paste from `prompt-library/cobol-to-java-prompts.md`:
```
Convert this COBOL program to a Java 17 Spring Boot service.
Use BigDecimal for all decimal fields. Use Java records for DTOs.
```

### 4. Verify MCP context
Ask Copilot:
```
What programs are related to CUSTMGMT? Use the mainframe context.
```

---

## Minute 25–30: Plan Your Migration

### 1. Review the architecture diagram
Open `architecture-diagram.md` and press `Ctrl+Shift+V` to see the Mermaid diagrams.

### 2. Pick your starting phase
| Your Situation | Start With |
|---------------|------------|
| Haven't inventoried the codebase yet | Phase 1: Discovery |
| Inventory done, ready to convert | Phase 2: COBOL → Java |
| Java services ready, need UI | Phase 4: UI Modernization |
| Everything converted, need to test | Phase 6: Testing |

### 3. Grab the relevant prompts
Each phase has a matching prompt library file:
| Phase | Prompt File |
|-------|------------|
| Phase 1: Discovery | `prompt-library/discovery-prompts.md` |
| Phase 2: COBOL → Java | `prompt-library/cobol-to-java-prompts.md` |
| Phase 3: JCL Migration | `prompt-library/jcl-migration-prompts.md` |
| Phase 4: UI Modernization | `prompt-library/react-ui-prompts.md` |
| Phase 5: Data Migration | `prompt-library/data-migration-prompts.md` |
| Phase 6: Testing | `prompt-library/testing-prompts.md` |

---

## What's Next

After validating the toolkit with the sample module:

1. **Run the scanner** on the full codebase (`scripts/scan-codebase.py`)
2. **Classify programs** by business domain (update `data/program-inventory.json`)
3. **Start Phase 1 Discovery** on the first cluster of programs
4. **Set up Coding Agent** (see `copilot-config/copilot-setup-steps.md`) for batch conversion

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

> **Legend:** ✅ = Fully supported | — = Not applicable
