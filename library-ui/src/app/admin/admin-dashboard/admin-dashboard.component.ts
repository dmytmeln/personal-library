import {Component, viewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatTabsModule} from '@angular/material/tabs';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {BookListComponent} from '../../book-list/book-list.component';
import {AuthorListComponent} from '../../author-list/author-list.component';
import {CategoryListComponent} from '../../category-list/category-list.component';
import {Router} from '@angular/router';
import {TranslocoDirective, TranslocoPipe, TranslocoService} from '@jsverse/transloco';
import {AdminService} from '../../services/admin.service';
import {Author} from '../../interfaces/author';
import {Category} from '../../interfaces/category';
import {Book} from '../../interfaces/book';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatSnackCommon} from '../../common/mat-snack-common';
import {MatDialog, MatDialogModule} from '@angular/material/dialog';
import {ConfirmationDialogComponent} from '../../dialogs/confirmation-dialog/confirmation-dialog.component';
import {MatMenuModule} from '@angular/material/menu';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    BookListComponent,
    AuthorListComponent,
    CategoryListComponent,
    TranslocoDirective,
    TranslocoPipe,
    MatDialogModule,
    MatMenuModule,
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss'
})
export class AdminDashboardComponent {

  bookList = viewChild(BookListComponent);
  authorList = viewChild(AuthorListComponent);
  categoryList = viewChild(CategoryListComponent);

  activeTab = 0;
  private snackCommon: MatSnackCommon;

  constructor(
    private router: Router,
    private adminService: AdminService,
    private translocoService: TranslocoService,
    private dialog: MatDialog,
    matSnackBar: MatSnackBar
  ) {
    this.snackCommon = new MatSnackCommon(matSnackBar);
  }

  addEntity(): void {
    if (this.activeTab === 0) this.router.navigate(['/admin/book/new']);
    if (this.activeTab === 1) this.router.navigate(['/admin/author/new']);
    if (this.activeTab === 2) this.router.navigate(['/admin/category/new']);
  }

  editBook(book: Book): void {
    this.router.navigate(['/admin/book', book.id]);
  }

  onBookDeleted(book: Book): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        message: this.translocoService.translate('common.confirmDelete'),
        confirmLabel: 'common.delete'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.adminService.deleteBook(book.id).subscribe({
          next: () => {
            this.snackCommon.showSuccess(this.translocoService.translate('common.success.deleted'));
            const list = this.bookList();
            if (list) list.loadBooks();
          },
          error: (err) => this.snackCommon.showError(err)
        });
      }
    });
  }

  onAuthorDeleted(author: Author): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        message: this.translocoService.translate('common.confirmDelete'),
        confirmLabel: 'common.delete'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.adminService.deleteAuthor(author.id).subscribe({
          next: () => {
            this.snackCommon.showSuccess(this.translocoService.translate('common.success.deleted'));
            const list = this.authorList();
            if (list) list.loadAuthors();
          },
          error: (err) => this.snackCommon.showError(err)
        });
      }
    });
  }

  onCategoryDeleted(category: Category): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        message: this.translocoService.translate('common.confirmDelete'),
        confirmLabel: 'common.delete'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.adminService.deleteCategory(category.id).subscribe({
          next: () => {
            this.snackCommon.showSuccess(this.translocoService.translate('common.success.deleted'));
            const list = this.categoryList();
            if (list) list.loadCategories();
          },
          error: (err) => this.snackCommon.showError(err)
        });
      }
    });
  }

  bulkDeleteBooks(): void {
    const list = this.bookList();
    if (!list) return;
    const ids = list.selection.selectedIds();

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        message: this.translocoService.translate('common.confirmDeleteBulk', {count: ids.length}),
        confirmLabel: 'common.delete'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.adminService.deleteBooks(ids).subscribe({
          next: () => {
            this.snackCommon.showSuccess(this.translocoService.translate('common.success.deleted'));
            list.selection.clear();
            list.loadBooks();
          },
          error: (err) => this.snackCommon.showError(err)
        });
      }
    });
  }

  bulkDeleteAuthors(): void {
    const list = this.authorList();
    if (!list) return;
    const ids = list.selection.selectedIds();

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        message: this.translocoService.translate('common.confirmDeleteBulk', {count: ids.length}),
        confirmLabel: 'common.delete'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.adminService.deleteAuthors(ids).subscribe({
          next: () => {
            this.snackCommon.showSuccess(this.translocoService.translate('common.success.deleted'));
            list.selection.clear();
            list.loadAuthors();
          },
          error: (err) => this.snackCommon.showError(err)
        });
      }
    });
  }

  bulkDeleteCategories(): void {
    const list = this.categoryList();
    if (!list) return;
    const ids = list.selection.selectedIds();

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        message: this.translocoService.translate('common.confirmDeleteBulk', {count: ids.length}),
        confirmLabel: 'common.delete'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.adminService.deleteCategories(ids).subscribe({
          next: () => {
            this.snackCommon.showSuccess(this.translocoService.translate('common.success.deleted'));
            list.selection.clear();
            list.loadCategories();
          },
          error: (err) => this.snackCommon.showError(err)
        });
      }
    });
  }
}
