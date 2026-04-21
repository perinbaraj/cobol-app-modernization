# Phase 4: UI Modernization — BMS/CICS Screens → React 18.2

## Objective
Convert mainframe BMS screen maps and CICS transaction programs into modern React 18.2 TypeScript components with Tailwind CSS, connected to the Java REST APIs built in Phase 2.

---

## Inputs
| Input | Source | Description |
|-------|--------|-------------|
| BMS map definitions | Source repo | Screen layouts, field definitions |
| CICS transaction programs | Source repo | Screen handling logic |
| REST APIs | Phase 2 | Backend endpoints to integrate |
| OpenAPI specs | Phase 2 | API contracts for type generation |

## Outputs
| Output | Format | Description |
|--------|--------|-------------|
| React components | TypeScript + JSX | One component per screen |
| Custom hooks | TypeScript | API integration hooks |
| Form schemas | Zod | Validation rules from BMS field definitions |
| Route definitions | React Router v6 | Navigation matching CICS transaction flow |
| API client | Generated from OpenAPI | Type-safe API calls |

---

## GitHub Copilot Features Used

### 1. `react-scaffolder` Custom Agent
Converts BMS screen definitions to React components.

**Example invocation:**
```
@react-scaffolder Convert this BMS map to a React 18.2 TypeScript component:

CUSTINQ  DFHMSD TYPE=MAP,MODE=INOUT,LANG=COBOL,STORAGE=AUTO
         DFHMDI SIZE=(24,80),LINE=1,COLUMN=1
TITLE    DFHMDF POS=(1,25),LENGTH=30,ATTRB=(ASKIP,BRT),
               INITIAL='CUSTOMER INQUIRY SCREEN'
CUSTID   DFHMDF POS=(4,15),LENGTH=10,ATTRB=(UNPROT,NUM)
CUSTNAME DFHMDF POS=(6,15),LENGTH=30,ATTRB=(ASKIP)
ADDRESS  DFHMDF POS=(8,15),LENGTH=40,ATTRB=(ASKIP)
PHONE    DFHMDF POS=(10,15),LENGTH=15,ATTRB=(ASKIP)
BALANCE  DFHMDF POS=(12,15),LENGTH=12,ATTRB=(ASKIP,BRT)
MSG      DFHMDF POS=(22,1),LENGTH=79,ATTRB=(ASKIP,BRT)
         DFHMSD TYPE=FINAL

Requirements:
- Use React 18.2 with TypeScript
- Use Tailwind CSS for styling
- Convert UNPROT fields to input fields
- Convert ASKIP fields to display fields
- Convert BRT attribute to bold/highlighted styling
- Add Zod validation for input fields
- Create API integration hook for customer lookup
```

### 2. Copilot Edits (Multi-Component Generation)
After scaffolding individual screens, use Edits for batch refinements:
```
Select all components in src/components/customer/
Edit: Add consistent error boundary, loading states, and accessibility attributes (aria-labels)
```

### 3. Copilot Chat (API Integration)
Generate API integration code from OpenAPI specs:
```
Given this OpenAPI spec for the Customer Service API, generate:
1. TypeScript types for all request/response models
2. A custom React hook useCustomerApi() with methods for each endpoint
3. React Query integration for caching and state management
```

---

## BMS → React Mapping

| BMS Concept | React Equivalent | Notes |
|-------------|-----------------|-------|
| `DFHMSD` (map set) | Component module | Group of related screens |
| `DFHMDI` (map) | React component | One screen = one component |
| `DFHMDF POS=(r,c)` | CSS Grid position | Map row/col to grid |
| `ATTRB=(UNPROT)` | `<input>` | User-editable field |
| `ATTRB=(ASKIP)` | `<span>` or `<p>` | Display-only field |
| `ATTRB=(UNPROT,NUM)` | `<input type="number">` | Numeric input |
| `ATTRB=(BRT)` | `font-bold text-white` | Highlighted text |
| `ATTRB=(DRK)` | `hidden` | Hidden field |
| `INITIAL='text'` | Default value / label | Static text |
| `LENGTH=n` | `maxLength={n}` | Field length constraint |
| PF keys (PF1-PF24) | Button bar / keyboard shortcuts | Navigation actions |
| CICS SEND MAP | Component render | Display screen |
| CICS RECEIVE MAP | Form submit handler | Capture user input |

---

## Conversion Patterns

### Pattern 1: BMS Screen → React Component

**BMS:**
```
CUSTINQ  DFHMDI SIZE=(24,80)
CUSTID   DFHMDF POS=(4,15),LENGTH=10,ATTRB=(UNPROT,NUM)
CUSTNAME DFHMDF POS=(6,15),LENGTH=30,ATTRB=(ASKIP)
```

