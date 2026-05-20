import {Component, computed, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {CollectionBookService} from '../services/collection-book.service';
import {CollectionService} from '../services/collection.service';
import {CollectionDetails} from '../interfaces/collection-details';
import {CommonModule} from '@angular/common';
import {MatButton, MatIconButton} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatDialog} from '@angular/material/dialog';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatSnackCommon} from '../common/mat-snack-common';
import {CreateCollection} from '../interfaces/create-collection';
import {ViewBookListDialog, ViewBookListDialogData} from '../dialogs/view-book-list-dialog/view-book-list-dialog';
import {LibraryBookService} from '../services/library-book.service';
import {LIBRARY_BOOK_STATUSES, LibraryBook, LibraryBookStatus} from '../interfaces/library-book';
import {LibraryBookMenuItemsComponent} from '../library-book-menu-items/library-book-menu-items.component';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {MatMenuModule} from '@angular/material/menu';
import {
  CollectionDialogComponent,
  CollectionDialogData
} from '../dialogs/collection-dialog/collection-dialog.component';
import {UpdateCollection} from '../interfaces/update-collection';
import {filter, map} from 'rxjs';
import {ConfirmationDialogComponent} from '../dialogs/confirmation-dialog/confirmation-dialog.component';
import {SelectedCollection} from '../interfaces/selected-collection';
import {
  CollectionSelectorDialogComponent
} from '../dialogs/collection-selector-dialog/collection-selector-dialog.component';
import {CollectionNode} from '../interfaces/collection-node';
import {MatDividerModule} from '@angular/material/divider';
import {
  LibraryBookDetailsDialogComponent,
  LibraryBookDetailsDialogData,
  LibraryBookDetailsDialogResult
} from '../dialogs/library-book-details-dialog/library-book-details-dialog.component';
import {UpdateLibraryBookDetails} from '../interfaces/update-library-book-details';
import {CollectionBookSearchParams} from '../interfaces/collection-book-search-params';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {MatPaginatorModule, PageEvent} from '@angular/material/paginator';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {FormsModule} from '@angular/forms';
import {SortBarComponent} from '../common/sort-bar/sort-bar.component';
import {BooksDisplayComponent} from '../books-display/books-display.component';
import {EntityFilterStore} from '../services/entity-filter.store';
import {FilterShellComponent, TopRowFiltersDirective} from '../common/filter-shell/filter-shell.component';
import {TextFilterComponent} from '../common/filters/text-filter/text-filter.component';
import {SelectionStore} from '../services/selection.store';
import {BulkActionBarComponent} from '../common/bulk-action-bar/bulk-action-bar.component';
import {MatTooltipModule} from '@angular/material/tooltip';
import {NoteDialogComponent} from '../dialogs/note-dialog/note-dialog.component';
import {LocationDialogComponent} from '../dialogs/location-dialog/location-dialog.component';
import {QuotesListDialogComponent} from '../dialogs/quotes-list-dialog/quotes-list-dialog.component';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {TranslocoDirective, TranslocoService} from '@jsverse/transloco';
import {takeUntilDestroyed, toSignal} from '@angular/core/rxjs-interop';
import {CreateLocalBookDialogComponent} from '../dialogs/create-local-book-dialog/create-local-book-dialog.component';

const EMPTY_SEARCH_PARAMS: CollectionBookSearchParams = {
  title: '',
  recursive: false
};

@Component({
  selector: 'app-collection',
  standalone: true,
  imports: [
    CommonModule,
    MatButton,
    MatIconModule,
    LibraryBookMenuItemsComponent,
    MatMenuModule,
    RouterLink,
    MatDividerModule,
    MatIconButton,
    MatSlideToggleModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    FormsModule,
    SortBarComponent,
    BooksDisplayComponent,
    FilterShellComponent,
    TopRowFiltersDirective,
    TextFilterComponent,
    BulkActionBarComponent,
    MatTooltipModule,
    MatButtonToggleModule,
    TranslocoDirective,
  ],
  templateUrl: './collection.component.html',
  styleUrl: './collection.component.scss'
})
export class CollectionComponent implements OnInit {

