import { test, expect } from '@playwright/test';
import { apiUrl, signupAndVerify, uniqueEmail } from './helpers';

/**
 * P1 Auth journeys:
 * 1. Parent signup → verify → login → see nav → logout
 * 2. Unverified parent cannot login (403)
 * 3. Login with bad credentials shows error (401)
 *
 * UI notes (from LoginPage.tsx):
 *  - Label: "Username or Email" (input inside the label element)
 *  - Submit button text: "Sign in"
 *  - Logout button text: "Logout"
 */

test.describe('Auth flows', () => {
  test('parent can sign up, verify, log in, and log out', async ({ page, request }) => {
    const email = uniqueEmail('parent');
    const password = 'Password1!';

    await signupAndVerify(request, email, password, 'Test', 'Parent');

    await page.goto('/login');
    await page.getByLabel('Username or Email').fill(email);
    await page.getByLabel('Password').fill(password);
    await page.getByRole('button', { name: /sign in/i }).click();

    // Should leave the login page
    await expect(page).not.toHaveURL(/\/login/, { timeout: 8_000 });

    // Nav should be visible
    await expect(page.locator('nav')).toBeVisible();

    // Logout
    await page.getByRole('button', { name: /logout/i }).click();
    await expect(page).toHaveURL(/\/login/);
  });

  test('unverified parent cannot log in', async ({ page, request }) => {
    const email = uniqueEmail('unverified');
    // Sign up but do NOT force-verify
    const res = await request.post(apiUrl('/auth/signup'), {
      data: { email, password: 'Password1!', firstName: 'Unverified', lastName: 'User' },
    });
    expect(res.ok()).toBeTruthy();

    await page.goto('/login');
    await page.getByLabel('Username or Email').fill(email);
    await page.getByLabel('Password').fill('Password1!');
    await page.getByRole('button', { name: /sign in/i }).click();

    // Backend returns 403 "Parent account not verified. Please check your email."
    await expect(page.getByText(/not verified/i)).toBeVisible({ timeout: 5_000 });
  });

  test('login with wrong password shows error', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Username or Email').fill('nobody@example.com');
    await page.getByLabel('Password').fill('wrongpassword');
    await page.getByRole('button', { name: /sign in/i }).click();

    // Backend returns 401 "Invalid credentials"
    await expect(page.getByText(/invalid/i)).toBeVisible({ timeout: 5_000 });
  });
});
