import {Component, DestroyRef, OnInit, signal, viewChild} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {LibraryBookService} from '../services/library-book.service';
import {LIBRARY_BOOK_STATUSES, LibraryBook, LibraryBookStatus} from '../interfaces/library-book';
import {MatTab, MatTabGroup} from '@angular/material/tabs';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatExpansionModule} from '@angular/material/expansion';
import {MatDialog} from '@angular/material/dialog';
import {ViewBookListDialog, ViewBookListDialogData} from '../dialogs/view-book-list-dialog/view-book-list-dialog';
import {BookService} from '../services/book.service';
import {LibraryBookMenuItemsComponent} from '../library-book-menu-items/library-book-menu-items.component';
import {MatMenuModule} from '@angular/material/menu';
import {CollectionService} from '../services/collection.service';
import {CollectionBookService} from '../services/collection-book.service';
import {DeleteLibraryBookDialog} from '../dialogs/delete-library-book-dialog/delete-library-book-dialog';
import {MatSnackCommon} from '../common/mat-snack-common';
import {MatSnackBar} from '@angular/material/snack-bar';
import {
  CollectionSelectorDialogComponent,
  CollectionSelectorDialogData
} from '../dialogs/collection-selector-dialog/collection-selector-dialog.component';
import {filter, map} from 'rxjs';
import {SelectedCollection} from '../interfaces/selected-collection';
import {ConfirmationDialogComponent} from '../dialogs/confirmation-dialog/confirmation-dialog.component';
import {
  LibraryBookDetailsDialogComponent,
  LibraryBookDetailsDialogData,
  LibraryBookDetailsDialogResult
} from '../dialogs/library-book-details-dialog/library-book-details-dialog.component';
import {TranslocoDirective, TranslocoService} from '@jsverse/transloco';
import {Author} from '../interfaces/author';
import {Category} from '../interfaces/category';
import {CommonModule} from '@angular/common';
import {AuthorListComponent} from '../author-list/author-list.component';
import {CategoryListComponent} from '../category-list/category-list.component';
import {UpdateLibraryBookDetails} from '../interfaces/update-library-book-details';
import {NoteDialogComponent} from '../dialogs/note-dialog/note-dialog.component';
import {LocationDialogComponent} from '../dialogs/location-dialog/location-dialog.component';
import {QuotesListDialogComponent} from '../dialogs/quotes-list-dialog/quotes-list-dialog.component';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {LibraryStore} from '../services/library.store';
import {BookListComponent} from '../book-list/book-list.component';
import {CreateLocalBookDialogComponent} from '../dialogs/create-local-book-dialog/create-local-book-dialog.component';
import {FormsModule} from '@angular/forms';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {BookCardComponent} from '../book-card/book-card.component';
import {BookListItemComponent} from '../book-list-item/book-list-item.component';
import {SelectionStore} from '../services/selection.store';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';

