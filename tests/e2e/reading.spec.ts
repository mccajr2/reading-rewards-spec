import { test, expect } from '@playwright/test';
import { signupAndVerify, uniqueEmail, uniqueUsername, createKid, login } from './helpers';

/**
 * P1 Reading journey:
 * Parent creates a child account → child logs in → adds books, marks chapters read, earns credits.
 *
 * Note: We use a hard-coded book DTO to avoid dependency on the Google Books external API.
 * API shape:
 *  - POST /api/books takes BookSummaryDto → returns { id (UUID), googleBookId, ... }
 *  - POST /api/books/{googleBookId}/chapters takes [{name, chapterIndex}] → returns chapters with UUIDs
 *  - POST /api/bookreads/{bookReadId}/chapters/{chapterId}/read marks a chapter read (idempotent)
 *  - GET /api/credits returns { cents, dollars }  (2 chapters = $2.00)
 */

/** A stable test book that does not require Google Books API */
function makeTestBook(suffix: string) {
  return {
    googleBookId: `e2e-test-charlottes-web-${suffix}`,
    title: "Charlotte's Web",
    authors: ['E.B. White'],
    description: 'A classic story about a pig named Wilbur and his spider friend Charlotte.',
    thumbnailUrl: null as string | null,
  };
}

test.describe('Reading and rewards journey', () => {
  let parentEmail: string;
  let kidUsername: string;
  let testBook: ReturnType<typeof makeTestBook>;
  const password = 'Password1!';
  const kidPassword = 'KidPass1!';

  test.beforeEach(async ({ request }) => {
    parentEmail = uniqueEmail('parent');
    kidUsername = uniqueUsername('reader');
    testBook = makeTestBook(Date.now().toString());

    // Create parent, verify, create child
    await signupAndVerify(request, parentEmail, password, 'Test', 'Parent');
    const parentToken = await login(request, parentEmail, password);
    await createKid(request, parentToken, kidUsername, 'TestKid', kidPassword);
  });

  test('child reading list shows added books', async ({ page, request }) => {
    // Add book via API (bypasses Google Books external dependency)
    const kidToken = await login(request, kidUsername, kidPassword);
    const headers = { Authorization: `Bearer ${kidToken}`, 'Content-Type': 'application/json' };
    const addRes = await request.post('/api/books', { data: testBook, headers });
    expect(addRes.ok()).toBeTruthy();

    // Log in via UI and navigate to reading list
    await page.goto('/login');
    await page.getByLabel('Username or Email').fill(kidUsername);
    await page.getByLabel('Password').fill(kidPassword);
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page).not.toHaveURL(/\/login/, { timeout: 8_000 });

    await page.goto('/reading-list');
    await expect(page.getByText(testBook.title).first()).toBeVisible({ timeout: 5_000 });
  });

  test('child can mark chapters read and earn credits', async ({ request }) => {
    const kidToken = await login(request, kidUsername, kidPassword);
    const headers = { Authorization: `Bearer ${kidToken}`, 'Content-Type': 'application/json' };

    // Add book directly (no external API dependency)
    const addRes = await request.post('/api/books', { data: testBook, headers });
    expect(addRes.ok()).toBeTruthy();
    const bookRead = await addRes.json(); // { id, googleBookId, userId, startDate }

    // Add 2 chapters to the book
    const chaptersRes = await request.post(`/api/books/${bookRead.googleBookId}/chapters`, {
      data: [
        { name: 'Chapter 1', chapterIndex: 0 },
        { name: 'Chapter 2', chapterIndex: 1 },
      ],
      headers,
    });
    expect(chaptersRes.ok()).toBeTruthy();
    const [ch1, ch2] = await chaptersRes.json();

    // Mark both chapters read
    const r1 = await request.post(`/api/bookreads/${bookRead.id}/chapters/${ch1.id}/read`, { headers });
    expect(r1.ok()).toBeTruthy();
    const r2 = await request.post(`/api/bookreads/${bookRead.id}/chapters/${ch2.id}/read`, { headers });
    expect(r2.ok()).toBeTruthy();

    // Verify credits earned: 2 chapters = $2.00
    const creditsRes = await request.get('/api/credits', { headers });
    expect(creditsRes.ok()).toBeTruthy();
    const { dollars } = await creditsRes.json();
    expect(dollars).toBe(2);
  });

  test('marking the same chapter twice does not duplicate credits', async ({ request }) => {
    const kidToken = await login(request, kidUsername, kidPassword);
    const headers = { Authorization: `Bearer ${kidToken}`, 'Content-Type': 'application/json' };

    const addRes = await request.post('/api/books', { data: testBook, headers });
    expect(addRes.ok()).toBeTruthy();
    const bookRead = await addRes.json();

    const chaptersRes = await request.post(`/api/books/${bookRead.googleBookId}/chapters`, {
      data: [{ name: 'Chapter 1', chapterIndex: 0 }],
      headers,
    });
    expect(chaptersRes.ok()).toBeTruthy();
    const [ch] = await chaptersRes.json();

    // Mark read twice — should be idempotent
    await request.post(`/api/bookreads/${bookRead.id}/chapters/${ch.id}/read`, { headers });
    await request.post(`/api/bookreads/${bookRead.id}/chapters/${ch.id}/read`, { headers });

    const creditsRes = await request.get('/api/credits', { headers });
    expect(creditsRes.ok()).toBeTruthy();
    const { dollars } = await creditsRes.json();
    expect(dollars).toBe(1); // Only 1 credit, not 2
  });
});
