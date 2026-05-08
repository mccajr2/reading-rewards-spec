import { test, expect } from '@playwright/test';
import { signupAndVerify, uniqueEmail, uniqueUsername, login } from './helpers';

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
    await expect(page.getByRole('heading', { name: /manage kids/i })).toBeVisible();

    // Add a child using the form
    const kidUsername = uniqueUsername('kid');
    await page.getByPlaceholder('Username').fill(kidUsername);
    await page.getByPlaceholder('First Name').fill('Junior');
    await page.getByPlaceholder('Password').fill('KidPass1!');
    await page.getByRole('button', { name: /add kid/i }).click();

    // Child should appear in the list
    await expect(page.getByText('Junior')).toBeVisible({ timeout: 5_000 });
  });

  test('parent can view kids summary via API', async ({ request }) => {
    const token = await login(request, parentEmail, password);
    const headers = { Authorization: `Bearer ${token}` };

    // Add a child via API
    const kidUsername = uniqueUsername('kidapi');
    const addRes = await request.post('/api/parent/kids', {
      data: { username: kidUsername, firstName: 'ApiKid', password: 'KidPass1!' },
      headers,
    });
    expect(addRes.ok()).toBeTruthy();

    // Get summary
    const summaryRes = await request.get('/api/parent/kids/summary', { headers });
    expect(summaryRes.ok()).toBeTruthy();
    const body = await summaryRes.json();
    // Response shape: { kids: [{id, firstName, username, booksRead, ...}] }
    expect(Array.isArray(body.kids)).toBeTruthy();
    expect(body.kids.length).toBeGreaterThan(0);
    expect(body.kids[0]).toHaveProperty('firstName');
    expect(body.kids[0]).toHaveProperty('username');
    expect(body.kids[0]).toHaveProperty('currentBalance');
  });
});
