import { chromium } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';

const baseUrl = 'http://127.0.0.1:4173';
const outputDir = path.resolve('frontend/test-results/visual-regression/phase8-baseline');

const viewports = [
  { key: 'mobile', width: 375, height: 812 },
  { key: 'tablet', width: 768, height: 1024 },
  { key: 'desktop', width: 1440, height: 900 },
];

const routesByRole = {
  unauth: ['/login', '/signup', '/verify-email'],
  child: ['/reading-list', '/search', '/history', '/rewards'],
  parent: ['/parent', '/parent/summary', '/rewards'],
};

function routeKey(route) {
  return route.replace(/^\//, '').replace(/\//g, '-') || 'home';
}

function sessionUser(role) {
  if (role === 'child') {
    return { id: 'seed-child', role: 'CHILD', firstName: 'Jamie', username: 'jamie' };
  }
  if (role === 'parent') {
    return { id: 'seed-parent', role: 'PARENT', firstName: 'Alex', username: 'alexparent' };
  }
  return null;
}

async function setSession(page, role) {
  await page.evaluate(({ role, user }) => {
    if (role === 'unauth') {
      localStorage.removeItem('jwtToken');
      localStorage.removeItem('user');
      return;
    }
    localStorage.setItem('jwtToken', 'test-token');
    localStorage.setItem('user', JSON.stringify(user));
  }, { role, user: sessionUser(role) });
}

async function captureRole(browser, role, routes, viewport) {
  const context = await browser.newContext({ viewport: { width: viewport.width, height: viewport.height } });
  const page = await context.newPage();
  let count = 0;

  await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' });
  await setSession(page, role);

  for (const route of routes) {
    await page.goto(`${baseUrl}${route}`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(250);
    const filename = `${routeKey(route)}-${role}-${viewport.key}.png`;
    await page.screenshot({ path: path.join(outputDir, filename), fullPage: true });
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
    for (const viewport of viewports) {
      total += await captureRole(browser, 'unauth', routesByRole.unauth, viewport);
      total += await captureRole(browser, 'child', routesByRole.child, viewport);
      total += await captureRole(browser, 'parent', routesByRole.parent, viewport);
    }
  } finally {
    await browser.close();
  }

  process.stdout.write(`SCREENSHOTS_CREATED=${total}\n`);
  process.stdout.write(`BASELINE_DIR=${outputDir}\n`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
