# Troubleshooting Guide

> Common issues and resolutions for the COBOL Modernization Toolkit.

## Quick Diagnostics

Before diving into specific issues, run these diagnostic checks:

```bash
# Check MCP server health
curl http://localhost:3001/health

# Validate metadata files
cd mcp-servers/mainframe-context
node validate.js --data-dir ../../data

# Check scanner dependencies
cd scripts
python -c "import json, re, logging; print('Dependencies OK')"

# Verify Node version
node --version  # Should be 20.x or higher

# Verify Python version
python --version  # Should be 3.11 or higher
```

---

## MCP Server Issues

### Issue: MCP Server Won't Start

**Symptoms:**
- `npm start` fails with errors
- Port already in use message
- Module not found errors

**Solutions:**

**1. Port Already in Use**
```bash
# Find process using port 3001
# Windows
netstat -ano | findstr :3001

# Kill the process
taskkill /PID <pid> /F

# Or change port in index.js
# const PORT = process.env.PORT || 3002;
```

**2. Missing Dependencies**
```bash
cd mcp-servers/mainframe-context
rm -rf node_modules package-lock.json
npm install
```

**3. Node Version Mismatch**
```bash
# Check version
node --version

# Use nvm to switch to correct version
nvm install 20
nvm use 20
```

---

### Issue: MCP Server Returns Empty Results

**Symptoms:**
- Tools return `{ "programs": [] }` or similar empty results
- No errors, but no data

**Solutions:**

**1. Check Data Directory Path**
```bash
# Verify data files exist
ls -la data/
# Should see:
# - program-inventory.json
# - copybook-catalog.json
# - jcl-catalog.json
# - data-dictionary.json
```

**2. Check Data File Contents**
```bash
# Verify files have content
wc -l data/*.json

# Check JSON is valid
cat data/program-inventory.json | jq . > /dev/null && echo "Valid JSON"
```

**3. Verify MCP Server Config**
```javascript
// In index.js, check data path
const DATA_DIR = path.resolve(__dirname, '../../data');
console.log('Loading data from:', DATA_DIR);
```

---

### Issue: MCP Server Tools Not Appearing in Copilot

**Symptoms:**
- VS Code doesn't show MCP tools
- "No tools available" in Copilot panel

**Solutions:**

**1. Check MCP Configuration**
```json
// .vscode/settings.json or User settings
{
  "github.copilot.chat.mcpServers": {
    "mainframe-context": {
      "command": "node",
      "args": ["mcp-servers/mainframe-context/index.js"]
    }
  }
}
```

**2. Restart VS Code**
- Close all VS Code windows
- Reopen workspace
- Check Output panel → GitHub Copilot Chat for errors

**3. Check MCP Server Logs**
```bash
# Run server manually to see output
cd mcp-servers/mainframe-context
node index.js

# Look for startup messages and errors
```

---

### Issue: MCP Server Timeout Errors

**Symptoms:**
- "Request timed out" errors
- Slow responses from tools

**Solutions:**

**1. Check Data File Sizes**
```bash
# Large files cause slow loading
ls -lh data/

# If files are > 50MB, consider:
# - Splitting by domain
# - Adding indexing
# - Using streaming responses
```

**2. Add Caching**
```javascript
// In index.js, cache loaded data
let cachedInventory = null;

function getInventory() {
  if (!cachedInventory) {
    cachedInventory = JSON.parse(
      fs.readFileSync(path.join(DATA_DIR, 'program-inventory.json'))
    );
  }
  return cachedInventory;
}
```

**3. Increase Timeout**
```json
// In MCP settings
{
  "github.copilot.chat.mcpServers": {
    "mainframe-context": {
      "command": "node",
      "args": ["mcp-servers/mainframe-context/index.js"],
      "timeout": 30000
    }
  }
}
```

---

## Scanner Errors

### Issue: Scanner Crashes on Specific Files

**Symptoms:**
- `scan-codebase.py` exits with traceback
- "UnicodeDecodeError" or "ParseError"

**Solutions:**

**1. Encoding Issues**
```bash
# Check file encoding
file samples/cobol/PROBLEM.cbl

# Force UTF-8 reading in scanner
python scan-codebase.py --encoding utf-8 --errors replace
```

