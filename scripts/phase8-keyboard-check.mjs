import { chromium } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';

const baseUrl = 'http://127.0.0.1:4173';
const outputPath = path.resolve('specs/003-ui-modernization/phase8-keyboard-results.json');

async function stubApi(page) {
  await page.route('**/*', async (route) => {
    const url = route.request().url();
    if (!url.includes('/api/')) {
      await route.continue();
      return;
    }

    const body = (() => {
      if (url.endsWith('/parent/kids')) return [{ id: 'kid-1', firstName: 'Jamie', username: 'jamie' }];
      if (url.includes('/parent/kids/summary')) {
        return {
          kids: [
            {
              id: 'kid-1',
              firstName: 'Jamie',
              username: 'jamie',
              booksRead: 2,
              chaptersRead: 8,
              totalEarned: 4,
              currentBalance: 1.5,
            },
          ],
        };
      }
      if (url.includes('/credits')) return { cents: 0, dollars: 0 };
      if (url.includes('/bookreads/in-progress')) return [];
      if (url.includes('/history')) return [];
      if (url.includes('/rewards/summary')) return { totalEarned: 0, totalPaidOut: 0, totalSpent: 0, currentBalance: 0 };
      if (url.includes('/rewards?page=')) return { rewards: [], totalCount: 0 };
      return {};
    })();

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(body),
    });
  });
}

async function setSession(page, role) {
  await page.evaluate((currentRole) => {
    if (currentRole === 'none') {
      localStorage.removeItem('jwtToken');
      localStorage.removeItem('user');
      return;
    }

    const user = currentRole === 'CHILD'
      ? { id: 'seed-child', role: 'CHILD', firstName: 'Jamie', username: 'jamie' }
      : { id: 'seed-parent', role: 'PARENT', firstName: 'Alex', username: 'alexparent' };

    localStorage.setItem('jwtToken', 'test-token');
    localStorage.setItem('user', JSON.stringify(user));
  }, role);
}

async function tabProbe(page, route, role) {
  await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' });
  await setSession(page, role);
  await page.goto(`${baseUrl}${route}`, { waitUntil: 'networkidle' });

  const focusLog = [];
  for (let i = 0; i < 8; i += 1) {
    await page.keyboard.press('Tab');
    const info = await page.evaluate(() => {
      const el = document.activeElement;
      if (!el) return null;
      return {
        tag: el.tagName.toLowerCase(),
        text: (el.textContent || '').trim().slice(0, 60),
        ariaLabel: el.getAttribute('aria-label'),
      };
    });

    if (info) focusLog.push(info);
  }

  return {
    route,
    role,
    focusablesSeen: focusLog.length,
    focusLog,
  };
}

async function arrowMenuProbe(page, role, route) {
  await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' });
  await setSession(page, role);
  await page.goto(`${baseUrl}${route}`, { waitUntil: 'networkidle' });

  await page.keyboard.press('Tab');
  await page.keyboard.press('Tab');
  await page.keyboard.press('ArrowRight');

  const active = await page.evaluate(() => {
    const el = document.activeElement;
    return {
      tag: el?.tagName.toLowerCase(),
      text: (el?.textContent || '').trim().slice(0, 60),
    };
  });

  return { role, route, activeAfterArrow: active };
}

async function escapeModalProbe(page) {
  await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' });
  await setSession(page, 'PARENT');
  await page.goto(`${baseUrl}/parent`, { waitUntil: 'networkidle' });

  await page.getByRole('button', { name: /reset password/i }).click();
  const openBeforeEscape = await page.getByRole('dialog').isVisible();
  await page.keyboard.press('Escape');
  const openAfterEscape = await page.getByRole('dialog').count();

  return {
    openBeforeEscape,
    dialogCountAfterEscape: openAfterEscape,
  };
}

async function main() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  const page = await context.newPage();

  await stubApi(page);

  const probes = [];
  probes.push(await tabProbe(page, '/login', 'none'));
  probes.push(await tabProbe(page, '/reading-list', 'CHILD'));
  probes.push(await tabProbe(page, '/parent', 'PARENT'));

  const arrowChecks = [];
  arrowChecks.push(await arrowMenuProbe(page, 'CHILD', '/reading-list'));
  arrowChecks.push(await arrowMenuProbe(page, 'PARENT', '/parent'));

  const escapeCheck = await escapeModalProbe(page);

  const report = {
    generatedAt: new Date().toISOString(),
    probes,
    arrowChecks,
    escapeCheck,
  };

  await fs.writeFile(outputPath, JSON.stringify(report, null, 2), 'utf8');
  await context.close();
  await browser.close();

  process.stdout.write(`KEYBOARD_REPORT=${outputPath}\n`);
  process.stdout.write(`KEYBOARD_PROBES=${probes.length}\n`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
