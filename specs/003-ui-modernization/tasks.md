# Tasks: UI Modernization with Page Guidance

**Feature**: `003-ui-modernization` | **Branch**: `003-ui-modernization`  
**Input**: Design documents from `/specs/003-ui-modernization/` (spec.md, plan.md, research.md, data-model.md, contracts/)  
**Organization**: Tasks grouped by user story (US1–US5) to enable independent implementation and parallel development

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Install shadcn/ui, configure design tokens, and create PageGuidance wrapper

**⚠️ CRITICAL**: All foundational tasks MUST complete before user story implementation

- [ ] T001 Install shadcn/ui CLI and copy essential components (Button, Card, Input, Label, Modal, Dialog, Checkbox, Select, Alert, Tabs, Badge, Pagination) into `frontend/components/ui/`
- [ ] T002 Create design tokens file `frontend/src/styles/tokens.css` with Tailwind CSS variables (colors, typography, spacing, shadows) per data-model.md
- [ ] T003 Create `frontend/src/theme.ts` exporting design system configuration (colors, spacing, breakpoints, typography scale) per research.md ¶2
- [ ] T004 [P] Create `frontend/src/components/shared/PageGuidance.tsx` component with parent/child tone support, icon rendering, and responsive layout per contracts/PageGuidance.contract.json
- [ ] T005 [P] Update `frontend/tailwind.config.ts` to reference CSS tokens and add responsive breakpoints (sm: 640px, md: 768px, lg: 1024px, xl: 1280px) per research.md ¶4

**Checkpoint**: Design system and PageGuidance wrapper are ready; all pages can now reference tokens

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Refactor existing shared components to use shadcn/ui and establish consistent styling

**⚠️ CRITICAL**: Components must be refactored before page-specific work begins

- [ ] T006 Refactor `frontend/src/components/shared/Button.tsx` to use shadcn/ui Button with size/variant support, responsive touch targets (min-h-11)
- [ ] T007 Refactor `frontend/src/components/shared/Card.tsx` to use shadcn/ui Card with padding (24px), shadow, border per design tokens
- [ ] T008 Refactor `frontend/src/components/shared/Input.tsx` to use shadcn/ui Input with label integration, error styling, focus rings per Accessibility.contract.json
- [ ] T009 [P] Refactor `frontend/src/components/shared/Modal.tsx` to use shadcn/ui Dialog with keyboard support (Escape to close), focus management
- [ ] T010 [P] Refactor `frontend/src/components/shared/Navigation.tsx` to use design tokens for spacing/colors; ensure keyboard navigation (Tab, Arrow keys) per Accessibility.contract.json
- [ ] T011 Create `frontend/src/components/shared/FormField.tsx` molecule component wrapping Label + Input + Error Message + Helper Text for DRY form composition per data-model.md
- [ ] T012 Update `frontend/src/components/shared/index.ts` to export all refactored components and PageGuidance
- [ ] T013 Update `frontend/globals.css` to import tokens.css and remove outdated custom component styles

**Checkpoint**: All shared components use design system; pages can now be refactored

---

## Phase 3: User Story 1 - Modern UI Framework Integration (Priority: P1) 🎯 MVP

**Goal**: All pages adopt consistent, modern styling with shadcn/ui components and design tokens

**Independent Test**: Open any page (login, dashboard, reading list) and verify buttons/cards/inputs have modern styling, consistent spacing, professional appearance across mobile (375px) and desktop (1440px)

### Component Tests for User Story 1

- [ ] T014 [P] [US1] Create unit test for PageGuidance component rendering (title, description, instructions) with parent/child tone styling in `frontend/src/components/shared/__tests__/PageGuidance.test.tsx`
- [ ] T015 [P] [US1] Create unit tests for refactored Button, Card, Input components verifying correct className application and responsive behavior in `frontend/src/components/shared/__tests__/`

### Implementation for User Story 1

