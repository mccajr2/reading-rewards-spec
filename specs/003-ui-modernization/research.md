# Research: UI Modernization & Page Guidance

**Phase**: 0 | **Feature**: `003-ui-modernization` | **Date**: 2026-05-11

## Decision Log

### 1. UI Framework Selection: shadcn/ui vs. Material-UI vs. Chakra UI

**Decision**: **shadcn/ui** (with Radix UI primitives and Tailwind CSS)

**Rationale**: 
- ✅ Headless components built on Radix UI (accessible by default, WCAG 2.1 AA compliant)
- ✅ Uses existing Tailwind CSS setup—no style duplication or CSS-in-JS overhead
- ✅ Copyable component code (not a dependency) allows full customization and tree-shaking
- ✅ Minimal bundle size impact (components are just React + Tailwind, ~10-15KB base overhead)
- ✅ Strong TypeScript support, perfect for current codebase
- ✅ Works seamlessly with Vite and modern build tools
- ✅ Active community, extensive component library (Button, Card, Input, Modal, Dialog, Tabs, etc.)
- ✅ Can co-exist with custom Tailwind classes for gradual migration

**Alternatives considered**:
- **Material-UI (MUI)**: Powerful, full-featured, but heavier (50KB+); CSS-in-JS adds complexity; strong ties to Material Design may limit customization for "fun" child-facing UI
- **Chakra UI**: Good accessibility, but CSS-in-JS adds build overhead; less Tailwind-native than shadcn; smaller component ecosystem

**Action**: Install shadcn/ui CLI, copy Button, Card, Input, Modal, Dialog components into codebase; customize colors/spacing via Tailwind design tokens

---

### 2. Design System: Color Palette, Typography, Spacing

**Decision**: Implement a tiered design system using Tailwind CSS variables (tokens)

**Color Palette**:
- **Primary**: Blue-600 (`#2563eb`) — Parent action buttons, critical CTAs
- **Secondary**: Blue-50 (`#eff6ff`) — Parent page backgrounds, subtle accents
- **Accent (Child)**: Amber-400 (`#fbbf24`) — Fun, energetic, celebratory for child pages
- **Child Background**: Amber-50 (`#fffbeb`) — Warm, inviting for child pages
- **Neutral**: Gray-900, Gray-700, Gray-600, Gray-400, Gray-200, Gray-50 — Text, borders, backgrounds
- **Status**: Green-600 (success), Red-600 (error), Amber-500 (warning)

**Typography**:
- **Font family**: `system-ui, -apple-system, sans-serif` (native system fonts; fast, accessible)
- **Parent text**:
  - Headings: 28px (h1), 24px (h2), 20px (h3)
  - Body: 16px, line-height 1.5
  - Label: 14px, font-weight 600
- **Child text** (same sizes, slightly more playful weight):
  - Headings: 28px, font-weight 700 (bold for emphasis)
  - Body: 16px, line-height 1.6 (extra breathing room)
  - Label: 14px, font-weight 500 (slightly lighter for friendliness)

**Spacing**:
- Base unit: 4px
- Scale: 4, 8, 12, 16, 24, 32, 40, 48px
- Card padding: 24px
- Button padding: 12px 16px (touch-friendly)
- Gap between sections: 32px

**Rationale**:
- System fonts are fast (no font downloads), accessible (respect user preferences), and familiar to users
- Neutral color palette for parents (professional, calm); warm/energetic palette for children (fun, approachable)
- Spacing scale ensures consistency and rhythm across layouts

**Action**: Create `frontend/src/styles/tokens.css` with Tailwind CSS variables; update `theme.ts` to export breakpoints and spacing scale

---

### 3. Page Guidance Structure & Tone Guidelines

**Decision**: Create a `PageGuidance` wrapper component that renders page title, description, and instructions at the top of each page

**Structure**:
```tsx
<PageGuidance
  title="Your Reading List"
  description="This is your personal list of books you're reading or want to read."
  instructions="Click 'Add Book' to start tracking a new book, or click on any book to log your reading progress."
  tone="child"  // or "parent"
/>
<div className="page-content">
  {/* Page content here */}
</div>
```

**Parent Tone Guidelines**:
- Professional, informative, direct
- Assume adult literacy and familiarity with digital platforms
- Include specific feature names ("Dashboard", "Rewards Settings", "Child Account Management")
- Example: "Your dashboard provides a snapshot of each child's reading activity, rewards earned, and milestones reached. Use this view to monitor progress at a glance."

**Child Tone Guidelines**:
- Friendly, encouraging, fun but clear
- Assume ages 7–12 reading level (short sentences, concrete language)
- Use celebratory language for achievements ("You're doing amazing!", "Great job tracking your reading!")
- Avoid technical jargon; use analogies ("Think of books like levels in a game—more books = more rewards!")
- Example: "Here's your reading list! 📚 This is where you keep track of all the books you've read or want to read. Want to add a new book? Click the 'Add Book' button and search for it!"

**Rationale**:
- Centralizing guidance in a reusable component ensures consistency and makes tone updates easy
- Separating guidance from page content keeps layouts clean and allows progressive enhancement

**Action**: Create `PageGuidance.tsx` component with styling for both tones; document tone guidelines in `data-model.md`

---

### 4. Responsive Design Strategy: Mobile-First

**Decision**: Use Tailwind CSS responsive prefixes (sm, md, lg, xl) with mobile-first breakpoints

**Breakpoints**:
- **Mobile**: <640px (default styles)
- **Small Tablet**: ≥640px (`sm:`)
- **Medium Tablet**: ≥768px (`md:`)
- **Desktop**: ≥1024px (`lg:`)
- **Large Desktop**: ≥1280px (`xl:`)