**2. Malformed COBOL**
```bash
# Run with verbose logging to find problem
python scan-codebase.py --cobol-dir samples/cobol --verbose --debug 2>&1 | tee scan.log

# Search for the error
grep -B5 "Error" scan.log
```

**3. Skip Problematic Files**
```bash
# Add to skip list
python scan-codebase.py --skip-files PROBLEM.cbl,ANOTHER.cbl
```

---

### Issue: Scanner Missing Dependencies

**Symptoms:**
- Programs show empty `calls` array
- Copybooks not detected

**Solutions:**

**1. Check COPY Statement Format**
```cobol
* Scanner looks for these patterns:
COPY COPYBOOK.         *> Works
COPY COPYBOOK          *> Works
COPY 'COPYBOOK'        *> Works
COPY "COPYBOOK".       *> Works
   COPY                *> Missing copybook name - skipped
```

**2. Check CALL Statement Format**
```cobol
* Scanner looks for these patterns:
CALL 'PROGRAM'         *> Works
CALL "PROGRAM"         *> Works
CALL WS-PROGRAM        *> Dynamic call - logged as warning
```

**3. Run Dependency Analysis**
```bash
# Regenerate dependencies specifically
python scan-codebase.py --cobol-dir samples/cobol --analyze-deps --verbose
```

---

### Issue: Scanner Reports Wrong Line Counts

**Symptoms:**
- `linesOfCode` much higher or lower than expected
- Counts don't match mainframe reports

**Solutions:**

**1. Check What's Being Counted**
```python
# Scanner counts non-blank, non-comment lines by default
# Adjust in scan-codebase.py:

def count_lines(content: str) -> int:
    lines = content.splitlines()
    # Count all lines
    return len(lines)
    
    # Or count non-blank only
    return sum(1 for line in lines if line.strip())
    
    # Or count non-comment only
    return sum(1 for line in lines 
               if line.strip() and not line.strip().startswith('*'))
```

**2. Verify Line Endings**
```bash
# Check for CRLF vs LF issues
file samples/cobol/*.cbl

# Convert if needed
dos2unix samples/cobol/*.cbl
```

---

## Validator Failures

### Issue: Schema Validation Errors

**Symptoms:**
- `❌ [ERROR] program-inventory.json: Missing required field "programId"`
- Validator exits with code 1

**Solutions:**

**1. Check Missing Fields**
```bash
# Find programs missing required fields
jq '.programs[] | select(.programId == null or .programId == "")' data/program-inventory.json
```

**2. Fix Data Manually**
```bash
# Add missing fields
jq '.programs |= map(if .complexity == null then .complexity = 5 else . end)' \
  data/program-inventory.json > tmp.json && mv tmp.json data/program-inventory.json
```

**3. Re-run Scanner with Fixes**
```bash
# Scanner should populate all required fields
python scan-codebase.py --cobol-dir samples/cobol --strict
```

---

### Issue: Cross-Reference Validation Errors

**Symptoms:**
- `❌ [ERROR] Program CUSTMGMT calls non-existent program UTILS01`
- `⚠️ [WARN] Orphaned copybook: OLD-COPYBOOK`

**Solutions:**

**1. Missing Called Programs**
```bash
# List all called programs
jq '.programs[].calls[]' data/program-inventory.json | sort -u

# Compare to existing programs
jq '.programs[].programId' data/program-inventory.json | sort -u

# Find missing
comm -23 <(jq -r '.programs[].calls[]' data/program-inventory.json | sort -u) \
         <(jq -r '.programs[].programId' data/program-inventory.json | sort -u)
```

**2. Add Stub Entries for External Programs**
```bash
# External programs (called but not in source) need stub entries
jq '.programs += [{"programId": "UTILS01", "fileName": "EXTERNAL", "external": true, "linesOfCode": 0, "complexity": 0, "migrationStatus": "external"}]' \
  data/program-inventory.json > tmp.json && mv tmp.json data/program-inventory.json
```

**3. Clean Orphaned Copybooks**
```bash
# Remove copybooks no longer referenced
jq '.copybooks |= with_entries(select(.value.usedBy | length > 0))' \
  data/copybook-catalog.json > tmp.json && mv tmp.json data/copybook-catalog.json
```

