       IDENTIFICATION DIVISION.
       PROGRAM-ID. CUSTVAL.
       AUTHOR. ACME-CORP-MAINFRAME-TEAM.
      *================================================================
      * CUSTVAL - CUSTOMER RECORD VALIDATION ROUTINE
      *================================================================
      * PURPOSE: Validate a customer record before write/rewrite.
      *          Called by CUSTMGMT paragraph 5000-POPULATE-RECORD
      *          and executed as STEP2 in CUSTBAT.jcl.
      *
      * PARAMETERS (LINKAGE SECTION):
      *   LS-CUSTOMER-RECORD  - Customer record to validate
      *   LS-RETURN-CODE      - Return code (0=OK, see codes below)
      *   LS-ERROR-MSG        - Error message if validation fails
      *
      * RETURN CODES:
      *   0000 - Validation passed
      *   5001 - INVALID-CUST-ID    (Customer ID is spaces)
      *   5002 - INVALID-STATUS     (Status not A, I, or S)
      *   5003 - NEGATIVE-BALANCE   (Balance is negative)
      *
      * CALLED BY: CUSTMGMT.cbl (CALL 'CUSTVAL')
      *            CUSTBAT.jcl  (EXEC PGM=CUSTVAL, STEP2)
      *================================================================
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. IBM-Z15.
       OBJECT-COMPUTER. IBM-Z15.
      *
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-VALID-STATUSES.
           05  WS-STATUS-ACTIVE     PIC X(1) VALUE 'A'.
           05  WS-STATUS-INACTIVE   PIC X(1) VALUE 'I'.
           05  WS-STATUS-SUSPENDED  PIC X(1) VALUE 'S'.
      *
       LINKAGE SECTION.
       01  LS-CUSTOMER-RECORD.
           COPY CUST-REC.
       01  LS-RETURN-CODE           PIC 9(4).
       01  LS-ERROR-MSG             PIC X(50).
      *
       PROCEDURE DIVISION USING LS-CUSTOMER-RECORD
                                  LS-RETURN-CODE
                                  LS-ERROR-MSG.
      *
       0000-MAIN.
           PERFORM 0100-INITIALIZE
           PERFORM 1000-VALIDATE-CUST-ID
           IF LS-RETURN-CODE = 0
               PERFORM 2000-VALIDATE-STATUS
           END-IF
           IF LS-RETURN-CODE = 0
               PERFORM 3000-VALIDATE-BALANCE
           END-IF
           GOBACK.
      *
       0100-INITIALIZE.
           MOVE 0    TO LS-RETURN-CODE
           MOVE SPACES TO LS-ERROR-MSG.
      *
      *-----------------------------------------------------------
      * 1000-VALIDATE-CUST-ID
      * Verifies CUST-ID is not blank/spaces.
      * Maps to: Java @NotBlank on CustomerRequest.customerId
      *-----------------------------------------------------------
       1000-VALIDATE-CUST-ID.
           IF CUST-ID = SPACES
               MOVE 5001 TO LS-RETURN-CODE
               MOVE 'INVALID-CUST-ID: Customer ID cannot be spaces'
                   TO LS-ERROR-MSG
           END-IF.
      *
      *-----------------------------------------------------------
      * 2000-VALIDATE-STATUS
      * Verifies CUST-STATUS is one of the valid 88-level values.
      * Maps to: Java @NotNull CustomerStatus enum (A=ACTIVE,
      *          I=INACTIVE, S=SUSPENDED)
      *-----------------------------------------------------------
       2000-VALIDATE-STATUS.
           IF CUST-STATUS NOT = WS-STATUS-ACTIVE
           AND CUST-STATUS NOT = WS-STATUS-INACTIVE
           AND CUST-STATUS NOT = WS-STATUS-SUSPENDED
               MOVE 5002 TO LS-RETURN-CODE
               MOVE 'INVALID-STATUS: Must be A (Active), I (Inactive)'
                   TO LS-ERROR-MSG
               STRING 'INVALID-STATUS: Must be A, I, or S. Got: ['
                      CUST-STATUS ']'
                   DELIMITED SIZE
                   INTO LS-ERROR-MSG
           END-IF.
      *
      *-----------------------------------------------------------
      * 3000-VALIDATE-BALANCE
      * Verifies CUST-BALANCE is not negative.
      * Maps to: Java @DecimalMin("0.00") on CustomerRequest
      *-----------------------------------------------------------
       3000-VALIDATE-BALANCE.
           IF CUST-BALANCE < 0
               MOVE 5003 TO LS-RETURN-CODE
               MOVE 'NEGATIVE-BALANCE: Balance cannot be negative'
                   TO LS-ERROR-MSG
           END-IF.
      *
       END PROGRAM CUSTVAL.
