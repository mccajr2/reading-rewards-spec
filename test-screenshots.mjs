import { chromium } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';

const baseUrl = 'http://127.0.0.1:4173';
const outputDir = path.resolve('frontend/test-results/visual-regression/phase3');

const viewports = [
  { key: 'mobile', width: 375, height: 812 },
  { key: 'tablet', width: 768, height: 1024 },
  { key: 'desktop', width: 1440, height: 900 }
];

const routes = {
  unauth: ['/login'],
  child: ['/search', '/reading-list', '/history', '/rewards'],
  parent: ['/parent', '/parent/summary']
};

function routeKey(route) {
  if (route === '/' || route === '') return 'home';
  return route.replace(/^\//, '').replace(/\//g, '-');
}

async function setSession(page, role) {
  if (role === 'unauth') {
    await page.evaluate(() => {
      localStorage.removeItem('jwtToken');
      localStorage.removeItem('user');
    });
    return;
  }

  const user = role === 'child'
    ? { id: 'seed-child', role: 'CHILD', firstName: 'Jamie', username: 'jamie' }
    : { id: 'seed-parent', role: 'PARENT', firstName: 'Alex', username: 'alexparent', email: 'alex@example.com' };

  await page.evaluate((payload) => {
    localStorage.setItem('jwtToken', 'demo-token');
    localStorage.setItem('user', JSON.stringify(payload));
  }, user);
}

async function captureSession(browser, sessionName, sessionRoutes) {
  let count = 0;

  for (const viewport of viewports) {
    const context = await browser.newContext({ viewport: { width: viewport.width, height: viewport.height } });
    const page = await context.newPage();

    await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' });
    await setSession(page, sessionName);

    for (const route of sessionRoutes) {
      const target = `${baseUrl}${route}`;
      await page.goto(target, { waitUntil: 'networkidle' });
      await page.waitForTimeout(250);
      const filename = `${routeKey(route)}-${sessionName}-${viewport.key}.png`;
      const filepath = path.join(outputDir, filename);
      await page.screenshot({ path: filepath, fullPage: true });
      count += 1;
    }

    await context.close();
  }

  return count;
}

async function main() {
  await fs.mkdir(outputDir, { recursive: true });
  const browser = await chromium.launch({ headless: true });

  try {
    const counts = {};
    counts.unauth = await captureSession(browser, 'unauth', routes.unauth);
    counts.child = await captureSession(browser, 'child', routes.child);
    counts.parent = await captureSession(browser, 'parent', routes.parent);

    const total = counts.unauth + counts.child + counts.parent;
    console.log(`SCREENSHOTS_CREATED=${total}`);
    console.log(`COUNTS=${JSON.stringify(counts)}`);
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
