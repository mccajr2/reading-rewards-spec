import { expect, test } from '@playwright/test';
import * as axe from 'axe-core';

import { createKid, login, signupAndVerify, uniqueEmail, uniqueUsername } from './helpers';

const axeSource = axe.source;

async function runAxe(page: import('@playwright/test').Page) {
  await page.addScriptTag({ content: axeSource });
  return page.evaluate(async () => {
    // @ts-expect-error axe is injected at runtime in browser context.
    return axe.run(document, {
      runOnly: {
        type: 'tag',
        values: ['wcag2a', 'wcag2aa'],
      },
    });
  });
}

test.describe('Rewards accessibility checks', () => {
  test('child and parent rewards pages have no critical/serious axe violations', async ({ page, request }) => {
    const parentEmail = uniqueEmail('a11y-parent');
    const parentPassword = 'Password1!';
    const childUsername = uniqueUsername('a11y-child');
    const childPassword = 'KidPass1!';

    await signupAndVerify(request, parentEmail, parentPassword, 'A11y', 'Parent');
    const parentToken = await login(request, parentEmail, parentPassword);
    await createKid(request, parentToken, childUsername, 'A11yKid', childPassword);

    await page.goto('/login');
    await page.getByLabel('Username or Email').fill(childUsername);
    await page.getByLabel('Password').fill(childPassword);
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page).not.toHaveURL(/\/login/, { timeout: 8_000 });
    await page.goto('/child/rewards');
    await expect(page.getByRole('heading', { name: /your rewards shop/i })).toBeVisible();

    const childResults = await runAxe(page);
    const childBlocking = childResults.violations.filter((v) => v.impact === 'critical' || v.impact === 'serious');
    expect(childBlocking, JSON.stringify(childBlocking, null, 2)).toHaveLength(0);

    await page.getByRole('button', { name: /logout/i }).click();

    await page.getByLabel('Username or Email').fill(parentEmail);
    await page.getByLabel('Password').fill(parentPassword);
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page).not.toHaveURL(/\/login/, { timeout: 8_000 });
    await page.goto('/parent/rewards');
    await expect(page.getByRole('heading', { name: /manage rewards/i })).toBeVisible();

    const parentResults = await runAxe(page);
    const parentBlocking = parentResults.violations.filter((v) => v.impact === 'critical' || v.impact === 'serious');
    expect(parentBlocking, JSON.stringify(parentBlocking, null, 2)).toHaveLength(0);
  });
});