- [ ] T016 [US1] Refactor `frontend/src/app/App.tsx` main layout: update wrapper classes to use design tokens, remove old custom styling, apply responsive grid (grid-cols-1 md:grid-cols-3) per data-model.md
- [ ] T017 [P] [US1] Refactor `frontend/src/app/Login.tsx` to use PageGuidance wrapper; replace input/button HTML with refactored FormField and Button components; apply design tokens for spacing/colors
- [ ] T018 [P] [US1] Refactor all `frontend/src/features/*/` page components to use refactored shared components (Button, Card, Input, Modal) per quickstart.md Step 5
- [ ] T019 [US1] Update CSS class usage across all components: replace arbitrary Tailwind classes with token-based classes (e.g., `p-6` → `p-[var(--spacing-lg)]` or use Tailwind variables if configured)
- [ ] T020 [US1] Run visual regression check: compare before/after screenshots at mobile (375px), tablet (768px), desktop (1440px) to verify consistent styling across pages per SC-001

**Checkpoint**: All pages use modern UI framework and design tokens; core styling complete

---

## Phase 4: User Story 2 - Page Guidance for Parents (Priority: P1) 🎯 MVP

**Goal**: All parent-facing pages display clear, professional guidance text explaining purpose and usage

**Independent Test**: Log in as parent and visit dashboard, child management, and rewards settings—each page displays clear description and instructions without external documentation

### Implementation for User Story 2

- [ ] T021 [P] [US2] Add PageGuidance wrapper to `frontend/src/app/ParentDashboard.tsx` with title "Your Dashboard", professional description per data-model.md §Page Guidance, and instructions; tone="parent"; test rendering in `frontend/tests/e2e/parent-dashboard.spec.ts`
- [ ] T022 [P] [US2] Add PageGuidance wrapper to child management page (likely `frontend/src/features/ChildManagement.tsx` or similar) with title "Manage Child Accounts", professional description and instructions per data-model.md; tone="parent"
- [ ] T023 [P] [US2] Add PageGuidance wrapper to rewards settings page with title "Rewards Settings", professional description and instructions per data-model.md; tone="parent"
- [ ] T024 [US2] Verify parent page guidance text is accessible by screen reader (NVDA/VoiceOver): aria-labels, semantic HTML, sufficient contrast per Accessibility.contract.json; document in accessibility audit checklist
- [ ] T025 [US2] Create visual reference/screenshot documentation of each parent page with guidance text for design review per quickstart.md Verification Checklist

**Checkpoint**: All parent pages have clear guidance text; SC-002 verified (first-time parent understanding)

---

## Phase 5: User Story 3 - Page Guidance for Children (Priority: P1) 🎯 MVP

**Goal**: All child-facing pages display fun, encouraging guidance text in kid-friendly language

**Independent Test**: Log in as child and visit reading list, book search, progress, and rewards pages—each displays friendly, encouraging text understandable to 7–12 year olds

### Implementation for User Story 3

- [ ] T026 [P] [US3] Add PageGuidance wrapper to `frontend/src/features/ReadingList.tsx` with title "Your Reading List 📚", fun description per data-model.md §Page Guidance, and encouraging instructions; tone="child"; icon={<BookOpen />}
- [ ] T027 [P] [US3] Add PageGuidance wrapper to book search page (likely `frontend/src/features/BookSearch.tsx`) with title "Find Your Next Book 🔍", fun description and instructions per data-model.md; tone="child"; icon={<Search />}
- [ ] T028 [P] [US3] Add PageGuidance wrapper to reading progress page with title "Log Your Reading 📖", fun description per data-model.md, and encouragement (e.g., "Great job! 🎉 You're X% done!"); tone="child"
- [ ] T029 [P] [US3] Add PageGuidance wrapper to rewards shop page with title "Your Rewards Shop 🎁", celebratory description and instructions per data-model.md; tone="child"; icon={<Gift />}
- [ ] T030 [US3] Verify child page guidance text is accessible by screen reader: aria-labels, semantic HTML, sufficient contrast; test tone appropriateness with user feedback or cognitive accessibility review
- [ ] T031 [US3] Create visual reference/screenshot documentation of each child page with guidance text for design/tone review per quickstart.md Verification Checklist

