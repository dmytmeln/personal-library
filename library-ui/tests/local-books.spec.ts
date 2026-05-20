import { test, expect } from '@playwright/test';

test.describe('Local Books Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.getByPlaceholder('Email').fill('user@example.com');
    await page.getByPlaceholder('Password').fill('password123');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL(/.*library/);
  });

  test('should create a local book and verify its presence', async ({ page }) => {
    await page.getByRole('link', { name: 'Library', exact: true }).click();
    
    const createBtn = page.getByRole('button', { name: 'Create Manual Book' });
    await expect(createBtn).toBeVisible({ timeout: 10000 });
    await createBtn.click();

    const dialog = page.locator('app-create-local-book-dialog');
    await expect(dialog).toBeVisible({ timeout: 10000 });

    const title = 'E2E Local Book ' + Date.now();
    await dialog.getByLabel('Title').fill(title);
    
    await dialog.getByLabel('Library Status').click();
    await page.locator('.mat-mdc-option').filter({ hasText: 'Want to read' }).click();

    await dialog.getByRole('button', { name: 'Type custom category name' }).click();
    await dialog.getByLabel('Custom Category Name').fill('E2E Category');

    await dialog.getByRole('button', { name: 'Type custom author name' }).click();
    await dialog.getByLabel('Custom Author Name').fill('E2E Author');

    await dialog.getByRole('button', { name: 'Add' }).click();

    const snackbar = page.locator('.mat-mdc-snack-bar-container').first();
    await expect(snackbar).toBeVisible({ timeout: 10000 });
    await expect(snackbar).toContainText('Book added to library');

    await page.getByLabel('Book Title').fill(title);
    await page.waitForTimeout(1000);

    const bookElement = page.locator('app-book-list-item, app-book-card').filter({ hasText: title }).first();
    await expect(bookElement).toBeVisible({ timeout: 15000 });

    await bookElement.locator('.menu-trigger').click();
    
    const menu = page.locator('.mat-mdc-menu-panel').first();
    await expect(menu).toBeVisible({ timeout: 10000 });
    
    const infoBtn = menu.locator('.mat-mdc-menu-item').filter({ hasText: 'Book Information' });
    await infoBtn.click();
    
    const subMenu = page.locator('.mat-mdc-menu-panel').last();
    await subMenu.locator('.mat-mdc-menu-item').filter({ hasText: 'Edit Details' }).click();

    const editDialog = page.locator('app-create-local-book-dialog');
    await expect(editDialog).toBeVisible({ timeout: 10000 });
    await editDialog.getByRole('button', { name: 'Cancel' }).click();

    await bookElement.locator('.menu-trigger').click();
    await menu.locator('.mat-mdc-menu-item').filter({ hasText: /Remove from Library/i }).click();

    const confirmDialog = page.locator('app-confirmation-dialog, app-delete-library-book-dialog').first();
    await expect(confirmDialog).toBeVisible({ timeout: 10000 });
    await confirmDialog.getByRole('button', { name: 'Delete' }).click();

    await expect(snackbar).toBeVisible({ timeout: 10000 });
    await expect(snackbar).toContainText('Book removed from library');
  });
});
