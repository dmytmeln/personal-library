import {Component, output, ViewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatMenuModule, MatMenuPanel} from '@angular/material/menu';
import {MatIconModule} from '@angular/material/icon';
import {BookRatingComponent} from '../book-rating/book-rating.component';
import {LIBRARY_BOOK_STATUSES, LibraryBook, LibraryBookStatus} from '../interfaces/library-book';
import {Router} from '@angular/router';
import {Book} from '../interfaces/book';
import {TranslocoDirective} from '@jsverse/transloco';

@Component({
  selector: 'app-library-book-menu-items',
  standalone: true,
  imports: [
    CommonModule,
    MatMenuModule,
    MatIconModule,
    BookRatingComponent,
    TranslocoDirective,
  ],
  templateUrl: './library-book-menu-items.component.html',
  styleUrl: './library-book-menu-items.component.scss'
})
export class LibraryBookMenuItemsComponent {

  protected readonly LIBRARY_BOOK_STATUSES = LIBRARY_BOOK_STATUSES;

  @ViewChild('statusMenu', {static: true}) statusMenu!: MatMenuPanel<any>;
  @ViewChild('ratingMenu', {static: true}) ratingMenu!: MatMenuPanel<any>;

  statusChange = output<[LibraryBook, LibraryBookStatus]>();
  ratingChange = output<{ libraryBookId: number; rating: number }>();

  constructor(
    private router: Router,
  ) {
  }
}
