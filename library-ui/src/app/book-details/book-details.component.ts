import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Book} from '../interfaces/book';
import {CommonModule, NgOptimizedImage} from '@angular/common';
import {BookRatingComponent} from '../book-rating/book-rating.component';
import {BookDetails} from '../interfaces/book-details';
import {BookService} from '../services/book.service';
import {MatAnchor, MatButton} from '@angular/material/button';
import {LibraryBookService} from '../services/library-book.service';
import {BasicCollection} from '../interfaces/basic-collection';
import {TranslocoDirective, TranslocoService} from '@jsverse/transloco';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatSnackCommon} from '../common/mat-snack-common';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {MatIcon} from '@angular/material/icon';
import {MatTooltip} from '@angular/material/tooltip';
import {RecommendationService} from '../services/recommendation.service';
import {BookCardComponent} from '../book-card/book-card.component';
import {SelectionStore} from '../services/selection.store';
import {BookListItemComponent} from '../book-list-item/book-list-item.component';
import {BulkActionBarComponent} from '../common/bulk-action-bar/bulk-action-bar.component';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {MatMenuModule} from '@angular/material/menu';
import {skip} from 'rxjs';

@Component({
  selector: 'app-book-details',
  imports: [
    CommonModule,
    NgOptimizedImage,
    BookRatingComponent,
    MatButton,
    MatAnchor,
    TranslocoDirective,
    MatIcon,
    MatTooltip,
    BookCardComponent,
    BookListItemComponent,
    BulkActionBarComponent,
    MatButtonToggleModule,
    MatMenuModule,
  ],
  templateUrl: './book-details.component.html',
  styleUrl: './book-details.component.scss'
})
export class BookDetailsComponent implements OnInit {

  private snackCommon: MatSnackCommon;
  private destroyRef = inject(DestroyRef);

  bookId!: number;
  bookDetails?: BookDetails;
  similarBooks = signal<Book[]>([]);
  viewMode = signal<'grid' | 'list'>('grid');
  readonly selection = new SelectionStore();
  private libraryBookIds: Set<number> = new Set<number>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookService: BookService,
    private libraryBookService: LibraryBookService,
    private recommendationService: RecommendationService,
    matSnackBar: MatSnackBar,
    private translocoService: TranslocoService,
  ) {
    this.snackCommon = new MatSnackCommon(matSnackBar);
  }

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.bookId = Number(id);
        this.loadAll();
      } else {
        this.router.navigate(['/']);
      }
    });

    this.translocoService.langChanges$.pipe(skip(1), takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.bookId) {
        this.loadAll();
      }
    });
  }

  get displayBook(): Book | undefined {
    return this.bookDetails?.libraryBook?.book ?? this.bookDetails?.book;
  }

  get authors(): Array<[number, string]> {
    return Object.entries(this.displayBook?.authors ?? {}) as {} as Array<[number, string]>;
  }

  get myRating(): number {
    return this.bookDetails?.libraryBook?.rating ?? 0;
  }

  addBookToLibrary(): void {
    this.libraryBookService.addBook(this.bookId).subscribe({
      next: (libraryBook) => {
        if (this.bookDetails) {
          this.bookDetails.libraryBook = libraryBook;
          this.bookDetails.book = undefined;
        }
        this.snackCommon.showSuccess(this.translocoService.translate('library.success.bookAdded'));
      },
      error: (err) => this.snackCommon.showError(err)
    });
  }

  addSimilarBookToLibrary(book: Book): void {
    this.libraryBookService.addBook(book.id).subscribe({
      next: () => {
        this.libraryBookIds.add(book.id);
        this.snackCommon.showSuccess(this.translocoService.translate('library.success.bookAdded'));
      },
      error: (err) => {
        this.snackCommon.showError(err);
        if (err.status === 400) {
          this.libraryBookIds.add(book.id);
        }
      }
    });
  }

  bulkAddSimilarBooks(): void {
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

  isSimilarBookInLibrary(book: Book): boolean {
    return this.libraryBookIds.has(book.id);
  }

  goToCollection(collection: BasicCollection): void {
    this.router.navigate(['/collections', collection.id]);
  }

  goToCategoryDetails(): void {
    if (this.displayBook?.categoryId) {
      this.router.navigate(['/category-details', this.displayBook.categoryId]);
    }
  }

  goToAuthorDetails(id: string | number): void {
    this.router.navigate(['/author-details', Number(id)]);
  }

  changeRating(rating: number): void {
    const libraryBookId = this.bookDetails?.libraryBook?.id;
    if (!libraryBookId) return;

    this.libraryBookService.changeRating(libraryBookId, rating).subscribe({
      next: (libraryBook) => {
        if (this.bookDetails) {
          this.bookDetails.libraryBook = libraryBook;
        }
        this.snackCommon.showSuccess(this.translocoService.translate('library.success.ratingChanged'));
      },
      error: (err) => this.snackCommon.showError(err)
    });
  }

  private loadAll(): void {
    this.loadBookDetails();
    this.loadSimilarBooks();
    this.selection.clear();
  }

  private loadBookDetails(): void {
    this.bookService.getBookDetails(this.bookId).subscribe(bookDetails => {
      this.bookDetails = bookDetails;
    });
  }

  private loadSimilarBooks(): void {
    this.recommendationService.getSimilar(this.bookId, 10).subscribe(books => {
      this.similarBooks.set(books);
    });
  }

}