@Component({
  selector: 'app-library',
  standalone: true,
  imports: [
    CommonModule,
    MatTabGroup,
    MatTab,
    MatButtonModule,
    MatIconModule,
    MatExpansionModule,
    LibraryBookMenuItemsComponent,
    MatMenuModule,
    AuthorListComponent,
    CategoryListComponent,
    TranslocoDirective,
    BookListComponent,
    MatButtonToggleModule,
    FormsModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    BookCardComponent,
    BookListItemComponent,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './library.component.html',
  styleUrl: './library.component.scss',
})
export class LibraryComponent implements OnInit {

  readonly bookList = viewChild(BookListComponent);

  readonly statusOptions = signal<{ value: LibraryBookStatus; label: string }[]>([]);

  readonly viewMode = signal<'grid' | 'list'>('grid');

  readonly moodQuery = signal<string>('');
  readonly moodResults = signal<LibraryBook[]>([]);
  readonly loadingMood = signal<boolean>(false);
  readonly moodSearched = signal<boolean>(false);
  readonly onlyToRead = signal<boolean>(false);

  uiState = {
    activeTabIndex: 0,
    authorsOpened: false,
    categoriesOpened: false,
  };

  private snackCommon: MatSnackCommon;

  constructor(
    private readonly translocoService: TranslocoService,
    private readonly libraryBookService: LibraryBookService,
    private readonly dialog: MatDialog,
    private readonly bookService: BookService,
    private readonly collectionService: CollectionService,
    private readonly collectionBookService: CollectionBookService,
    private readonly libraryStore: LibraryStore,
    private readonly destroyRef: DestroyRef,
    private readonly matSnackBar: MatSnackBar
  ) {
    this.snackCommon = new MatSnackCommon(this.matSnackBar);
  }

  ngOnInit(): void {
    this.translocoService.selectTranslateObject('library.statuses').pipe(
      takeUntilDestroyed(this.destroyRef),
      map((t: Record<string, string>) => LIBRARY_BOOK_STATUSES.map(s => ({
        value: s,
        label: t[s]
      })))
    ).subscribe(options => this.statusOptions.set(options));
  }

  onMoodSearch(): void {
    const query = this.moodQuery().trim();
    if (!query) return;

    this.loadingMood.set(true);
    this.moodSearched.set(false);
    const status = this.onlyToRead() ? LibraryBookStatus.TO_READ : null;

    this.libraryBookService.searchByMood(query, status, 10).subscribe({
      next: results => {
        this.moodResults.set(results);
        this.loadingMood.set(false);
        this.moodSearched.set(true);
      },
      error: err => {
        this.snackCommon.showError(err);
        this.loadingMood.set(false);
      }
    });
  }

  onTabChange(index: number): void {
    this.uiState.activeTabIndex = index;
    if (index === 1) this.uiState.authorsOpened = true;
    if (index === 2) this.uiState.categoriesOpened = true;
  }

  private loadBooks(): void {
    this.bookList()?.loadBooks();
  }

  goToAuthorBooks(author: Author): void {
    this.uiState.activeTabIndex = 0;
    setTimeout(() => {
      this.bookList()?.onAuthorSelected(author);
    });
  }

  goToCategoryBooks(category: Category): void {
    this.uiState.activeTabIndex = 0;
    setTimeout(() => {
      this.bookList()?.onCategorySelected(category);
    });
  }

  deleteLibraryBook(libraryBook: LibraryBook): void {
    this.collectionService.getCollectionsContainingBook(libraryBook.id).subscribe(collections => {
      const dialogRef = this.dialog.open(DeleteLibraryBookDialog, {
        data: {
          bookTitle: libraryBook.book.title,
          collections
        }
      });

      dialogRef.afterClosed().subscribe((confirmed: boolean) => {
        if (confirmed) {
          this.performDelete(libraryBook);
        }
      });
    });
  }

  private performDelete(libraryBook: LibraryBook): void {
    this.libraryBookService.removeBook(libraryBook.id).subscribe({
      next: () => {
        this.loadBooks();
        this.libraryStore.triggerRefresh();
        this.snackCommon.showSuccess(this.translocoService.translate('library.success.bookRemoved'));
      },
      error: (err) => this.snackCommon.showError(err)
    });
  }

  changeLibraryBookStatus(data: [LibraryBook, LibraryBookStatus]): void {
    this.libraryBookService.changeStatus(data[0].id, data[1]).subscribe({
      next: () => {
        this.loadBooks();
        this.snackCommon.showSuccess(this.translocoService.translate('library.success.statusChanged'));
      },
      error: (err) => this.snackCommon.showError(err)
    });
  }

  changeLibraryBookRating(data: { libraryBookId: number; rating: number }): void {
    this.libraryBookService.changeRating(data.libraryBookId, data.rating).subscribe({
      next: () => {
        this.loadBooks();
        this.snackCommon.showSuccess(this.translocoService.translate('library.success.ratingChanged'));
      },
      error: (err) => this.snackCommon.showError(err)
    });
  }

  bulkRemoveFromLibrary(): void {
    const list = this.bookList();
    if (!list) return;
    const ids = list.selection.selectedIds();
    const message = this.translocoService.translate('library.bulkRemoveConfirm', {count: ids.length});
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        message,
        confirmLabel: 'common.delete'
      }
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe(() => {
      this.libraryBookService.bulkRemove(ids).subscribe({
        next: () => {
          this.loadBooks();
          this.libraryStore.triggerRefresh();
          list.selection.clear();
          this.snackCommon.showSuccess(this.translocoService.translate('library.success.booksRemoved'));
        },
        error: (err) => this.snackCommon.showError(err)
      });
    });
  }

  bulkUpdateStatus(status: LibraryBookStatus): void {
    const list = this.bookList();
    if (!list) return;
    const ids = list.selection.selectedIds();
    this.libraryBookService.bulkUpdateStatus(ids, status).subscribe({
      next: () => {
        this.loadBooks();
        list.selection.clear();
        this.snackCommon.showSuccess(this.translocoService.translate('library.success.statusChanged'));
      },
      error: (err) => this.snackCommon.showError(err)
    });
  }

  openBulkAddToCollectionDialog(): void {
    const list = this.bookList();
    if (!list) return;
    const ids = list.selection.selectedIds();
    const dialogRef = this.dialog.open(CollectionSelectorDialogComponent, {
      data: {
        initialSelectionId: null,
        disabledIds: [],
        showRoot: false
      } as CollectionSelectorDialogData
    });

    dialogRef.afterClosed().pipe(filter(result => result !== undefined)).subscribe((selection: SelectedCollection) => {
      if (selection.id) {
        this.collectionBookService.bulkAdd(selection.id, ids).subscribe({
          next: () => {
            list.selection.clear();
            this.snackCommon.showSuccess(this.translocoService.translate('library.success.booksAddedToCollection'));
          },
          error: (err) => this.snackCommon.showError(err)
        });
      }
    });
  }

  removeFromAllCollections(libraryBook: LibraryBook): void {
    const message = this.translocoService.translate('library.removeFromAllCollectionsConfirm', {title: libraryBook.book.title});
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        message,
        confirmLabel: 'common.remove'
      }
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe(() => {
      this.collectionBookService.removeFromAllCollections(libraryBook.id).subscribe({
        next: () => this.snackCommon.showSuccess(this.translocoService.translate('library.success.bookRemovedFromAllCollections')),
        error: (err) => this.snackCommon.showError(err)
      });
    });
  }

  addToCollection(libraryBook: LibraryBook): void {
    this.collectionService.getCollectionsContainingBook(libraryBook.id).subscribe(collections => {
      const disabledIds = collections.map(c => c.id);

      const dialogRef = this.dialog.open(CollectionSelectorDialogComponent, {
        data: {
          initialSelectionId: null,
          disabledIds: disabledIds,
          showRoot: false
        } as CollectionSelectorDialogData
      });

      dialogRef.afterClosed().pipe(filter(result => result !== undefined)).subscribe((selection: SelectedCollection) => {
        if (selection.id) {
          this.collectionBookService.addBookToCollection(selection.id, libraryBook.id).subscribe({
            next: () => {
              this.snackCommon.showSuccess(this.translocoService.translate('library.success.bookAddedToCollection'));
            },
            error: (err) => this.snackCommon.showError(err)
          });
        }
      });
    });
  }

  openEditDetailsDialog(libraryBook: LibraryBook): void {
    const dialogRef = this.dialog.open(LibraryBookDetailsDialogComponent, {
      data: {libraryBook} as LibraryBookDetailsDialogData
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe((result: LibraryBookDetailsDialogResult) => {
      switch (result.action) {
        case 'save':
          this.updateDetails(libraryBook, result);
          break;
        case 'reset':
          this.resetDetails(libraryBook);
          break;
      }
    });
  }

  openUpdateLocalBookDialog(libraryBook: LibraryBook): void {
    const dialogRef = this.dialog.open(CreateLocalBookDialogComponent, {
      width: '600px',
      autoFocus: false,
      data: {libraryBook}
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe((dto) => {
      this.libraryBookService.updateLocalBook(libraryBook.id, dto).subscribe({
        next: () => {
          this.loadBooks();
          this.libraryStore.triggerRefresh();
          this.snackCommon.showSuccess(this.translocoService.translate('library.success.detailsUpdated'));
        },
        error: (err) => this.snackCommon.showError(err)
      });
    });
  }

  openNoteDialog(libraryBook: LibraryBook): void {
    this.dialog.open(NoteDialogComponent, {
      data: {
        libraryBookId: libraryBook.id,
        bookTitle: libraryBook.book.title
      }
    }).afterClosed().pipe(filter(Boolean)).subscribe(result => {
      if (result === 'saved') {
        this.snackCommon.showSuccess(this.translocoService.translate('library.success.noteSaved'));
      } else if (result === 'deleted') {
        this.snackCommon.showSuccess(this.translocoService.translate('library.success.noteDeleted'));
      }
    });
  }

  openQuotesDialog(libraryBook: LibraryBook): void {
    this.dialog.open(QuotesListDialogComponent, {
      data: {
        libraryBookId: libraryBook.id,
        bookTitle: libraryBook.book.title
      },
      width: '600px'
    });
  }

  openLocationDialog(libraryBook: LibraryBook): void {
    this.dialog.open(LocationDialogComponent, {
      data: {
        libraryBookId: libraryBook.id,
        location: libraryBook.location
      }
    }).afterClosed().pipe(filter(Boolean)).subscribe(() => {
      this.loadBooks();
      this.snackCommon.showSuccess(this.translocoService.translate('common.success.saved'));
    });
  }

  private resetDetails(libraryBook: LibraryBook) {
    this.libraryBookService.resetDetails(libraryBook.id).subscribe({
      next: () => {
        this.loadBooks();
        this.snackCommon.showSuccess(this.translocoService.translate('library.success.detailsReset'));
      },
      error: (err) => this.snackCommon.showError(err)
    });
  }

  private updateDetails(libraryBook: LibraryBook, result: {
    action: "save";
    payload: Partial<UpdateLibraryBookDetails>
  }) {
    this.libraryBookService.updateDetails(libraryBook.id, result.payload).subscribe({
      next: () => {
        this.loadBooks();
        this.snackCommon.showSuccess(this.translocoService.translate('library.success.detailsUpdated'));
      },
      error: (err) => this.snackCommon.showError(err)
    });
  }

  openViewBookListDialog() {
    const data: ViewBookListDialogData = {
      libraryBooks: [],
      categoryColumn: 'category.name',
      fetchBooksFn: (options) => this.bookService.getAll(options),
    };
    const dialogRef = this.dialog.open(ViewBookListDialog, {data});
    dialogRef.afterClosed().subscribe((result: number | string | undefined) => {
      if (typeof result === 'number') {
        this.libraryBookService.addBook(result).subscribe({
          next: () => {
            this.loadBooks();
            this.libraryStore.triggerRefresh();
            this.snackCommon.showSuccess(this.translocoService.translate('library.success.bookAdded'));
          },
          error: (err) => this.snackCommon.showError(err)
        });
      } else if (result === 'create-local') {
        this.openCreateLocalBookDialog();
      }
    });
  }

  openCreateLocalBookDialog(): void {
    const dialogRef = this.dialog.open(CreateLocalBookDialogComponent, {
      width: '600px',
      autoFocus: false
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe((dto) => {
      this.libraryBookService.createLocalBook(dto).subscribe({
        next: () => {
          this.loadBooks();
          this.libraryStore.triggerRefresh();
          this.snackCommon.showSuccess(this.translocoService.translate('library.success.bookAdded'));
        },
        error: (err) => this.snackCommon.showError(err)
      });
    });
  }

}
