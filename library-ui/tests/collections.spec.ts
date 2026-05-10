import { test, expect } from '@playwright/test';

test.describe('Collections Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.getByPlaceholder('Email').fill('user@example.com');
    await page.getByPlaceholder('Password').fill('password123');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL(/.*library/);
  });

  test('should create a collection and delete it', async ({ page }) => {
    await page.getByRole('link', { name: 'Collections' }).click();
    await expect(page).toHaveURL(/.*collections/);

    const createBtn = page.locator('button').filter({ hasText: /Create collection/i }).first();
    await expect(createBtn).toBeVisible({ timeout: 10000 });
    await createBtn.click();
    
    const dialog = page.locator('app-collection-dialog').first();
    await expect(dialog).toBeVisible({ timeout: 10000 });

    await dialog.locator('input[formControlName="name"]').fill('My Favorites E2E');
    await dialog.locator('button[type="submit"]').click();

    const snackbar = page.locator('.mat-mdc-snack-bar-container').first();
    await expect(snackbar).toBeVisible({ timeout: 10000 });
    
    const collectionNode = page.locator('mat-tree-node').filter({ hasText: 'My Favorites E2E' }).first();
    await expect(collectionNode).toBeVisible({ timeout: 10000 });

    await collectionNode.locator('.menu-button').click();
    
    const deleteMenuBtn = page.locator('.mat-mdc-menu-item').filter({ hasText: /Delete collection/i }).first();
    await expect(deleteMenuBtn).toBeVisible({ timeout: 10000 });
    await deleteMenuBtn.click();
    
    const confirmDialog = page.locator('app-confirmation-dialog').first();
    await expect(confirmDialog).toBeVisible({ timeout: 10000 });
    const deleteBtn = confirmDialog.locator('button').filter({ hasText: /Delete/i }).first();
    await deleteBtn.click();

    await expect(snackbar).toBeVisible({ timeout: 10000 });
    await expect(collectionNode).not.toBeVisible();
  });
});
