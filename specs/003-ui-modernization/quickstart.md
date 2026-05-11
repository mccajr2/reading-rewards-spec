# Quickstart: UI Modernization & Page Guidance

**Feature**: `003-ui-modernization` | **Date**: 2026-05-11

This guide walks through the implementation of the UI modernization feature: integrating shadcn/ui, applying design tokens, and adding page guidance text to all pages.

---

## Phase 1: Setup & Framework Integration

### Step 1: Install shadcn/ui

```bash
cd frontend

# Initialize shadcn/ui
npx shadcn-ui@latest init

# Select options:
# - Yes, use TypeScript
# - Tailwind CSS (already configured)
# - Yes, use CSS variables for theming
```

This creates:
- `frontend/components/ui/` — shadcn/ui components (copyable)
- `frontend/lib/utils.ts` — Utility functions
- `frontend/tailwind.config.ts` — Updated Tailwind config

### Step 2: Copy Essential Components

Copy the following components from shadcn/ui:

```bash
npx shadcn-ui@latest add button
npx shadcn-ui@latest add card
npx shadcn-ui@latest add input
npx shadcn-ui@latest add label
npx shadcn-ui@latest add modal
npx shadcn-ui@latest add dialog
npx shadcn-ui@latest add checkbox
npx shadcn-ui@latest add select
npx shadcn-ui@latest add alert
npx shadcn-ui@latest add tabs
npx shadcn-ui@latest add badge
npx shadcn-ui@latest add pagination
```

### Step 3: Create Design System Tokens

Create `frontend/src/styles/tokens.css`:

```css
@layer base {
  :root {
    /* Colors */
    --color-primary: 37 99 235; /* #2563eb / Blue-600 */
    --color-primary-light: 239 245 255; /* #eff6ff / Blue-50 */
    --color-accent: 251 191 36; /* #fbbf24 / Amber-400 */
    --color-accent-light: 255 251 235; /* #fffbeb / Amber-50 */
    --color-success: 22 163 74; /* #16a34a / Green-600 */
    --color-error: 220 38 38; /* #dc2626 / Red-600 */
    --color-warning: 234 88 12; /* #ea580c / Orange-600 */
    --color-text-primary: 17 24 39; /* #111827 / Gray-900 */
    --color-text-secondary: 75 85 99; /* #4b5563 / Gray-700 */
    --color-border: 229 231 235; /* #e5e7eb / Gray-200 */
    --color-background: 255 255 255; /* #ffffff / White */
    --color-background-alt: 249 250 251; /* #f9fafb / Gray-50 */

    /* Typography */
    --font-family-base: system-ui, -apple-system, "Segoe UI", sans-serif;

    /* Spacing (multiples of 4px) */
    --spacing-xs: 0.25rem; /* 4px */
    --spacing-sm: 0.5rem; /* 8px */
    --spacing-md: 0.75rem; /* 12px */
    --spacing-base: 1rem; /* 16px */
    --spacing-lg: 1.5rem; /* 24px */
    --spacing-xl: 2rem; /* 32px */
    --spacing-2xl: 3rem; /* 48px */

    /* Shadows */
    --shadow-sm: 0 1px 2px rgb(0 0 0 / 0.05);
    --shadow-base: 0 1px 3px rgb(0 0 0 / 0.1);
    --shadow-lg: 0 10px 15px rgb(0 0 0 / 0.1);
  }
}
```

Update `frontend/src/theme.ts` to export tokens:

```typescript
export const theme = {
  colors: {
    primary: '#2563eb',
    primaryLight: '#eff6ff',
    accent: '#fbbf24',
    accentLight: '#fffbeb',
    success: '#16a34a',
    error: '#dc2626',
    warning: '#ea580c',
    textPrimary: '#111827',
    textSecondary: '#4b5563',
    border: '#e5e7eb',
    background: '#ffffff',
    backgroundAlt: '#f9fafb',
  },
  spacing: {
    xs: '0.25rem',
    sm: '0.5rem',
    md: '0.75rem',
    base: '1rem',
    lg: '1.5rem',
    xl: '2rem',
    '2xl': '3rem',
  },
  breakpoints: {
    mobile: '0px',
    sm: '640px',
    md: '768px',
    lg: '1024px',
    xl: '1280px',
  },
};
```

### Step 4: Create PageGuidance Component

Create `frontend/src/components/shared/PageGuidance.tsx`:

```typescript
import React from 'react';

interface PageGuidanceProps {
  title: string;
  description: string;
  instructions: string;
  tone: 'parent' | 'child';
  icon?: React.ReactNode;
}

export const PageGuidance: React.FC<PageGuidanceProps> = ({
  title,
  description,
  instructions,
  tone,
  icon,
}) => {
  const bgColor = tone === 'child' ? 'bg-amber-50' : 'bg-blue-50';
  const borderColor = tone === 'child' ? 'border-amber-200' : 'border-blue-200';
  const headingColor = tone === 'child' ? 'text-amber-900' : 'text-blue-900';

  return (
    <div className={`${bgColor} ${borderColor} border rounded-lg p-6 mb-8`}>
      <div className="flex items-start gap-4">
        {icon && <div className="text-2xl flex-shrink-0">{icon}</div>}
        <div className="flex-1">
          <h1 className={`${headingColor} text-3xl font-bold mb-2`}>{title}</h1>
          <p className="text-gray-700 mb-3">{description}</p>
          <p className="text-sm text-gray-600 italic">{instructions}</p>
        </div>
      </div>
    </div>
  );
};
```

---

## Phase 2: Page Refactoring

### Step 5: Apply PageGuidance to Pages

For each page, wrap the content and add guidance:

**Example: Parent Dashboard**

```typescript
import { PageGuidance } from '@/components/shared/PageGuidance';

export const ParentDashboard: React.FC = () => {
  return (
    <div className="page-container">
      <PageGuidance
        title="Your Dashboard"
        description="Here's a snapshot of each child's reading progress. You can see books read, rewards earned, and upcoming milestones at a glance."
        instructions="Click on any child's card to view detailed progress, or use the menu to manage accounts and settings."
        tone="parent"
      />

      {/* Dashboard content here */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {/* Child summary cards */}
      </div>
    </div>
  );
};
```

**Example: Child Reading List**

```typescript
import { PageGuidance } from '@/components/shared/PageGuidance';
import { BookOpen } from 'lucide-react';

export const ChildReadingList: React.FC = () => {
  return (
    <div className="page-container">
      <PageGuidance
        title="Your Reading List"
        description="This is your personal list of books you're reading or want to read."
        instructions="Click 'Add Book' to search for a new book, or click on any book to log your progress. Books you finish earn you rewards!"
        tone="child"
        icon={<BookOpen size={32} className="text-amber-600" />}
      />

      {/* Reading list content here */}
      <div className="space-y-4">
        {/* Book cards */}
      </div>
    </div>
  );
};
```

### Step 6: Update Shared Components

Replace existing Button, Card, Input, Modal with shadcn/ui versions:

**Frontend/src/components/shared/Button.tsx** → Use `@/components/ui/button`
**Frontend/src/components/shared/Card.tsx** → Use `@/components/ui/card`
**Frontend/src/components/shared/Input.tsx** → Use `@/components/ui/input`
**Frontend/src/components/shared/Modal.tsx** → Use `@/components/ui/dialog`

### Step 7: Apply Responsive Design

Update component styling with Tailwind responsive prefixes:

```typescript
// Example: Responsive grid
<div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
  {/* Cards that stack on mobile, 2 columns on tablet, 3+ on desktop */}
</div>

// Example: Responsive text size
<h1 className="text-2xl sm:text-3xl md:text-4xl font-bold">
  Your Reading List
</h1>

// Example: Responsive padding
<div className="p-4 sm:p-6 md:p-8">
  {/* Content */}
</div>
```

---

## Phase 3: Testing & Validation

### Step 8: Unit & Component Tests

Create `frontend/src/components/shared/__tests__/PageGuidance.test.tsx`:

```typescript
import { render, screen } from '@testing-library/react';
import { PageGuidance } from '../PageGuidance';
import { describe, it, expect } from 'vitest';

describe('PageGuidance', () => {
  it('renders title, description, and instructions', () => {
    render(
      <PageGuidance
        title="Test Page"
        description="This is a test page."
        instructions="Click the button."
        tone="parent"
      />
    );

    expect(screen.getByText('Test Page')).toBeInTheDocument();
    expect(screen.getByText('This is a test page.')).toBeInTheDocument();
    expect(screen.getByText('Click the button.')).toBeInTheDocument();
  });

  it('applies correct styling for parent tone', () => {
    const { container } = render(
      <PageGuidance
        title="Parent Page"
        description="Description"
        instructions="Instructions"
        tone="parent"
      />
    );

    expect(container.firstChild).toHaveClass('bg-blue-50');
  });

  it('applies correct styling for child tone', () => {
    const { container } = render(
      <PageGuidance
        title="Child Page"
        description="Description"
        instructions="Instructions"
        tone="child"
      />
    );

    expect(container.firstChild).toHaveClass('bg-amber-50');
  });
});
```

