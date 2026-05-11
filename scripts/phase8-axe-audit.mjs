import { chromium } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';
import axe from 'axe-core';

const baseUrl = 'http://127.0.0.1:4173';
const outputPath = path.resolve('specs/003-ui-modernization/phase8-axe-results.json');

const roleRoutes = {
  unauth: ['/login', '/signup', '/verify-email'],
  child: ['/reading-list', '/search', '/history', '/rewards'],
  parent: ['/parent', '/parent/summary', '/rewards'],
};

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

async function stubApi(page) {
  await page.route('**/*', async (route) => {
    const url = route.request().url();
    if (!url.includes('/api/')) {
      await route.continue();
      return;
    }

    const body = (() => {
      if (url.includes('/bookreads/in-progress')) return [];
      if (url.includes('/history')) return [];
      if (url.includes('/rewards/summary')) return { totalEarned: 0, totalPaidOut: 0, totalSpent: 0, currentBalance: 0 };
      if (url.includes('/rewards?page=')) return { rewards: [], totalCount: 0 };
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
      if (url.endsWith('/parent/kids')) return [{ id: 'kid-1', firstName: 'Jamie', username: 'jamie' }];
      if (url.includes('/child-detail')) {
        return {
          child: { id: 'kid-1', firstName: 'Jamie', username: 'jamie' },
          books: [],
          rewards: [],
          totalEarned: 4,
          currentBalance: 1.5,
        };
      }
      if (url.includes('/credits')) return { cents: 0, dollars: 0 };
      return {};
    })();

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(body),
    });
  });
}

async function runAxe(page, role, route) {
  await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' });
  await setSession(page, role);
  await page.goto(`${baseUrl}${route}`, { waitUntil: 'networkidle' });
  await page.addScriptTag({ content: axe.source });
  const result = await page.evaluate(async () => {
    return window.axe.run(document, {
      runOnly: {
        type: 'tag',
        values: ['wcag2a', 'wcag2aa'],
      },
    });
  });

  return {
    role,
    route,
    violationCount: result.violations.length,
    violations: result.violations.map((v) => ({
      id: v.id,
      impact: v.impact,
      description: v.description,
      help: v.help,
      helpUrl: v.helpUrl,
      nodes: v.nodes.map((node) => ({
        target: node.target,
        failureSummary: node.failureSummary,
      })),
    })),
  };
}

async function main() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  const page = await context.newPage();

  await stubApi(page);

  const results = [];
  for (const [role, routes] of Object.entries(roleRoutes)) {
    for (const route of routes) {
      const result = await runAxe(page, role, route);
      results.push(result);
      process.stdout.write(`AUDITED ${role}:${route} violations=${result.violationCount}\n`);
    }
  }

  const totalViolations = results.reduce((sum, r) => sum + r.violationCount, 0);
  const colorContrastIssues = results.flatMap((r) => r.violations.filter((v) => v.id === 'color-contrast'));

  const report = {
    generatedAt: new Date().toISOString(),
    totalPages: results.length,
    totalViolations,
    colorContrastIssueCount: colorContrastIssues.length,
    pages: results,
  };

  await fs.writeFile(outputPath, JSON.stringify(report, null, 2), 'utf8');

  await context.close();
  await browser.close();

  process.stdout.write(`AXE_TOTAL_VIOLATIONS=${totalViolations}\n`);
  process.stdout.write(`AXE_COLOR_CONTRAST_ISSUES=${colorContrastIssues.length}\n`);
  process.stdout.write(`AXE_REPORT=${outputPath}\n`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
