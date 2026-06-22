import {Component, computed, DestroyRef, effect, inject, input, OnInit, signal, untracked} from '@angular/core';
import {takeUntilDestroyed, toObservable, toSignal} from '@angular/core/rxjs-interop';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatAutocompleteModule} from '@angular/material/autocomplete';
import {MatInputModule} from '@angular/material/input';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatSelectModule} from '@angular/material/select';
import {PageEvent} from '@angular/material/paginator';
import {MatMenuPanel} from '@angular/material/menu';
import {TranslocoDirective, TranslocoService} from '@jsverse/transloco';
import {map, Observable, switchMap} from 'rxjs';

import {BookService} from '../services/book.service';
import {AuthorService} from '../services/author.service';
import {CategoryService} from '../services/category.service';
import {LibraryBookService} from '../services/library-book.service';
import {LibraryAuthorService} from '../services/library-author.service';
import {LibraryCategoryService} from '../services/library-category.service';
import {EntityFilterStore} from '../services/entity-filter.store';
import {AutocompleteSearchStore} from '../services/autocomplete-search.store';
import {SelectionStore} from '../services/selection.store';
import {LibraryStore} from '../services/library.store';

import {Book} from '../interfaces/book';
import {LibraryBook, LIBRARY_BOOK_STATUSES} from '../interfaces/library-book';
import {Author} from '../interfaces/author';
import {Category} from '../interfaces/category';
import {LanguageWithCount} from '../interfaces/language-with-count';
import {BaseBookFilters, LibraryFilters} from '../interfaces/filters';
import {Page} from '../interfaces/page';

import {BooksDisplayComponent} from '../books-display/books-display.component';
import {SortBarComponent} from '../common/sort-bar/sort-bar.component';
import {BulkActionBarComponent} from '../common/bulk-action-bar/bulk-action-bar.component';
import {
  FilterShellComponent,
  FooterFiltersDirective,
  MainFiltersDirective,
  SecondaryFiltersDirective,
  TopRowFiltersDirective
} from '../common/filter-shell/filter-shell.component';
import {TextFilterComponent} from '../common/filters/text-filter/text-filter.component';
import {AutocompleteFilterComponent} from '../common/filters/autocomplete-filter/autocomplete-filter.component';
import {RangeFilterComponent} from '../common/filters/range-filter/range-filter.component';
import {LanguageFilterComponent} from '../common/filters/language-filter/language-filter.component';
import {SelectFilterComponent} from '../common/filters/select-filter/select-filter.component';
import {MatButtonToggleModule} from '@angular/material/button-toggle';

const EMPTY_BOOK_FILTERS: BaseBookFilters = {
  title: '',
  author: null,
  category: null,
  publishYear: {min: null, max: null},
  pages: {min: null, max: null},
  languages: []
};

const EMPTY_LIBRARY_FILTERS: LibraryFilters = {
  ...EMPTY_BOOK_FILTERS,
  status: null,
  rating: {min: null, max: null},
  location: ''
};

@Component({
  selector: 'app-book-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatAutocompleteModule,
    MatInputModule,
    MatTooltipModule,
    MatCheckboxModule,
    MatSelectModule,
    BooksDisplayComponent,
    SortBarComponent,
    FilterShellComponent,
    TopRowFiltersDirective,
    MainFiltersDirective,
    SecondaryFiltersDirective,
    FooterFiltersDirective,
    TextFilterComponent,
    AutocompleteFilterComponent,
    RangeFilterComponent,
    LanguageFilterComponent,
    SelectFilterComponent,
    BulkActionBarComponent,
    TranslocoDirective,
    MatButtonToggleModule
  ],
  templateUrl: './book-list.component.html',
  styleUrl: './book-list.component.scss'
})
export class BookListComponent implements OnInit {

  mode = input.required<'search' | 'admin' | 'library'>();
  actionsMenu = input<MatMenuPanel<any> | null>(null);
  viewMode = input<'grid' | 'list'>('grid');

  private translocoService = inject(TranslocoService);
  private bookService = inject(BookService);
  private libraryBookService = inject(LibraryBookService);
  private authorService = inject(AuthorService);
  private categoryService = inject(CategoryService);
  private libraryAuthorService = inject(LibraryAuthorService);
  private libraryCategoryService = inject(LibraryCategoryService);
  private destroyRef = inject(DestroyRef);
  private libraryStore = inject(LibraryStore);

  readonly selection = new SelectionStore();
  readonly filters = new EntityFilterStore<LibraryFilters>(EMPTY_LIBRARY_FILTERS);

  booksState = {
    items: [] as (Book | LibraryBook)[],
    totalElements: 0,
    pageSize: 15,
    currentPage: 0,
    loading: false,
    sort: undefined as string[] | undefined,
  };

  readonly isFiltersExpanded = signal(false);
  readonly activeFiltersCount = computed(() => {
    const f = this.filters.state();
    let count = 0;
    if (f.author) count++;
    if (f.category) count++;
    if (f.publishYear.min || f.publishYear.max) count++;
    if (f.pages.min || f.pages.max) count++;
    if (f.languages.length > 0) count++;

    if (this.mode() === 'library') {
      if (f.status) count++;
      if (f.rating.min || f.rating.max) count++;
      if (f.location) count++;
    }
    return count;
  });

  readonly authorSearch = new AutocompleteSearchStore<Author>(
    (q, p, s) => this.mode() === 'library'
      ? this.libraryAuthorService.getAll({name: q, page: p, size: s})
      : this.authorService.search({name: q, page: p, size: s}),
    450,
    10
  );

