import { test, expect } from '@playwright/test';
import { apiUrl, signupAndVerify, uniqueEmail, uniqueUsername, login, createKid } from './helpers';

/**
 * P1 Parent journey:
 * Parent logs in → adds a child account → views kids summary
 *
 * API/UI notes:
 *  - POST /api/parent/kids takes { username, firstName, password } (no email for kids)
 *  - ParentDashboard form placeholders: "Username", "First Name", "Password"
 *  - Add button text: "Add Kid"
 *  - Summary endpoint: GET /api/parent/kids/summary → { kids: [{firstName, username, ...}] }
 */

test.describe('Parent dashboard journey', () => {
  let parentEmail: string;
  const password = 'Password1!';

  test.beforeEach(async ({ request }) => {
    parentEmail = uniqueEmail('parent');
    await signupAndVerify(request, parentEmail, password, 'Test', 'Parent');
  });

  test('parent can add a child and view them in the list', async ({ page }) => {
    // Login via UI
    await page.goto('/login');
    await page.getByLabel('Username or Email').fill(parentEmail);
    await page.getByLabel('Password').fill(password);
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page).not.toHaveURL(/\/login/, { timeout: 8_000 });

    // Navigate to parent dashboard
    await page.goto('/parent');
    await expect(page.getByRole('heading', { name: /your dashboard/i })).toBeVisible();

    // Add a child using the form
    const kidUsername = uniqueUsername('kid');
    await page.getByPlaceholder('Username').fill(kidUsername);
    await page.getByPlaceholder('First Name').fill('Junior');
    await page.getByPlaceholder('Password').fill('KidPass1!');
    await page.getByRole('button', { name: /add kid/i }).click();

    // Child should appear in the list
    await expect(page.getByText('Junior').first()).toBeVisible({ timeout: 5_000 });
  });

  test('parent can view kids summary via API', async ({ request }) => {
    const token = await login(request, parentEmail, password);
    const headers = { Authorization: `Bearer ${token}` };

    // Add a child via API
    const kidUsername = uniqueUsername('kidapi');
    const addRes = await request.post(apiUrl('/parent/kids'), {
      data: { username: kidUsername, firstName: 'ApiKid', password: 'KidPass1!' },
      headers,
    });
    expect(addRes.ok()).toBeTruthy();

    // Get summary
    const summaryRes = await request.get(apiUrl('/parent/kids/summary'), { headers });
    expect(summaryRes.ok()).toBeTruthy();
    const body = await summaryRes.json();
    // Response shape: { kids: [{id, firstName, username, booksRead, ...}] }
    expect(Array.isArray(body.kids)).toBeTruthy();
    expect(body.kids.length).toBeGreaterThan(0);
    expect(body.kids[0]).toHaveProperty('firstName');
    expect(body.kids[0]).toHaveProperty('username');
    expect(body.kids[0]).toHaveProperty('currentBalance');
  });

  test('parent can drill down into child detail and reverse a chapter read', async ({ page, request }) => {
    const parentToken = await login(request, parentEmail, password);
    const headers = { Authorization: `Bearer ${parentToken}` };

    const kidUsername = uniqueUsername('kidrev');
    await createKid(request, parentToken, kidUsername, 'ReversalKid', 'KidPass1!');
    const kidsRes = await request.get(apiUrl('/parent/kids'), { headers });
    expect(kidsRes.ok()).toBeTruthy();
    const kids = await kidsRes.json();
    const kid = kids.find((k: { username: string }) => k.username === kidUsername);
    expect(kid).toBeTruthy();

    const kidToken = await login(request, kidUsername, 'KidPass1!');
    const kidHeaders = {
      Authorization: `Bearer ${kidToken}`,
      'Content-Type': 'application/json',
    };

    const bookDto = {
      googleBookId: `e2e-parent-reverse-${Date.now()}`,
      title: 'Parent Reversal Book',
      authors: ['E2E Author'],
      description: 'seed',
      thumbnailUrl: '',
    };

    const addBookRes = await request.post(apiUrl('/books'), { data: bookDto, headers: kidHeaders });
    expect(addBookRes.ok()).toBeTruthy();
    const bookRead = await addBookRes.json();

    const addChaptersRes = await request.post(apiUrl(`/books/${bookRead.googleBookId}/chapters`), {
      data: [{ name: 'Chapter 1', chapterIndex: 0 }],
      headers: kidHeaders,
    });
    expect(
      addChaptersRes.ok(),
      `chapters create failed: ${addChaptersRes.status()} ${await addChaptersRes.text()}`
    ).toBeTruthy();
    const chapters = await addChaptersRes.json();

    const markReadRes = await request.post(apiUrl(`/bookreads/${bookRead.id}/chapters/${chapters[0].id}/read`), { headers: kidHeaders });
    expect(markReadRes.ok()).toBeTruthy();

    await page.goto('/login');
    await page.getByLabel('Username or Email').fill(parentEmail);
    await page.getByLabel('Password').fill(password);
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page).not.toHaveURL(/\/login/, { timeout: 8_000 });

    await page.goto('/parent/summary');
    await expect(page.getByRole('heading', { name: /manage child accounts/i })).toBeVisible();

    await page.getByRole('button', { name: /view details for reversalkid/i }).click();
    await expect(page.getByRole('heading', { name: /child account details/i })).toBeVisible();
    await expect(page.getByText(/chapter 1/i)).toBeVisible();
    await expect(page.getByRole('button', { name: /reverse/i })).toBeVisible();

    await page.getByRole('button', { name: /reverse/i }).click();
    await page.getByRole('button', { name: /confirm/i }).click();
    await expect(page.getByText(/not read/i)).toBeVisible({ timeout: 5_000 });

    const detailRes = await request.get(apiUrl(`/parent/${kid.id}/child-detail`), { headers });
    expect(detailRes.ok()).toBeTruthy();
    const detail = await detailRes.json();
    expect(detail.rewards.length).toBe(0);
    expect(detail.books[0].chapters[0].isRead).toBeFalsy();
  });

  test('parent can use own reading list and child cannot access parent items', async ({ page, request }) => {
    const parentToken = await login(request, parentEmail, password);
    const parentHeaders = { Authorization: `Bearer ${parentToken}`, 'Content-Type': 'application/json' };

    const ownBook = {
      googleBookId: `e2e-parent-self-${Date.now()}`,
      title: 'Parent Personal Book',
      authors: ['Parent Author'],
      description: 'parent self list',
      thumbnailUrl: '',
    };

    const addParentBookRes = await request.post(apiUrl('/books'), { data: ownBook, headers: parentHeaders });
    expect(addParentBookRes.ok()).toBeTruthy();

    const kidUsername = uniqueUsername('kidbound');
    await createKid(request, parentToken, kidUsername, 'BoundaryKid', 'KidPass1!');

    await page.goto('/login');
    await page.getByLabel('Username or Email').fill(parentEmail);
    await page.getByLabel('Password').fill(password);
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page).not.toHaveURL(/\/login/, { timeout: 8_000 });

    await page.goto('/reading-list');
    await expect(page.getByText('Parent Personal Book')).toBeVisible({ timeout: 5_000 });

    await page.getByRole('button', { name: /logout/i }).click();
    await expect(page).toHaveURL(/\/login/);

    await page.getByLabel('Username or Email').fill(kidUsername);
    await page.getByLabel('Password').fill('KidPass1!');
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page).not.toHaveURL(/\/login/, { timeout: 8_000 });

    await page.goto('/reading-list');
    await expect(page.getByText('Parent Personal Book')).toHaveCount(0);

    const kidToken = await login(request, kidUsername, 'KidPass1!');
    const kidBooksRes = await request.get(apiUrl('/books'), {
      headers: { Authorization: `Bearer ${kidToken}` },
    });
    expect(kidBooksRes.ok()).toBeTruthy();
    const kidBooks = await kidBooksRes.json();
    expect(kidBooks.some((b: { googleBookId: string }) => b.googleBookId === ownBook.googleBookId)).toBeFalsy();
  });
});
