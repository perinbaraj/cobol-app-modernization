#!/usr/bin/env python3
"""
COBOL Codebase Scanner

Production-grade scanner that extracts metadata from COBOL, JCL, and BMS files
and generates JSON catalogs for the mainframe-context MCP server.

Usage:
    python scan-codebase.py --cobol-dir samples/cobol --jcl-dir samples/jcl --bms-dir samples/bms --output-dir data

Output Files:
    - program-inventory.json: COBOL program metadata including dependencies
    - copybook-catalog.json: Copybook usage across programs
    - jcl-catalog.json: JCL job metadata including steps and datasets
    - data-dictionary.json: File/dataset definitions with record layouts
"""

import argparse
import json
import logging
import os
import re
import sys
from collections import defaultdict
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Set, Tuple

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)

# File extension patterns (case-insensitive)
COBOL_EXTENSIONS = {'.cbl', '.cob', '.CBL', '.COB'}
COPYBOOK_EXTENSIONS = {'.cpy', '.CPY'}
JCL_EXTENSIONS = {'.jcl', '.JCL'}
BMS_EXTENSIONS = {'.bms', '.BMS'}

# Regex patterns for COBOL parsing
PATTERNS = {
    'program_id': re.compile(r'^\s*PROGRAM-ID\.\s*([A-Z0-9-]+)', re.IGNORECASE | re.MULTILINE),
    'call': re.compile(r'\bCALL\s+[\'"]([A-Z0-9-]+)[\'"]', re.IGNORECASE),
    'copy': re.compile(r'\bCOPY\s+([A-Z0-9-]+)', re.IGNORECASE),
    'exec_sql': re.compile(r'EXEC\s+SQL\s+(.*?)END-EXEC', re.IGNORECASE | re.DOTALL),
    'exec_cics': re.compile(r'EXEC\s+CICS\s+(\w+)', re.IGNORECASE),
    'select_assign': re.compile(r'SELECT\s+([A-Z0-9-]+)\s+ASSIGN\s+TO\s+[\'"]?([A-Z0-9\.-]+)[\'"]?', re.IGNORECASE),
    'sql_table': re.compile(r'\b(?:FROM|INTO|UPDATE|DELETE\s+FROM|INSERT\s+INTO)\s+([A-Z0-9_]+)', re.IGNORECASE),
    'fd_file': re.compile(r'^\s*FD\s+([A-Z0-9-]+)', re.IGNORECASE | re.MULTILINE),
}

# JCL regex patterns
JCL_PATTERNS = {
    'job_card': re.compile(r'^//([A-Z0-9]+)\s+JOB\s+', re.IGNORECASE | re.MULTILINE),
    'exec_pgm': re.compile(r'EXEC\s+PGM=([A-Z0-9]+)', re.IGNORECASE),
    'exec_proc': re.compile(r'EXEC\s+([A-Z0-9]+)(?:,|$|\s)', re.IGNORECASE),
    'dd_dsn': re.compile(r'DSN=([A-Z0-9\.\&]+)', re.IGNORECASE),
    'step': re.compile(r'^//([A-Z0-9]+)\s+EXEC\s+', re.IGNORECASE | re.MULTILINE),
    'comment_schedule': re.compile(r'\*\s*SCHEDULE:\s*(.+)', re.IGNORECASE),
    'comment_sla': re.compile(r'\*\s*SLA:\s*(.+)', re.IGNORECASE),
    'comment_dependencies': re.compile(r'\*\s*DEPENDENCIES?:\s*(.+)', re.IGNORECASE),
}

# BMS regex patterns
BMS_PATTERNS = {
    'map_name': re.compile(r'^([A-Z0-9]+)\s+DFHMDI\s+', re.IGNORECASE | re.MULTILINE),
    'mapset_name': re.compile(r'^([A-Z0-9]+)\s+DFHMSD\s+', re.IGNORECASE | re.MULTILINE),
    'field_def': re.compile(r'^([A-Z0-9]+)\s+DFHMDF\s+.*?LENGTH=(\d+)', re.IGNORECASE | re.MULTILINE),
}


