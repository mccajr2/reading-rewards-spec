import { chromium } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';

const baseUrl = 'http://127.0.0.1:4173';
const outputDir = path.resolve('frontend/test-results/visual-regression/phase4-parent-guidance');

const viewports = [
  { key: 'mobile', width: 375, height: 812 },
  { key: 'tablet', width: 768, height: 1024 },
  { key: 'desktop', width: 1440, height: 900 }
];

const routes = ['/parent', '/parent/summary', '/rewards'];

function routeKey(route) {
  return route.replace(/^\//, '').replace(/\//g, '-') || 'home';
}

async function setParentSession(page) {
  const parentUser = {
    id: 'seed-parent',
    role: 'PARENT',
    firstName: 'Alex',
    username: 'alexparent',
    email: 'alex@example.com'
  };

  await page.evaluate((payload) => {
    localStorage.setItem('jwtToken', 'demo-token');
    localStorage.setItem('user', JSON.stringify(payload));
  }, parentUser);
}

async function main() {
  await fs.mkdir(outputDir, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  let count = 0;

  try {
    for (const viewport of viewports) {
      const context = await browser.newContext({
        viewport: { width: viewport.width, height: viewport.height }
      });
      const page = await context.newPage();

      await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' });
      await setParentSession(page);

      for (const route of routes) {
        await page.goto(`${baseUrl}${route}`, { waitUntil: 'networkidle' });
        await page.waitForTimeout(250);
        const file = `${routeKey(route)}-parent-${viewport.key}.png`;
        await page.screenshot({ path: path.join(outputDir, file), fullPage: true });
        count += 1;
      }

      await context.close();
    }

    console.log(`SCREENSHOTS_CREATED=${count}`);
    console.log(`ROUTES=${JSON.stringify(routes)}`);
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
