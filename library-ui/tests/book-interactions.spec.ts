import { test, expect } from '@playwright/test';

test.describe('Book Interactions', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.getByPlaceholder('Email').fill('user@example.com');
    await page.getByPlaceholder('Password').fill('password123');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL(/.*library/);

    // Ensure "1984" is in library for consistent testing
    await page.getByRole('link', { name: 'Search' }).click();
    await page.getByRole('textbox').first().fill('1984');
    const searchResult = page.locator('app-book-list-item, app-book-card').filter({ hasText: '1984' }).first();
    await expect(searchResult).toBeVisible({ timeout: 10000 });
    
    await searchResult.locator('.menu-trigger').click();
    const searchMenu = page.locator('.mat-mdc-menu-panel').first();
    const addBtn = searchMenu.locator('.mat-mdc-menu-item').filter({ hasText: 'Add to Library' });
    if (await addBtn.isVisible()) {
        await addBtn.click();
        await expect(page.locator('.mat-mdc-snack-bar-container')).toBeVisible();
    } else {
        await page.keyboard.press('Escape');
    }

    await page.getByRole('link', { name: 'Library', exact: true }).click();
    await page.getByLabel('Book Title').fill('1984');
    await page.waitForTimeout(1000);
  });

  test('should add a note to a book', async ({ page }) => {
    const bookCard = page.locator('app-book-list-item, app-book-card').filter({ hasText: '1984' }).first();
    await expect(bookCard).toBeVisible({ timeout: 10000 });

    await bookCard.locator('.menu-trigger').click();
    
    const menu = page.locator('.mat-mdc-menu-panel').first();
    await expect(menu).toBeVisible({ timeout: 10000 });
    await menu.locator('.mat-mdc-menu-item').filter({ hasText: 'My Notes' }).click();

    const dialog = page.locator('app-note-dialog');
    await expect(dialog).toBeVisible({ timeout: 10000 });

    const noteText = 'E2E Note ' + Date.now();
    await dialog.getByPlaceholder('Write something...').fill(noteText);
    await dialog.getByRole('button', { name: 'Save' }).click();

    const snackbar = page.locator('.mat-mdc-snack-bar-container').first();
    await expect(snackbar).toBeVisible({ timeout: 10000 });
    await expect(snackbar).toContainText('Note saved');
  });

  test('should add a quote to a book', async ({ page }) => {
    const bookCard = page.locator('app-book-list-item, app-book-card').filter({ hasText: '1984' }).first();
    await expect(bookCard).toBeVisible({ timeout: 10000 });

    await bookCard.locator('.menu-trigger').click();
    
    const menu = page.locator('.mat-mdc-menu-panel').first();
    await expect(menu).toBeVisible({ timeout: 10000 });
    await menu.locator('.mat-mdc-menu-item').filter({ hasText: 'My Quotes' }).click();

    const listDialog = page.locator('app-quotes-list-dialog');
    await expect(listDialog).toBeVisible({ timeout: 10000 });

    await listDialog.getByRole('button', { name: 'Add Quote' }).click();

    const formDialog = page.locator('app-quote-form-dialog');
    await expect(formDialog).toBeVisible({ timeout: 10000 });

    const quoteText = 'E2E Quote ' + Date.now();
    await formDialog.getByLabel('Quote Text').fill(quoteText);
    await formDialog.getByLabel('Page / Chapter').fill('42');
    await formDialog.getByRole('button', { name: 'Save' }).click();

    const snackbar = page.locator('.mat-mdc-snack-bar-container').first();
    await expect(snackbar).toBeVisible({ timeout: 10000 });
    await expect(snackbar).toContainText('Quote saved');

    await page.getByRole('button', { name: 'Close' }).click();
  });

  test('should update book rating and status', async ({ page }) => {
    const bookCard = page.locator('app-book-list-item, app-book-card').filter({ hasText: '1984' }).first();
    await expect(bookCard).toBeVisible({ timeout: 10000 });

    await bookCard.locator('.menu-trigger').click();
    
    let menu = page.locator('.mat-mdc-menu-panel').first();
    await expect(menu).toBeVisible({ timeout: 10000 });
    await menu.locator('.mat-mdc-menu-item').filter({ hasText: 'Rate' }).click();
    
    const ratingMenu = page.locator('.mat-mdc-menu-panel').last();
    await ratingMenu.getByRole('button').nth(3).click();

    const snackbar = page.locator('.mat-mdc-snack-bar-container').first();
    await expect(snackbar).toBeVisible({ timeout: 10000 });
    await expect(snackbar).toContainText('Rating changed');

    await bookCard.locator('.menu-trigger').click();
    menu = page.locator('.mat-mdc-menu-panel').first();
    await expect(menu).toBeVisible({ timeout: 10000 });
    await menu.locator('.mat-mdc-menu-item').filter({ hasText: 'Change Status' }).click();
    
    const statusMenu = page.locator('.mat-mdc-menu-panel').last();
    await statusMenu.getByRole('button', { name: 'Read', exact: true }).click();

    await expect(snackbar).toBeVisible({ timeout: 10000 });
    await expect(snackbar).toContainText('Status changed');
  });
});
