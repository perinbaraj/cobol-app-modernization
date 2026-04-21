      ******************************************************************
      * COPYBOOK: CUST-REC
      * PURPOSE: Customer master record layout
      * USED BY: CUSTMGMT, CUSTINQ, CUSTUPD, CUSTDEL, CUSTRPT
      ******************************************************************
       01  CUST-DATA.
           05 CUST-ID             PIC X(10).
           05 CUST-NAME           PIC X(30).
           05 CUST-ADDR           PIC X(40).
           05 CUST-CITY           PIC X(20).
           05 CUST-STATE          PIC X(2).
           05 CUST-ZIP            PIC X(10).
           05 CUST-BALANCE        PIC S9(7)V99 COMP-3.
           05 CUST-STATUS         PIC X(1).
              88 CUST-ACTIVE      VALUE 'A'.
              88 CUST-INACTIVE    VALUE 'I'.
              88 CUST-SUSPENDED   VALUE 'S'.
           05 CUST-LAST-UPDATE    PIC 9(8).
           05 CUST-PHONE-COUNT    PIC 9(2).
           05 CUST-PHONES OCCURS 3 TIMES.
              10 PHONE-TYPE       PIC X(1).
                 88 PHONE-HOME    VALUE 'H'.
                 88 PHONE-WORK    VALUE 'W'.
                 88 PHONE-MOBILE  VALUE 'M'.
              10 PHONE-NUMBER     PIC X(15).
           05 FILLER              PIC X(23).
