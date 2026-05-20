import {Component, DestroyRef, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RecommendationService} from '../services/recommendation.service';
import {Book} from '../interfaces/book';
import {BookCardComponent} from '../book-card/book-card.component';
import {BookListItemComponent} from '../book-list-item/book-list-item.component';
import {TranslocoDirective, TranslocoService} from '@jsverse/transloco';
import {MatProgressSpinner} from '@angular/material/progress-spinner';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize} from 'rxjs';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {MatIconModule} from '@angular/material/icon';
import {SelectionStore} from '../services/selection.store';
import {LibraryBookService} from '../services/library-book.service';
import {MatSnackCommon} from '../common/mat-snack-common';
import {MatSnackBar} from '@angular/material/snack-bar';
import {BulkActionBarComponent} from '../common/bulk-action-bar/bulk-action-bar.component';
import {MatButtonModule} from '@angular/material/button';
import {MatMenuModule} from '@angular/material/menu';
import {MatExpansionModule} from '@angular/material/expansion';

@Component({
  selector: 'app-recommendations',
  standalone: true,
  imports: [
    CommonModule,
    BookCardComponent,
    BookListItemComponent,
    TranslocoDirective,
    MatProgressSpinner,
    MatButtonToggleModule,
    MatIconModule,
    BulkActionBarComponent,
    MatButtonModule,
    MatMenuModule,
    MatExpansionModule,
  ],
  templateUrl: './recommendations.component.html',
  styleUrl: './recommendations.component.scss'
})
export class RecommendationsComponent implements OnInit {

  private snackCommon: MatSnackCommon;

  personalized = signal<Book[]>([]);
  popular = signal<Book[]>([]);
  newArrivals = signal<Book[]>([]);
  trendingGenres = signal<Book[]>([]);

  loadingPersonalized = signal<boolean>(false);
  loadingPopular = signal<boolean>(false);
  loadingNewArrivals = signal<boolean>(false);
  loadingTrendingGenres = signal<boolean>(false);

  loadedPersonalized = signal<boolean>(false);
  loadedPopular = signal<boolean>(false);
  loadedNewArrivals = signal<boolean>(false);
  loadedTrendingGenres = signal<boolean>(false);

  expandedPersonalized = signal<boolean>(false);
  expandedPopular = signal<boolean>(false);
  expandedNewArrivals = signal<boolean>(false);
  expandedTrendingGenres = signal<boolean>(false);

  viewMode = signal<'grid' | 'list'>('grid');

  readonly selection = new SelectionStore();
  private libraryBookIds: Set<number> = new Set<number>();

  constructor(
    private recommendationService: RecommendationService,
    private translocoService: TranslocoService,
    private libraryBookService: LibraryBookService,
    private destroyRef: DestroyRef,
    matSnackBar: MatSnackBar,
  ) {
    this.snackCommon = new MatSnackCommon(matSnackBar);
  }

  ngOnInit(): void {
    this.setupLanguageSubscription();
  }

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

  private setupLanguageSubscription(): void {
    this.translocoService.langChanges$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        const needsReload = {
          personalized: this.personalized().length > 0 && this.expandedPersonalized(),
          popular: this.popular().length > 0 && this.expandedPopular(),
          newArrivals: this.newArrivals().length > 0 && this.expandedNewArrivals(),
          trendingGenres: this.trendingGenres().length > 0 && this.expandedTrendingGenres()
        };

        this.clearData();

        if (needsReload.personalized) this.loadPersonalized();
        if (needsReload.popular) this.loadPopular();
        if (needsReload.newArrivals) this.loadNewArrivals();
        if (needsReload.trendingGenres) this.loadTrendingGenres();
      });
  }

  private clearData(): void {
    this.personalized.set([]);
    this.popular.set([]);
    this.newArrivals.set([]);
    this.trendingGenres.set([]);

    this.loadedPersonalized.set(false);
    this.loadedPopular.set(false);
    this.loadedNewArrivals.set(false);
    this.loadedTrendingGenres.set(false);
  }

  loadPersonalized(): void {
    if (this.loadedPersonalized() || this.loadingPersonalized()) return;
    this.loadingPersonalized.set(true);
    this.recommendationService.getPersonalized(20)
      .pipe(finalize(() => {
        this.loadingPersonalized.set(false);
        this.loadedPersonalized.set(true);
      }))
      .subscribe(books => this.personalized.set(books));
  }

  loadPopular(): void {
    if (this.loadedPopular() || this.loadingPopular()) return;
    this.loadingPopular.set(true);
    this.recommendationService.getPopular(20)
      .pipe(finalize(() => {
        this.loadingPopular.set(false);
        this.loadedPopular.set(true);
      }))
      .subscribe(books => this.popular.set(books));
  }

  loadNewArrivals(): void {
    if (this.loadedNewArrivals() || this.loadingNewArrivals()) return;
    this.loadingNewArrivals.set(true);
    this.recommendationService.getNewArrivals(20)
      .pipe(finalize(() => {
        this.loadingNewArrivals.set(false);
        this.loadedNewArrivals.set(true);
      }))
      .subscribe(books => this.newArrivals.set(books));
  }

  loadTrendingGenres(): void {
    if (this.loadedTrendingGenres() || this.loadingTrendingGenres()) return;
    this.loadingTrendingGenres.set(true);
    this.recommendationService.getTrendingInFavoriteGenres(20)
      .pipe(finalize(() => {
        this.loadingTrendingGenres.set(false);
        this.loadedTrendingGenres.set(true);
      }))
      .subscribe(books => this.trendingGenres.set(books));
  }

}
