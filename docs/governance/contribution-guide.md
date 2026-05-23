# Contribution Guide

> How to contribute to the COBOL Modernization Toolkit.

## Overview

This toolkit is maintained by the Migration Enablement Team and accepts contributions from all teams working on the modernization project. This guide covers contribution workflows, standards, and release processes.

---

## Getting Started

### Prerequisites

- Git 2.40+
- Node.js 20 LTS
- Python 3.11+
- VS Code with GitHub Copilot extension
- Access to the repository (request via [access request form])

### Clone and Setup

```bash
# Clone the repository
git clone https://github.com/acme-corp/cobol-app-modernization.git
cd cobol-app-modernization

# Install MCP server dependencies
cd mcp-servers/mainframe-context
npm install
npm run validate

# Verify scanner works
cd ../../scripts
python scan-codebase.py --help
```

---

## Contribution Types

### 1. Bug Fixes

- Fix issues in scanner, validator, or MCP server
- Correct documentation errors
- Fix sample code issues

### 2. Feature Enhancements

- New scanner capabilities (e.g., additional COBOL patterns)
- New MCP server tools
- New agents or skills
- Enhanced prompt templates

### 3. Documentation

- Runbooks and operational guides
- Prompt library additions
- Architecture documentation

### 4. Sample Code

- Additional COBOL samples demonstrating edge cases
- Expected output examples
- Test fixtures

---

## Workflow

### Branch Naming Convention

```
<type>/<short-description>

Types:
- fix/      Bug fixes
- feat/     New features
- docs/     Documentation only
- refactor/ Code restructuring
- test/     Test additions/fixes
- chore/    Maintenance tasks

Examples:
- fix/scanner-copybook-parsing
- feat/mcp-dependency-graph-tool
- docs/rollback-runbook
- refactor/validator-error-messages
```

### Development Flow

```
main (protected)
  │
  ├── feat/new-mcp-tool ──────────────────┐
  │                                       │
  │   1. Create branch                    │
  │   2. Make changes                     │
  │   3. Write/update tests               │
  │   4. Self-review                      │
  │   5. Create PR                        │
  │                                       │
  │◄──────────── PR Review ───────────────┤
  │              (2+ approvers)           │
  │                                       │
  │◄──────────── Merge ───────────────────┘
  │
  ▼
Release (tagged)
```

### Creating a Pull Request

1. **Create your branch**
   ```bash
   git checkout main
   git pull origin main
   git checkout -b feat/your-feature-name
   ```

2. **Make changes and commit**
   ```bash
   # Make changes
   git add .
   git commit -m "feat: add dependency graph tool to MCP server
   
   - Added getDependencyGraph tool
   - Returns upstream/downstream dependencies
   - Includes transitive dependency resolution
   
   Closes #123"
   ```

3. **Push and create PR**
   ```bash
   git push -u origin feat/your-feature-name
   # Then create PR via GitHub UI or CLI
   gh pr create --title "feat: add dependency graph tool" --body "..."
   ```

### Commit Message Format

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Formatting (no code change)
- `refactor`: Code restructuring
- `test`: Adding/fixing tests
- `chore`: Maintenance

**Examples:**
```
feat(scanner): add BMS field type extraction

fix(validator): handle missing optional fields gracefully

docs(runbook): add metadata refresh troubleshooting section

refactor(mcp): extract common file parsing utilities
```

---

## PR Review Standards

### Required Reviewers

| Change Type | Reviewers Required |
|-------------|-------------------|
| MCP Server code | 2 (1 must be server maintainer) |
| Scanner code | 2 (1 must be Python expert) |
| Documentation | 1 |
| Agents/Skills | 2 (1 must be Copilot expert) |
| Sample code | 1 |

### Review Checklist

Reviewers should verify:

**For Code Changes:**
- [ ] Code follows project style guidelines
- [ ] Tests added/updated for changes
- [ ] All tests pass locally and in CI
- [ ] No security vulnerabilities introduced
- [ ] Error handling is appropriate
- [ ] Logging is sufficient but not excessive
- [ ] Documentation updated if needed

**For Documentation Changes:**
- [ ] Technical accuracy verified
- [ ] Examples work as documented
- [ ] Links are valid
- [ ] Spelling/grammar checked
- [ ] Consistent with existing style

**For MCP Server Changes:**
- [ ] Tool schema is valid JSON Schema
- [ ] Tool description is clear and helpful
- [ ] Error responses are informative
- [ ] Performance is acceptable (< 500ms response)

### Providing Review Feedback

Use GitHub review features:
- **Comment**: Questions or suggestions
- **Request Changes**: Must be addressed before merge
- **Approve**: Ready to merge

Be specific and constructive:
```markdown
❌ "This is wrong"
✅ "This will fail for copybooks with REDEFINES clauses. 
    Consider checking for `REDEFINES` keyword and handling 
    the overlay structure. See CUST-REC.cpy lines 45-52 
    for an example."
```

---

## Testing Requirements

### MCP Server (JavaScript)