class CobolScanner:
    """Scanner for COBOL source files."""

    def __init__(self):
        self.programs: Dict[str, Dict[str, Any]] = {}
        self.copybooks: Dict[str, Set[str]] = defaultdict(set)
        self.files_referenced: Dict[str, Dict[str, Any]] = {}

    def scan_file(self, filepath: Path) -> Optional[Dict[str, Any]]:
        """Scan a single COBOL file and extract metadata."""
        logger.debug(f"Scanning COBOL file: {filepath}")
        
        try:
            content = filepath.read_text(encoding='utf-8', errors='replace')
        except Exception as e:
            logger.error(f"Failed to read {filepath}: {e}")
            return None

        lines = content.split('\n')
        loc = len([l for l in lines if l.strip() and not l.strip().startswith('*')])

        # Extract PROGRAM-ID
        match = PATTERNS['program_id'].search(content)
        program_id = match.group(1).upper() if match else filepath.stem.upper()

        # Extract CALL statements
        calls = list(set(m.upper() for m in PATTERNS['call'].findall(content)))

        # Extract COPY statements
        copybooks = list(set(m.upper() for m in PATTERNS['copy'].findall(content)))
        for cpy in copybooks:
            self.copybooks[cpy].add(program_id)

        # Extract EXEC SQL statements and table names
        sql_statements = PATTERNS['exec_sql'].findall(content)
        sql_tables = set()
        for stmt in sql_statements:
            tables = PATTERNS['sql_table'].findall(stmt)
            sql_tables.update(t.upper() for t in tables)
        sql_tables = sorted(sql_tables)

        # Extract EXEC CICS commands
        cics_commands = list(set(m.upper() for m in PATTERNS['exec_cics'].findall(content)))

        # Extract SELECT/ASSIGN file references
        file_refs = []
        for match in PATTERNS['select_assign'].finditer(content):
            file_name = match.group(1).upper()
            assign_to = match.group(2).upper()
            file_refs.append(assign_to)
            self.files_referenced[assign_to] = {
                'name': assign_to,
                'internal_name': file_name,
                'source_program': program_id,
            }

        # Also check FD statements for file definitions
        fd_files = [m.upper() for m in PATTERNS['fd_file'].findall(content)]

        # Calculate complexity score (1-5)
        complexity = self._calculate_complexity(content, calls, sql_tables, cics_commands, loc)

        program_data = {
            'programId': program_id,
            'fileName': filepath.name,
            'linesOfCode': loc,
            'businessDomain': self._infer_domain(program_id, content),
            'complexity': complexity,
            'calls': sorted(calls),
            'calledBy': [],  # Will be populated in build_reverse_call_graph
            'copybooks': sorted(copybooks),
            'files': sorted(file_refs),
            'sqlTables': sql_tables,
            'cicsCommands': sorted(cics_commands),
            'migrationStatus': 'pending',
        }

        self.programs[program_id] = program_data
        logger.info(f"  Scanned program: {program_id} ({loc} LOC, complexity={complexity})")
        return program_data

    def _calculate_complexity(self, content: str, calls: List[str], sql_tables: List[str], 
                             cics_commands: List[str], loc: int) -> int:
        """Calculate complexity score from 1 (simple) to 5 (very complex)."""
        score = 1

        # LOC factor
        if loc > 500:
            score += 1
        if loc > 1000:
            score += 1

        # Dependency factor
        if len(calls) > 3:
            score += 1
        
        # SQL factor
        if sql_tables:
            score += 1

        # CICS factor
        if cics_commands:
            score += 1

        # Nested IF/PERFORM factor
        nested_count = len(re.findall(r'\bPERFORM\b.*\bUNTIL\b', content, re.IGNORECASE))
        nested_count += len(re.findall(r'\bEVALUATE\s+TRUE\b', content, re.IGNORECASE))
        if nested_count > 5:
            score += 1

        return min(score, 5)

    def _infer_domain(self, program_id: str, content: str) -> str:
        """Infer business domain from program name and content."""
        program_id = program_id.upper()
        content_upper = content.upper()

        domain_indicators = {
            'Customer Management': ['CUST', 'CLIENT', 'CUSTOMER'],
            'Account Management': ['ACCT', 'ACCOUNT', 'BALANCE'],
            'Transaction Processing': ['TRANS', 'TXN', 'PAYMENT'],
            'Reporting': ['RPT', 'REPORT', 'PRINT'],
            'Batch Processing': ['BAT', 'BATCH', 'LOAD'],
            'Validation': ['VAL', 'VALID', 'CHECK', 'EDIT'],
            'Menu/Navigation': ['MENU', 'NAV', 'MAIN'],
            'File Maintenance': ['MAINT', 'FILE', 'UPDATE'],
            'Inquiry': ['INQ', 'INQUIRY', 'QUERY', 'SEARCH'],
        }

        for domain, keywords in domain_indicators.items():
            for kw in keywords:
                if kw in program_id or kw in content_upper[:2000]:
                    return domain

        return 'General'

    def build_reverse_call_graph(self):
        """Populate calledBy field for all programs."""
        for prog_id, prog_data in self.programs.items():
            for called_prog in prog_data['calls']:
                if called_prog in self.programs:
                    if prog_id not in self.programs[called_prog]['calledBy']:
                        self.programs[called_prog]['calledBy'].append(prog_id)
        
        # Sort calledBy lists
        for prog_data in self.programs.values():
            prog_data['calledBy'] = sorted(prog_data['calledBy'])

    def get_copybook_catalog(self) -> List[Dict[str, Any]]:
        """Generate copybook catalog with usage information."""
        catalog = []
        for name, users in sorted(self.copybooks.items()):
            catalog.append({
                'name': name,
                'usedBy': sorted(users),
                'usageCount': len(users),
            })
        return catalog

    def get_program_inventory(self) -> List[Dict[str, Any]]:
        """Get sorted program inventory."""
        return [self.programs[k] for k in sorted(self.programs.keys())]


