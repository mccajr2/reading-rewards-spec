# Reading Rewards Design System

## Design Philosophy

The UI system balances two product voices while keeping a single implementation model:
- Parent experience: clear, professional, action-oriented
- Child experience: encouraging, celebratory, easy to scan

Implementation goals:
- Token-first styling (no arbitrary hardcoded colors in features)
- Reusable component primitives + shared wrappers
- Accessibility by default (focus states, labels, touch targets)
- Responsive consistency across mobile/tablet/desktop

## Token Foundation

Source of truth:
- `src/styles/tokens.css`
- `src/theme.ts`

### Color Palette

- `primary`: `#2563eb`
- `primaryLight`: `#eff6ff`
- `accent`: `#fbbf24`
- `accentLight`: `#fffbeb`
- `success`: `#16a34a`
- `warning`: `#ea580c`
- `error`: `#dc2626`
- `textPrimary`: `#111827`
- `textSecondary`: `#4b5563`
- `border`: `#e5e7eb`
- `background`: `#ffffff`
- `backgroundAlt`: `#f9fafb`

### Typography

Font family:
- `system-ui, -apple-system, 'Segoe UI', sans-serif`

Scale:
- `xs`: 12
- `sm`: 14
- `base`: 16
- `lg`: 18
- `xl`: 20
- `2xl`: 24
- `3xl`: 28

Line heights:
- `tight`: 1.2
- `normal`: 1.5
- `relaxed`: 1.6

### Spacing Scale (px)

- `xs`: 4
- `sm`: 8
- `md`: 12
- `base`: 16
- `lg`: 24
- `xl`: 32
- `2xl`: 48

### Breakpoints

- `mobile`: `0px`
- `sm`: `640px`
- `md`: `768px`
- `lg`: `1024px`
- `xl`: `1280px`

## Component Architecture

### UI Primitives (`src/components/ui`)

Atoms/low-level controls:
- Button, Input, Label, Checkbox, Select, Badge
- Alert, Card, Tabs, Pagination
- Dialog/Modal

These primitives encapsulate styling and interaction behavior.

### Shared Components (`src/components/shared`)

Molecules/app wrappers:
- `PageGuidance` (tone-aware page intro)
- `FormField` (label + input + helper/error)
- `Navigation` (role-aware top nav)
- Wrapper aliases (`Button`, `Card`, `Input`, `Modal`, `Pagination`)

Use shared components first in feature pages to preserve consistency.

## Accessibility Checklist

Baseline expectations:
- Semantic elements (`button`, `nav`, headings)
- Form labeling with `htmlFor`/`id`
- `aria-describedby` for helper and error states
- Visible keyboard focus (`focus-visible:ring-2`)
- Minimum touch targets 44px (`min-h-11`)
- Responsive no-horizontal-scroll checks at target breakpoints

Known validation commands:
- `npm run test -- --poolOptions.threads.singleThread`
- `npm run test:e2e -- tests/e2e/responsive-design.spec.ts`

## Usage Patterns

### 1) Page shell pattern

1. Render `PageGuidance` at top
2. Render feature-specific sections beneath
3. Keep spacing consistent with tokenized utility classes

### 2) Forms

- Prefer `FormField` for text inputs
- Use `Modal` for confirm/edit flows
- Surface errors in text regions linked by ARIA

### 3) Lists and status

- Use `Card` for summary/grouped content
- Use `Badge` or `Alert` for status emphasis
- Use `Pagination` when page size exceeds list viewport

### 4) Responsive behavior

- Default to single-column mobile flow
- Introduce `sm:`/`md:`/`lg:` progressively
- Wrap dense tables in horizontal scroll containers when needed

## File Map

- Tokens: `src/styles/tokens.css`
- Theme object: `src/theme.ts`
- UI primitives: `src/components/ui`
- Shared wrappers/molecules: `src/components/shared`
- Example gallery: `COMPONENT_EXAMPLES.tsx`
