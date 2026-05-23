       IDENTIFICATION DIVISION.
       PROGRAM-ID. CUSTRPT.
       AUTHOR. ACME-CORP-MAINFRAME-TEAM.
      *================================================================
      * CUSTRPT - DAILY CUSTOMER REPORT GENERATOR
      *================================================================
      * PURPOSE: Read the customer master VSAM KSDS file and produce
      *          a formatted FBA 133-byte report with totals by status.
      *          Executed as STEP3 in CUSTBAT.jcl.
      *
      * JCL DD STATEMENTS:
      *   INPUT  - PROD.CUST.MASTER   (VSAM KSDS, DISP=SHR)
      *   REPORT - SYSOUT=*           (FBA, LRECL=133)
      *
      * RETURN CODES:
      *   0  - Report produced successfully
      *   4  - No records found (triggers COND=(4,LT,STEP2) skip)
      *
      * CALLED BY: CUSTBAT.jcl (EXEC PGM=CUSTRPT, STEP3)
      *            COND=(4,LT,STEP2) — only runs if STEP2 rc < 4
      *================================================================
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-Z15.
       OBJECT-COMPUTER. IBM-Z15.
      *
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT MASTER-FILE
               ASSIGN TO INPUT
               ORGANIZATION IS INDEXED
               ACCESS MODE  IS SEQUENTIAL
               RECORD KEY   IS CUST-ID
               FILE STATUS  IS WS-MASTER-STATUS.
      *
           SELECT REPORT-FILE
               ASSIGN TO REPORT
               ORGANIZATION IS SEQUENTIAL
               FILE STATUS  IS WS-REPORT-STATUS.
      *
       DATA DIVISION.
       FILE SECTION.
       FD  MASTER-FILE.
       01  MASTER-RECORD.
           COPY CUST-REC.
      *
       FD  REPORT-FILE
           RECORDING MODE IS F
           BLOCK CONTAINS 0 RECORDS.
       01  REPORT-LINE              PIC X(133).
      *
       WORKING-STORAGE SECTION.
       01  WS-MASTER-STATUS         PIC X(2) VALUE '00'.
           88  WS-MASTER-OK         VALUE '00'.
           88  WS-MASTER-EOF        VALUE '10'.
       01  WS-REPORT-STATUS         PIC X(2) VALUE '00'.
       01  WS-RETURN-CODE           PIC 9(1) VALUE 0.
      *
      *--- Report counters ---
       01  WS-TOTAL-RECORDS         PIC 9(7) VALUE 0.
       01  WS-ACTIVE-COUNT          PIC 9(7) VALUE 0.
       01  WS-INACTIVE-COUNT        PIC 9(7) VALUE 0.
       01  WS-SUSPENDED-COUNT       PIC 9(7) VALUE 0.
       01  WS-TOTAL-BALANCE         PIC S9(15)V99 COMP-3 VALUE 0.
      *
      *--- Report layout (FBA, LRECL=133, ASA control character) ---
       01  WS-REPORT-HEADER.
           05  WS-ASA-H             PIC X(1)  VALUE '1'.
           05  FILLER               PIC X(20) VALUE SPACES.
           05  FILLER               PIC X(40)
               VALUE 'ACME CORP — DAILY CUSTOMER MASTER REPORT'.
           05  FILLER               PIC X(20) VALUE SPACES.
           05  WS-HEADER-DATE       PIC X(10) VALUE SPACES.
           05  FILLER               PIC X(42) VALUE SPACES.
      *
       01  WS-REPORT-COLHDR.
           05  WS-ASA-C             PIC X(1)  VALUE ' '.
           05  FILLER               PIC X(10) VALUE 'CUST-ID   '.
           05  FILLER               PIC X(30) VALUE 'NAME                          '.
           05  FILLER               PIC X(2)  VALUE 'ST'.
           05  FILLER               PIC X(12) VALUE '     BALANCE'.
           05  FILLER               PIC X(8)  VALUE '  STATUS'.
           05  FILLER               PIC X(70) VALUE SPACES.
      *
       01  WS-DETAIL-LINE.
           05  WS-ASA-D             PIC X(1)  VALUE ' '.
           05  WS-DL-CUST-ID        PIC X(10) VALUE SPACES.
           05  FILLER               PIC X(1)  VALUE SPACES.
           05  WS-DL-CUST-NAME      PIC X(30) VALUE SPACES.
           05  FILLER               PIC X(1)  VALUE SPACES.
           05  WS-DL-STATE          PIC X(2)  VALUE SPACES.
           05  FILLER               PIC X(1)  VALUE SPACES.
           05  WS-DL-BALANCE        PIC Z,ZZZ,ZZ9.99 VALUE 0.
           05  FILLER               PIC X(2)  VALUE SPACES.
           05  WS-DL-STATUS         PIC X(8)  VALUE SPACES.
           05  FILLER               PIC X(66) VALUE SPACES.
      *
       01  WS-SUMMARY-LINE.
           05  WS-ASA-S             PIC X(1)  VALUE '0'.
           05  FILLER               PIC X(20) VALUE 'TOTALS:'.
           05  FILLER               PIC X(20) VALUE 'TOTAL RECORDS: '.
           05  WS-SL-TOTAL          PIC ZZZ,ZZ9 VALUE 0.
           05  FILLER               PIC X(2)  VALUE SPACES.
           05  FILLER               PIC X(10) VALUE 'ACTIVE: '.
           05  WS-SL-ACTIVE         PIC ZZZ,ZZ9 VALUE 0.
           05  FILLER               PIC X(2)  VALUE SPACES.
           05  FILLER               PIC X(11) VALUE 'INACTIVE: '.
           05  WS-SL-INACTIVE       PIC ZZZ,ZZ9 VALUE 0.
           05  FILLER               PIC X(2)  VALUE SPACES.
           05  FILLER               PIC X(12) VALUE 'SUSPENDED: '.
           05  WS-SL-SUSPENDED      PIC ZZZ,ZZ9 VALUE 0.
           05  FILLER               PIC X(26) VALUE SPACES.
      *
       01  WS-BALANCE-LINE.
           05  WS-ASA-B             PIC X(1)  VALUE ' '.
           05  FILLER               PIC X(20) VALUE SPACES.
           05  FILLER               PIC X(15) VALUE 'TOTAL BALANCE: '.
           05  WS-BL-BALANCE     PIC ZZ,ZZZ,ZZZ,ZZ9.99 VALUE 0.
           05  FILLER               PIC X(84) VALUE SPACES.
      *
       PROCEDURE DIVISION.
      *
       0000-MAIN.
           PERFORM 1000-OPEN-FILES
           IF WS-RETURN-CODE = 0
               PERFORM 2000-WRITE-HEADERS
               PERFORM 3000-PROCESS-RECORDS
                   UNTIL WS-MASTER-EOF
               PERFORM 4000-WRITE-SUMMARY
               PERFORM 5000-CLOSE-FILES
           END-IF
           MOVE WS-RETURN-CODE TO RETURN-CODE
           GOBACK.
      *
      *-----------------------------------------------------------
      * 1000-OPEN-FILES
      *-----------------------------------------------------------
       1000-OPEN-FILES.
           OPEN INPUT MASTER-FILE
           IF NOT WS-MASTER-OK
               DISPLAY 'CUSTRPT: FAILED TO OPEN MASTER-FILE '
                       WS-MASTER-STATUS
               MOVE 8 TO WS-RETURN-CODE
               GOBACK
           END-IF
           OPEN OUTPUT REPORT-FILE
           IF WS-REPORT-STATUS NOT = '00'
               DISPLAY 'CUSTRPT: FAILED TO OPEN REPORT-FILE '
                       WS-REPORT-STATUS
               MOVE 8 TO WS-RETURN-CODE
               CLOSE MASTER-FILE
               GOBACK
           END-IF.
      *
      *-----------------------------------------------------------
      * 2000-WRITE-HEADERS
      * Write page header and column headers (ASA control chars)
      *-----------------------------------------------------------
       2000-WRITE-HEADERS.
           MOVE FUNCTION CURRENT-DATE(1:10) TO WS-HEADER-DATE
           WRITE REPORT-LINE FROM WS-REPORT-HEADER
           WRITE REPORT-LINE FROM WS-REPORT-COLHDR.
      *
      *-----------------------------------------------------------
      * 3000-PROCESS-RECORDS
      * Read one master record, print detail line, accumulate totals
      * Maps to: Spring Batch ItemReader + ItemProcessor + ItemWriter
      *-----------------------------------------------------------
       3000-PROCESS-RECORDS.
           READ MASTER-FILE NEXT RECORD
               AT END
                   SET WS-MASTER-EOF TO TRUE
               NOT AT END
                   ADD 1 TO WS-TOTAL-RECORDS
                   ADD CUST-BALANCE TO WS-TOTAL-BALANCE
                   EVALUATE CUST-STATUS
                       WHEN 'A'
                           ADD 1 TO WS-ACTIVE-COUNT
                           MOVE 'ACTIVE  ' TO WS-DL-STATUS
                       WHEN 'I'
                           ADD 1 TO WS-INACTIVE-COUNT
                           MOVE 'INACTIVE' TO WS-DL-STATUS
                       WHEN 'S'
                           ADD 1 TO WS-SUSPENDED-COUNT
                           MOVE 'SUSPNDED' TO WS-DL-STATUS
                       WHEN OTHER
                           MOVE 'UNKNOWN ' TO WS-DL-STATUS
                   END-EVALUATE
                   MOVE CUST-ID      TO WS-DL-CUST-ID
                   MOVE CUST-NAME    TO WS-DL-CUST-NAME
                   MOVE CUST-STATE   TO WS-DL-STATE
                   MOVE CUST-BALANCE TO WS-DL-BALANCE
                   WRITE REPORT-LINE FROM WS-DETAIL-LINE
           END-READ.
      *
      *-----------------------------------------------------------
      * 4000-WRITE-SUMMARY
      * If no records found return code 4 (no-data condition).
      * The JCL COND=(4,LT,STEP2) means STEP3 only runs if STEP2 < 4.
      *-----------------------------------------------------------
       4000-WRITE-SUMMARY.
           IF WS-TOTAL-RECORDS = 0
               DISPLAY 'CUSTRPT: NO RECORDS FOUND IN MASTER FILE'
               MOVE 4 TO WS-RETURN-CODE
               GOBACK
           END-IF
           MOVE WS-TOTAL-RECORDS    TO WS-SL-TOTAL
           MOVE WS-ACTIVE-COUNT     TO WS-SL-ACTIVE
           MOVE WS-INACTIVE-COUNT   TO WS-SL-INACTIVE
           MOVE WS-SUSPENDED-COUNT  TO WS-SL-SUSPENDED
           WRITE REPORT-LINE FROM WS-SUMMARY-LINE
           MOVE WS-TOTAL-BALANCE    TO WS-BL-BALANCE
           WRITE REPORT-LINE FROM WS-BALANCE-LINE
           DISPLAY 'CUSTRPT: REPORT COMPLETE. RECORDS=' WS-TOTAL-RECORDS.
      *
       5000-CLOSE-FILES.
           CLOSE MASTER-FILE
           CLOSE REPORT-FILE.
      *
       END PROGRAM CUSTRPT.
