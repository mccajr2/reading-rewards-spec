import { test, expect } from '@playwright/test';
import { signupAndVerify, uniqueEmail, uniqueUsername, createKid, login } from './helpers';

async function loginViaUi(page: import('@playwright/test').Page, username: string, password: string) {
  await page.goto('/login');
  await page.getByLabel('Username or Email').fill(username);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: /sign in/i }).click();
  await expect(page).not.toHaveURL(/\/login/, { timeout: 8_000 });
}

test('chapter count prompt seeds chapters for first reader and reuses for second reader', async ({ page, request }) => {
  const password = 'Password1!';
  const kidPassword = 'KidPass1!';

  const parentEmail1 = uniqueEmail('seed-parent-1');
  const kidUsername1 = uniqueUsername('seed-kid-1');
  const parentEmail2 = uniqueEmail('seed-parent-2');
  const kidUsername2 = uniqueUsername('seed-kid-2');

  await signupAndVerify(request, parentEmail1, password, 'Seed', 'ParentOne');
  const parentToken1 = await login(request, parentEmail1, password);
  await createKid(request, parentToken1, kidUsername1, 'KidOne', kidPassword);

  await signupAndVerify(request, parentEmail2, password, 'Seed', 'ParentTwo');
  const parentToken2 = await login(request, parentEmail2, password);
  await createKid(request, parentToken2, kidUsername2, 'KidTwo', kidPassword);

  const sharedGoogleBookId = `e2e-shared-seeded-book-${Date.now()}`;
  const mockSearchResult = [{
    googleBookId: sharedGoogleBookId,
    title: 'E2E Shared Chapter Seed Book',
    authors: ['E2E Author'],
    description: 'Used to verify chapter seeding and reuse behavior.',
    thumbnailUrl: null,
  }];

  await page.route('**/api/search?**', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockSearchResult),
    });
  });

  // First reader: should see prompt and seed Chapter 1..3.
  await loginViaUi(page, kidUsername1, kidPassword);
  await page.goto('/search');
  await page.getByPlaceholder('Title').fill('anything');
  await page.getByRole('button', { name: /^search$/i }).click();
  await expect(page.getByText('E2E Shared Chapter Seed Book')).toBeVisible({ timeout: 5_000 });

  let firstPromptSeen = false;
  page.once('dialog', async dialog => {
    firstPromptSeen = true;
    expect(dialog.type()).toBe('prompt');
    expect(dialog.message()).toContain('How many chapters are in this book?');
    await dialog.accept('3');
  });

  await page.getByRole('button', { name: /add to reading list/i }).click();
  await expect(page).toHaveURL(/\/reading-list/, { timeout: 8_000 });
  await expect(page.getByText('Chapter 1')).toBeVisible({ timeout: 8_000 });
  await expect(page.getByText('Chapter 3')).toBeVisible({ timeout: 8_000 });
  expect(firstPromptSeen).toBeTruthy();

  await page.getByRole('button', { name: /logout/i }).click();
  await expect(page).toHaveURL(/\/login/, { timeout: 8_000 });

  // Second reader: same book should reuse existing chapters and skip prompt.
  await loginViaUi(page, kidUsername2, kidPassword);
  await page.goto('/search');
  await page.getByPlaceholder('Title').fill('anything else');
  await page.getByRole('button', { name: /^search$/i }).click();
  await expect(page.getByText('E2E Shared Chapter Seed Book')).toBeVisible({ timeout: 5_000 });

  let secondDialogCount = 0;
  const secondDialogListener = async (dialog: import('@playwright/test').Dialog) => {
    secondDialogCount += 1;
    await dialog.dismiss();
  };
  page.on('dialog', secondDialogListener);

  await page.getByRole('button', { name: /add to reading list/i }).click();
  await expect(page).toHaveURL(/\/reading-list/, { timeout: 8_000 });
  await expect(page.getByText('Chapter 1')).toBeVisible({ timeout: 8_000 });
  await expect(page.getByText('Chapter 3')).toBeVisible({ timeout: 8_000 });
  expect(secondDialogCount).toBe(0);

  page.off('dialog', secondDialogListener);
});
