# Shared Components

This folder contains app-level molecules/wrappers built on top of `src/components/ui` primitives.

## Exports

From `index.ts`:
- `Button`
- `Card` (+ `CardHeader`, `CardContent`, etc.)
- `FormField`
- `Input`
- `Modal`
- `Navigation`
- `Pagination`
- `PageGuidance`

## Component Details

### Button
- File: `Button.tsx`
- Type signature:
  - `type ButtonProps = ComponentPropsWithoutRef<typeof UIButton>`
- Purpose: project-level button alias to keep import paths consistent.

```tsx
<Button variant="default">Save</Button>
```

### Card
- File: `Card.tsx`
- Type signature:
  - `type CardProps = ComponentPropsWithoutRef<typeof UICard>`
- Purpose: wraps UI card and re-exports composition helpers.

```tsx
<Card>
  <CardTitle>Child Summary</CardTitle>
  <CardContent>...</CardContent>
</Card>
```

### FormField
- File: `FormField.tsx`
- Interface:
  - `type FormFieldProps = { label: string; error?: string; helperText?: string } & InputHTMLAttributes<HTMLInputElement>`
- Purpose: label + input + helper/error composition with linked ARIA attributes.
- Accessibility:
  - `aria-invalid`
  - `aria-describedby` wiring for helper and error text

```tsx
<FormField
  label="Username"
  name="username"
  value={username}
  onChange={(e) => setUsername(e.target.value)}
  helperText="Use 3-20 characters"
/>
```

### Input
- File: `Input.tsx`
- Type signature:
  - `type InputProps = ComponentPropsWithoutRef<typeof UIInput>`
- Purpose: shared ref-forwarding input wrapper.

```tsx
<Input placeholder="Search by title" />
```

### Modal
- File: `Modal.tsx`
- Interface:
  - `open: boolean`
  - `onOpenChange: (open: boolean) => void`
  - `title: string`
  - `description?: string`
  - `children: ReactNode`
  - `footer?: ReactNode`
- Purpose: common dialog shell for form and confirmation flows.

```tsx
<Modal open={open} onOpenChange={setOpen} title="Reset Password">
  <ResetForm />
</Modal>
```

### Navigation
- File: `Navigation.tsx`
- Interfaces:
  - `NavigationItem = { label: string; to: string; visible?: boolean }`
  - `NavigationProps = { brand, items, activePath, creditsText?, userText?, onLogout }`
- Purpose: top navigation with role-aware links and keyboard arrow navigation.
- Accessibility:
  - `aria-label="Primary navigation"`
  - `role="menubar"` + keyboard left/right handling
  - `aria-current="page"` for active route

```tsx
<Navigation
  brand="Reading Rewards"
  items={items}
  activePath={pathname}
  onLogout={logout}
/>
```

### Pagination
- File: `Pagination.tsx`
- Type signature:
  - `type PaginationProps = ComponentPropsWithoutRef<typeof UIPagination>`
- Purpose: app-level pagination wrapper.

```tsx
<Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
```

### PageGuidance
- File: `PageGuidance.tsx`
- Interface:
  - `title: string`
  - `description: string`
  - `instructions: string`
  - `tone: 'parent' | 'child'`
  - `icon?: ReactNode`
- Purpose: consistent top-of-page guidance and tone framing.
- Accessibility:
  - guidance container uses `aria-label` based on title
- Design-token usage:
  - parent tone: blue guidance palette
  - child tone: amber guidance palette

```tsx
<PageGuidance
  title="Your Reading List 📚"
  description="Track books in progress and finished books."
  instructions="Check off chapters as you read."
  tone="child"
/>
```

## Design Token Notes

Shared components consume tokenized utility classes such as:
- Colors: `text-text-primary`, `bg-background`, `border-border`, `bg-primary`
- Spacing/size: `h-11`, `p-4`, `sm:p-6`
- Focus/accessibility: `focus-visible:ring-2 focus-visible:ring-primary`

Prefer these shared components over direct primitive imports in feature pages.
