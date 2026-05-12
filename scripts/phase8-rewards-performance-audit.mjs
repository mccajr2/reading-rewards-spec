#!/usr/bin/env node

import { mkdirSync, writeFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';

const targetUrl = process.env.REWARDS_AUDIT_URL ?? 'http://localhost:3000/child/rewards';
const outputDir = process.env.REWARDS_AUDIT_OUT_DIR ?? 'test-results/perf';
const outputJson = `${outputDir}/rewards-lighthouse.json`;

mkdirSync(outputDir, { recursive: true });

const args = [
  'lighthouse',
  targetUrl,
  '--output=json',
  `--output-path=${outputJson}`,
  '--only-categories=performance,accessibility,best-practices',
  '--quiet',
  '--chrome-flags=--headless=new --no-sandbox',
  '--throttling-method=simulate',
  '--preset=desktop',
];

const run = spawnSync('npx', args, { stdio: 'inherit' });
if (run.status !== 0) {
  console.error('Lighthouse run failed. Ensure frontend preview is running at:', targetUrl);
  process.exit(run.status ?? 1);
}

const summary = {
  targetUrl,
  outputJson,
  generatedAt: new Date().toISOString(),
  notes: [
    'Baseline target for SC-005: rewards page load < 1s on 4G simulated profile.',
    'Review TTI/LCP and accessibility score in generated JSON report.',
  ],
};

writeFileSync(`${outputDir}/rewards-lighthouse-summary.json`, JSON.stringify(summary, null, 2));
console.log('Performance baseline complete:', summary);