  private translocoService = inject(TranslocoService);
  private destroyRef = inject(DestroyRef);

  collection!: CollectionDetails;

  booksState = {
    items: [] as LibraryBook[],
    totalElements: 0,
    pageSize: 15,
    currentPage: 0,
    loading: false,
    sort: undefined as string[] | undefined,
  };

  readonly selection = new SelectionStore();
  readonly filters = new EntityFilterStore<CollectionBookSearchParams>(EMPTY_SEARCH_PARAMS);
  readonly viewMode = signal<'grid' | 'list'>('grid');

  readonly isFiltersExpanded = signal(false);
  readonly activeFiltersCount = computed(() => 0);

  readonly statusOptions = toSignal(
    this.translocoService.selectTranslateObject('library.statuses').pipe(
      map(t => LIBRARY_BOOK_STATUSES.map(s => ({
        value: s,
        label: t[s]
      })))
    ),
    {initialValue: []}
  );

  readonly bookSortOptions = toSignal(
    this.translocoService.selectTranslateObject('library.sort').pipe(
      map(t => [
        {field: 'title', label: t.title},
        {field: 'publishYear', label: t.publishYear},
        {field: 'rating', label: t.rating},
        {field: 'addedAt', label: t.addedAt},
        {field: 'pages', label: t.pages},
      ])
    ),
    {initialValue: []}
  );