---

## Agent/Skill Not Loading Issues

### Issue: Custom Agent Not Appearing

**Symptoms:**
- `@cobol-analyzer` not recognized in chat
- Agent not in agent picker

**Solutions:**

**1. Check Agent Definition File**
```bash
# Agent must be in .github/ directory
ls -la .github/agents/
# Or
ls -la custom-agents/

# File must have correct frontmatter
head -20 custom-agents/cobol-analyzer.md
```

**2. Verify Agent Frontmatter**
```yaml
---
name: cobol-analyzer
description: Analyze COBOL programs for migration complexity
tools:
  - semantic_search
  - read_file
---
```

**3. Reload VS Code Window**
```
Ctrl+Shift+P → Developer: Reload Window
```

---

### Issue: Custom Skill Not Invoking

**Symptoms:**
- Skill instructions not being followed
- Copilot ignores skill file

**Solutions:**

**1. Check Skill File Location**
```bash
# Skills must be in .github/skills/ or custom-skills/
ls -la .github/skills/
ls -la custom-skills/
```

**2. Verify Skill Frontmatter**
```yaml
---
name: copybook-mapper
description: Maps COBOL copybooks to Java/SQL
applyTo: "**/*.cpy"
---
```

**3. Check Skill Triggers**
```markdown
# In SKILL.md, ensure trigger phrases are clear

Use this skill when:
- User mentions "copybook"
- User asks to "map data types"
- User wants to "convert COBOL structure"
```

**4. Test Skill Directly**
```
# In Copilot chat:
Use the copybook-mapper skill to analyze CUST-REC.cpy
```

---

## Common Error Messages

### "Cannot find module"

```
Error: Cannot find module './utils'
```

**Fix:**
```bash
cd mcp-servers/mainframe-context
npm install
# Or check if file exists
ls -la utils.js
```

---

### "ENOENT: no such file or directory"

```
ENOENT: no such file or directory, open 'data/program-inventory.json'
```

**Fix:**
```bash
# Create data directory and run scanner
mkdir -p data
cd scripts
python scan-codebase.py --cobol-dir ../samples/cobol --output-dir ../data
```

---

### "JSON Parse error"

```
SyntaxError: Unexpected token in JSON at position 1234
```

**Fix:**
```bash
# Find the error location
python -c "import json; json.load(open('data/program-inventory.json'))"

# Validate JSON
cat data/program-inventory.json | jq . 2>&1 | head -20

# Fix common issues:
# - Trailing commas
# - Missing quotes
# - Invalid escape sequences
```

---

### "Permission denied"

```
EACCES: permission denied, open 'data/program-inventory.json'
```

**Fix:**
```bash
# Fix file permissions
chmod 644 data/*.json

# Fix directory permissions
chmod 755 data/
```

---

## Getting Additional Help

### Collect Diagnostic Information

Before requesting help, gather:

```bash
# System info
echo "=== System ===" > diagnostic.txt
uname -a >> diagnostic.txt
node --version >> diagnostic.txt
python --version >> diagnostic.txt

# File listing
echo "=== Files ===" >> diagnostic.txt
ls -laR data/ >> diagnostic.txt

# Validation output
echo "=== Validation ===" >> diagnostic.txt
cd mcp-servers/mainframe-context
node validate.js --data-dir ../../data >> ../../diagnostic.txt 2>&1

# Recent errors
echo "=== Errors ===" >> diagnostic.txt
tail -100 ~/.vscode/logs/*/exthost.log | grep -i error >> diagnostic.txt
```

### Contact Support

| Issue Type | Contact |
|------------|---------|
| Scanner bugs | migration-scanner@acme.com |
| MCP server issues | mcp-support@acme.com |
| Documentation | docs-team@acme.com |
| General questions | Slack: #migration-toolkit |

### Slack Channel

**#migration-toolkit**
- Post diagnostic info
- Include steps to reproduce
- Mention which version you're using

### Office Hours

**When:** Wednesdays 2-3 PM PT
**Where:** Teams link in Slack channel topic
**What:** Live troubleshooting, Q&A
