#!/usr/bin/env node
/**
 * Metadata Validator for Mainframe Context MCP Server
 *
 * Validates JSON metadata files against expected schemas and checks
 * cross-references between catalogs.
 *
 * Usage:
 *   node validate.js [--data-dir path/to/data]
 *
 * Exit codes:
 *   0 - All validations passed
 *   1 - Validation errors found
 */

import { readFileSync, existsSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";

const __dirname = dirname(fileURLToPath(import.meta.url));

// Parse command line arguments
const args = process.argv.slice(2);
let dataDir = resolve(__dirname, "../../data");

for (let i = 0; i < args.length; i++) {
  if (args[i] === "--data-dir" && args[i + 1]) {
    dataDir = resolve(args[i + 1]);
    i++;
  } else if (args[i] === "--help" || args[i] === "-h") {
    console.log(`
Metadata Validator for Mainframe Context MCP Server

Usage:
  node validate.js [options]

Options:
  --data-dir <path>  Path to data directory (default: ../../data)
  --help, -h         Show this help message

Exit codes:
  0 - All validations passed
  1 - Validation errors found
`);
    process.exit(0);
  }
}

// Validation result tracking
let totalErrors = 0;
let totalWarnings = 0;

function logError(context, message) {
  totalErrors++;
  console.error(`❌ [ERROR] ${context}: ${message}`);
}

function logWarning(context, message) {
  totalWarnings++;
  console.warn(`⚠️  [WARN] ${context}: ${message}`);
}

function logSuccess(message) {
  console.log(`✅ ${message}`);
}

function logInfo(message) {
  console.log(`ℹ️  ${message}`);
}

// Schema definitions
const PROGRAM_SCHEMA = {
  required: ["programId", "fileName", "linesOfCode", "complexity", "migrationStatus"],
  types: {
    programId: "string",
    fileName: "string",
    linesOfCode: "number",
    businessDomain: "string",
    complexity: "number",
    calls: "array",
    calledBy: "array",
    copybooks: "array",
    files: "array",
    sqlTables: "array",
    cicsCommands: "array",
    migrationStatus: "string",
  },
  enums: {
    migrationStatus: ["pending", "in-progress", "completed", "blocked"],
  },
  ranges: {
    complexity: { min: 1, max: 5 },
    linesOfCode: { min: 1 },
  },
};

const COPYBOOK_SCHEMA = {
  required: ["name", "usedBy", "usageCount"],
  types: {
    name: "string",
    usedBy: "array",
    usageCount: "number",
  },
};

const JCL_SCHEMA = {
  required: ["jobName", "fileName", "programs", "stepCount", "migrationStatus"],
  types: {
    jobName: "string",
    fileName: "string",
    programs: "array",
    procedures: "array",
    datasets: "array",
    stepCount: "number",
    schedule: ["string", "null"],
    sla: ["string", "null"],
    dependencies: "array",
    migrationStatus: "string",
  },
  enums: {
    migrationStatus: ["pending", "in-progress", "completed", "blocked"],
  },
};

const DATA_DICT_SCHEMA = {
  required: ["name", "type"],
  types: {
    name: "string",
    type: "string",
    copybook: ["string", "null"],
    keyFields: "array",
    keyLength: ["number", "null"],
    keyOffset: ["number", "null"],
    recordSize: ["number", "null"],
    estimatedRecords: ["number", "null"],
    targetDatabase: "string",
    targetTable: "string",
    referencedBy: "array",
  },
  enums: {
    type: ["VSAM-KSDS", "VSAM-ESDS", "VSAM-RRDS", "Sequential", "PDS", "PDS-LoadLib", "Unknown"],
    targetDatabase: ["Azure SQL", "PostgreSQL", "MySQL", "Oracle", "Blob Storage"],
  },
};

function loadJSON(filename) {
  const filePath = resolve(dataDir, filename);
  if (!existsSync(filePath)) {
    logError(filename, `File not found: ${filePath}`);
    return null;
  }
  try {
    const content = readFileSync(filePath, "utf-8");
    return JSON.parse(content);
  } catch (e) {
    logError(filename, `Failed to parse JSON: ${e.message}`);
    return null;
  }
}

function checkType(value, expectedType) {
  if (Array.isArray(expectedType)) {
    return expectedType.some((t) => checkType(value, t));
  }
  if (expectedType === "array") {
    return Array.isArray(value);
  }
  if (expectedType === "null") {
    return value === null;
  }
  return typeof value === expectedType;
}

function validateSchema(data, schema, context) {
  let errors = 0;

  // Check required fields
  for (const field of schema.required) {
    if (!(field in data)) {
      logError(context, `Missing required field: ${field}`);
      errors++;
    }
  }

  // Check types
  for (const [field, expectedType] of Object.entries(schema.types)) {
    if (field in data && data[field] !== undefined) {
      if (!checkType(data[field], expectedType)) {
        logError(
          context,
          `Field '${field}' has wrong type: expected ${JSON.stringify(expectedType)}, got ${typeof data[field]}`
        );
        errors++;
      }
    }
  }

  // Check enum values
  if (schema.enums) {
    for (const [field, validValues] of Object.entries(schema.enums)) {
      if (field in data && data[field] !== null && data[field] !== undefined) {
        if (!validValues.includes(data[field])) {
          logError(
            context,
            `Field '${field}' has invalid value: '${data[field]}'. Valid values: ${validValues.join(", ")}`
          );
          errors++;
        }
      }
    }
  }

  // Check ranges
  if (schema.ranges) {
    for (const [field, range] of Object.entries(schema.ranges)) {
      if (field in data && typeof data[field] === "number") {
        if (range.min !== undefined && data[field] < range.min) {
          logError(context, `Field '${field}' value ${data[field]} is below minimum ${range.min}`);
          errors++;
        }
        if (range.max !== undefined && data[field] > range.max) {
          logError(context, `Field '${field}' value ${data[field]} is above maximum ${range.max}`);
          errors++;
        }
      }
    }
  }

  return errors;
}

function validatePrograms(programs) {
  logInfo("Validating program-inventory.json...");

  if (!programs || !Array.isArray(programs)) {
    logError("program-inventory.json", "Must be an array");
    return;
  }

  if (programs.length === 0) {
    logWarning("program-inventory.json", "Array is empty");
    return;
  }

  const programIds = new Set();
  for (const [index, prog] of programs.entries()) {
    const context = `program-inventory.json[${index}] (${prog.programId || "unknown"})`;
    validateSchema(prog, PROGRAM_SCHEMA, context);

    if (prog.programId) {
      if (programIds.has(prog.programId)) {
        logError(context, `Duplicate programId: ${prog.programId}`);
      }
      programIds.add(prog.programId);
    }

    // Validate array elements are strings
    for (const field of ["calls", "calledBy", "copybooks", "files", "sqlTables", "cicsCommands"]) {
      if (Array.isArray(prog[field])) {
        for (const item of prog[field]) {
          if (typeof item !== "string") {
            logError(context, `Array field '${field}' contains non-string element: ${typeof item}`);
          }
        }
      }
    }
  }

  logSuccess(`Validated ${programs.length} programs`);
  return programIds;
}

function validateCopybooks(copybooks) {
  logInfo("Validating copybook-catalog.json...");

  if (!copybooks || !Array.isArray(copybooks)) {
    logError("copybook-catalog.json", "Must be an array");
    return;
  }

  if (copybooks.length === 0) {
    logWarning("copybook-catalog.json", "Array is empty");
    return;
  }

  const copybookNames = new Set();
  for (const [index, cpy] of copybooks.entries()) {
    const context = `copybook-catalog.json[${index}] (${cpy.name || "unknown"})`;
    validateSchema(cpy, COPYBOOK_SCHEMA, context);

    if (cpy.name) {
      if (copybookNames.has(cpy.name)) {
        logError(context, `Duplicate copybook name: ${cpy.name}`);
      }
      copybookNames.add(cpy.name);
    }

    // Validate usageCount matches usedBy length
    if (Array.isArray(cpy.usedBy) && typeof cpy.usageCount === "number") {
      if (cpy.usedBy.length !== cpy.usageCount) {
        logWarning(context, `usageCount (${cpy.usageCount}) doesn't match usedBy length (${cpy.usedBy.length})`);
      }
    }
  }

  logSuccess(`Validated ${copybooks.length} copybooks`);
  return copybookNames;
}

function validateJcl(jclJobs) {
  logInfo("Validating jcl-catalog.json...");

  if (!jclJobs || !Array.isArray(jclJobs)) {
    logError("jcl-catalog.json", "Must be an array");
    return;
  }

  if (jclJobs.length === 0) {
    logWarning("jcl-catalog.json", "Array is empty");
    return;
  }

  const jobNames = new Set();
  const referencedPrograms = new Set();

  for (const [index, job] of jclJobs.entries()) {
    const context = `jcl-catalog.json[${index}] (${job.jobName || "unknown"})`;
    validateSchema(job, JCL_SCHEMA, context);

    if (job.jobName) {
      if (jobNames.has(job.jobName)) {
        logError(context, `Duplicate jobName: ${job.jobName}`);
      }
      jobNames.add(job.jobName);
    }

    // Collect referenced programs
    if (Array.isArray(job.programs)) {
      job.programs.forEach((p) => referencedPrograms.add(p));
    }
  }

  logSuccess(`Validated ${jclJobs.length} JCL jobs`);
  return { jobNames, referencedPrograms };
}

function validateDataDictionary(dataDict) {
  logInfo("Validating data-dictionary.json...");

  if (!dataDict || !Array.isArray(dataDict)) {
    logError("data-dictionary.json", "Must be an array");
    return;
  }

  if (dataDict.length === 0) {
    logWarning("data-dictionary.json", "Array is empty");
    return;
  }

  const dataNames = new Set();
  for (const [index, entry] of dataDict.entries()) {
    const context = `data-dictionary.json[${index}] (${entry.name || "unknown"})`;
    validateSchema(entry, DATA_DICT_SCHEMA, context);

    if (entry.name) {
      if (dataNames.has(entry.name)) {
        logError(context, `Duplicate data entry name: ${entry.name}`);
      }
      dataNames.add(entry.name);
    }
  }

  logSuccess(`Validated ${dataDict.length} data dictionary entries`);
  return dataNames;
}

function validateCrossReferences(programs, copybooks, jclJobs, dataDict, programIds, copybookNames) {
  logInfo("Validating cross-references...");

  // Check that all copybooks referenced by programs exist in catalog
  if (programs && copybookNames) {
    for (const prog of programs) {
      if (Array.isArray(prog.copybooks)) {
        for (const cpy of prog.copybooks) {
          if (!copybookNames.has(cpy)) {
            logWarning(
              `program ${prog.programId}`,
              `References copybook '${cpy}' not found in copybook-catalog.json`
            );
          }
        }
      }
    }
  }

  // Check that all programs referenced in calledBy/calls exist
  if (programs && programIds) {
    for (const prog of programs) {
      if (Array.isArray(prog.calls)) {
        for (const called of prog.calls) {
          if (!programIds.has(called)) {
            logWarning(
              `program ${prog.programId}`,
              `Calls program '${called}' not found in program-inventory.json`
            );
          }
        }
      }
      if (Array.isArray(prog.calledBy)) {
        for (const caller of prog.calledBy) {
          if (!programIds.has(caller)) {
            logWarning(
              `program ${prog.programId}`,
              `CalledBy program '${caller}' not found in program-inventory.json`
            );
          }
        }
      }
    }
  }

  // Check that copybook usedBy references valid programs
  if (copybooks && programIds) {
    for (const cpy of copybooks) {
      if (Array.isArray(cpy.usedBy)) {
        for (const user of cpy.usedBy) {
          if (!programIds.has(user)) {
            logWarning(
              `copybook ${cpy.name}`,
              `UsedBy program '${user}' not found in program-inventory.json`
            );
          }
        }
      }
    }
  }

  // Check call graph symmetry (if A calls B, B should have A in calledBy)
  if (programs && programIds) {
    const programMap = Object.fromEntries(programs.map((p) => [p.programId, p]));
    for (const prog of programs) {
      if (Array.isArray(prog.calls)) {
        for (const called of prog.calls) {
          const calledProg = programMap[called];
          if (calledProg && Array.isArray(calledProg.calledBy)) {
            if (!calledProg.calledBy.includes(prog.programId)) {
              logWarning(
                `call graph`,
                `${prog.programId} calls ${called}, but ${called}.calledBy doesn't include ${prog.programId}`
              );
            }
          }
        }
      }
    }
  }

  logSuccess("Cross-reference validation complete");
}

// Main execution
console.log("═".repeat(60));
console.log("Metadata Validator for Mainframe Context MCP Server");
console.log("═".repeat(60));
console.log(`Data directory: ${dataDir}`);
console.log("");

// Load all files
const programs = loadJSON("program-inventory.json");
const copybooks = loadJSON("copybook-catalog.json");
const jclJobs = loadJSON("jcl-catalog.json");
const dataDict = loadJSON("data-dictionary.json");

console.log("");

// Validate each file
const programIds = programs ? validatePrograms(programs) : null;
const copybookNames = copybooks ? validateCopybooks(copybooks) : null;
const jclResult = jclJobs ? validateJcl(jclJobs) : null;
const dataNames = dataDict ? validateDataDictionary(dataDict) : null;

console.log("");

// Cross-reference validation
validateCrossReferences(programs, copybooks, jclJobs, dataDict, programIds, copybookNames);

// Summary
console.log("");
console.log("═".repeat(60));
console.log("Validation Summary");
console.log("═".repeat(60));
console.log(`  Errors:   ${totalErrors}`);
console.log(`  Warnings: ${totalWarnings}`);
console.log("");

if (totalErrors > 0) {
  console.log("❌ Validation FAILED");
  process.exit(1);
} else if (totalWarnings > 0) {
  console.log("⚠️  Validation PASSED with warnings");
  process.exit(0);
} else {
  console.log("✅ Validation PASSED");
  process.exit(0);
}
