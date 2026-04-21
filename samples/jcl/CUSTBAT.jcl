//CUSTBAT  JOB (ACCT),'CUSTOMER BATCH',CLASS=A,MSGCLASS=X,
//         NOTIFY=&SYSUID,TIME=(,30)
//*
//* DAILY CUSTOMER BATCH PROCESSING
//* SCHEDULE: DAILY 06:00 UTC
//* SLA: COMPLETE BY 08:00 UTC
//* DEPENDENCIES: CUSTEXTRACT MUST COMPLETE FIRST
//*
//*-----------------------------------------------------------
//* STEP 1: LOAD DAILY CUSTOMER UPDATES
//*-----------------------------------------------------------
//STEP1    EXEC PGM=CUSTLOAD
//STEPLIB  DD DSN=PROD.LOADLIB,DISP=SHR
//INPUT    DD DSN=PROD.CUST.DAILY.UPDATES,DISP=SHR
//MASTER   DD DSN=PROD.CUST.MASTER,DISP=OLD
//SYSOUT   DD SYSOUT=*
//SYSPRINT DD SYSOUT=*
//*
//*-----------------------------------------------------------
//* STEP 2: VALIDATE CUSTOMER DATA
//*-----------------------------------------------------------
//STEP2    EXEC PGM=CUSTVAL,COND=(0,NE,STEP1)
//STEPLIB  DD DSN=PROD.LOADLIB,DISP=SHR
//INPUT    DD DSN=PROD.CUST.MASTER,DISP=SHR
//ERRFILE  DD DSN=PROD.CUST.ERRORS,DISP=(NEW,CATLG),
//            SPACE=(CYL,(5,5)),DCB=(RECFM=FB,LRECL=250)
//SYSOUT   DD SYSOUT=*
//*
//*-----------------------------------------------------------
//* STEP 3: GENERATE DAILY REPORT
//*-----------------------------------------------------------
//STEP3    EXEC PGM=CUSTRPT,COND=(4,LT,STEP2)
//STEPLIB  DD DSN=PROD.LOADLIB,DISP=SHR
//INPUT    DD DSN=PROD.CUST.MASTER,DISP=SHR
//REPORT   DD SYSOUT=*,DCB=(RECFM=FBA,LRECL=133)
//SYSOUT   DD SYSOUT=*