class JclScanner:
    """Scanner for JCL files."""

    def __init__(self):
        self.jobs: Dict[str, Dict[str, Any]] = {}

    def scan_file(self, filepath: Path) -> Optional[Dict[str, Any]]:
        """Scan a single JCL file and extract metadata."""
        logger.debug(f"Scanning JCL file: {filepath}")
        
        try:
            content = filepath.read_text(encoding='utf-8', errors='replace')
        except Exception as e:
            logger.error(f"Failed to read {filepath}: {e}")
            return None

        # Extract job name
        match = JCL_PATTERNS['job_card'].search(content)
        job_name = match.group(1).upper() if match else filepath.stem.upper()

        # Extract programs executed
        programs = []
        for match in JCL_PATTERNS['exec_pgm'].finditer(content):
            pgm = match.group(1).upper()
            if pgm not in programs:
                programs.append(pgm)

        # Extract procedures (EXEC without PGM=)
        procedures = []
        for line in content.split('\n'):
            if 'EXEC' in line.upper() and 'PGM=' not in line.upper():
                match = re.search(r'EXEC\s+([A-Z0-9]+)', line, re.IGNORECASE)
                if match:
                    proc = match.group(1).upper()
                    if proc not in procedures and proc not in ['PROC']:
                        procedures.append(proc)

        # Extract datasets
        datasets = list(set(m.upper() for m in JCL_PATTERNS['dd_dsn'].findall(content) 
                           if not m.startswith('&')))

        # Count steps
        steps = JCL_PATTERNS['step'].findall(content)
        step_count = len(steps)

        # Extract schedule from comments
        schedule_match = JCL_PATTERNS['comment_schedule'].search(content)
        schedule = schedule_match.group(1).strip() if schedule_match else None

        # Extract SLA from comments
        sla_match = JCL_PATTERNS['comment_sla'].search(content)
        sla = sla_match.group(1).strip() if sla_match else None

        # Extract dependencies from comments
        dependencies = []
        dep_match = JCL_PATTERNS['comment_dependencies'].search(content)
        if dep_match:
            dep_str = dep_match.group(1).strip()
            dependencies = [d.strip().upper() for d in re.split(r'[,\s]+', dep_str) if d.strip()]

        job_data = {
            'jobName': job_name,
            'fileName': filepath.name,
            'programs': sorted(programs),
            'procedures': sorted(procedures),
            'datasets': sorted(datasets),
            'stepCount': step_count,
            'schedule': schedule,
            'sla': sla,
            'dependencies': sorted(dependencies),
            'migrationStatus': 'pending',
        }

        self.jobs[job_name] = job_data
        logger.info(f"  Scanned JCL job: {job_name} ({step_count} steps, {len(programs)} programs)")
        return job_data

    def get_jcl_catalog(self) -> List[Dict[str, Any]]:
        """Get sorted JCL catalog."""
        return [self.jobs[k] for k in sorted(self.jobs.keys())]


