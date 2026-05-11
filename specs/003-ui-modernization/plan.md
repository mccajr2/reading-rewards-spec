# Implementation Plan: UI Modernization with Page Guidance

**Branch**: `003-ui-modernization` | **Date**: 2026-05-11 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/003-ui-modernization/spec.md`

## Summary

Apply a modern, production-ready React component library (e.g., shadcn/ui, Material-UI, Chakra UI) to modernize the visual appearance and interaction patterns across all pages. Simultaneously, add clear, purpose-driven guidance text to each page: parent-facing pages use professional adult tone; child-facing pages use fun, encouraging tone. Ensure responsive design for mobile, tablet, and desktop, and establish a reusable design system for future features.

**Technical Approach**: 
- Select and integrate a React-compatible, Tailwind-compatible component library (or headless library if custom styling preferred)
- Create a page layout component that wraps descriptions and usage guidance at the top of each page
- Audit existing pages, refactor component usage, and apply consistent spacing/typography
- Document all components in a reference guide (Storybook or inline)
- Verify responsive design and accessibility (WCAG 2.1 AA) across all breakpoints

## Technical Context

**Language/Version**: TypeScript (current); React 18+ (Vite 7)  
**Primary Dependencies**: React 18, Vite 7, Tailwind CSS 3+, chosen UI framework (e.g., shadcn/ui with Radix UI, or Material-UI, or Chakra UI)  
**Storage**: N/A (pure UI layer; backend APIs unchanged)  
**Testing**: Vitest 2.1.9 (unit), Playwright (E2E)  
**Target Platform**: Web (browser); responsive mobile/tablet/desktop  
**Project Type**: Web application (React SPA with Spring Boot backend)  
**Performance Goals**: Page load time <3s on 4G; no increase >10% vs. baseline  
**Constraints**: Bundle size increase <50KB gzip; 95% keyboard accessibility; WCAG 2.1 AA compliance  
**Scale/Scope**: ~15 pages (auth, dashboard, child mgmt, reading list, book search, rewards, etc.); ~50+ UI components  

## Constitution Check

**Gate: All items below must pass or have documented justification before Phase 0 research.**

✅ **I. Behavior Parity First**  
- ✅ No API endpoints or business logic changes; purely UI/UX modernization  
- ✅ Existing auth flows, permissions, and reward calculations preserved  
- ✅ Page routing and controller logic unchanged  
- ✅ **Status**: PASS — Feature is additive, not destructive

✅ **II. Spec Before Structure**  
- ✅ Detailed spec with 5 user stories, 9 requirements, and 6 success criteria created before implementation  
- ✅ Quality checklist completed; all ambiguities resolved  
- ✅ **Status**: PASS — Spec is complete and validated

✅ **III. Tests Are Delivery Criteria**  
- ✅ Component tests required for all new/refactored components (Vitest)  
- ✅ E2E smoke tests required for critical journeys (Playwright): parent login → dashboard, child login → reading list → search → rewards  
- ✅ Accessibility audit required (axe-core or similar)  
- ✅ **Status**: PASS — Testing requirements documented in requirements (FR-005, SC-006)

✅ **IV. Clean Architecture Over Legacy Shape**  
- ✅ Existing component structure to be improved, not reproduced  
- ✅ New page layout component (PageGuidance wrapper) creates clean separation between guidance text and page content  
- ✅ Design system tokens centralized (colors, spacing, typography)  
- ✅ **Status**: PASS — Opportunity to refactor toward cleaner module boundaries

✅ **V. Secure Configuration By Default**  
- ✅ No secrets required for UI modernization feature  
- ✅ No `.env` or configuration files needed  
- ✅ All UI text is static (no dynamic secret injection)  
- ✅ **Status**: PASS — No security hygiene issues

**Constitution Check Result**: ✅ **PASS** — Feature aligns with all core principles

## Project Structure

### Documentation (this feature)

```text
specs/003-ui-modernization/
├── plan.md              # This file
├── research.md          # Phase 0 output (research decisions)
├── data-model.md        # Phase 1 output (component model, page structure)
├── quickstart.md        # Phase 1 output (setup and usage guide)
├── contracts/           # Phase 1 output (component API contracts)
├── tasks.md             # Phase 2 output (actionable task list)
└── checklists/
    └── requirements.md  # Quality validation checklist