**Checkpoint**: All child pages have engaging guidance text; SC-003 verified (first-time child navigation)

---

## Phase 6: User Story 4 - Responsive Design Consistency (Priority: P2)

**Goal**: All pages render correctly and maintain usability on mobile, tablet, and desktop without horizontal scrolling

**Independent Test**: Open any page on mobile (375px), tablet (768px), and desktop (1440px); verify readable text, touch-friendly buttons (44px), no horizontal scroll, responsive layout

### Responsive Testing & Fixes for User Story 4

- [ ] T032 [P] [US4] Create comprehensive Playwright E2E test `frontend/tests/e2e/responsive-design.spec.ts` covering: mobile (375px), tablet (768px), desktop (1440px); verify no horizontal scroll, button sizes ≥44px, text readable at each breakpoint per SC-004
- [ ] T033 [P] [US4] Audit all page components for responsive classes: add/verify `sm:`, `md:`, `lg:` Tailwind prefixes for grid, text size, padding to ensure proper reflow per research.md ¶4
- [ ] T034 [P] [US4] Test text zoom at 200% on mobile: verify no horizontal scroll, content remains accessible per Accessibility.contract.json responsive requirements
- [ ] T035 [US4] Fix any layout breakage identified in T032-T034: adjust grid columns, padding, text sizes for each breakpoint; test again to verify fixes
- [ ] T036 [US4] Test landscape orientation: rotate device from portrait to landscape on mobile/tablet; verify layout adapts without loss of functionality per SC-004
- [ ] T037 [US4] Create responsive design verification checklist: document all breakpoints tested, pages verified, issues found/fixed; attach screenshots per quickstart.md

**Checkpoint**: All pages render correctly across all breakpoints; SC-004 verified

---

## Phase 7: User Story 5 - Design System Documentation (Priority: P2)

**Goal**: Comprehensive documentation enabling developers to reuse components and design tokens consistently

**Independent Test**: New developers can reference component docs and create a new page using existing components without needing to hardcode colors/spacing

### Documentation for User Story 5

- [ ] T038 Create `frontend/src/components/ui/README.md` documenting each shadcn/ui component: props, usage examples, accessibility notes, responsive behavior per data-model.md §Component Taxonomy
- [ ] T039 Create `frontend/src/components/shared/README.md` documenting all shared/molecule components (PageGuidance, FormField, Button variants, etc.) with TypeScript interface signatures, usage examples, and design tokens applied
- [ ] T040 Create `frontend/src/theme.ts` inline JSDoc documentation for all exported theme objects (colors, spacing, breakpoints); include examples of how to use in components
- [ ] T041 Create `frontend/DESIGN_SYSTEM.md` comprehensive design system guide: design philosophy, color palette (with hex values), typography scale, spacing scale, breakpoints, component library overview, accessibility checklist, usage patterns per data-model.md §Design Tokens
- [ ] T042 [P] Create `frontend/COMPONENT_EXAMPLES.tsx` or Storybook-equivalent (if time permits) showing visual examples of all major components (Button variants, Card states, FormField with error, Modal, Tabs, Alert) with code snippets
- [ ] T043 Add TypeScript JSDoc comments to all new/refactored components: @param, @returns, @example blocks for IDE autocomplete and documentation generation per quickstart.md §Setup Guide

**Checkpoint**: Design system fully documented; future features can reuse patterns

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Accessibility verification, performance validation, and final quality checks

### Accessibility Audit & Fixes for Phase 8