```bash
cd mcp-servers/mainframe-context

# Run tests
npm test

# Run tests with coverage
npm run test:coverage

# Lint
npm run lint
```

**Minimum Coverage:** 80%

### Scanner (Python)

```bash
cd scripts

# Run tests
python -m pytest tests/ -v

# Run with coverage
python -m pytest tests/ --cov=. --cov-report=term-missing

# Lint
flake8 scan-codebase.py
black --check scan-codebase.py
```

**Minimum Coverage:** 80%

### Integration Tests

```bash
# Run full integration test
./scripts/integration-test.sh

# This runs:
# 1. Scanner against samples/
# 2. Validator against generated data/
# 3. MCP server startup and health check
# 4. Sample queries against MCP server
```

---

## Release Process

### Version Numbering

Follow [Semantic Versioning](https://semver.org/):

```
MAJOR.MINOR.PATCH

- MAJOR: Breaking changes (incompatible API changes)
- MINOR: New features (backward compatible)
- PATCH: Bug fixes (backward compatible)

Examples:
- 1.0.0 → 1.0.1: Bug fix
- 1.0.1 → 1.1.0: New MCP tool added
- 1.1.0 → 2.0.0: Changed MCP tool response format
```

### Release Tagging Convention

```bash
# Format
v<major>.<minor>.<patch>

# Examples
v1.0.0
v1.1.0
v2.0.0-beta.1
v2.0.0-rc.1
```

### Creating a Release

1. **Update Changelog**
   ```markdown
   # Changelog

   ## [1.2.0] - 2026-05-23

   ### Added
   - New `getDependencyGraph` MCP tool (#123)
   - Support for REDEFINES clause in copybook scanner (#125)

   ### Fixed
   - Scanner crash on malformed COPY statements (#124)

   ### Changed
   - Improved error messages in validator
   ```

2. **Create Release Branch (for major/minor)**
   ```bash
   git checkout main
   git pull
   git checkout -b release/v1.2.0
   ```

3. **Update Version Numbers**
   ```bash
   # package.json
   npm version 1.2.0 --no-git-tag-version
   
   # Update version in documentation headers if applicable
   ```

4. **Create PR to main**
   ```bash
   git add .
   git commit -m "chore: release v1.2.0"
   git push -u origin release/v1.2.0
   gh pr create --title "Release v1.2.0"
   ```

5. **After Merge, Tag Release**
   ```bash
   git checkout main
   git pull
   git tag -a v1.2.0 -m "Release v1.2.0"
   git push origin v1.2.0
   ```

6. **Create GitHub Release**
   - Go to Releases → New Release
   - Select tag `v1.2.0`
   - Copy changelog entry to description
   - Publish release

### Hotfix Process

For urgent fixes to released versions:

```bash
# Branch from tag
git checkout v1.2.0
git checkout -b hotfix/v1.2.1

# Make fix
git commit -m "fix: critical scanner crash on empty files"

# Create PR targeting main
gh pr create --title "Hotfix: critical scanner crash"

# After merge, tag
git checkout main
git pull
git tag -a v1.2.1 -m "Hotfix v1.2.1"
git push origin v1.2.1
```

---

## Code Style Guidelines

### JavaScript (MCP Server)

- ES Modules (`import`/`export`)
- 2 space indentation
- Single quotes for strings
- Semicolons required
- JSDoc comments for functions

```javascript
/**
 * Retrieves program details from the catalog.
 * @param {string} programId - The COBOL program identifier
 * @returns {Promise<Program>} Program metadata
 * @throws {NotFoundError} If program doesn't exist
 */
export async function getProgram(programId) {
  // Implementation
}
```

### Python (Scanner)

- PEP 8 compliant
- Type hints required
- 4 space indentation
- Docstrings for modules, classes, functions

```python
def parse_cobol_program(file_path: Path) -> ProgramMetadata:
    """
    Parse a COBOL program and extract metadata.

    Args:
        file_path: Path to the COBOL source file

    Returns:
        ProgramMetadata containing program details

    Raises:
        ParseError: If the COBOL syntax is invalid
    """
    # Implementation
```

### Markdown (Documentation)

- Use ATX headers (`#`, `##`, etc.)
- One sentence per line (for better diffs)
- Code blocks with language specified
- Tables for structured data
- Relative links to other docs

---

## Getting Help

### Questions

- **Slack:** #migration-toolkit
- **Office Hours:** Wednesdays 2-3 PM PT

### Issue Templates

Use GitHub issue templates:
- **Bug Report:** For scanner/MCP/validator issues
- **Feature Request:** For enhancements
- **Documentation:** For doc improvements

### Maintainers

| Area | Maintainer | GitHub |
|------|------------|--------|
| MCP Server | Alice Smith | @asmith |
| Scanner | Bob Jones | @bjones |
| Documentation | Carol White | @cwhite |
| Agents/Skills | David Brown | @dbrown |

---

## License

This toolkit is proprietary to Acme Corp. See [LICENSE](../LICENSE) for details.

Contributions are subject to the Contributor License Agreement (CLA). First-time contributors will be prompted to sign via CLA bot.