**Strategy**:
1. Design layouts for 375px (mobile) first
2. Enhance for 768px (tablet): 2-column layouts, sidebar navigation
3. Enhance for 1024px+ (desktop): 3-column layouts, expanded sidebar, larger cards

**Testing**:
- Mobile (375px): iPhone SE, older phones
- Tablet (768px): iPad, Android tablets
- Desktop (1440px+): Standard laptops and monitors

**Rationale**:
- Mobile-first ensures core functionality works on constrained screens
- Tailwind's responsive classes make it easy to adapt layouts without media query boilerplate
- Testing on actual device widths catches reflow and touch-target issues

**Action**: Audit existing components; refactor with `sm:`, `md:`, `lg:` prefixes; test on physical devices and browser DevTools

---

### 5. Accessibility (WCAG 2.1 AA) Implementation

**Decision**: Use shadcn/ui (Radix UI primitives) for built-in accessibility + manual audits

**Accessibility Requirements**:
- ✅ All interactive elements keyboard-accessible (Tab, Enter, Esc, Arrow keys)
- ✅ Focus indicators visible (Tailwind `focus:ring-2 focus:ring-blue-500`)
- ✅ Color not sole conveyor of information (always pair color with text/icons)
- ✅ Button/input minimum sizes: 44px × 44px touch targets (CSS: `min-h-11 min-w-11`)
- ✅ Semantic HTML: `<button>`, `<input>`, `<label>` for form fields (no div-based buttons)
- ✅ ARIA labels where needed: `aria-label`, `aria-describedby`, `aria-hidden` on decorative icons
- ✅ Screen reader testing: VoiceOver (Mac), NVDA (Windows)
- ✅ Contrast ratio ≥4.5:1 for text (checked via axe-core)

**Tools**:
- **axe DevTools** (browser extension): Run accessibility audit on each page
- **Jest + axe-core**: Automated accessibility testing in CI
- **Storybook + axe addon**: Component accessibility checks during development

**Rationale**:
- Radix UI handles ARIA attributes and keyboard interactions automatically
- Manual audits catch context-specific issues (labels on forms, focus management in modals)
- Automated testing in CI prevents regressions

**Action**: Configure `@axe-core/react` in tests; update component tests to include accessibility assertions

---

### 6. Performance Constraints: Bundle Size & Load Time

**Decision**: Enforce <50KB gzip increase; target <3s load time on 4G

**Baseline** (before UI framework):
- Frontend bundle: ~150KB gzip (measured via `npm run build`)
- Load time (4G throttled): ~2.2s

**Budget**:
- shadcn/ui + Radix UI + icons: ~40KB gzip
- Design tokens + typography: ~2KB gzip
- **Total increase**: ~42KB gzip (within 50KB budget)

**Monitoring**:
- Compare bundle sizes: `npm run build` before and after
- Measure load time: Lighthouse audit in CI

**Tree-Shaking Strategy**:
- shadcn/ui components are copied (not dependency); only import used components
- Radix UI is modular; only import needed primitives
- Tailwind CSS tree-shakes unused styles automatically
- Icons: Use a lazy-loaded icon library (e.g., `lucide-react`) instead of bundling all icons upfront

**Action**: Run `npm run build` after adding components; verify bundle size increase; enable CI check to fail if >50KB increase

---

### 7. Testing Strategy

**Decision**: Three-layer testing: unit + component, E2E smoke, accessibility audit

**Layer 1: Unit & Component Tests (Vitest)**
- Test `PageGuidance` component renders with correct tone and text
- Test refactored components (Button, Card, Input) with UI framework styling
- Test responsive behavior (e.g., sidebar collapsible on mobile)

**Layer 2: E2E Smoke Tests (Playwright)**
- **Parent journey**: Login → Dashboard (verify guidance text visible) → Child Management → Rewards Settings
- **Child journey**: Login → Reading List (add book) → Book Search (search for title) → Rewards (check status)
- **Responsive flow**: Verify pages render and are interactive on mobile (375px), tablet (768px), desktop (1440px)

**Layer 3: Accessibility Audit**
- Run axe-core on each page (automated)
- Manual screen reader test: VoiceOver on Mac, NVDA on Windows (spot-check)
- Verify contrast ratios ≥4.5:1

**Rationale**:
- Unit tests catch component-level bugs early
- E2E tests verify user-facing flows (critical journeys) work end-to-end
- Accessibility audits ensure compliance with WCAG 2.1 AA

**Action**: Write tests as components are refactored; add accessibility tests to CI pipeline

---

## Summary of Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| UI Framework | shadcn/ui (Radix UI + Tailwind) | Lightweight, accessible, Tailwind-native, high customization |
| Design System | Tailwind tokens + spacing scale | Fast, consistent, responsive-friendly |
| Page Guidance | PageGuidance wrapper component | Reusable, centralized, tone-aware |
| Responsive Strategy | Mobile-first with Tailwind prefixes | Progressive enhancement, touch-friendly |
| Accessibility | Radix UI + manual audits + automated checks | WCAG 2.1 AA compliant, inclusive |
| Performance | <50KB gzip; <3s load on 4G | Maintains fast, responsive experience |
| Testing | 3-layer: unit, E2E, accessibility | Comprehensive coverage, regression prevention |

---

## No Clarifications Needed

✅ All requirements from spec are now grounded in concrete technical decisions  
✅ No [NEEDS CLARIFICATION] markers remain  
✅ Next phase: Generate data-model.md, contracts/, and quickstart.md
