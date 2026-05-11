import { test, expect } from '@playwright/test';
import { apiUrl, signupAndVerify, uniqueEmail, uniqueUsername, createKid, login } from './helpers';

/**
 * P2 History and rewards journey (US3 gap G3):
 * Child reads a chapter via API, then navigates to the history and rewards pages
 * via UI and verifies the data is displayed correctly.
 */

test.describe('Child history and rewards pages', () => {
  let parentEmail: string;
  let kidUsername: string;
  const password = 'Password1!';
  const kidPassword = 'KidPass1!';

  test.beforeEach(async ({ request }) => {
    parentEmail = uniqueEmail('hparent');
    kidUsername = uniqueUsername('hreader');

    await signupAndVerify(request, parentEmail, password, 'History', 'Parent');
    const parentToken = await login(request, parentEmail, password);
    await createKid(request, parentToken, kidUsername, 'HistoryKid', kidPassword);
  });

  test('child history page shows completed chapter reads', async ({ page, request }) => {
    // Seed: add book + chapter + mark read via API
    const kidToken = await login(request, kidUsername, kidPassword);
    const headers = { Authorization: `Bearer ${kidToken}`, 'Content-Type': 'application/json' };
    const googleBookId = `e2e-history-book-${Date.now()}`;

    const addRes = await request.post(apiUrl('/books'), {
      data: {
        googleBookId,
        title: 'History Test Book',
        authors: ['Test Author'],
        description: 'desc',
        thumbnailUrl: null,
      },
      headers,
    });
    expect(addRes.ok()).toBeTruthy();
    const bookRead = await addRes.json();

    const chapRes = await request.post(apiUrl(`/books/${googleBookId}/chapters`), {
      data: [{ name: 'Chapter One', chapterIndex: 0 }],
      headers,
    });
    expect(
      chapRes.ok(),
      `chapters create failed: ${chapRes.status()} ${await chapRes.text()}`
    ).toBeTruthy();
    const chapters = await chapRes.json();
    const chapterId = chapters[0].id;

    const markRes = await request.post(
      apiUrl(`/bookreads/${bookRead.id}/chapters/${chapterId}/read`),
      { headers }
    );
    expect(markRes.ok()).toBeTruthy();

    // Log in via UI and navigate to history page
    await page.goto('/login');
    await page.getByLabel('Username or Email').fill(kidUsername);
    await page.getByLabel('Password').fill(kidPassword);
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page).not.toHaveURL(/\/login/, { timeout: 8_000 });

    await page.goto('/history');
    await expect(page.getByRole('heading', { name: /log your reading/i })).toBeVisible({ timeout: 5_000 });
    await expect(page.getByText('History Test Book')).toBeVisible({ timeout: 5_000 });
    await expect(page.getByText('Chapter One')).toBeVisible({ timeout: 5_000 });
  });

  test('child rewards page shows earned balance after reading a chapter', async ({ page, request }) => {
    // Seed: add book + chapter + mark read via API
    const kidToken = await login(request, kidUsername, kidPassword);
    const headers = { Authorization: `Bearer ${kidToken}`, 'Content-Type': 'application/json' };
    const googleBookId = `e2e-rewards-book-${Date.now()}`;

    const addRes = await request.post(apiUrl('/books'), {
      data: {
        googleBookId,
        title: 'Rewards Test Book',
        authors: ['Test Author'],
        description: 'desc',
        thumbnailUrl: null,
      },
      headers,
    });
    expect(addRes.ok()).toBeTruthy();
    const bookRead = await addRes.json();

    const chapRes = await request.post(apiUrl(`/books/${googleBookId}/chapters`), {
      data: [{ name: 'Chapter One', chapterIndex: 0 }],
      headers,
    });
    expect(
      chapRes.ok(),
      `chapters create failed: ${chapRes.status()} ${await chapRes.text()}`
    ).toBeTruthy();
    const chapters = await chapRes.json();
    const chapterId = chapters[0].id;

    await request.post(apiUrl(`/bookreads/${bookRead.id}/chapters/${chapterId}/read`), { headers });

    // Log in via UI and navigate to rewards page
    await page.goto('/login');
    await page.getByLabel('Username or Email').fill(kidUsername);
    await page.getByLabel('Password').fill(kidPassword);
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page).not.toHaveURL(/\/login/, { timeout: 8_000 });

    await page.goto('/rewards');
    await expect(page.getByRole('heading', { name: /your rewards shop/i })).toBeVisible({ timeout: 5_000 });
    // One chapter read = $1.00 earned
    await expect(page.getByText('$1.00').first()).toBeVisible({ timeout: 5_000 });
  });
});
