# Frontend: Reading Rewards Spec

This frontend uses React + Vite + TypeScript with a tokenized design system and shared guidance wrappers.

## Stack

- React 19
- Vite 7
- TypeScript 5.9
- Tailwind CSS
- shadcn/Radix-style shared UI wrappers
- Vitest for unit tests

## Design System Files

- `src/styles/tokens.css`: CSS custom properties for color, spacing, typography
- `src/theme.ts`: typed theme tokens and breakpoints
- `src/components/shared/PageGuidance.tsx`: guidance wrapper for parent/child page context
- `DESIGN_SYSTEM.md`: design principles and usage reference
- `COMPONENT_EXAMPLES.tsx`: visual component examples

## Commands

From this `frontend` directory:

```bash
npm install
npm run dev
npm run test
npm run build
npm run preview
```

From repository root (E2E):

```bash
npm run test:e2e
npm run test:e2e:ui
npm run test:e2e:report
```

## Accessibility and Performance Artifacts

Phase 8 validation outputs live in:

- `../specs/003-ui-modernization/phase8-axe-results.json`
- `../specs/003-ui-modernization/phase8-keyboard-results.json`
- `../specs/003-ui-modernization/lighthouse-login.json`
- `../specs/003-ui-modernization/lighthouse-signup.json`
- `../specs/003-ui-modernization/lighthouse-verify-email.json`
- `test-results/visual-regression/phase8-baseline`

## Notes

- Barcode scanner code path lazy-loads `@zxing/library` to improve initial payload behavior.
- Manual screen reader verification and full E2E stabilization are tracked in Phase 8 completion notes.
