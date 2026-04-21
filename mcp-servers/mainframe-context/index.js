#!/usr/bin/env node
/**
 * Mainframe Context MCP Server
 *
 * Provides GitHub Copilot with queryable access to mainframe codebase metadata.
 * See mcp-servers/mainframe-context-server.md for full documentation.
 *
 * Usage:
 *   node index.js
 *
 * Environment variables (optional):
 *   INVENTORY_PATH         - Path to program-inventory.json
 *   COPYBOOK_CATALOG_PATH  - Path to copybook-catalog.json
 *   JCL_CATALOG_PATH       - Path to jcl-catalog.json
 *   DATA_DICTIONARY_PATH   - Path to data-dictionary.json
 */

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";

const __dirname = dirname(fileURLToPath(import.meta.url));

function loadJSON(envVar, defaultPath) {
  const filePath = resolve(__dirname, process.env[envVar] || defaultPath);
  try {
    return JSON.parse(readFileSync(filePath, "utf-8"));
  } catch (e) {
    console.error(`Warning: Could not load ${filePath}: ${e.message}`);
    return [];
  }
}

const programs = loadJSON("INVENTORY_PATH", "../../data/program-inventory.json");
const copybooks = loadJSON("COPYBOOK_CATALOG_PATH", "../../data/copybook-catalog.json");
const jclJobs = loadJSON("JCL_CATALOG_PATH", "../../data/jcl-catalog.json");
const dataDictionary = loadJSON("DATA_DICTIONARY_PATH", "../../data/data-dictionary.json");

const programIndex = Object.fromEntries(programs.map((p) => [p.programId, p]));
const copybookIndex = Object.fromEntries(copybooks.map((c) => [c.name, c]));
const jclIndex = Object.fromEntries(jclJobs.map((j) => [j.jobName, j]));
const dataIndex = Object.fromEntries(dataDictionary.map((d) => [d.name, d]));

const server = new McpServer({
  name: "mainframe-context",
  version: "1.0.0",
  description: "Provides mainframe codebase context for COBOL-to-Java migration",
});

// Tool 1: get_program_info
server.tool(
  "get_program_info",
  "Get detailed information about a COBOL program including dependencies, business domain, complexity, and migration status.",
  { program_id: z.string().describe("COBOL program ID (e.g., 'CUSTMGMT')") },
  async ({ program_id }) => {
    const prog = programIndex[program_id.toUpperCase()];
    if (!prog) {
      return { content: [{ type: "text", text: `Program '${program_id}' not found. Available: ${Object.keys(programIndex).join(", ")}` }] };
    }
    const jclRefs = jclJobs.filter((j) => j.programs?.includes(prog.programId)).map((j) => j.jobName);
    return { content: [{ type: "text", text: JSON.stringify({ ...prog, jclJobs: jclRefs }, null, 2) }] };
  }
);

// Tool 2: get_copybook_info
server.tool(
  "get_copybook_info",
  "Get copybook details including which programs use it.",
  { copybook_name: z.string().describe("Copybook name (e.g., 'CUST-REC')") },
  async ({ copybook_name }) => {
    const cpy = copybookIndex[copybook_name.toUpperCase()];
    if (!cpy) {
      return { content: [{ type: "text", text: `Copybook '${copybook_name}' not found. Available: ${Object.keys(copybookIndex).join(", ")}` }] };
    }
    return { content: [{ type: "text", text: JSON.stringify(cpy, null, 2) }] };
  }
);

// Tool 3: get_call_graph
server.tool(
  "get_call_graph",
  "Get the CALL dependency graph for a program, traversing N levels deep.",
  {
    program_id: z.string().describe("Starting program ID"),
    depth: z.number().default(3).describe("Levels to traverse (default: 3)"),
  },
  async ({ program_id, depth }) => {
    const graph = {};
    const visited = new Set();
    function traverse(progId, currentDepth) {
      if (currentDepth > depth || visited.has(progId)) return;
      visited.add(progId);
      const prog = programIndex[progId.toUpperCase()];
      if (!prog) return;
      graph[progId] = { calls: prog.calls || [], calledBy: prog.calledBy || [], copybooks: prog.copybooks || [] };
      for (const target of prog.calls || []) traverse(target, currentDepth + 1);
    }
    traverse(program_id.toUpperCase(), 0);
    return { content: [{ type: "text", text: JSON.stringify({ rootProgram: program_id, depth, nodesTraversed: visited.size, graph }, null, 2) }] };
  }
);

