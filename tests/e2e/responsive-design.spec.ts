import { expect, test, type Page } from '@playwright/test';

type Role = 'CHILD' | 'PARENT';

type ViewportSpec = {
  name: string;
  width: number;
  height: number;
};

const portraitViewports: ViewportSpec[] = [
  { name: 'mobile', width: 375, height: 812 },
  { name: 'tablet', width: 768, height: 1024 },
  { name: 'desktop', width: 1440, height: 900 },
];

const landscapeViewports: ViewportSpec[] = [
  { name: 'mobile-landscape', width: 812, height: 375 },
  { name: 'tablet-landscape', width: 1024, height: 768 },
];

const roleRoutes: Record<Role, string[]> = {
  CHILD: ['/reading-list', '/search', '/history', '/rewards'],
  PARENT: ['/parent', '/parent/summary'],
};

async function setSession(page: Page, role: Role) {
  await page.evaluate((sessionRole) => {
    const user = sessionRole === 'CHILD'
      ? { id: 'seed-child', role: 'CHILD', firstName: 'Jamie', username: 'jamie' }
      : { id: 'seed-parent', role: 'PARENT', firstName: 'Alex', username: 'alexparent' };

    localStorage.setItem('jwtToken', 'test-token');
    localStorage.setItem('user', JSON.stringify(user));
  }, role);
}

async function stubApi(page: Page) {
  await page.route('**/*', async (route) => {
    const url = route.request().url();
    if (!url.includes('/api/')) {
      await route.continue();
      return;
    }

    const json = (() => {
      if (url.includes('/bookreads/in-progress')) return [];
      if (url.includes('/history')) return [];
      if (url.includes('/rewards/summary')) {
        return { totalEarned: 0, totalPaidOut: 0, totalSpent: 0, currentBalance: 0 };
      }
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
      if (url.endsWith('/parent/kids')) {
        return [{ id: 'kid-1', firstName: 'Jamie', username: 'jamie' }];
      }
      if (url.includes('/child-detail')) {
        return {
          child: { id: 'kid-1', firstName: 'Jamie', username: 'jamie' },
          books: [],
          rewards: [],
          totalEarned: 4,
          currentBalance: 1.5,
        };
      }
      return {};
    })();

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(json),
    });
  });
}

async function expectNoHorizontalOverflow(page: Page, context: string) {
  const overflow = await page.evaluate(() => {
    const viewportWidth = window.innerWidth;
    const docWidth = document.documentElement.scrollWidth;
    const offenders = Array.from(document.querySelectorAll<HTMLElement>('*'))
      .map((node) => {
        const rect = node.getBoundingClientRect();
        return {
          tag: node.tagName.toLowerCase(),
          className: node.className,
          right: rect.right,
          left: rect.left,
          width: rect.width,
          text: (node.textContent ?? '').trim().slice(0, 80),
        };
      })
      .filter((item) => item.right > viewportWidth + 1)
      .slice(0, 5);

    return {
      hasOverflow: docWidth > viewportWidth + 4,
      viewportWidth,
      docWidth,
      offenders,
    };
  });

  expect(overflow.hasOverflow, `${context} overflow diagnostics: ${JSON.stringify(overflow)}`).toBeFalsy();
}

async function expectReadableText(page: Page) {
  const sizes = await page.evaluate(() => {
    const bodySize = parseFloat(getComputedStyle(document.body).fontSize || '0');
    const heading = document.querySelector('h1');
    const headingSize = heading ? parseFloat(getComputedStyle(heading).fontSize || '0') : 0;
    return { bodySize, headingSize };
  });

  expect(sizes.bodySize).toBeGreaterThanOrEqual(14);
  expect(sizes.headingSize).toBeGreaterThanOrEqual(20);
}

async function expectTouchTargets(page: Page) {
  const tooSmallButtons = await page.evaluate(() => {
    return Array.from(document.querySelectorAll('button'))
      .map((button) => {
        const style = getComputedStyle(button);
        const rect = button.getBoundingClientRect();
        const visible = style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0;
        return {
          visible,
          text: button.textContent?.trim() ?? '(icon)',
          width: rect.width,
          height: rect.height,
        };
      })
      .filter((button) => button.visible)
      .filter((button) => button.width < 44 || button.height < 44)
      .slice(0, 5);
  });

  expect(tooSmallButtons).toEqual([]);
}

test.describe('Responsive design coverage', () => {
  for (const viewport of portraitViewports) {
    test(`no overflow and readable layout at ${viewport.name}`, async ({ page }) => {
      await page.setViewportSize({ width: viewport.width, height: viewport.height });
      await stubApi(page);

      for (const role of ['CHILD', 'PARENT'] as const) {
        await page.goto('/login');
        await setSession(page, role);

        for (const route of roleRoutes[role]) {
          await page.goto(route, { waitUntil: 'networkidle' });
          await expect(page.locator('h1').first()).toBeVisible();
          await expectNoHorizontalOverflow(page, `${viewport.name}:${role}:${route}`);
          await expectReadableText(page);
          await expectTouchTargets(page);
        }
      }
    });
  }

  test('200% text zoom remains usable on mobile width', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 });
    await stubApi(page);

    await page.goto('/login');
    await setSession(page, 'CHILD');

    for (const route of roleRoutes.CHILD) {
      await page.goto(route, { waitUntil: 'networkidle' });
      await page.evaluate(() => {
        document.documentElement.style.fontSize = '200%';
      });

      await expect(page.locator('h1').first()).toBeVisible();
      await expectNoHorizontalOverflow(page, `zoom-mobile:CHILD:${route}`);
    }
  });

  for (const viewport of landscapeViewports) {
    test(`landscape orientation remains usable at ${viewport.name}`, async ({ page }) => {
      await page.setViewportSize({ width: viewport.width, height: viewport.height });
      await stubApi(page);

      await page.goto('/login');
      await setSession(page, 'CHILD');

      for (const route of ['/reading-list', '/search', '/rewards']) {
        await page.goto(route, { waitUntil: 'networkidle' });
        await expect(page.locator('h1').first()).toBeVisible();
        await expectNoHorizontalOverflow(page, `${viewport.name}:CHILD:${route}`);
      }
    });
  }
});
