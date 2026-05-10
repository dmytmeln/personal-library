import { test, expect } from '@playwright/test';

test.describe('Library Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.getByPlaceholder('Email').fill('user@example.com');
    await page.getByPlaceholder('Password').fill('password123');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL(/.*library/);
  });

  test('should add a book to library and then delete it', async ({ page }) => {
    await page.getByRole('link', { name: 'Search' }).click();
    await expect(page).toHaveURL(/.*search/);

    const searchInput = page.locator('input').first();
    await expect(searchInput).toBeVisible({ timeout: 10000 });
    await searchInput.fill('The Shining');
    
    const bookCard = page.locator('app-book-list-item, app-book-card').filter({ hasText: 'The Shining' }).first();
    await expect(bookCard).toBeVisible({ timeout: 10000 });

    await bookCard.locator('.menu-trigger').click();
    
    const menu = page.locator('.mat-mdc-menu-panel').first();
    await expect(menu).toBeVisible({ timeout: 10000 });

    const menuItem = menu.locator('.mat-mdc-menu-item').filter({ hasText: /Add to Library/i }).first();
    if (await menuItem.isVisible() && await menuItem.isEnabled()) {
      await menuItem.click();
      const snackbar = page.locator('.mat-mdc-snack-bar-container').first();
      await expect(snackbar).toBeVisible({ timeout: 10000 });
    } else {
      await page.keyboard.press('Escape');
    }

    await page.getByRole('link', { name: 'Library', exact: true }).click();
    await expect(page).toHaveURL(/.*library/);

    const libraryBookCard = page.locator('app-book-list-item, app-book-card').filter({ hasText: 'The Shining' }).first();
    await expect(libraryBookCard).toBeVisible({ timeout: 10000 });

    await libraryBookCard.locator('.menu-trigger').click();
    
    const libMenu = page.locator('.mat-mdc-menu-panel').first();
    await expect(libMenu).toBeVisible({ timeout: 10000 });
    
    const removeBtn = libMenu.locator('.mat-mdc-menu-item').filter({ hasText: /Remove from Library/i }).first();
    await expect(removeBtn).toBeVisible({ timeout: 10000 });
    await removeBtn.click();

    const snackbar = page.locator('.mat-mdc-snack-bar-container').first();
    await expect(snackbar).toBeVisible({ timeout: 10000 });
    await expect(page.locator('app-book-list-item, app-book-card').filter({ hasText: 'The Shining' })).toHaveCount(0);
  });
});
