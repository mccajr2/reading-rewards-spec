# UI Primitives

This folder contains reusable UI primitives based on local shadcn-style patterns and Radix where appropriate.

## Components

### Alert
- File: `alert.tsx`
- Exports: `Alert`, `AlertTitle`, `AlertDescription`
- Variants: `info`, `success`, `warning`, `error`
- Accessibility:
  - Uses `role="alert"` on root
  - Intended for status/error messaging regions
- Responsive behavior:
  - Full-width container by default

```tsx
<Alert variant="warning">
  <AlertTitle>Heads up</AlertTitle>
  <AlertDescription>Please review this value before continuing.</AlertDescription>
</Alert>
```

### Badge
- File: `badge.tsx`
- Exports: `Badge`
- Variants: `default`, `secondary`, `success`, `warning`, `error`
- Accessibility:
  - For non-interactive status labels

```tsx
<Badge variant="success">Active</Badge>
```

### Button
- File: `button.tsx`
- Exports: `Button`, `buttonVariants`
- Variants: `default`, `secondary`, `outline`, `ghost`
- Sizes: `sm`, `md`, `lg`
- Accessibility:
  - Keyboard focus ring (`focus-visible:ring-2`)
  - Disabled state support
  - Minimum touch target (`min-h-11`, 44px+)
- Responsive behavior:
  - Utility classes can be extended via `className`

```tsx
<Button variant="secondary" size="sm">Cancel</Button>
<Button>Save</Button>
```

### Card
- File: `card.tsx`
- Exports: `Card`, `CardHeader`, `CardTitle`, `CardDescription`, `CardContent`, `CardFooter`
- Accessibility:
  - Structural container; semantic heading/content supplied by caller

```tsx
<Card>
  <CardHeader>
    <CardTitle>Reading Summary</CardTitle>
    <CardDescription>Last 7 days</CardDescription>
  </CardHeader>
  <CardContent>...</CardContent>
</Card>
```

### Checkbox
- File: `checkbox.tsx`
- Exports: `Checkbox`
- Accessibility:
  - Radix checkbox primitive with keyboard support
  - Use with a `<label htmlFor>` or `Label` component

```tsx
<Checkbox id="consent" />
```

### Dialog + Modal
- Files: `dialog.tsx`, `modal.tsx`
- Exports:
  - Dialog primitives: `Dialog`, `DialogTrigger`, `DialogContent`, etc.
  - Composed modal: `Modal`
- Accessibility:
  - Radix dialog focus trap and escape handling
  - `DialogTitle`/`DialogDescription` supported
- Responsive behavior:
  - Constrained width: `w-[min(90vw,32rem)]`

```tsx
<Modal
  open={open}
  onOpenChange={setOpen}
  title="Confirm"
  description="This action cannot be undone."
>
  <p>Are you sure?</p>
</Modal>
```

### Input
- File: `input.tsx`
- Exports: `Input`
- Accessibility:
  - Supports `aria-invalid`, `aria-describedby`, etc.
  - Focus-visible ring styles
- Responsive behavior:
  - Full-width by default (`w-full`)

```tsx
<Input type="email" placeholder="Email" />
```

### Label
- File: `label.tsx`
- Exports: `Label`
- Accessibility:
  - Wraps Radix label primitive; pair with form control IDs

```tsx
<Label htmlFor="email">Email</Label>
```

### Pagination
- File: `pagination.tsx`
- Exports: `Pagination`
- Props: `page`, `totalPages`, `onPageChange`, `className`
- Accessibility:
  - Uses `nav` with `aria-label="Pagination"`
  - Active page uses `aria-current="page"`
- Responsive behavior:
  - Buttons wrap naturally via surrounding layout styles

```tsx
<Pagination page={1} totalPages={4} onPageChange={setPage} />
```

### Select
- File: `select.tsx`
- Exports: `Select`, `SelectOption`
- Accessibility:
  - Native select semantics and keyboard behavior

```tsx
<Select
  value={status}
  onChange={(e) => setStatus(e.target.value)}
  options={[
    { value: 'todo', label: 'To Do' },
    { value: 'done', label: 'Done' },
  ]}
/>
```

### Tabs
- File: `tabs.tsx`
- Exports: `Tabs`, `TabsList`, `TabsTrigger`, `TabsContent`
- Accessibility:
  - Radix tab semantics and keyboard navigation

```tsx
<Tabs defaultValue="books">
  <TabsList>
    <TabsTrigger value="books">Books</TabsTrigger>
    <TabsTrigger value="history">History</TabsTrigger>
  </TabsList>
  <TabsContent value="books">...</TabsContent>
  <TabsContent value="history">...</TabsContent>
</Tabs>
```

## Import Pattern

Use the barrel export in `index.ts`:

```tsx
import { Button, Card, Input } from '@/components/ui';
```
