# Quickstart: Reading Rewards Parity Rebuild

## Current bootstrap state

- This repository is initialized with Git and GitHub Spec Kit.
- Spec Kit is installed locally in `.venv` using Python 3.12.
- The legacy behavior source remains `/Users/jasonmccarthy/projects/reading-rewards`.

## Immediate next steps

1. Scaffold the backend using the approved runtime and package structure.
2. Scaffold the frontend using React, TypeScript, Vite, and plain CSS.
3. Implement the auth and parity-critical contracts first.
4. Add backend, frontend, and Playwright tests alongside the implementation.

## Local tool requirements

- Java 25 runtime for the new backend target.
- Node.js 20 or newer.
- Python 3.12 for the repo-local Spec Kit tooling.
- Docker for PostgreSQL and end-to-end environment setup.

## Spec Kit commands

- `./.venv/bin/specify check`
- `./.venv/bin/specify version`

## Repository goals

- Keep the old app unchanged.
- Keep all new work isolated to this repository.
- Preserve product behavior while improving structure and tests.