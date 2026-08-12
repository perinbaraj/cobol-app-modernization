import re

content = """//CUSTBAT  JOB (ACCT),'CUSTOMER BATCH',CLASS=A,MSGCLASS=X,
//         NOTIFY=&SYSUID,TIME=(,30)
//*
//* DAILY CUSTOMER BATCH PROCESSING
//* SCHEDULE: DAILY 06:00 UTC
//* SLA: COMPLETE BY 08:00 UTC
//* DEPENDENCIES: CUSTEXTRACT MUST COMPLETE FIRST
//*               JOBB; JOBC,
//*               NONE
//* AFTER: JOBX
//*
//*-----------------------------------------------------------
//* STEP 1: LOAD DAILY CUSTOMER UPDATES
//*-----------------------------------------------------------
//STEP1    EXEC PGM=CUSTLOAD
//STEPLIB  DD DSN=PROD.LOADLIB,DISP=SHR
//STEPX    EXEC PROC1,COND=(0,NE)
//STEPY    EXEC PROC=PROC2,PARM='XYZ'
"""

procedures = []
dependencies = []
in_deps = False

for line in content.split('\n'):
    line_upper = line.strip().upper()
    
    # Process procedures
    if line_upper.startswith('//') and not line_upper.startswith('//*'):
        match = re.match(r'^//[A-Z0-9@#$]*\s+EXEC\s+(.*)', line_upper)
        if match:
            rest = match.group(1).strip()
            if not rest.startswith('PGM='):
                if rest.startswith('PROC='):
                    proc_name = re.split(r'[, \t]', rest[5:])[0]
                else:
                    proc_name = re.split(r'[, \t]', rest)[0]
                
                if re.match(r'^[A-Z][A-Z0-9@#$]{0,7}$', proc_name):
                    if proc_name not in procedures:
                        procedures.append(proc_name)
    
    # Process dependencies
    if line.startswith('//*') or line.startswith('*'):
        # remove the comment prefix
        text = line.lstrip('/*').strip().upper()
        
        match = re.match(r'^(?:DEPENDENCIES|DEPENDENCY|AFTER|PRED|PREDECESSOR)S?:(.*)', text)
        if match:
            in_deps = True
            deps_text = match.group(1).strip()
        elif in_deps:
            if re.match(r'^[A-Z0-9_]+:', text):
                in_deps = False
                continue
            else:
                deps_text = text
        else:
            continue

        if in_deps and deps_text:
            entries = re.split(r'[,;]', deps_text)
            for entry in entries:
                entry = entry.strip()
                if not entry:
                    continue
                tokens = entry.split()
                if not tokens:
                    continue
                first_token = tokens[0]
                if first_token in ['NONE', 'N/A', 'NA', 'N']:
                    continue
                if re.match(r'^[A-Z][A-Z0-9#@$]{0,7}$', first_token):
                    if first_token not in dependencies:
                        dependencies.append(first_token)
    else:
        in_deps = False

print("Procedures:", procedures)
print("Dependencies:", dependencies)
