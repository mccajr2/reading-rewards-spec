import { chromium } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';

const baseUrl = 'http://127.0.0.1:4173';
const outputDir = path.resolve('frontend/test-results/visual-regression/phase6-responsive');

const portraitViewports = [
  { key: 'mobile', width: 375, height: 812 },
  { key: 'tablet', width: 768, height: 1024 },
  { key: 'desktop', width: 1440, height: 900 }
];

const landscapeViewports = [
  { key: 'mobile-landscape', width: 812, height: 375 },
  { key: 'tablet-landscape', width: 1024, height: 768 }
];

const childRoutes = ['/reading-list', '/search', '/history', '/rewards'];
const parentRoutes = ['/parent', '/parent/summary'];

function routeKey(route) {
  return route.replace(/^\//, '').replace(/\//g, '-') || 'home';
}

async function setSession(page, role) {
  const user = role === 'CHILD'
    ? { id: 'seed-child', role: 'CHILD', firstName: 'Jamie', username: 'jamie' }
    : { id: 'seed-parent', role: 'PARENT', firstName: 'Alex', username: 'alexparent' };

  await page.evaluate((payload) => {
    localStorage.setItem('jwtToken', 'demo-token');
    localStorage.setItem('user', JSON.stringify(payload));
  }, user);
}

async function captureSet(browser, role, routes, viewport, mode = 'portrait') {
  const context = await browser.newContext({ viewport: { width: viewport.width, height: viewport.height } });
  const page = await context.newPage();
  let count = 0;

  await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' });
  await setSession(page, role);

  for (const route of routes) {
    await page.goto(`${baseUrl}${route}`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(250);
    const file = `${routeKey(route)}-${role.toLowerCase()}-${viewport.key}-${mode}.png`;
    await page.screenshot({ path: path.join(outputDir, file), fullPage: true });
    count += 1;
  }

  await context.close();
  return count;
}

async function main() {
  await fs.mkdir(outputDir, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  let total = 0;

  try {
    for (const viewport of portraitViewports) {
      total += await captureSet(browser, 'CHILD', childRoutes, viewport, 'portrait');
      total += await captureSet(browser, 'PARENT', parentRoutes, viewport, 'portrait');
    }

    for (const viewport of landscapeViewports) {
      total += await captureSet(browser, 'CHILD', ['/reading-list', '/rewards'], viewport, 'landscape');
      total += await captureSet(browser, 'PARENT', ['/parent'], viewport, 'landscape');
    }

    console.log(`SCREENSHOTS_CREATED=${total}`);
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
