import {Component, signal, viewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatTab, MatTabGroup} from '@angular/material/tabs';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {MatSnackBar} from '@angular/material/snack-bar';
import {LibraryBookService} from '../services/library-book.service';
import {Book} from '../interfaces/book';
import {Author} from '../interfaces/author';
import {Category} from '../interfaces/category';
import {MatSnackCommon} from '../common/mat-snack-common';
import {MatMenu, MatMenuContent, MatMenuItem} from '@angular/material/menu';
import {AuthorListComponent} from '../author-list/author-list.component';
import {CategoryListComponent} from '../category-list/category-list.component';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {TranslocoDirective, TranslocoService} from '@jsverse/transloco';
import {BookListComponent} from '../book-list/book-list.component';
import {FormsModule} from '@angular/forms';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {RecommendationService} from '../services/recommendation.service';
import {BookCardComponent} from '../book-card/book-card.component';
import {BookListItemComponent} from '../book-list-item/book-list-item.component';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [
    CommonModule,
    MatTabGroup,
    MatTab,
    MatIconModule,
    MatButtonModule,
    AuthorListComponent,
    CategoryListComponent,
    MatButtonToggleModule,
    TranslocoDirective,
    BookListComponent,
    MatMenu,
    MatMenuContent,
    MatMenuItem,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    BookCardComponent,
    BookListItemComponent
  ],
  templateUrl: './search.component.html',
  styleUrl: './search.component.scss'
})
export class SearchComponent {

  private snackCommon: MatSnackCommon;

  readonly bookListComponent = viewChild(BookListComponent);
  readonly viewMode = signal<'grid' | 'list'>('grid');

  readonly moodQuery = signal<string>('');
  readonly moodResults = signal<Book[]>([]);
  readonly loadingMood = signal<boolean>(false);
  readonly moodSearched = signal<boolean>(false);

  private libraryBookIds: Set<number> = new Set<number>();

  uiState = {
    activeTabIndex: 0,
    authorsOpened: false,
    categoriesOpened: false,
  };

  constructor(
    private translocoService: TranslocoService,
    private libraryBookService: LibraryBookService,
    private recommendationService: RecommendationService,
    matSnackBar: MatSnackBar
  ) {
    this.snackCommon = new MatSnackCommon(matSnackBar);
  }

  onMoodSearch(): void {
    const query = this.moodQuery().trim();
    if (!query) return;

    this.loadingMood.set(true);
    this.moodSearched.set(false);
    this.recommendationService.searchByMood(query, 10).subscribe({
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
    const list = this.bookListComponent();
    if (!list) return;
    const ids = list.selection.selectedIds();
    this.libraryBookService.bulkAdd(ids).subscribe({
      next: () => {
        ids.forEach(id => this.libraryBookIds.add(id));
        list.selection.clear();
        this.snackCommon.showSuccess(this.translocoService.translate('library.success.bookAdded'));
      },
      error: err => this.snackCommon.showError(err)
    });
  }

  isBookInLibrary(book: Book | any): boolean {
    return this.libraryBookIds.has(book.id);
  }

  onTabChange(index: number): void {
    this.uiState.activeTabIndex = index;
    if (index === 1) this.uiState.authorsOpened = true;
    if (index === 2) this.uiState.categoriesOpened = true;
  }

  showAuthorBooks(author: Author): void {
    this.uiState.activeTabIndex = 0;
    const bookListComponent = this.bookListComponent();
    if (bookListComponent) {
      bookListComponent.onAuthorSelected(author);
    }
  }

  goToCategoryBooks(category: Category): void {
    this.uiState.activeTabIndex = 0;
    const bookListComponent = this.bookListComponent();
    if (bookListComponent) {
      bookListComponent.onCategorySelected(category);
    }
  }

}
