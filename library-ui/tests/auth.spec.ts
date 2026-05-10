import { test, expect } from '@playwright/test';

test.describe('Authentication', () => {
  test('should show login page', async ({ page }) => {
    await page.goto('/login');
    await expect(page).toHaveURL(/.*login/);
    await expect(page.locator('mat-card-title').getByText('Login', { exact: true })).toBeVisible();
  });

  test('should show validation errors for empty fields', async ({ page }) => {
    await page.goto('/login');
    
    const submitButton = page.getByRole('button', { name: 'Sign In' });
    await expect(submitButton).toBeDisabled();

    const emailInput = page.getByPlaceholder('Email');
    await emailInput.focus();
    await emailInput.blur();
    
    await expect(page.getByText('Email is required')).toBeVisible();
  });

  test('should login successfully with valid credentials', async ({ page }) => {
    await page.goto('/login');

    await page.getByPlaceholder('Email').fill('user@example.com');
    await page.getByPlaceholder('Password').fill('password123');
    
    await page.getByRole('button', { name: 'Sign In' }).click();

    await expect(page).toHaveURL(/.*library/);
    await expect(page.getByRole('heading', { name: 'My Library' })).toBeVisible();
  });
});
