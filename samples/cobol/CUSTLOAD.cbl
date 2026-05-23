       IDENTIFICATION DIVISION.
       PROGRAM-ID. CUSTLOAD.
       AUTHOR. ACME-CORP-MAINFRAME-TEAM.
      *================================================================
      * CUSTLOAD - DAILY CUSTOMER UPDATE LOADER
      *================================================================
      * PURPOSE: Read daily customer updates from a sequential flat
      *          file (INPUT) and load them into the VSAM KSDS master
      *          file (MASTER). Executed as STEP1 in CUSTBAT.jcl.
      *
      * JCL DD STATEMENTS:
      *   INPUT  - PROD.CUST.DAILY.UPDATES (sequential, DISP=SHR)
      *   MASTER - PROD.CUST.MASTER        (VSAM KSDS, DISP=OLD)
      *
      * RETURN CODES:
      *   0  - All records loaded successfully
      *   4  - Some records skipped (duplicates encountered)
      *   8  - Fatal I/O error; batch abended
      *
      * CALLED BY: CUSTBAT.jcl (EXEC PGM=CUSTLOAD, STEP1)
      * NEXT STEP: CUSTVAL (COND=(0,NE,STEP1) — runs if this rc=0)
      *================================================================
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-Z15.
       OBJECT-COMPUTER. IBM-Z15.
      *
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT INPUT-FILE
               ASSIGN TO INPUT
               ORGANIZATION IS SEQUENTIAL
               ACCESS MODE  IS SEQUENTIAL
               FILE STATUS  IS WS-INPUT-STATUS.
      *
           SELECT MASTER-FILE
               ASSIGN TO MASTER
               ORGANIZATION IS INDEXED
               ACCESS MODE  IS RANDOM
               RECORD KEY   IS CUST-ID
               FILE STATUS  IS WS-MASTER-STATUS.
      *
       DATA DIVISION.
       FILE SECTION.
       FD  INPUT-FILE
           RECORDING MODE IS F
           BLOCK CONTAINS 0 RECORDS.
       01  INPUT-RECORD.
           COPY CUST-REC.
      *
       FD  MASTER-FILE.
       01  MASTER-RECORD.
           COPY CUST-REC.
      *
       WORKING-STORAGE SECTION.
       01  WS-INPUT-STATUS          PIC X(2) VALUE '00'.
           88  WS-INPUT-OK          VALUE '00'.
           88  WS-INPUT-EOF         VALUE '10'.
       01  WS-MASTER-STATUS         PIC X(2) VALUE '00'.
           88  WS-MASTER-OK         VALUE '00'.
           88  WS-MASTER-DUPLICATE  VALUE '22'.
       01  WS-RETURN-CODE           PIC 9(1) VALUE 0.
       01  WS-RECORDS-READ          PIC 9(7) VALUE 0.
       01  WS-RECORDS-LOADED        PIC 9(7) VALUE 0.
       01  WS-RECORDS-SKIPPED       PIC 9(7) VALUE 0.
      *
       PROCEDURE DIVISION.
      *
       0000-MAIN.
           PERFORM 1000-OPEN-FILES
           IF WS-RETURN-CODE = 0
               PERFORM 2000-LOAD-RECORDS
                   UNTIL WS-INPUT-EOF
               PERFORM 3000-CLOSE-FILES
           END-IF
           DISPLAY 'CUSTLOAD: READ='    WS-RECORDS-READ
                   ' LOADED='          WS-RECORDS-LOADED
                   ' SKIPPED='         WS-RECORDS-SKIPPED
           MOVE WS-RETURN-CODE TO RETURN-CODE
           GOBACK.
      *
      *-----------------------------------------------------------
      * 1000-OPEN-FILES
      * Opens INPUT (sequential) and MASTER (VSAM KSDS).
      * Maps to: Spring Batch ItemReader/ItemWriter open()
      *-----------------------------------------------------------
       1000-OPEN-FILES.
           OPEN INPUT INPUT-FILE
           IF NOT WS-INPUT-OK
               DISPLAY 'CUSTLOAD: FAILED TO OPEN INPUT-FILE '
                       WS-INPUT-STATUS
               MOVE 8 TO WS-RETURN-CODE
               GOBACK
           END-IF
           OPEN I-O MASTER-FILE
           IF NOT WS-MASTER-OK
               DISPLAY 'CUSTLOAD: FAILED TO OPEN MASTER-FILE '
                       WS-MASTER-STATUS
               MOVE 8 TO WS-RETURN-CODE
               CLOSE INPUT-FILE
               GOBACK
           END-IF.
      *
      *-----------------------------------------------------------
      * 2000-LOAD-RECORDS
      * Read one record from INPUT and write to MASTER.
      * On DUPLICATE KEY (22): log and skip (return code 4).
      * Maps to: Spring Batch ItemProcessor + ItemWriter
      *-----------------------------------------------------------
       2000-LOAD-RECORDS.
           READ INPUT-FILE INTO INPUT-RECORD
               AT END
                   SET WS-INPUT-EOF TO TRUE
               NOT AT END
                   ADD 1 TO WS-RECORDS-READ
                   MOVE INPUT-RECORD TO MASTER-RECORD
                   WRITE MASTER-RECORD
                       INVALID KEY
                           IF WS-MASTER-DUPLICATE
                               DISPLAY 'CUSTLOAD: DUPLICATE KEY '
                                       CUST-ID ' - SKIPPING'
                               ADD 1 TO WS-RECORDS-SKIPPED
                               MOVE 4 TO WS-RETURN-CODE
                           ELSE
                               DISPLAY 'CUSTLOAD: WRITE ERROR '
                                       WS-MASTER-STATUS ' FOR '
                                       CUST-ID
                               MOVE 8 TO WS-RETURN-CODE
                               PERFORM 3000-CLOSE-FILES
                               GOBACK
                           END-IF
                       NOT INVALID KEY
                           ADD 1 TO WS-RECORDS-LOADED
                   END-WRITE
           END-READ.
      *
       3000-CLOSE-FILES.
           CLOSE INPUT-FILE
           CLOSE MASTER-FILE.
      *
       END PROGRAM CUSTLOAD.
