#!/usr/bin/env bash
#
# Bootstrap script for COBOL Modernization Toolkit (Linux/macOS)
#
# Sets up the development environment by:
#   1. Checking Node.js and Python prerequisites
#   2. Installing npm dependencies for the MCP server
#   3. Running the scanner against sample files
#   4. Validating the generated metadata
#
# Usage:
#   ./bootstrap.sh [--skip-scan] [--skip-validation]
#
# Requirements:
#   - Node.js 18+
#   - Python 3.10+
#

set -e

# Parse arguments
SKIP_SCAN=false
SKIP_VALIDATION=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-scan)
            SKIP_SCAN=true
            shift
            ;;
        --skip-validation)
            SKIP_VALIDATION=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [--skip-scan] [--skip-validation]"
            echo ""
            echo "Options:"
            echo "  --skip-scan        Skip running the codebase scanner"
            echo "  --skip-validation  Skip running the metadata validator"
            echo "  --help, -h         Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
NC='\033[0m' # No Color

# Output functions
header() {
    echo ""
    echo -e "${CYAN}$(printf '=%.0s' {1..60})${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}$(printf '=%.0s' {1..60})${NC}"
}

step() {
    echo ""
    echo -e "${YELLOW}▶ $1${NC}"
}

success() {
    echo -e "${GREEN}✅ $1${NC}"
}

failure() {
    echo -e "${RED}❌ $1${NC}"
}

info() {
    echo -e "${GRAY}ℹ️  $1${NC}"
}

# Track errors
HAS_ERRORS=false

# Get script and project directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

header "COBOL Modernization Toolkit - Bootstrap"
echo "Started: $(date '+%Y-%m-%d %H:%M:%S')"
info "Project root: $PROJECT_ROOT"

# Change to project root
cd "$PROJECT_ROOT"

# ========================================
# Step 1: Check Prerequisites
# ========================================
step "Checking prerequisites..."

# Check Node.js
if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version)
    success "Node.js found: $NODE_VERSION"
    
    # Parse version and check minimum
    MAJOR_VERSION=$(echo "$NODE_VERSION" | sed 's/v\([0-9]*\).*/\1/')
    if [ "$MAJOR_VERSION" -lt 18 ]; then
        failure "Node.js 18+ required, found v$MAJOR_VERSION"
        HAS_ERRORS=true
    fi
else
    failure "Node.js not found. Please install Node.js 18+ from https://nodejs.org"
    HAS_ERRORS=true
fi

# Check npm
if command -v npm &> /dev/null; then
    NPM_VERSION=$(npm --version)
    success "npm found: v$NPM_VERSION"
else
    failure "npm not found. Please install Node.js which includes npm."
    HAS_ERRORS=true
fi

# Check Python
PYTHON_CMD=""
for cmd in python3 python; do
    if command -v $cmd &> /dev/null; then
        PYTHON_VERSION=$($cmd --version 2>&1)
        if [[ $PYTHON_VERSION =~ Python\ ([0-9]+)\.([0-9]+) ]]; then
            MAJOR=${BASH_REMATCH[1]}
            MINOR=${BASH_REMATCH[2]}
            
            # Compare using sort -V or just handle manually for major 3
            if [ "$MAJOR" -gt 3 ] || ([ "$MAJOR" -eq 3 ] && [ "$MINOR" -ge 10 ]); then
                PYTHON_CMD=$cmd
                success "Python found: $PYTHON_VERSION"
                break
            fi
        fi
    fi
done

if [ -z "$PYTHON_CMD" ]; then
    failure "Python 3.10+ not found. Please install Python from https://python.org"
    HAS_ERRORS=true
fi

if [ "$HAS_ERRORS" = true ]; then
    echo ""
    failure "Prerequisites check failed. Please install missing dependencies and try again."
    exit 1
fi

success "All prerequisites satisfied"

# ========================================
# Step 2: Install npm dependencies
# ========================================
step "Installing MCP server dependencies..."

MCP_SERVER_DIR="$PROJECT_ROOT/mcp-servers/mainframe-context"

if [ ! -d "$MCP_SERVER_DIR" ]; then
    failure "MCP server directory not found: $MCP_SERVER_DIR"
    exit 1
fi

info "Running npm install in $MCP_SERVER_DIR"
cd "$MCP_SERVER_DIR"

if npm install; then
    success "npm dependencies installed"
else
    failure "npm install failed"
    HAS_ERRORS=true
fi

cd "$PROJECT_ROOT"

# ========================================
# Step 3: Run the scanner
# ========================================
if [ "$SKIP_SCAN" = false ]; then
    step "Running codebase scanner..."

    SCANNER_PATH="$SCRIPT_DIR/scan-codebase.py"
    COBOL_DIR="$PROJECT_ROOT/samples/cobol"
    JCL_DIR="$PROJECT_ROOT/samples/jcl"
    BMS_DIR="$PROJECT_ROOT/samples/bms"
    DATA_DIR="$PROJECT_ROOT/data"

    if [ ! -f "$SCANNER_PATH" ]; then
        failure "Scanner not found: $SCANNER_PATH"
        HAS_ERRORS=true
    else
        info "Scanning samples/ directory..."
        
        if $PYTHON_CMD "$SCANNER_PATH" \
            --cobol-dir "$COBOL_DIR" \
            --jcl-dir "$JCL_DIR" \
            --bms-dir "$BMS_DIR" \
            --output-dir "$DATA_DIR"; then
            
            success "Scanner completed successfully"
            
            # Show generated files
            info "Generated files:"
            for f in "$DATA_DIR"/*.json; do
                if [ -f "$f" ]; then
                    SIZE=$(wc -c < "$f" | tr -d ' ')
                    echo "    📄 $(basename "$f") ($SIZE bytes)"
                fi
            done
        else
            failure "Scanner failed"
            HAS_ERRORS=true
        fi
    fi
else
    info "Skipping scanner (--skip-scan)"
fi

# ========================================
# Step 4: Run the validator
# ========================================
if [ "$SKIP_VALIDATION" = false ]; then
    step "Running metadata validator..."

    VALIDATOR_PATH="$MCP_SERVER_DIR/validate.js"

    if [ ! -f "$VALIDATOR_PATH" ]; then
        failure "Validator not found: $VALIDATOR_PATH"
        HAS_ERRORS=true
    else
        cd "$MCP_SERVER_DIR"
        
        if node validate.js --data-dir "$PROJECT_ROOT/data"; then
            success "Metadata validation passed"
        else
            failure "Validator found errors"
            HAS_ERRORS=true
        fi
        
        cd "$PROJECT_ROOT"
    fi
else
    info "Skipping validation (--skip-validation)"
fi

# ========================================
# Summary
# ========================================
header "Bootstrap Summary"
echo "Completed: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

if [ "$HAS_ERRORS" = true ]; then
    failure "Bootstrap completed with errors"
    echo ""
    echo "Please review the errors above and fix them before proceeding."
    exit 1
else
    success "Bootstrap completed successfully!"
    echo ""
    echo -e "${CYAN}Next steps:${NC}"
    echo "  1. Review generated metadata in data/*.json"
    echo "  2. Configure the MCP server in your VS Code settings"
    echo "  3. Start using the mainframe-context tools with GitHub Copilot"
    echo ""
    echo "To start the MCP server manually:"
    echo "  cd mcp-servers/mainframe-context && npm start"
    exit 0
fi
