import { test, expect } from '@playwright/test';

test.describe('Search basic scenarios', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.getByPlaceholder('Email').fill('user@example.com');
    await page.getByPlaceholder('Password').fill('password123');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL(/.*library/);
  });

  test('should search for a book by title and view results', async ({ page }) => {
    await page.getByRole('link', { name: 'Search' }).click();
    await expect(page).toHaveURL(/.*search/);

    const searchInput = page.getByRole('textbox').first();
    await searchInput.fill('1984');
    
    await expect(page.getByText('1984').first()).toBeVisible({ timeout: 10000 });
  });
});
