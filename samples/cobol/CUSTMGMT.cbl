      ******************************************************************
      * PROGRAM-ID: CUSTMGMT
      * AUTHOR: MIGRATION SAMPLE
      * DATE-WRITTEN: 2024-01-15
      * PURPOSE: Customer Management - CRUD operations for customer
      *          master file. Sample program for migration toolkit
      *          validation.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CUSTMGMT.
       AUTHOR. MIGRATION-SAMPLE.

       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT CUSTOMER-FILE
               ASSIGN TO 'PROD.CUST.MASTER'
               ORGANIZATION IS INDEXED
               ACCESS MODE IS DYNAMIC
               RECORD KEY IS CUST-ID
               FILE STATUS IS WS-FILE-STATUS.

       DATA DIVISION.
       FILE SECTION.
       FD  CUSTOMER-FILE.
       01  CUSTOMER-RECORD.
           COPY CUST-REC.

       WORKING-STORAGE SECTION.
       01  WS-FILE-STATUS        PIC X(2).
           88 WS-SUCCESS          VALUE '00'.
           88 WS-EOF              VALUE '10'.
           88 WS-NOT-FOUND        VALUE '23'.
           88 WS-DUPLICATE        VALUE '22'.

       01  WS-TRANSACTION-TYPE   PIC X(1).
           88 WS-ADD              VALUE 'A'.
           88 WS-UPDATE           VALUE 'U'.
           88 WS-DELETE           VALUE 'D'.
           88 WS-INQUIRY          VALUE 'I'.

       01  WS-RETURN-CODE        PIC 9(4) VALUE 0.
       01  WS-ERROR-MSG          PIC X(80) VALUE SPACES.
       01  WS-TOTAL-BALANCE      PIC S9(9)V99 COMP-3 VALUE 0.
       01  WS-DISCOUNT-RATE      PIC 9V9(4) COMP-3 VALUE 0.0500.
       01  WS-DISCOUNT-AMOUNT    PIC S9(7)V99 COMP-3 VALUE 0.

       LINKAGE SECTION.
       01  LS-REQUEST.
           05 LS-TRANS-TYPE      PIC X(1).
           05 LS-CUST-ID         PIC X(10).
           05 LS-CUST-DATA.
              10 LS-CUST-NAME    PIC X(30).
              10 LS-CUST-ADDR    PIC X(40).
              10 LS-CUST-CITY    PIC X(20).
              10 LS-CUST-STATE   PIC X(2).
              10 LS-CUST-ZIP     PIC X(10).
              10 LS-CUST-BAL     PIC S9(7)V99 COMP-3.
              10 LS-CUST-STATUS  PIC X(1).

       PROCEDURE DIVISION USING LS-REQUEST.

       0000-MAIN.
           OPEN I-O CUSTOMER-FILE
           IF NOT WS-SUCCESS
               MOVE 'FAILED TO OPEN CUSTOMER FILE' TO WS-ERROR-MSG
               MOVE 9999 TO WS-RETURN-CODE
               GOBACK
           END-IF

           MOVE LS-TRANS-TYPE TO WS-TRANSACTION-TYPE

           EVALUATE TRUE
               WHEN WS-ADD
                   PERFORM 1000-ADD-CUSTOMER
               WHEN WS-UPDATE
                   PERFORM 2000-UPDATE-CUSTOMER
               WHEN WS-DELETE
                   PERFORM 3000-DELETE-CUSTOMER
               WHEN WS-INQUIRY
                   PERFORM 4000-INQUIRE-CUSTOMER
               WHEN OTHER
                   MOVE 'INVALID TRANSACTION TYPE' TO WS-ERROR-MSG
                   MOVE 1001 TO WS-RETURN-CODE
           END-EVALUATE

           CLOSE CUSTOMER-FILE
           GOBACK.

       1000-ADD-CUSTOMER.
           MOVE LS-CUST-ID TO CUST-ID
           READ CUSTOMER-FILE
               INVALID KEY CONTINUE
               NOT INVALID KEY
                   MOVE 'CUSTOMER ALREADY EXISTS' TO WS-ERROR-MSG
                   MOVE 1002 TO WS-RETURN-CODE
                   GO TO 1000-EXIT
           END-READ

           PERFORM 5000-POPULATE-RECORD
           PERFORM 6000-APPLY-DISCOUNT

           WRITE CUSTOMER-RECORD
           IF NOT WS-SUCCESS
               MOVE 'WRITE FAILED' TO WS-ERROR-MSG
               MOVE 1003 TO WS-RETURN-CODE
           END-IF.
       1000-EXIT.
           EXIT.

       2000-UPDATE-CUSTOMER.
           MOVE LS-CUST-ID TO CUST-ID
           READ CUSTOMER-FILE
               INVALID KEY
                   MOVE 'CUSTOMER NOT FOUND' TO WS-ERROR-MSG
                   MOVE 2001 TO WS-RETURN-CODE
                   GO TO 2000-EXIT
           END-READ

           PERFORM 5000-POPULATE-RECORD
           PERFORM 6000-APPLY-DISCOUNT

           REWRITE CUSTOMER-RECORD
           IF NOT WS-SUCCESS
               MOVE 'REWRITE FAILED' TO WS-ERROR-MSG
               MOVE 2002 TO WS-RETURN-CODE
           END-IF.
       2000-EXIT.
           EXIT.

       3000-DELETE-CUSTOMER.
           MOVE LS-CUST-ID TO CUST-ID
           READ CUSTOMER-FILE
               INVALID KEY
                   MOVE 'CUSTOMER NOT FOUND' TO WS-ERROR-MSG
                   MOVE 3001 TO WS-RETURN-CODE
                   GO TO 3000-EXIT
           END-READ

           DELETE CUSTOMER-FILE
           IF NOT WS-SUCCESS
               MOVE 'DELETE FAILED' TO WS-ERROR-MSG
               MOVE 3002 TO WS-RETURN-CODE
           END-IF.
       3000-EXIT.
           EXIT.

       4000-INQUIRE-CUSTOMER.
           MOVE LS-CUST-ID TO CUST-ID
           READ CUSTOMER-FILE
               INVALID KEY
                   MOVE 'CUSTOMER NOT FOUND' TO WS-ERROR-MSG
                   MOVE 4001 TO WS-RETURN-CODE
                   GO TO 4000-EXIT
           END-READ

           MOVE CUST-NAME     TO LS-CUST-NAME
           MOVE CUST-ADDR     TO LS-CUST-ADDR
           MOVE CUST-CITY     TO LS-CUST-CITY
           MOVE CUST-STATE    TO LS-CUST-STATE
           MOVE CUST-ZIP      TO LS-CUST-ZIP
           MOVE CUST-BALANCE  TO LS-CUST-BAL
           MOVE CUST-STATUS   TO LS-CUST-STATUS.
       4000-EXIT.
           EXIT.

       5000-POPULATE-RECORD.
           MOVE LS-CUST-NAME    TO CUST-NAME
           MOVE LS-CUST-ADDR    TO CUST-ADDR
           MOVE LS-CUST-CITY    TO CUST-CITY
           MOVE LS-CUST-STATE   TO CUST-STATE
           MOVE LS-CUST-ZIP     TO CUST-ZIP
           MOVE LS-CUST-BAL     TO CUST-BALANCE
           MOVE LS-CUST-STATUS  TO CUST-STATUS

           CALL 'CUSTVAL' USING CUSTOMER-RECORD
                                WS-RETURN-CODE
                                WS-ERROR-MSG.

       6000-APPLY-DISCOUNT.
           IF CUST-BALANCE > 10000.00
               COMPUTE WS-DISCOUNT-AMOUNT =
                   CUST-BALANCE * WS-DISCOUNT-RATE
               SUBTRACT WS-DISCOUNT-AMOUNT FROM CUST-BALANCE
                   ON SIZE ERROR
                       MOVE 'DISCOUNT CALC OVERFLOW' TO WS-ERROR-MSG
                       MOVE 6001 TO WS-RETURN-CODE
               END-SUBTRACT
           END-IF.
