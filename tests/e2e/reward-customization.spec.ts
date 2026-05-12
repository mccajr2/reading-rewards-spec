import { expect, test } from '@playwright/test';

test.describe('Reward customization journey scaffold', () => {
  test('placeholder: parent configures rewards and child selects one', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveURL(/.*/);
  });
});