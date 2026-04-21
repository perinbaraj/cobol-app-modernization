# Prompt Library: Phase 4 — React UI Modernization

> Prompts for converting BMS/CICS screens to React 18.2 TypeScript components.

---

## Prompt 1: BMS Map to React Component

```
Convert this BMS screen map to a React 18.2 TypeScript component.

BMS field mapping:
| BMS Attribute | React Element |
|---------------|---------------|
| ATTRB=(UNPROT) | <input> (editable) |
| ATTRB=(UNPROT,NUM) | <input type="number"> |
| ATTRB=(ASKIP) | <span> (display-only) |
| ATTRB=(PROT) | <input disabled> |
| ATTRB=(BRT) | className="font-bold" |
| ATTRB=(DRK) | className="hidden" |
| POS=(row,col) | CSS Grid positioning |
| LENGTH=n | maxLength={n} |
| INITIAL='text' | Default value or label |
| PICIN/PICOUT | Input mask / display format |

Requirements:
- React 18.2 with TypeScript
- Tailwind CSS for styling
- Zod schema for form validation
- Custom hook for API integration
- Accessible (WCAG 2.1): aria-labels, keyboard navigation
- Responsive design (not fixed 24x80 grid)
- Error display area matching MSG field position
```

---

## Prompt 2: CICS Transaction Flow to React Router

```
Convert this CICS transaction navigation flow to React Router v6:

[Paste CICS transaction IDs and their navigation relationships]

For each CICS transaction:
1. Create a route path
2. Map CICS SEND MAP to component render
3. Map CICS RECEIVE MAP to form submission handler
4. Map CICS XCTL to navigate()
5. Map CICS LINK to component composition
6. Map CICS RETURN TRANSID to route redirect

Generate:
- createBrowserRouter configuration
- Layout component with navigation
- Breadcrumb component
- Protected routes (where CICS checks security)
```

---

## Prompt 3: PF Key to Action Bar

```
Convert these CICS PF key assignments to a React action bar component:

[Paste EVALUATE EIBAID block from CICS program]

Generate:
1. An ActionBar component with buttons matching PF key functions
2. useHotkeys hook for keyboard shortcuts (F1-F12 mapping)
3. Tooltip showing keyboard shortcut on each button
4. Responsive layout: horizontal bar on desktop, hamburger menu on mobile
5. Consistent styling with the rest of the application

PF key conventions:
- PF1 = Help → Opens help panel
- PF3 = Exit → Navigate back
- PF5 = Refresh → Reload data
- PF7 = Page Up → Previous page
- PF8 = Page Down → Next page
- PF12 = Cancel → Discard changes
- ENTER = Submit → Form submission
- CLEAR = Reset → Clear form
```

---

## Prompt 4: API Integration Hook

```
Generate a React custom hook for integrating with this REST API:

[Paste OpenAPI spec or list of endpoints]

Requirements:
- Use React Query (TanStack Query) for server state management
- Generate TypeScript types from the API response schemas
- Include loading, error, and success states
- Add optimistic updates for mutations
- Include proper error handling with user-friendly messages
- Add request/response interceptors for auth tokens
- Cache configuration: staleTime, gcTime based on data volatility

Output:
1. TypeScript interface for all request/response types
2. Custom hook: use[Domain]Api()
3. Query key factory for cache management
4. Error boundary component for API failures
```

---

## Prompt 5: Form Validation from BMS

```
Generate Zod validation schemas from these BMS field definitions:

[Paste BMS DFHMDF field definitions]

Mapping rules:
| BMS | Zod |
|-----|-----|
| LENGTH=n | z.string().max(n) |
| ATTRB=(UNPROT,NUM) | z.coerce.number() |
| ATTRB=(UNPROT) required | z.string().min(1, 'Required') |
| PICIN='9(5)' | z.string().regex(/^\d{1,5}$/) |
| PICIN='X(10)' | z.string().max(10) |
| PICIN='99/99/9999' | z.string().date() |

Generate:
1. Zod schema object
2. TypeScript inferred type
3. React Hook Form integration with zodResolver
4. Inline error message components
```

---

## Prompt 6: Screen-to-Component Batch Conversion

```markdown
## Convert BMS Screen Set: [TRANSACTION_NAME]

### Context
This is a set of related CICS screens for the [DOMAIN] workflow.

### Screens to Convert
| Screen | BMS Map | Purpose |
|--------|---------|---------|
| [List all screens in the transaction] |

### Generate for Each Screen
1. React component (TypeScript + Tailwind)
2. Zod validation schema
3. API integration hook
4. Unit test (React Testing Library)
5. Storybook story

### Generate Shared Components
1. Layout wrapper matching the CICS screen template
2. Action bar with PF key equivalents
3. Message display area (info, warning, error)
4. Loading spinner overlay
5. Session timeout handler (matching CICS timeout)

### Navigation
- Generate React Router configuration
- Add breadcrumbs
- Add keyboard navigation (Tab order matching BMS field order)
```