### Step 9: E2E Smoke Tests

Create `frontend/tests/e2e/ui-modernization.spec.ts`:

```typescript
import { test, expect } from '@playwright/test';

test('parent dashboard loads with guidance', async ({ page }) => {
  // Login as parent
  await page.goto('/login');
  await page.fill('input[type="email"]', 'parent@example.com');
  await page.fill('input[type="password"]', 'password');
  await page.click('button:has-text("Login")');

  // Dashboard should display guidance
  await page.waitForSelector('h1:has-text("Your Dashboard")');
  expect(await page.locator('p:has-text("snapshot of each child")').isVisible()).toBe(true);

  // Verify responsive: on mobile
  await page.setViewportSize({ width: 375, height: 667 });
  expect(await page.locator('h1').isVisible()).toBe(true);
  expect(await page.locator('button').first().isVisible()).toBe(true);
});

test('child reading list loads with fun guidance', async ({ page }) => {
  // Login as child
  await page.goto('/login');
  await page.fill('input[type="email"]', 'child@example.com');
  await page.fill('input[type="password"]', 'password');
  await page.click('button:has-text("Login")');

  // Reading list should display fun guidance
  await page.waitForSelector('h1:has-text("Your Reading List")');
  expect(await page.locator('p:has-text("📚")').isVisible()).toBe(true);
  expect(await page.locator('p:has-text("Click \'Add Book\'")').isVisible()).toBe(true);
});

test('pages render correctly on mobile, tablet, desktop', async ({ page }) => {
  const viewports = [
    { width: 375, height: 667, name: 'mobile' },
    { width: 768, height: 1024, name: 'tablet' },
    { width: 1440, height: 900, name: 'desktop' },
  ];

  for (const viewport of viewports) {
    await page.setViewportSize({ width: viewport.width, height: viewport.height });
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');

    // Verify no horizontal scroll
    const bodyWidth = await page.evaluate(() => document.body.scrollWidth);
    const viewportWidth = viewport.width;
    expect(bodyWidth).toBeLessThanOrEqual(viewportWidth);

    // Verify content is visible
    expect(await page.locator('h1').first().isVisible()).toBe(true);
  }
});
```

### Step 10: Accessibility Audit

Run automated accessibility checks:

```bash
# Install axe-core testing library
npm install --save-dev @axe-core/react axe-playwright

# Run accessibility audit on all pages
npm run test:a11y

# Manually test with VoiceOver (Mac) or NVDA (Windows)
# Verify:
# - All interactive elements keyboard-navigable
# - Form labels announced by screen reader
# - Focus indicators visible
# - Color contrast ≥4.5:1
```

### Step 11: Bundle Size Check

```bash
# Build and check bundle size
npm run build

# Compare to baseline:
# Before: ~150KB gzip
# After: <200KB gzip (target: <50KB increase)
```

---

## Verification Checklist

Before marking the feature complete:

- [ ] shadcn/ui installed and components copied
- [ ] Design tokens defined in `tokens.css` and `theme.ts`
- [ ] `PageGuidance` component created and used on all pages
- [ ] Parent pages use professional tone
- [ ] Child pages use fun, encouraging tone
- [ ] All pages render correctly on mobile (375px), tablet (768px), desktop (1440px+)
- [ ] Responsive design verified: no horizontal scroll, buttons touch-friendly
- [ ] Component tests pass (Vitest)
- [ ] E2E smoke tests pass (Playwright)
- [ ] Accessibility audit passes (axe-core): zero critical violations, 95% pass rate
- [ ] Screen reader test passes (VoiceOver/NVDA): all content readable
- [ ] Bundle size increase <50KB gzip
- [ ] Local development still works: `npm run dev` starts cleanly

---

## Rollback Plan (If Needed)

If the UI modernization causes issues:

1. Revert feature branch to last working commit: `git revert <commit-hash>`
2. Redeploy from previous Docker image in Render
3. Investigate issues in a separate branch before re-attempting

---

**Next**: Run `/speckit.tasks` to generate the detailed task list for implementation phase.