```

### Source Code (repository root)

```text
frontend/src/
├── components/
│   ├── shared/
│   │   ├── PageGuidance.tsx        # NEW: Wrapper for page guidance text
│   │   ├── Button.tsx              # REFACTOR: Adopt UI framework
│   │   ├── Card.tsx                # REFACTOR: Adopt UI framework
│   │   ├── Modal.tsx               # REFACTOR: Adopt UI framework
│   │   ├── Input.tsx               # REFACTOR: Adopt UI framework
│   │   ├── Navigation.tsx           # REFACTOR: Apply modern styling
│   │   └── ...                     # Other shared components
│   │
│   └── [feature-modules]/          # Existing structure preserved
│       ├── App.tsx                 # UPDATE: Use PageGuidance wrapper on pages
│       ├── ParentDashboard.tsx      # UPDATE: Modern styling + guidance text
│       ├── ChildReadingList.tsx     # UPDATE: Modern styling + guidance text
│       └── ...
│
├── styles/
│   ├── globals.css                 # REFACTOR: Add design tokens (colors, spacing, typography)
│   ├── tokens.css                  # NEW: Design system variables
│   └── ...
│
├── theme.ts                        # UPDATE: Design system configuration (colors, breakpoints)
└── tests/
    ├── components/
    │   ├── PageGuidance.test.tsx   # NEW: Test guidance rendering
    │   └── ...
    └── e2e/
        ├── parent-dashboard.spec.ts # UPDATE: Verify modern UI, guidance text visible
        ├── child-reading-list.spec.ts # UPDATE: Verify modern UI, guidance text visible
        └── ...

backend/                            # UNCHANGED - No backend changes for UI feature
└── [existing structure]
```

**Structure Decision**: The feature reuses the existing monorepo structure (backend + frontend folders in single repo). All UI work is in `frontend/src/`. A new `PageGuidance.tsx` component is the key architectural addition—it wraps page content and injects guidance text. Existing component hierarchy is preserved but refactored to use the chosen UI framework. Design tokens are centralized in `theme.ts` and `tokens.css`.

## Complexity Tracking

| Item | Status | Notes |
|------|--------|-------|
| **Constitution violations** | None ✅ | Feature is pure UI enhancement; no parity or config issues |
| **Scope creep risk** | Low ✅ | Clear feature boundary: UI + guidance text only; no business logic changes |
| **Dependency coupling** | Low ✅ | UI framework choice is isolated to component layer; API calls and state management unchanged |
| **Testing complexity** | Medium ⚠️ | Responsive testing (3+ breakpoints) + accessibility audit adds test matrix; manageable with Playwright + axe-core |
| **Performance risk** | Low ✅ | Constraint of <50KB bundle increase is achievable with tree-shaking; baseline comparison built into SC-005 |

---

## Phases & Workflow

### Phase 0: Research & Decisions

**Goal**: Resolve all unknowns and make framework/design system choices.

**Research Tasks**:
1. Compare React-compatible UI frameworks (shadcn/ui vs. Material-UI vs. Chakra UI vs. Headless UI)—evaluate TypeScript support, Tailwind compatibility, tree-shaking, bundle size, accessibility
2. Define design system: color palette, typography (font families, sizes, weights, line heights), spacing scale, component sizing
3. Audit existing pages for current styling patterns and component usage
4. Identify parent vs. child pages and tone guidelines
5. Establish accessibility baseline (WCAG 2.1 AA conformance standards)

**Output**: `research.md` (decisions captured)

---

### Phase 1: Design & Contracts

**Goal**: Document component architecture and design system; define page guidance structure.

**Deliverables**:

1. **data-model.md**: 
   - Component taxonomy (atomic design: atoms, molecules, organisms)
   - Page layout model (Header + Sidebar + PageGuidance + Content)
   - Page descriptions (5–7 pages per user type: parents + children)
   - Tone guidelines (adult vs. fun)

2. **contracts/**:
   - `PageGuidance.contract.json`: Component props interface (title, description, instructions)
   - `DesignTokens.contract.json`: Color, spacing, typography variables
   - `Accessibility.contract.json`: WCAG 2.1 AA requirements

3. **quickstart.md**:
   - Setup instructions (install UI framework, configure Tailwind, import tokens)
   - Component usage examples
   - Guidance text format guide (where/how to add descriptions)
   - Testing checklist (responsive, accessibility, E2E flows)

**Output**: `data-model.md`, `contracts/`, `quickstart.md`, plus agent context update

---

### Phase 2: Implementation Planning

**Goal**: Break down implementation into priority-ordered, independently testable tasks.

**Tasks** (generated by `/speckit.tasks` command, not here):
- Install and configure UI framework
- Create PageGuidance component and design tokens
- Refactor shared components (Button, Card, Input, Modal, Navigation)
- Add guidance text to parent pages (dashboard, child mgmt, rewards settings)
- Add guidance text to child pages (reading list, book search, rewards)
- Verify responsive design (mobile, tablet, desktop)
- Run accessibility audit and fix violations
- Update component tests; add E2E smoke tests
- Final visual review and polish

**Output**: `tasks.md` (created by `/speckit.tasks` command)

---

## Next Steps

1. ✅ **Phase 0 Research**: Prepare research decisions (UI framework choice, design system definition)
2. ✅ **Phase 1 Design**: Document component architecture and page guidance structure
3. ⏭️ **Phase 2 Planning**: Run `/speckit.tasks` to generate actionable task list
4. ⏭️ **Implementation**: Execute tasks in priority order
5. ⏭️ **Verification**: Run all tests (unit, E2E, accessibility audit) before marking complete

---

**Status**: ✅ Plan complete. Ready for Phase 0 research execution.