// Tool 4: get_migration_cluster
server.tool(
  "get_migration_cluster",
  "Get all programs that should be migrated together (shared copybooks + call dependencies).",
  { program_id: z.string().describe("Any program in the cluster") },
  async ({ program_id }) => {
    const cluster = new Set();
    const queue = [program_id.toUpperCase()];
    while (queue.length > 0) {
      const current = queue.shift();
      if (cluster.has(current)) continue;
      const prog = programIndex[current];
      if (!prog) continue;
      cluster.add(current);
      for (const cpy of prog.copybooks || []) {
        const cpyInfo = copybookIndex[cpy];
        if (cpyInfo) for (const user of cpyInfo.usedBy || []) if (!cluster.has(user)) queue.push(user);
      }
      for (const c of [...(prog.calls || []), ...(prog.calledBy || [])]) if (!cluster.has(c)) queue.push(c);
    }
    const clusterPrograms = [...cluster].map((id) => {
      const p = programIndex[id];
      return p ? { programId: p.programId, complexity: p.complexity, loc: p.linesOfCode } : { programId: id, complexity: null, loc: null };
    });
    return {
      content: [{
        type: "text",
        text: JSON.stringify({
          seedProgram: program_id,
          clusterSize: cluster.size,
          totalLOC: clusterPrograms.reduce((sum, p) => sum + (p.loc || 0), 0),
          programs: clusterPrograms,
        }, null, 2),
      }],
    };
  }
);

// Tool 5: search_programs
server.tool(
  "search_programs",
  "Search programs by business domain, complexity, migration status, or name pattern.",
  {
    domain: z.string().optional().describe("Business domain filter"),
    complexity_min: z.number().optional().describe("Minimum complexity (1-5)"),
    complexity_max: z.number().optional().describe("Maximum complexity (1-5)"),
    status: z.enum(["pending", "in_progress", "done"]).optional(),
    pattern: z.string().optional().describe("Program name substring match"),
    limit: z.number().default(20).describe("Max results"),
  },
  async ({ domain, complexity_min, complexity_max, status, pattern, limit }) => {
    let results = programs;
    if (domain) results = results.filter((p) => p.businessDomain?.toLowerCase().includes(domain.toLowerCase()));
    if (complexity_min !== undefined) results = results.filter((p) => p.complexity >= complexity_min);
    if (complexity_max !== undefined) results = results.filter((p) => p.complexity <= complexity_max);
    if (status) results = results.filter((p) => p.migrationStatus === status);
    if (pattern) { const pat = pattern.toUpperCase(); results = results.filter((p) => p.programId.includes(pat)); }
    const limited = results.slice(0, limit).map((p) => ({
      programId: p.programId, domain: p.businessDomain, complexity: p.complexity, loc: p.linesOfCode, status: p.migrationStatus,
    }));
    return { content: [{ type: "text", text: JSON.stringify({ totalMatches: results.length, showing: limited.length, results: limited }, null, 2) }] };
  }
);

// Tool 6: get_data_dictionary
server.tool(
  "get_data_dictionary",
  "Get data dictionary entry for a VSAM file or DB2 table. Pass '*' to list all entries.",
  { name: z.string().describe("VSAM cluster or DB2 table name. Use '*' to list all.") },
  async ({ name }) => {
    if (name === "*") {
      const summary = dataDictionary.map((d) => ({ name: d.name, type: d.type, target: d.targetTable }));
      return { content: [{ type: "text", text: JSON.stringify({ totalEntries: summary.length, entries: summary }, null, 2) }] };
    }
    const entry = dataIndex[name.toUpperCase()];
    if (!entry) {
      const available = Object.keys(dataIndex).join(", ") || "(empty)";
      return { content: [{ type: "text", text: `Entry '${name}' not found. Available: ${available}` }] };
    }
    return { content: [{ type: "text", text: JSON.stringify(entry, null, 2) }] };
  }
);

// Resource: inventory summary
server.resource("inventory-summary", "mainframe://summary", async () => {
  const totalLOC = programs.reduce((sum, p) => sum + (p.linesOfCode || 0), 0);
  const byStatus = programs.reduce((acc, p) => { acc[p.migrationStatus] = (acc[p.migrationStatus] || 0) + 1; return acc; }, {});
  const byDomain = programs.reduce((acc, p) => { const d = p.businessDomain || "UNCLASSIFIED"; acc[d] = (acc[d] || 0) + 1; return acc; }, {});
  return {
    contents: [{
      uri: "mainframe://summary",
      mimeType: "application/json",
      text: JSON.stringify({ totalPrograms: programs.length, totalJCLJobs: jclJobs.length, totalCopybooks: copybooks.length, totalDataEntries: dataDictionary.length, totalLinesOfCode: totalLOC, migrationStatus: byStatus, businessDomains: byDomain }, null, 2),
    }],
  };
});

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("Mainframe Context MCP Server running (stdio)");
  console.error(`Loaded: ${programs.length} programs, ${jclJobs.length} JCL jobs, ${copybooks.length} copybooks, ${dataDictionary.length} data entries`);
}

main().catch(console.error);
