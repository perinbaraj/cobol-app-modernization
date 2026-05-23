---
name: COBOL to Java Conversion
about: Request conversion of a COBOL program to Java Spring Boot service
title: '[COBOL→Java] Convert {PROGRAM_NAME}'
labels: ['migration', 'cobol-to-java', 'phase-2']
assignees: ''
---

## COBOL Program Details

**Program Name:** <!-- e.g., CUSTMGMT -->
**Source File:** <!-- e.g., samples/cobol/CUSTMGMT.cbl -->

### Related Copybooks
<!-- List all copybooks used by this program -->
- [ ] <!-- e.g., CUST-REC.cpy -->
- [ ] <!-- Add more as needed -->

### Related Files (VSAM/DB2)
<!-- List data files or tables accessed -->
- <!-- e.g., PROD.CUSTOMER.MASTER (VSAM KSDS) -->

---

## Target Java Configuration

**Domain:** <!-- e.g., Customer Management -->
**Target Package:** <!-- e.g., com.acme.customer -->
**Target Database:** <!-- Azure SQL / MongoDB / Both -->

---

## Requirements

### Functional Requirements
<!-- What business function does this program perform? -->
1. 
2. 

### Non-Functional Requirements
- [ ] REST API required
- [ ] Batch processing required
- [ ] Real-time processing required
- [ ] Integration with existing services

---

## Acceptance Criteria

### Code Quality
- [ ] Java 17 features used (records, sealed classes, switch expressions)
- [ ] BigDecimal used for all decimal/financial fields (no double/float)
- [ ] Spring Boot conventions followed (@Service, @Repository, etc.)
- [ ] SLF4J logging implemented
- [ ] OpenAPI annotations on all REST endpoints

### Testing
- [ ] Unit tests with JUnit 5 + Mockito (90%+ coverage)
- [ ] Integration tests with TestContainers
- [ ] Parity tests comparing Java output to COBOL reference output
- [ ] All tests pass

### Documentation
- [ ] Javadoc references original COBOL paragraphs
- [ ] README.md updated with new service
- [ ] API documentation generated

---

## Reference Materials
<!-- Optional: Link to any additional context -->
- Analysis doc: 
- Business requirements: 
- Related PRs: 

---

## Agent Instructions

@cobol-to-java-converter Please convert this program following all conversion rules.
Include: service, repository, controller, DTOs, entities, exception handling, and tests.
