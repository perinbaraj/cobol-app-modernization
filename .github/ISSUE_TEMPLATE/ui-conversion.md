---
name: BMS to React Conversion
about: Request conversion of a BMS screen to React TypeScript component
title: '[BMS→React] Convert {SCREEN_NAME}'
labels: ['migration', 'ui-conversion', 'phase-4']
assignees: ''
---

## BMS Screen Details

**Map Name:** <!-- e.g., CUSTINQ -->
**Source File:** <!-- e.g., samples/bms/CUSTINQ.bms -->

### Related CICS Transaction
**Transaction ID:** <!-- e.g., CINQ -->
**CICS Program:** <!-- e.g., CUSTPGM.cbl -->

### Screen Type
- [ ] Inquiry (read-only)
- [ ] Data entry (create)
- [ ] Update (edit existing)
- [ ] Delete confirmation
- [ ] Menu/Navigation
- [ ] Report display
- [ ] Other: <!-- specify -->

---

## Target React Configuration

**Domain:** <!-- e.g., Customer Management -->
**Component Path:** <!-- e.g., src/components/customer/ -->
**Route:** <!-- e.g., /customers/:id -->

### Backend API
**API Endpoint:** <!-- e.g., /api/customers/{id} -->
**HTTP Methods:** 
- [ ] GET (read)
- [ ] POST (create)
- [ ] PUT (update)
- [ ] DELETE (delete)

---

## Screen Layout

### Fields to Convert
<!-- List all input/output fields from the BMS map -->

| Field Name | Type | Length | Required | Notes |
|------------|------|--------|----------|-------|
| <!-- e.g., CUSTID --> | <!-- input/display --> | <!-- 10 --> | <!-- Yes/No --> | <!-- Primary key --> |
| | | | | |

### PF Key Mappings
<!-- List PF keys used in this screen -->

| PF Key | Action | React Implementation |
|--------|--------|---------------------|
| ENTER | Submit | Form submit button |
| PF3 | Exit | Navigate back |
| PF5 | Refresh | React Query refetch |
| | | |

---

## Requirements

### Functional Requirements
1. 
2. 

### UX Requirements
- [ ] Mobile responsive
- [ ] Dark mode support
- [ ] Keyboard navigation
- [ ] Screen reader accessible (WCAG 2.1 AA)

---

## Acceptance Criteria

### Component Quality
- [ ] React 18.2 with TypeScript (strict mode)
- [ ] Tailwind CSS for styling (no CSS modules)
- [ ] React Hook Form + Zod for validation
- [ ] React Query for server state
- [ ] Proper loading and error states

### Accessibility
- [ ] All inputs have associated labels
- [ ] Proper heading hierarchy
- [ ] Keyboard navigable
- [ ] Color contrast meets WCAG AA

### Testing
- [ ] Unit tests with React Testing Library
- [ ] Storybook story for visual testing
- [ ] Integration with backend API tested

### Documentation
- [ ] Component props documented
- [ ] Storybook demonstrates all states
- [ ] Route registered in app routes

---

## Reference Materials
<!-- Optional: Link to any additional context -->
- Screen screenshot: 
- UX mockup: 
- Backend API PR: 

---

## Agent Instructions

@react-scaffolder Convert this BMS screen to a React component.
Include: component, tests, Storybook story, custom hook, and Zod schema.