  private snackCommon: MatSnackCommon;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private collectionService: CollectionService,
    private collectionBookService: CollectionBookService,
    private dialog: MatDialog,
    private libraryBookService: LibraryBookService,
    matSnackBar: MatSnackBar,
  ) {
    this.snackCommon = new MatSnackCommon(matSnackBar);
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      const collectionId = +params['id'];
      this.loadCollection(collectionId);
    });

    this.translocoService.langChanges$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      const hadFilters = this.hasActiveFilters();
      this.clearAllFilters();
      if (!hadFilters) {
        this.loadBooks();
      }
    });

    this.filters.filtersChanged$.subscribe(() => {
      this.booksState.currentPage = 0;
      this.loadBooks();
    });
  }

  private loadCollection(id: number): void {
    this.collectionService.getById(id).subscribe(collection => {
      this.collection = collection;
      this.loadBooks();
    });
  }

  private loadBooks(): void {
    if (!this.collection) return;
    this.booksState.loading = true;
    this.collectionBookService.getCollectionBooks(this.collection.id, {
      ...this.filters.state(),
      page: this.booksState.currentPage,
      size: this.booksState.pageSize,
      sort: this.booksState.sort
    }).subscribe({
      next: page => {
        this.booksState.items = page.content;
        this.booksState.totalElements = page.page.totalElements;
        this.booksState.loading = false;
      },
      error: () => this.booksState.loading = false
    });
  }

  onPageChange(event: PageEvent): void {
    this.booksState.currentPage = event.pageIndex;
    this.booksState.pageSize = event.pageSize;
    this.loadBooks();
  }

  onSortChange(sort: string[] | undefined): void {
    this.booksState.sort = sort;
    this.booksState.currentPage = 0;
    this.loadBooks();
  }

  hasActiveFilters(): boolean {
    return this.filters.hasActiveFilters(f => f.title !== '' || f.recursive !== false);
  }

  clearAllFilters(): void {
    this.filters.reset(EMPTY_SEARCH_PARAMS);
  }

  openUpdateDialog(): void {
    const dialogRef = this.dialog.open(CollectionDialogComponent, {
      data: {
        isEdit: true,
        collection: this.collection
      } as CollectionDialogData
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe((updatedCollection: UpdateCollection) => {
      this.collectionService.update(this.collection.id, updatedCollection).subscribe({
        next: () => {
          this.loadCollection(this.collection.id);
          this.snackCommon.showSuccess(this.translocoService.translate('collections.success.updated'));
        },
        error: (err) => this.snackCommon.showError(err)
      });
    });
  }

  openCreateSubDialog(): void {
    const dialogRef = this.dialog.open(CollectionDialogComponent, {
      data: {
        isEdit: false,
        parentId: this.collection.id
      } as CollectionDialogData
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe((collection: CreateCollection) => {
      this.collectionService.create(collection).subscribe({
        next: () => {
          this.loadCollection(this.collection.id);
          this.snackCommon.showSuccess(this.translocoService.translate('collections.success.subcollectionCreated'));
        },
        error: (err) => this.snackCommon.showError(err)
      });
    });
  }

  addChildCollection(): void {
    this.collectionService.getTree().subscribe(tree => {
      const disabledIds = [
        this.collection.id,
        ...this.getAncestorIds(this.collection.id, tree),
        ...this.getSubtreeIds(this.collection.id, tree)
      ];

      const dialogRef = this.dialog.open(CollectionSelectorDialogComponent, {
        data: {
          initialSelectionId: null,
          disabledIds: disabledIds,
          showRoot: false
        }
      });

      dialogRef.afterClosed().pipe(filter(result => result !== undefined)).subscribe((selection: SelectedCollection) => {
        if (selection.id) {
          this.collectionService.move(selection.id, this.collection.id).subscribe({
            next: () => {
              this.loadCollection(this.collection.id);
              this.snackCommon.showSuccess(this.translocoService.translate('collections.success.added'));
            },
            error: (err) => this.snackCommon.showError(err)
          });
        }
      });
    });
  }

  moveCollection(): void {
    this.collectionService.getTree().subscribe(tree => {
      const disabledIds = [this.collection.id, ...this.getSubtreeIds(this.collection.id, tree)];
      if (this.collection.parentId) {
        disabledIds.push(this.collection.parentId);
      }

      const dialogRef = this.dialog.open(CollectionSelectorDialogComponent, {
        data: {
          initialSelectionId: this.collection.parentId || null,
          disabledIds: disabledIds,
          showRoot: true
        }
      });

      dialogRef.afterClosed().pipe(filter(result => result !== undefined)).subscribe((selection: SelectedCollection) => {
        this.collectionService.move(this.collection.id, selection.id).subscribe({
          next: () => {
            this.loadCollection(this.collection.id);
            this.snackCommon.showSuccess(this.translocoService.translate('collections.success.moved'));
          },
          error: (err) => this.snackCommon.showError(err)
        });
      });
    });
  }

  deleteCollection(): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        message: this.translocoService.translate('collections.deleteConfirm', {name: this.collection.name}),
        confirmLabel: 'common.delete'
      }
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe(() => {
      this.collectionService.delete(this.collection.id).subscribe({
        next: () => {
          this.snackCommon.showSuccess(this.translocoService.translate('collections.success.deleted'));
          this.goBack();
        },
        error: (err) => this.snackCommon.showError(err)
      });
    });
  }

  openViewBookListDialog(): void {
    const data: ViewBookListDialogData = {
      libraryBooks: this.booksState.items,
      categoryColumn: 'categoryName',
      fetchBooksFn: (options) => this.libraryBookService.getAll(options),
    };
    const dialogRef = this.dialog.open(ViewBookListDialog, {data});
    dialogRef.afterClosed().subscribe((result: number | string | undefined) => {
      if (typeof result === 'number') {
        this.collectionBookService.addBookToCollection(this.collection.id, result).subscribe({
          next: () => {
            this.loadBooks();
            this.snackCommon.showSuccess(this.translocoService.translate('collections.success.bookAdded'));
          },
          error: (err) => this.snackCommon.showError(err)
        });
      } else if (result === 'create-local') {
        this.openCreateLocalBookDialog();
      }
    });
  }

  private openCreateLocalBookDialog(): void {
    const dialogRef = this.dialog.open(CreateLocalBookDialogComponent, {
      width: '600px',
      autoFocus: false
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe((dto) => {
      this.libraryBookService.createLocalBook(dto).subscribe({
        next: () => {
          this.loadBooks();
          this.snackCommon.showSuccess(this.translocoService.translate('library.success.bookAdded'));
        },
        error: (err) => this.snackCommon.showError(err)
      });
    });
  }

  deleteBook(libraryBook: LibraryBook): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        message: this.translocoService.translate('collections.removeBookConfirm', {title: libraryBook.book.title}),
        confirmLabel: 'common.remove'
      }
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe(() => {
      this.collectionBookService.removeBookFromCollection(this.collection.id, libraryBook.id).subscribe({
        next: () => {
          this.loadBooks();
          this.snackCommon.showSuccess(this.translocoService.translate('collections.success.bookRemoved'));
        },
        error: (err) => this.snackCommon.showError(err)
      });
    });
  }

  statusChange(data: [LibraryBook, LibraryBookStatus]): void {
    this.libraryBookService.changeStatus(data[0].id, data[1]).subscribe({
      next: () => {
        this.loadBooks();
        this.snackCommon.showSuccess(this.translocoService.translate('library.success.statusChanged'));
      },
      error: (err) => this.snackCommon.showError(err)
    });
  }

  ratingChange(data: { libraryBookId: number; rating: number }): void {
    this.libraryBookService.changeRating(data.libraryBookId, data.rating).subscribe({
      next: () => {
        this.loadBooks();
        this.snackCommon.showSuccess(this.translocoService.translate('library.success.ratingChanged'));
      },
      error: (err) => this.snackCommon.showError(err)
    });
  }

  removeFromAllCollections(libraryBook: LibraryBook): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        message: this.translocoService.translate('library.removeFromAllCollectionsConfirm', {title: libraryBook.book.title}),
        confirmLabel: 'common.remove'
      }
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe(() => {
      this.collectionBookService.removeFromAllCollections(libraryBook.id).subscribe({
        next: () => {
          this.loadBooks();
          this.snackCommon.showSuccess(this.translocoService.translate('library.success.bookRemovedFromAllCollections'));
        },
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
        }
      });

      dialogRef.afterClosed().pipe(filter(result => result !== undefined)).subscribe((selection: SelectedCollection) => {
        if (selection.id) {
          this.collectionBookService.addBookToCollection(selection.id, libraryBook.id).subscribe({
            next: () => {
              this.snackCommon.showSuccess(this.translocoService.translate('collections.success.bookAdded'));
            },
            error: (err) => this.snackCommon.showError(err)
          });
        }
      });
    });
  }

  moveBookToOtherCollection(libraryBook: LibraryBook): void {
    const dialogRef = this.dialog.open(CollectionSelectorDialogComponent, {
      data: {
        initialSelectionId: null,
        disabledIds: [this.collection.id],
        showRoot: false
      }
    });

    dialogRef.afterClosed().pipe(filter(result => result !== undefined)).subscribe((selection: SelectedCollection) => {
      if (selection.id) {
        this.collectionService.moveBook(this.collection.id, libraryBook.id, selection.id).subscribe({
          next: () => {
            this.loadBooks();
            this.snackCommon.showSuccess(this.translocoService.translate('collections.success.bookMoved'));
          },
          error: (err) => this.snackCommon.showError(err)
        });
      }
    });
  }

  openEditDetailsDialog(libraryBook: LibraryBook): void {
    const dialogRef = this.dialog.open(LibraryBookDetailsDialogComponent, {
      data: {libraryBook} as LibraryBookDetailsDialogData
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe((result: LibraryBookDetailsDialogResult) => {
      switch (result.action) {
        case 'save':
          this.updateBookDetails(libraryBook.id, result.payload);
          break;
        case 'reset':
          this.resetBookDetails(libraryBook.id);
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
          this.snackCommon.showSuccess(this.translocoService.translate('library.success.detailsUpdated'));
        },
        error: err => this.snackCommon.showError(err)
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

  goBack() {
    this.router.navigate(['collections']);
  }

  bulkRemoveFromCollection(): void {
    const ids = this.selection.selectedIds();
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        message: this.translocoService.translate('collections.bulkRemoveFromCollectionConfirm', {count: ids.length}),
        confirmLabel: 'common.remove'
      }
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe(() => {
      this.collectionBookService.bulkRemove(this.collection.id, ids).subscribe({
        next: () => {
          this.loadBooks();
          this.selection.clear();
          this.snackCommon.showSuccess(this.translocoService.translate('collections.success.booksRemoved'));
        },
        error: (err) => this.snackCommon.showError(err)
      });
    });
  }

  bulkUpdateStatus(status: LibraryBookStatus): void {
    const ids = this.selection.selectedIds();
    this.libraryBookService.bulkUpdateStatus(ids, status).subscribe({
      next: () => {
        this.loadBooks();
        this.selection.clear();
        this.snackCommon.showSuccess(this.translocoService.translate('library.success.statusChanged'));
      },
      error: (err) => this.snackCommon.showError(err)
    });
  }

  openBulkAddToOtherCollectionDialog(): void {
    const ids = this.selection.selectedIds();
    const dialogRef = this.dialog.open(CollectionSelectorDialogComponent, {
      data: {
        initialSelectionId: null,
        disabledIds: [this.collection.id],
        showRoot: false
      }
    });

    dialogRef.afterClosed().pipe(filter(result => result !== undefined)).subscribe((selection: SelectedCollection) => {
      if (selection.id) {
        this.collectionBookService.bulkAdd(selection.id, ids).subscribe({
          next: () => {
            this.selection.clear();
            this.snackCommon.showSuccess(this.translocoService.translate('library.success.booksAddedToCollection'));
          },
          error: (err) => this.snackCommon.showError(err)
        });
      }
    });
  }

  private updateBookDetails(libraryBookId: number, dto: Partial<UpdateLibraryBookDetails>): void {
    this.libraryBookService.updateDetails(libraryBookId, dto).subscribe({
      next: () => {
        this.loadBooks();
        this.snackCommon.showSuccess(this.translocoService.translate('library.success.detailsUpdated'));
      },
      error: (err) => this.snackCommon.showError(err)
    });
  }

  private resetBookDetails(libraryBookId: number): void {
    this.libraryBookService.resetDetails(libraryBookId).subscribe({
      next: () => {
        this.loadBooks();
        this.snackCommon.showSuccess(this.translocoService.translate('library.success.detailsReset'));
      },
      error: (err) => this.snackCommon.showError(err)
    });
  }

  private getSubtreeIds(targetId: number, tree: CollectionNode[]): number[] {
    const node = this.findNode(targetId, tree);
    if (!node) return [];
    return this.collectSubtreeIds(node);
  }

  private collectSubtreeIds(node: CollectionNode): number[] {
    let ids: number[] = [];
    if (node.children) {
      for (const child of node.children) {
        ids.push(child.id);
        ids = ids.concat(this.collectSubtreeIds(child));
      }
    }
    return ids;
  }

  private getAncestorIds(targetId: number, tree: CollectionNode[]): number[] {
    const path = this.findPath(targetId, tree);
    if (path) {
      return path.map(c => c.id).filter(id => id !== targetId);
    }
    return [];
  }

  private findNode(id: number, currentLevel: CollectionNode[]): CollectionNode | null {
    for (const node of currentLevel) {
      if (node.id === id) return node;
      if (node.children) {
        const found = this.findNode(id, node.children);
        if (found) return found;
      }
    }
    return null;
  }

  private findPath(targetId: number, currentLevel: CollectionNode[], path: CollectionNode[] = []): CollectionNode[] | null {
    for (const col of currentLevel) {
      const newPath = [...path, col];
      if (col.id === targetId) return newPath;
      if (col.children) {
        const found = this.findPath(targetId, col.children, newPath);
        if (found) return found;
      }
    }
    return null;
  }

}
