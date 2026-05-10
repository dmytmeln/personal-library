import { test, expect } from '@playwright/test';

test.describe('Dashboard and Goals', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.getByPlaceholder('Email').fill('user@example.com');
    await page.getByPlaceholder('Password').fill('password123');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL(/.*library/);
  });

  test('should set a reading goal and verify it', async ({ page }) => {
    await page.getByRole('link', { name: 'Dashboard' }).click();
    await expect(page).toHaveURL(/.*dashboard/);

    const setGoalBtn = page.locator('button').filter({ hasText: /Set Goals/i }).first();
    await expect(setGoalBtn).toBeVisible({ timeout: 10000 });
    await setGoalBtn.click();

    const dialog = page.locator('app-set-goal-dialog').first();
    await expect(dialog).toBeVisible({ timeout: 10000 });

    const targetBooksInput = dialog.locator('input[formControlName="targetBooks"]').first();
    await expect(targetBooksInput).toBeVisible({ timeout: 10000 });
    await targetBooksInput.fill('10');

    const saveBtn = dialog.locator('button').filter({ hasText: /Save/i }).first();
    await saveBtn.click();

    const snackbar = page.locator('.mat-mdc-snack-bar-container').first();
    await expect(snackbar).toBeVisible({ timeout: 10000 });

    await expect(page.getByText(/10/).first()).toBeVisible({ timeout: 10000 });
  });
});
