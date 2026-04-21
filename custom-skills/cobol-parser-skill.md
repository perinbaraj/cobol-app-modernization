# Custom Skill: COBOL Parser

> A VS Code Agent Skill that parses COBOL program structures for use by Copilot agents and chat.

---

## Skill Definition

Create this skill in your project as `.github/skills/cobol-parser/SKILL.md`:

### `.github/skills/cobol-parser/SKILL.md`

```markdown
---
name: cobol-parser
description: Parses COBOL programs into structured representations — divisions, sections, paragraphs, data items, and dependencies. Used by agents for program understanding.
---

# COBOL Parser Skill

## When to Use This Skill
Activate this skill when:
- Working with `.cbl`, `.cob`, or `.cpy` files
- Need to understand COBOL program structure
- Need to extract data item definitions
- Need to map PERFORM hierarchy
- Need to identify dependencies (CALL, COPY, SQL)

## Parsing Rules

### Division Detection
```
Pattern: /^       (IDENTIFICATION|ENVIRONMENT|DATA|PROCEDURE) DIVISION/
```
Every COBOL program has up to 4 divisions in order.

### Section Detection
```
Pattern: /^       [\w-]+ SECTION\./
```
Sections appear within divisions.

### Paragraph Detection (PROCEDURE DIVISION)
```
Pattern: /^       [\w-]+\./  (not a reserved word, not inside DATA DIVISION)
```

### Data Item Extraction
```
Level numbers: 01, 02, 03, ..., 49, 66, 77, 88
Pattern: /^\s+(01|02|...|88)\s+([\w-]+)\s+(PIC|PICTURE|USAGE|OCCURS|REDEFINES|VALUE)/

Extract:
- Level number → hierarchy depth
- Name → field name
- PIC clause → data type and size
- USAGE → storage format (COMP, COMP-3, etc.)
- OCCURS → array with count
- REDEFINES → overlay definition
- VALUE → initial value
- 88-level → condition name
```

### Dependency Extraction
```
CALL: /CALL\s+['"]?([\w-]+)['"]?\s+(USING\s+(.+))?/
COPY: /COPY\s+([\w-]+)(\s+REPLACING\s+(.+))?/
SQL:  /EXEC\s+SQL\s+(SELECT|INSERT|UPDATE|DELETE|DECLARE)/
CICS: /EXEC\s+CICS\s+(SEND|RECEIVE|READ|WRITE|XCTL|LINK|RETURN)/
```

### PERFORM Hierarchy
```
PERFORM: /PERFORM\s+([\w-]+)(\s+THRU\s+[\w-]+)?/
PERFORM VARYING: /PERFORM\s+([\w-]+)\s+VARYING\s+(.+)/
PERFORM UNTIL: /PERFORM\s+([\w-]+)\s+UNTIL\s+(.+)/
```

## Output Format

When this skill parses a COBOL file, it produces:

```json
{
  "programId": "CUSTMGMT",
  "divisions": {
    "identification": {
      "programId": "CUSTMGMT",
      "author": "the implementation partner",
      "dateWritten": "2020-01-15"
    },
    "environment": {
      "fileAssignments": [
        {"name": "CUSTOMER-FILE", "dsname": "PROD.CUST.MASTER", "org": "INDEXED"}
      ]
    },
    "data": {
      "fileSection": [...],
      "workingStorage": [...],
      "linkageSection": [...]
    },
    "procedure": {
      "paragraphs": [...],
      "performGraph": {...}
    }
  },
  "dependencies": {
    "calls": [...],
    "copybooks": [...],
    "sqlStatements": [...],
    "cicsCommands": [...]
  },
  "metrics": {
    "totalLines": 1250,
    "codeLines": 980,
    "commentLines": 150,
    "blankLines": 120,
    "paragraphCount": 25,
    "dataItemCount": 85,
    "complexityScore": 3
  }
}
```

## Integration with Agents
This skill is automatically available to:
- `cobol-analyzer` agent (uses for inventory generation)
- `cobol-to-java-converter` agent (uses for structure understanding)
- `migration-reviewer` agent (uses for parity checking)
```

---

## How to Install

1. Create the directory: `.github/skills/cobol-parser/`
2. Add the `SKILL.md` file above
3. The skill auto-loads when Copilot encounters COBOL files

## How to Invoke

The skill is invoked implicitly by agents, or explicitly:
```
Use the cobol-parser skill to extract the structure of CUSTMGMT.cbl
```
