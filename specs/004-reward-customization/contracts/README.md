# Reward Customization Contract Mapping Baseline

This document maps the initial task set to contract artifacts for feature 004.

## Contract Files

- parent-reward-api.md: Parent reward configuration, accumulation, payout, and encouragement APIs.
- child-reward-api.md: Child reward selection, progress tracking, balances, and payout reminder APIs.

## Task-to-Contract Baseline

- T001-T002: DB migration scaffolding supports both parent and child contract persistence.
- T003: Domain package scaffold supports shared reward entities used by both contracts.
- T004: Frontend reward API service stub aligns with parent and child reward contract endpoints.
- T005: Frontend messaging API service stub aligns with reminder and encouragement endpoints.
- T006: E2E shell reserved for end-to-end validation across both contracts.
- T007: This mapping baseline establishes source-of-truth linkage for subsequent tasks.