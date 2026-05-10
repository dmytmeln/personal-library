import { test, expect } from '@playwright/test';

test.describe('Library Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.getByPlaceholder('Email').fill('user@example.com');
    await page.getByPlaceholder('Password').fill('password123');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL(/.*library/);
  });

  test('should navigate to Collections', async ({ page }) => {
    await page.getByRole('link', { name: 'Collections' }).click();
    await expect(page).toHaveURL(/.*collections/);
    await expect(page.getByRole('heading', { name: 'Collections' })).toBeVisible();
  });

  test('should navigate to Dashboard', async ({ page }) => {
    await page.getByRole('link', { name: 'Dashboard' }).click();
    await expect(page).toHaveURL(/.*dashboard/);
    await expect(page.getByRole('heading', { name: 'Reading Dashboard' })).toBeVisible();
  });

  test('should navigate to Search', async ({ page }) => {
    await page.getByRole('link', { name: 'Search' }).click();
    await expect(page).toHaveURL(/.*search/);
    await expect(page.getByRole('heading', { name: 'Search Books' })).toBeVisible();
  });

  test('should be restricted from admin panel', async ({ page }) => {
    await page.goto('/admin');
    await expect(page).toHaveURL(/.*library/);
  });

  test('should logout successfully', async ({ page }) => {
    await page.locator('mat-toolbar').getByText('Logout').click();
    await expect(page).toHaveURL(/.*login/);
  });
});