class BmsScanner:
    """Scanner for BMS map files."""

    def __init__(self):
        self.maps: Dict[str, Dict[str, Any]] = {}

    def scan_file(self, filepath: Path) -> Optional[Dict[str, Any]]:
        """Scan a single BMS file and extract metadata."""
        logger.debug(f"Scanning BMS file: {filepath}")
        
        try:
            content = filepath.read_text(encoding='utf-8', errors='replace')
        except Exception as e:
            logger.error(f"Failed to read {filepath}: {e}")
            return None

        # Extract mapset name
        mapset_match = BMS_PATTERNS['mapset_name'].search(content)
        mapset_name = mapset_match.group(1).upper() if mapset_match else filepath.stem.upper()

        # Extract map names
        map_names = [m.upper() for m in BMS_PATTERNS['map_name'].findall(content)]

        # Extract field definitions
        fields = []
        for match in BMS_PATTERNS['field_def'].finditer(content):
            fields.append({
                'name': match.group(1).upper(),
                'length': int(match.group(2)),
            })

        map_data = {
            'mapsetName': mapset_name,
            'fileName': filepath.name,
            'maps': sorted(map_names),
            'fieldCount': len(fields),
            'fields': fields,
            'migrationStatus': 'pending',
        }

        self.maps[mapset_name] = map_data
        logger.info(f"  Scanned BMS mapset: {mapset_name} ({len(map_names)} maps, {len(fields)} fields)")
        return map_data


def build_data_dictionary(cobol_scanner: CobolScanner, jcl_scanner: JclScanner) -> List[Dict[str, Any]]:
    """Build data dictionary from scanned files and datasets."""
    data_dict: Dict[str, Dict[str, Any]] = {}

    # Collect all referenced files from COBOL programs
    for prog_id, prog_data in cobol_scanner.programs.items():
        for file_ref in prog_data['files']:
            if file_ref not in data_dict:
                data_dict[file_ref] = {
                    'name': file_ref,
                    'type': 'Unknown',
                    'copybook': None,
                    'keyFields': [],
                    'recordSize': None,
                    'estimatedRecords': None,
                    'targetDatabase': 'Azure SQL',
                    'targetTable': file_ref.split('.')[-1].lower().replace('-', '_'),
                    'referencedBy': [],
                }
            if prog_id not in data_dict[file_ref]['referencedBy']:
                data_dict[file_ref]['referencedBy'].append(prog_id)

    # Collect datasets from JCL
    for job_name, job_data in jcl_scanner.jobs.items():
        for ds in job_data['datasets']:
            if ds not in data_dict:
                data_dict[ds] = {
                    'name': ds,
                    'type': 'Unknown',
                    'copybook': None,
                    'keyFields': [],
                    'recordSize': None,
                    'estimatedRecords': None,
                    'targetDatabase': 'Azure SQL',
                    'targetTable': ds.split('.')[-1].lower().replace('-', '_'),
                    'referencedBy': [],
                }
            if job_name not in data_dict[ds]['referencedBy']:
                data_dict[ds]['referencedBy'].append(job_name)

    # Infer file types
    for name, entry in data_dict.items():
        name_upper = name.upper()
        if 'VSAM' in name_upper or 'MASTER' in name_upper:
            entry['type'] = 'VSAM-KSDS'
        elif 'ERROR' in name_upper or 'LOG' in name_upper:
            entry['type'] = 'Sequential'
        elif 'UPDATE' in name_upper or 'DAILY' in name_upper:
            entry['type'] = 'Sequential'
        elif 'LOADLIB' in name_upper:
            entry['type'] = 'PDS-LoadLib'
        else:
            entry['type'] = 'Sequential'
        
        # Sort referencedBy
        entry['referencedBy'] = sorted(entry['referencedBy'])

    return [data_dict[k] for k in sorted(data_dict.keys())]


def find_files(directory: Path, extensions: Set[str]) -> List[Path]:
    """Find all files with given extensions (case-insensitive)."""
    if not directory.exists():
        logger.warning(f"Directory does not exist: {directory}")
        return []
    
    files = []
    for ext in extensions:
        # Handle both cases
        files.extend(directory.glob(f'*{ext}'))
        files.extend(directory.glob(f'*{ext.lower()}'))
        files.extend(directory.glob(f'*{ext.upper()}'))
    
    # Remove duplicates while preserving order
    seen = set()
    unique_files = []
    for f in files:
        if f not in seen:
            seen.add(f)
            unique_files.append(f)
    
    return sorted(unique_files)


def write_json(data: Any, filepath: Path):
    """Write JSON with deterministic ordering."""
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, sort_keys=True, ensure_ascii=False)
    logger.info(f"  Written: {filepath}")


