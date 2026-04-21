# Copilot Configuration: Setup Steps for Coding Agent

> Template for `copilot-setup-steps.yml` — configures the GitHub Copilot Coding Agent environment for the the client migration project.

---

## How to Use

Copy the YAML content below into `.github/copilot-setup-steps.yml` at the root of your migration repository. This file runs automatically when the Copilot Coding Agent starts working on an issue.

---

### `.github/copilot-setup-steps.yml`

```yaml
# Copilot Coding Agent — Environment Setup for the client Migration
# This runs when Copilot Coding Agent is assigned an issue in this repo.

steps:
  # Step 1: Install Java 17 (required for Spring Boot services)
  - name: Set up Java 17
    uses: actions/setup-java@v4
    with:
      distribution: 'temurin'
      java-version: '17'
      cache: 'maven'

  # Step 2: Install Node.js 18 (required for React 18.2 frontend)
  - name: Set up Node.js 18
    uses: actions/setup-node@v4
    with:
      node-version: '18'
      cache: 'npm'

  # Step 3: Install project dependencies
  - name: Install Java dependencies
    run: |
      if [ -f pom.xml ]; then
        mvn dependency:resolve -q
      fi
      if [ -f build.gradle ]; then
        ./gradlew dependencies --quiet
      fi

  - name: Install Node.js dependencies
    run: |
      if [ -f package.json ]; then
        npm ci
      fi

  # Step 4: Install COBOL analysis tools (for Phase 1 Discovery)
  - name: Install COBOL tools
    run: |
      # Install GnuCOBOL for COBOL syntax validation
      sudo apt-get update -qq
      sudo apt-get install -y -qq gnucobol

  # Step 5: Set up MCP Server for mainframe context
  - name: Set up Mainframe Context MCP Server
    run: |
      if [ -d mcp-servers/mainframe-context ]; then
        cd mcp-servers/mainframe-context
        npm install --quiet
      fi

  # Step 6: Verify the environment
  - name: Verify environment
    run: |
      echo "=== Environment Verification ==="
      echo "Java: $(java --version 2>&1 | head -1)"
      echo "Maven: $(mvn --version 2>&1 | head -1)"
      echo "Node: $(node --version)"
      echo "npm: $(npm --version)"
      echo "COBOL: $(cobc --version 2>&1 | head -1)"
      echo "=== Ready for the client Migration ==="
```

---

## What the Coding Agent Can Do With This Setup

Once the environment is configured, the Coding Agent can:

### COBOL → Java Conversion
- Read COBOL source files from the repository
- Generate Java 17 Spring Boot services
- Create JPA entities from copybooks
- Generate REST controllers with OpenAPI annotations
- Write JUnit 5 tests
- Create Flyway migration scripts
- Open a draft PR with all changes

### JCL → GitHub Actions
- Parse JCL job definitions
- Generate `.github/workflows/*.yml` files
- Create Spring Batch configurations
- Add cron schedules and triggers

### BMS → React
- Read BMS screen definitions
- Generate React 18.2 TypeScript components
- Create Zod validation schemas
- Generate custom hooks for API integration
- Add Tailwind CSS styling

### Testing
- Run existing test suites (`mvn test`, `npm test`)
- Generate new tests for converted code
- Run parity tests against reference data
- Report test results in PR comments

---

## Customizing for Your Environment

### If using Gradle instead of Maven:
Replace the Maven steps with:
```yaml
  - name: Install Java dependencies
    run: ./gradlew dependencies --quiet
```

### If using pnpm instead of npm:
```yaml
  - name: Install Node.js dependencies
    run: pnpm install --frozen-lockfile
```

### If you need database access for integration tests:
```yaml
  - name: Start test database
    run: |
      docker run -d --name test-db \
        -e ACCEPT_EULA=Y \
        -e SA_PASSWORD='TestPassword123!' \
        -p 1433:1433 \
        mcr.microsoft.com/mssql/server:2022-latest

      # Wait for SQL Server to be ready
      for i in {1..30}; do
        if docker exec test-db /opt/mssql-tools/bin/sqlcmd \
          -S localhost -U SA -P 'TestPassword123!' \
          -Q "SELECT 1" &> /dev/null; then
          echo "SQL Server is ready"
          break
        fi
        sleep 2
      done
```

---

## Assigning Issues to Coding Agent

Once this setup file is in place, create Issues with clear instructions:

```markdown
## Convert COBOL: CUSTMGMT → CustomerService

**Assigned to:** Copilot Coding Agent

### Source
- COBOL program: `src/cobol/customer/CUSTMGMT.cbl`
- Copybooks: `src/cobol/copybooks/CUST-REC.cpy`, `ADDR-REC.cpy`

### Target
- Package: `com.acme.customer`
- Service: `CustomerService.java`
- Entity: `Customer.java`
- Repository: `CustomerRepository.java`
- Controller: `CustomerController.java`

### Requirements
- Follow `.github/copilot-instructions.md` conversion rules
- Use `copybook-mapper` skill for DTO generation
- Include JUnit 5 tests (90%+ coverage)
- Include Flyway migration for database schema
- Add OpenAPI annotations to controller

### Acceptance Criteria
- [ ] All COBOL paragraphs converted to Java methods
- [ ] BigDecimal used for all decimal fields
- [ ] Tests pass with `mvn test`
- [ ] No compiler warnings
```
