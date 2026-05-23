#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Bootstrap script for COBOL Modernization Toolkit (Windows/PowerShell)

.DESCRIPTION
    Sets up the development environment by:
    1. Checking Node.js and Python prerequisites
    2. Installing npm dependencies for the MCP server
    3. Running the scanner against sample files
    4. Validating the generated metadata

.EXAMPLE
    .\bootstrap.ps1

.EXAMPLE
    .\bootstrap.ps1 -Verbose

.NOTES
    Requires Node.js 18+ and Python 3.8+
#>

[CmdletBinding()]
param(
    [switch]$SkipScan,
    [switch]$SkipValidation
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

# Colors for output
function Write-Header { param($msg) Write-Host "`n$("=" * 60)" -ForegroundColor Cyan; Write-Host $msg -ForegroundColor Cyan; Write-Host "$("=" * 60)" -ForegroundColor Cyan }
function Write-Step { param($msg) Write-Host "`n▶ $msg" -ForegroundColor Yellow }
function Write-Success { param($msg) Write-Host "✅ $msg" -ForegroundColor Green }
function Write-Failure { param($msg) Write-Host "❌ $msg" -ForegroundColor Red }
function Write-Info { param($msg) Write-Host "ℹ️  $msg" -ForegroundColor Gray }

# Track overall status
$script:hasErrors = $false

function Test-Command {
    param([string]$Command)
    $null = Get-Command $Command -ErrorAction SilentlyContinue
    return $?
}

function Get-Version {
    param([string]$Command, [string]$Args)
    try {
        $output = & $Command $Args 2>&1
        return $output -join "`n"
    } catch {
        return $null
    }
}

Write-Header "COBOL Modernization Toolkit - Bootstrap"
Write-Host "Started: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"

# Get script and project root directories
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
Write-Info "Project root: $ProjectRoot"

# Change to project root
Push-Location $ProjectRoot

try {
    # ========================================
    # Step 1: Check Prerequisites
    # ========================================
    Write-Step "Checking prerequisites..."

    # Check Node.js
    if (Test-Command "node") {
        $nodeVersion = Get-Version "node" "--version"
        Write-Success "Node.js found: $nodeVersion"
        
        # Parse version and check minimum
        if ($nodeVersion -match "v(\d+)\.") {
            $majorVersion = [int]$matches[1]
            if ($majorVersion -lt 18) {
                Write-Failure "Node.js 18+ required, found v$majorVersion"
                $script:hasErrors = $true
            }
        }
    } else {
        Write-Failure "Node.js not found. Please install Node.js 18+ from https://nodejs.org"
        $script:hasErrors = $true
    }

    # Check npm
    if (Test-Command "npm") {
        $npmVersion = Get-Version "npm" "--version"
        Write-Success "npm found: v$npmVersion"
    } else {
        Write-Failure "npm not found. Please install Node.js which includes npm."
        $script:hasErrors = $true
    }

    # Check Python
    $pythonCmd = $null
    foreach ($cmd in @("python", "python3", "py")) {
        if (Test-Command $cmd) {
            $pythonVersion = Get-Version $cmd "--version"
            if ($pythonVersion -match "Python (\d+)\.(\d+)") {
                $major = [int]$matches[1]
                $minor = [int]$matches[2]
                if ($major -ge 3 -and $minor -ge 8) {
                    $pythonCmd = $cmd
                    Write-Success "Python found: $pythonVersion"
                    break
                }
            }
        }
    }
    if (-not $pythonCmd) {
        Write-Failure "Python 3.8+ not found. Please install Python from https://python.org"
        $script:hasErrors = $true
    }

    if ($script:hasErrors) {
        Write-Host ""
        Write-Failure "Prerequisites check failed. Please install missing dependencies and try again."
        exit 1
    }

    Write-Success "All prerequisites satisfied"

    # ========================================
    # Step 2: Install npm dependencies
    # ========================================
    Write-Step "Installing MCP server dependencies..."

    $mcpServerDir = Join-Path $ProjectRoot "mcp-servers" "mainframe-context"
    
    if (-not (Test-Path $mcpServerDir)) {
        Write-Failure "MCP server directory not found: $mcpServerDir"
        exit 1
    }

    Push-Location $mcpServerDir
    try {
        Write-Info "Running npm install in $mcpServerDir"
        $npmOutput = npm install 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Host $npmOutput
            Write-Failure "npm install failed"
            $script:hasErrors = $true
        } else {
            Write-Success "npm dependencies installed"
        }
    } finally {
        Pop-Location
    }

    # ========================================
    # Step 3: Run the scanner
    # ========================================
    if (-not $SkipScan) {
        Write-Step "Running codebase scanner..."

        $scannerPath = Join-Path $ScriptDir "scan-codebase.py"
        $cobolDir = Join-Path $ProjectRoot "samples" "cobol"
        $jclDir = Join-Path $ProjectRoot "samples" "jcl"
        $bmsDir = Join-Path $ProjectRoot "samples" "bms"
        $dataDir = Join-Path $ProjectRoot "data"

        if (-not (Test-Path $scannerPath)) {
            Write-Failure "Scanner not found: $scannerPath"
            $script:hasErrors = $true
        } else {
            Write-Info "Scanning samples/ directory..."
            
            $scanArgs = @(
                $scannerPath,
                "--cobol-dir", $cobolDir,
                "--jcl-dir", $jclDir,
                "--bms-dir", $bmsDir,
                "--output-dir", $dataDir
            )
            
            & $pythonCmd @scanArgs
            
            if ($LASTEXITCODE -ne 0) {
                Write-Failure "Scanner failed with exit code $LASTEXITCODE"
                $script:hasErrors = $true
            } else {
                Write-Success "Scanner completed successfully"
                
                # Show generated files
                Write-Info "Generated files:"
                Get-ChildItem $dataDir -Filter "*.json" | ForEach-Object {
                    $size = "{0:N0}" -f $_.Length
                    Write-Host "    📄 $($_.Name) ($size bytes)"
                }
            }
        }
    } else {
        Write-Info "Skipping scanner (--SkipScan)"
    }

    # ========================================
    # Step 4: Run the validator
    # ========================================
    if (-not $SkipValidation) {
        Write-Step "Running metadata validator..."

        $validatorPath = Join-Path $mcpServerDir "validate.js"
        
        if (-not (Test-Path $validatorPath)) {
            Write-Failure "Validator not found: $validatorPath"
            $script:hasErrors = $true
        } else {
            Push-Location $mcpServerDir
            try {
                node validate.js --data-dir (Join-Path $ProjectRoot "data")
                
                if ($LASTEXITCODE -ne 0) {
                    Write-Failure "Validator found errors"
                    $script:hasErrors = $true
                } else {
                    Write-Success "Metadata validation passed"
                }
            } finally {
                Pop-Location
            }
        }
    } else {
        Write-Info "Skipping validation (--SkipValidation)"
    }

    # ========================================
    # Summary
    # ========================================
    Write-Header "Bootstrap Summary"
    Write-Host "Completed: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    Write-Host ""

    if ($script:hasErrors) {
        Write-Failure "Bootstrap completed with errors"
        Write-Host ""
        Write-Host "Please review the errors above and fix them before proceeding."
        exit 1
    } else {
        Write-Success "Bootstrap completed successfully!"
        Write-Host ""
        Write-Host "Next steps:" -ForegroundColor Cyan
        Write-Host "  1. Review generated metadata in data/*.json"
        Write-Host "  2. Configure the MCP server in your VS Code settings"
        Write-Host "  3. Start using the mainframe-context tools with GitHub Copilot"
        Write-Host ""
        Write-Host "To start the MCP server manually:"
        Write-Host "  cd mcp-servers/mainframe-context && npm start"
        exit 0
    }

} finally {
    Pop-Location
}