def main():
    parser = argparse.ArgumentParser(
        description='Scan COBOL, JCL, and BMS files to generate metadata catalogs.',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
    python scan-codebase.py --cobol-dir samples/cobol --output-dir data
    python scan-codebase.py --cobol-dir src/cobol --jcl-dir src/jcl --bms-dir src/bms -o output
        """
    )
    parser.add_argument('--cobol-dir', '-c', type=Path, default=Path('samples/cobol'),
                        help='Directory containing COBOL source files (.cbl, .cob, .cpy)')
    parser.add_argument('--jcl-dir', '-j', type=Path, default=Path('samples/jcl'),
                        help='Directory containing JCL files (.jcl)')
    parser.add_argument('--bms-dir', '-b', type=Path, default=Path('samples/bms'),
                        help='Directory containing BMS map files (.bms)')
    parser.add_argument('--output-dir', '-o', type=Path, default=Path('data'),
                        help='Output directory for JSON catalogs')
    parser.add_argument('--verbose', '-v', action='store_true',
                        help='Enable verbose debug logging')
    
    args = parser.parse_args()

    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)

    logger.info("=" * 60)
    logger.info("COBOL Codebase Scanner")
    logger.info("=" * 60)
    logger.info(f"Start time: {datetime.now().isoformat()}")
    logger.info(f"COBOL directory: {args.cobol_dir}")
    logger.info(f"JCL directory: {args.jcl_dir}")
    logger.info(f"BMS directory: {args.bms_dir}")
    logger.info(f"Output directory: {args.output_dir}")
    logger.info("")

    # Create output directory
    args.output_dir.mkdir(parents=True, exist_ok=True)

    # Initialize scanners
    cobol_scanner = CobolScanner()
    jcl_scanner = JclScanner()
    bms_scanner = BmsScanner()

    # Scan COBOL programs
    logger.info("Scanning COBOL programs...")
    cobol_files = find_files(args.cobol_dir, COBOL_EXTENSIONS)
    for filepath in cobol_files:
        cobol_scanner.scan_file(filepath)
    
    # Scan copybooks
    logger.info("Scanning copybooks...")
    copybook_files = find_files(args.cobol_dir, COPYBOOK_EXTENSIONS)
    for filepath in copybook_files:
        # Add copybook to catalog if not already referenced
        cpy_name = filepath.stem.upper()
        if cpy_name not in cobol_scanner.copybooks:
            cobol_scanner.copybooks[cpy_name] = set()
        logger.info(f"  Found copybook: {cpy_name}")

    # Build reverse call graph
    logger.info("Building reverse call graph...")
    cobol_scanner.build_reverse_call_graph()

    # Scan JCL
    logger.info("Scanning JCL jobs...")
    jcl_files = find_files(args.jcl_dir, JCL_EXTENSIONS)
    for filepath in jcl_files:
        jcl_scanner.scan_file(filepath)

    # Scan BMS
    logger.info("Scanning BMS maps...")
    bms_files = find_files(args.bms_dir, BMS_EXTENSIONS)
    for filepath in bms_files:
        bms_scanner.scan_file(filepath)

    # Generate outputs
    logger.info("")
    logger.info("Generating output files...")
    
    program_inventory = cobol_scanner.get_program_inventory()
    copybook_catalog = cobol_scanner.get_copybook_catalog()
    jcl_catalog = jcl_scanner.get_jcl_catalog()
    data_dictionary = build_data_dictionary(cobol_scanner, jcl_scanner)

    write_json(program_inventory, args.output_dir / 'program-inventory.json')
    write_json(copybook_catalog, args.output_dir / 'copybook-catalog.json')
    write_json(jcl_catalog, args.output_dir / 'jcl-catalog.json')
    write_json(data_dictionary, args.output_dir / 'data-dictionary.json')

    # Summary
    logger.info("")
    logger.info("=" * 60)
    logger.info("Scan Summary")
    logger.info("=" * 60)
    logger.info(f"  Programs scanned: {len(program_inventory)}")
    logger.info(f"  Copybooks found: {len(copybook_catalog)}")
    logger.info(f"  JCL jobs scanned: {len(jcl_catalog)}")
    logger.info(f"  Data entries: {len(data_dictionary)}")
    logger.info(f"  End time: {datetime.now().isoformat()}")
    logger.info("")
    logger.info("Scan complete!")

    return 0


if __name__ == '__main__':
    sys.exit(main())