  readonly categorySearch = new AutocompleteSearchStore<Category>(
    (q, p, s) => this.mode() === 'library'
      ? this.libraryCategoryService.getAll({name: q, page: p, size: s})
      : this.categoryService.search({name: q, page: p, size: s}),
    450,
    10
  );

  languages = signal<LanguageWithCount[]>([]);
  showAllLanguages = signal(false);

  readonly filterPrefix = computed(() => this.mode() === 'library' ? 'library.filters' : 'search.filters');

  readonly sortOptions = toSignal(
    toObservable(this.mode).pipe(
      switchMap(m => this.translocoService.selectTranslateObject(m === 'library' ? 'library.sort' : 'search.sort').pipe(
        map(t => {
          const options = [
            {field: 'title', label: t.title},
            {field: 'publishYear', label: t.publishYear},
            {field: 'pages', label: t.pages},
          ];
          if (m === 'library') {
            options.push({field: 'rating', label: t.rating});
            options.push({field: 'addedAt', label: t.addedAt});
          } else {
            options.push({field: 'language', label: t.language});
            options.push({field: 'categoryName', label: t.category});
            options.push({field: 'popularityCount', label: t.popularity});
          }
          return options;
        })
      ))
    ),
    {initialValue: []}
  );

  readonly statusOptions = toSignal(
    this.translocoService.selectTranslateObject('library.statuses').pipe(
      map(t => LIBRARY_BOOK_STATUSES.map(s => ({
        value: s,
        label: t[s]
      })))
    ),
    {initialValue: []}
  );

  constructor() {
    effect(() => {
      const version = this.libraryStore.refreshVersion();
      untracked(() => {
        if (version !== 0 && this.mode() === 'library') {
          this.loadBooks();
          this.loadLanguages();
        }
      });
    });
  }

  ngOnInit(): void {
    if (this.mode() !== 'library') {
      this.filters.reset(EMPTY_BOOK_FILTERS as LibraryFilters);
    }
    this.setupSubscriptions();
  }

  loadBooks(): void {
    this.booksState.loading = true;
    const f = this.filters.state();
    const isLibrary = this.mode() === 'library';

    const options: any = {
      page: this.booksState.currentPage,
      size: this.booksState.pageSize,
      sort: this.booksState.sort,
      title: f.title || undefined,
      authorId: f.author?.id,
      categoryId: f.category?.id,
      publishYearMin: f.publishYear.min ?? undefined,
      publishYearMax: f.publishYear.max ?? undefined,
      pagesMin: f.pages.min ?? undefined,
      pagesMax: f.pages.max ?? undefined,
      languages: f.languages.length > 0 ? f.languages : undefined,
    };

    if (isLibrary) {
      options.status = f.status;
      options.ratingMin = f.rating.min ?? undefined;
      options.ratingMax = f.rating.max ?? undefined;
      options.location = f.location || undefined;
    }

    const request: Observable<Page<any>> = isLibrary
      ? this.libraryBookService.getAll(options)
      : this.bookService.getAll(options);

    request.subscribe({
      next: page => {
        this.booksState.items = page.content;
        this.booksState.totalElements = page.page.totalElements;
        this.booksState.loading = false;
      },
      error: () => this.booksState.loading = false
    });
  }

  private loadLanguages(): void {
    const request = this.mode() === 'library'
      ? this.libraryBookService.getLanguages()
      : this.bookService.getLanguages();

    request.subscribe(langs => this.languages.set(langs));
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

  onAuthorSearchInput(val: string): void {
    this.authorSearch.search(val);
  }

  onCategorySearchInput(val: string): void {
    this.categorySearch.search(val);
  }

  onAuthorSelected(author: Author): void {
    this.filters.update('author', author);
    this.authorSearch.clear();
  }

  onCategorySelected(category: Category): void {
    this.filters.update('category', category);
    this.categorySearch.clear();
  }

  clearAuthorFilter(): void {
    this.filters.update('author', null);
    this.authorSearch.clear();
  }

  clearCategoryFilter(): void {
    this.filters.update('category', null);
    this.categorySearch.clear();
  }

  toggleLanguage(language: string): void {
    const current = [...this.filters.state().languages];
    const index = current.indexOf(language);
    if (index === -1) {
      current.push(language);
    } else {
      current.splice(index, 1);
    }
    this.filters.update('languages', current);
  }

  hasActiveFilters(): boolean {
    return this.filters.hasActiveFilters(f => {
      let active = f.author !== null || f.category !== null || f.title !== '' ||
        f.publishYear.min !== null || f.publishYear.max !== null ||
        f.pages.min !== null || f.pages.max !== null ||
        f.languages.length > 0;

      if (this.mode() === 'library') {
        active = active || f.status !== null || f.rating.min !== null || f.rating.max !== null || (f.location !== undefined && f.location !== '');
      }
      return active;
    });
  }

  clearAllFilters(): void {
    this.filters.reset(this.mode() === 'library' ? EMPTY_LIBRARY_FILTERS : EMPTY_BOOK_FILTERS as LibraryFilters);
    this.authorSearch.clear();
    this.categorySearch.clear();
  }

  asLibraryFilters(f: LibraryFilters): LibraryFilters {
    return f;
  }

  private setupSubscriptions(): void {
    this.translocoService.langChanges$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      const hadFilters = this.hasActiveFilters();
      this.clearAllFilters();
      if (!hadFilters) {
        this.loadBooks();
      }
      this.loadLanguages();
    });

    this.filters.filtersChanged$.subscribe(() => {
      this.booksState.currentPage = 0;
      this.loadBooks();
    });
  }

}
