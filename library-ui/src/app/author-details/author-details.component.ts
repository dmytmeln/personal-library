import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {Author} from '../interfaces/author';
import {ActivatedRoute, Router} from '@angular/router';
import {BookService} from '../services/book.service';
import {AuthorService} from '../services/author.service';
import {Book} from '../interfaces/book';
import {MatIconModule} from '@angular/material/icon';
import {MatMenuModule} from '@angular/material/menu';
import {LibraryBookService} from '../services/library-book.service';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatSnackCommon} from '../common/mat-snack-common';
import {BooksDisplayComponent} from '../books-display/books-display.component';
import {PageEvent} from '@angular/material/paginator';
import {SortBarComponent} from '../common/sort-bar/sort-bar.component';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {CommonModule} from '@angular/common';
import {MatButtonModule} from '@angular/material/button';
import {SelectionStore} from '../services/selection.store';
import {BulkActionBarComponent} from '../common/bulk-action-bar/bulk-action-bar.component';
import {TranslocoDirective, TranslocoService} from '@jsverse/transloco';
import {takeUntilDestroyed, toSignal} from '@angular/core/rxjs-interop';
import {map, skip} from 'rxjs';

@Component({
  selector: 'app-author-details',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatMenuModule,
    BooksDisplayComponent,
    SortBarComponent,
    MatButtonToggleModule,
    MatButtonModule,
    BulkActionBarComponent,
    TranslocoDirective,
  ],
  templateUrl: './author-details.component.html',
  styleUrl: './author-details.component.scss'
})
export class AuthorDetailsComponent implements OnInit {

  private translocoService = inject(TranslocoService);
  private destroyRef = inject(DestroyRef);

  protected readonly bookSortOptions = toSignal(
    this.translocoService.selectTranslateObject('search.sort').pipe(
      map(t => [
        {field: 'title', label: t.title},
        {field: 'publishYear', label: t.publishYear},
        {field: 'language', label: t.language},
        {field: 'pages', label: t.pages},
        {field: 'category.name', label: t.category},
      ])
    ),
    {initialValue: []}
  );

  private authorId!: number;
  private libraryBookIds: Set<number> = new Set<number>();
  private snackCommon: MatSnackCommon;
  private currentSort: string[] | undefined;

  author!: Author;
  books: Book[] = [];
  totalElements = 0;
  pageSize = 15;
  pageIndex = 0;
  loading = false;
  readonly viewMode = signal<'grid' | 'list'>('grid');
  readonly selection = new SelectionStore();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookService: BookService,
    private authorService: AuthorService,
    private libraryBookService: LibraryBookService,
    matSnackBar: MatSnackBar,
  ) {
    this.snackCommon = new MatSnackCommon(matSnackBar);
  }

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.authorId = +id;
        this.initAuthor();
        this.loadBooks();
      } else {
        this.router.navigate(['/']);
      }
    });

    this.translocoService.langChanges$.pipe(skip(1), takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.authorId) {
        this.initAuthor();
        this.loadBooks();
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadBooks();
  }

  onSortChange(sort: string[] | undefined): void {
    this.currentSort = sort;
    this.pageIndex = 0;
    this.loadBooks();
  }

  // todo duplicate code
  addBookToLibrary(book: Book): void {
    this.libraryBookService.addBook(book.id).subscribe({
      next: () => {
        this.libraryBookIds.add(book.id);
        this.snackCommon.showSuccess(this.translocoService.translate('library.success.bookAdded'));
      },
      error: err => {
        this.snackCommon.showError(err);
        if (err.status === 400) {
          this.libraryBookIds.add(book.id);
        }
      }
    });
  }

  // todo duplicate code
  bulkAddBooks(): void {
    const ids = this.selection.selectedIds();
    this.libraryBookService.bulkAdd(ids).subscribe({
      next: () => {
        ids.forEach(id => this.libraryBookIds.add(id));
        this.selection.clear();
        this.snackCommon.showSuccess(this.translocoService.translate('library.success.bookAdded'));
      },
      error: err => this.snackCommon.showError(err)
    });
  }

  isBookInLibrary(book: Book): boolean {
    return this.libraryBookIds.has(book.id);
  }

  private initAuthor(): void {
    this.authorService.getById(this.authorId).subscribe(author => {
      this.author = author;
    });
  }

  private loadBooks(): void {
    this.loading = true;
    this.bookService.getAll({
      authorId: this.authorId,
      page: this.pageIndex,
      size: this.pageSize,
      sort: this.currentSort
    }).subscribe({
      next: page => {
        this.books = page.content;
        this.totalElements = page.page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

}