**React (Copilot output):**
```tsx
import { useState } from 'react';
import { z } from 'zod';
import { useCustomerApi } from '@/hooks/useCustomerApi';

const customerIdSchema = z.string().regex(/^\d{1,10}$/, 'Must be numeric, max 10 digits');

export function CustomerInquiry() {
  const [customerId, setCustomerId] = useState('');
  const { data: customer, isLoading, error } = useCustomerApi(customerId);

  return (
    <div className="min-h-screen bg-gray-900 text-green-400 font-mono p-4">
      <h1 className="text-center text-xl font-bold mb-8">
        CUSTOMER INQUIRY SCREEN
      </h1>

      <div className="grid grid-cols-[120px_1fr] gap-y-4 max-w-2xl mx-auto">
        <label htmlFor="custId" className="self-center">Customer ID:</label>
        <input
          id="custId"
          type="text"
          inputMode="numeric"
          maxLength={10}
          value={customerId}
          onChange={(e) => setCustomerId(e.target.value)}
          className="bg-gray-800 border border-green-400 px-2 py-1 w-40"
          aria-label="Customer ID"
        />

        <span>Name:</span>
        <span className="font-bold">{customer?.name ?? '—'}</span>

        <span>Address:</span>
        <span>{customer?.address ?? '—'}</span>

        <span>Phone:</span>
        <span>{customer?.phone ?? '—'}</span>

        <span>Balance:</span>
        <span className="font-bold text-yellow-400">
          {customer?.balance ? `$${customer.balance.toFixed(2)}` : '—'}
        </span>
      </div>

      {error && (
        <p className="text-red-400 mt-8 text-center">{error.message}</p>
      )}
    </div>
  );
}
```

### Pattern 2: CICS Transaction Flow → React Router

**CICS flow:**
```
CUST (main menu) → CINQ (inquiry) → CUPD (update) → CDEL (delete)
```

**React Router (Copilot output):**
```tsx
import { createBrowserRouter } from 'react-router-dom';

export const router = createBrowserRouter([
  {
    path: '/customers',
    element: <CustomerLayout />,
    children: [
      { index: true, element: <CustomerMenu /> },
      { path: 'inquiry/:id?', element: <CustomerInquiry /> },
      { path: 'update/:id', element: <CustomerUpdate /> },
      { path: 'delete/:id', element: <CustomerDelete /> },
    ],
  },
]);
```

### Pattern 3: PF Keys → Action Bar

**CICS:**
```cobol
EVALUATE EIBAID
    WHEN DFHPF1  PERFORM HELP-SCREEN
    WHEN DFHPF3  PERFORM EXIT-PROGRAM
    WHEN DFHPF5  PERFORM SAVE-DATA
    WHEN DFHENTER PERFORM PROCESS-INPUT
END-EVALUATE.
```

**React (Copilot output):**
```tsx
function ActionBar({ onSave, onExit, onHelp }: ActionBarProps) {
  useHotkeys('f1', onHelp);
  useHotkeys('f3', onExit);
  useHotkeys('f5', onSave);

  return (
    <div className="fixed bottom-0 left-0 right-0 bg-gray-800 p-2 flex gap-4">
      <button onClick={onHelp} className="text-green-400">F1=Help</button>
      <button onClick={onExit} className="text-green-400">F3=Exit</button>
      <button onClick={onSave} className="text-green-400">F5=Save</button>
    </div>
  );
}
```

---

## Step-by-Step Workflow

### Step 1: Inventory BMS Maps
1. List all BMS map definitions
2. Group by CICS transaction (one transaction = one user flow)
3. Map to React component hierarchy

### Step 2: Generate API Client
1. Feed OpenAPI specs from Phase 2 into Copilot
2. Generate TypeScript types and API client
3. Create React Query hooks for each endpoint

### Step 3: Convert Screens
1. Use `react-scaffolder` agent for each BMS map
2. Generate React component + Zod schema + custom hook
3. Add Tailwind CSS styling

### Step 4: Wire Navigation
1. Convert CICS transaction flow to React Router routes
2. Add PF key equivalents as keyboard shortcuts
3. Add breadcrumb navigation

### Step 5: Enhance UX
1. Don't just replicate green screens — modernize the UX
2. Add responsive design
3. Add accessibility (WCAG 2.1)
4. Add loading states, error boundaries, toast notifications

---

## Tips
- **Don't replicate the green screen exactly**: Use this as an opportunity to improve UX
- **Generate types from OpenAPI**: Never hand-write API types — generate from specs
- **Use React Query**: Handles caching, loading states, and error handling automatically
- **Keyboard shortcuts matter**: Mainframe users rely on keyboard navigation — keep PF key equivalents
- **Batch convert with Copilot Edits**: Select multiple BMS files and convert in one operation