- [ ] T044 Run automated accessibility audit using axe-core: `npx axe-core` on each page; document all violations and fixes needed per Accessibility.contract.json
- [ ] T045 [P] Manual screen reader test: test all pages with VoiceOver (Mac) and NVDA (Windows); verify form labels announced, error messages linked via aria-describedby, focus management in modals; document issues and fixes
- [ ] T046 [P] Manual keyboard navigation test: navigate entire app using only Tab, Enter, Escape, Arrow keys on each page; verify all interactive elements reachable and usable without mouse per Accessibility.contract.json
- [ ] T047 Verify color contrast: run contrast checker on all text colors; ensure ≥4.5:1 for normal text, ≥3:1 for large text and graphics per SC-006
- [ ] T048 Fix any accessibility violations identified in T044-T047; re-run automated audit to confirm zero critical violations, ≥95% pass rate per SC-006

### Performance & Bundle Size Validation

- [ ] T049 Measure bundle size: run `npm run build` and compare gzipped bundle size to baseline (~150KB); verify increase <50KB per SC-005
- [ ] T050 Measure page load time: use Lighthouse audit on each page (4G throttled); verify <3s load time, no increase >10% from baseline per SC-005
- [ ] T051 If bundle size or load time exceed targets: analyze slow components, consider tree-shaking, lazy load heavy components, optimize images

### Final QA & Deployment Prep

- [ ] T052 [P] Run full test suite: `npm run test` (unit tests via Vitest) and `npm run test:e2e` (E2E via Playwright); fix any failures
- [ ] T053 [P] Run linting: `npm run lint`; fix any style or TypeScript errors
- [ ] T054 Build locally: `npm run build`; verify no errors, check bundle report
- [ ] T055 Manual smoke test: test primary flows locally (parent login → dashboard → child mgmt; child login → reading list → add book → rewards)
- [ ] T056 Create visual regression baseline: take screenshots of all pages at mobile/tablet/desktop for future comparison
- [ ] T057 Create deployment notes: document any environment variables, breaking changes, migration steps needed for Render deployment per plan.md
- [ ] T058 Update `frontend/README.md` with new design system info, component library setup instructions, and testing commands

**Checkpoint**: Feature complete, tested, documented, ready for deployment

---

## Summary

| Phase | Priority | Task Count | Goal |
|-------|----------|-----------|------|
| **Phase 1: Setup** | Foundational | 5 | Design tokens, PageGuidance component, Tailwind config |
| **Phase 2: Foundational** | Foundational | 8 | Refactor shared components, establish consistency |
| **Phase 3: US1** | P1 | 7 | Modern UI framework adoption across all pages |
| **Phase 4: US2** | P1 | 5 | Parent page guidance (professional tone) |
| **Phase 5: US3** | P1 | 6 | Child page guidance (fun tone) |
| **Phase 6: US4** | P2 | 6 | Responsive design verification & fixes |
| **Phase 7: US5** | P2 | 6 | Design system documentation |
| **Phase 8: Polish** | Final | 7 | Accessibility, performance, QA, deployment prep |
| **TOTAL** | — | **58 tasks** | Complete UI modernization feature |

---

## Parallel Execution Map

**Safe to run in parallel** (different files, no dependencies):
- All T001-T005 (setup can happen simultaneously)
- T006-T010 (component refactoring in different files)
- T014-T015 (tests while components refactor)
- T017-T018 (different page refactoring)
- T021-T023 (different parent pages)
- T026-T029 (different child pages)
- T032-T034 (different test aspects)
- T044-T046 (different audit methods)

**MVP Scope** (minimum to ship US1–US3 basic functionality):
- Phase 1: All 5 tasks
- Phase 2: All 8 tasks (required before pages work)
- Phase 3: T014-T020 (7 tasks)
- Phase 4: T021-T025 (5 tasks)
- Phase 5: T026-T031 (6 tasks)
- **Subtotal**: 31 tasks for core feature
- Phase 6-8: Testing, documentation, and polish (27 additional tasks for production quality)

---

**Next**: Execute Phase 1 & 2 tasks in parallel; then proceed with user stories in priority order (US1→US2→US3→US4→US5).

**Estimated Effort**: 60–80 development hours for complete feature + thorough testing
