import {Component, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {AdminService} from '../../services/admin.service';
import {AdminBookDto} from '../../interfaces/admin-book-dto';
import {MatTabsModule} from '@angular/material/tabs';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatSelectModule} from '@angular/material/select';
import {CategoryService} from '../../services/category.service';
import {AuthorService} from '../../services/author.service';
import {Category} from '../../interfaces/category';
import {Author} from '../../interfaces/author';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatSnackCommon} from '../../common/mat-snack-common';
import {TranslocoDirective, TranslocoService} from '@jsverse/transloco';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {AutocompleteFilterComponent} from '../../common/filters/autocomplete-filter/autocomplete-filter.component';
import {MatDialog, MatDialogModule} from '@angular/material/dialog';
import {AuthorSelectionDialogComponent} from '../author-selection-dialog/author-selection-dialog.component';
import {MatChipsModule} from '@angular/material/chips';
import {forkJoin, of} from 'rxjs';
import {AutocompleteSearchStore} from '../../services/autocomplete-search.store';
import {ConfirmationDialogComponent} from '../../dialogs/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-admin-book-details',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    TranslocoDirective,
    MatProgressSpinnerModule,
    AutocompleteFilterComponent,
    MatDialogModule,
    MatChipsModule,
  ],
  templateUrl: './admin-book-details.component.html',
  styleUrl: './admin-book-details.component.scss'
})
export class AdminBookDetailsComponent implements OnInit {

  bookId: number | null = null;
  form: FormGroup;
  languages = ['en', 'uk'];

  readonly categorySearch: AutocompleteSearchStore<Category>;
  selectedCategory = signal<Category | null>(null);
  selectedAuthors = signal<Author[]>([]);

  loading = signal(false);
  private snackCommon: MatSnackCommon;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private adminService: AdminService,
    private categoryService: CategoryService,
    private authorService: AuthorService,
    private translocoService: TranslocoService,
    private dialog: MatDialog,
    private matSnackBar: MatSnackBar
  ) {
    this.snackCommon = new MatSnackCommon(this.matSnackBar);
    this.categorySearch = new AutocompleteSearchStore<Category>(
      (query: string, page: number, size: number) => this.categoryService.search({name: query, page, size})
    );

    this.form = this.fb.group({
      categoryId: [null, Validators.required],
      publishYear: [null, [Validators.required, Validators.min(0)]],
      pages: [null, [Validators.required, Validators.min(1)]],
      coverImageUrl: [''],
      authorIds: [[], Validators.required],
      translations: this.fb.group({
        en: this.fb.group({
          title: ['', Validators.required],
          bookLanguage: ['', Validators.required],
          description: ['']
        }),
        uk: this.fb.group({
          title: ['', Validators.required],
          bookLanguage: ['', Validators.required],
          description: ['']
        })
      })
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.bookId = Number(id);
      this.loadBook();
    }
  }

  private loadBook(): void {
    if (!this.bookId) return;
    this.loading.set(true);
    this.adminService.getBook(this.bookId).subscribe({
      next: (book) => {
        this.form.patchValue({
          publishYear: book.publishYear,
          pages: book.pages,
          coverImageUrl: book.coverImageUrl,
          translations: book.translations
        });

        const authorRequests = (book.authorIds as number[] || []).map(id => this.authorService.getById(id));
        const categoryRequest = book.categoryId ? this.categoryService.getById(book.categoryId) : of(null);

        forkJoin({
          authors: authorRequests.length > 0 ? forkJoin(authorRequests) : of([]),
          category: categoryRequest
        }).subscribe(results => {
          this.selectedAuthors.set(results.authors);
          this.selectedCategory.set(results.category);
          this.form.patchValue({
            categoryId: results.category?.id,
            authorIds: results.authors.map(a => a.id)
          });
          this.loading.set(false);
        });
      },
      error: (err) => {
        this.snackCommon.showError(err);
        this.loading.set(false);
      }
    });
  }

  onCategorySearch(query: string): void {
    this.categorySearch.search(query);
  }

  onCategorySelected(cat: Category): void {
    this.selectedCategory.set(cat);
    this.form.patchValue({categoryId: cat.id});
  }

  clearCategory(): void {
    this.selectedCategory.set(null);
    this.form.patchValue({categoryId: null});
    this.categorySearch.clear();
  }

  openAuthorDialog(): void {
    const dialogRef = this.dialog.open(AuthorSelectionDialogComponent, {
      data: {selectedAuthors: this.selectedAuthors()},
      width: '500px'
    });

    dialogRef.afterClosed().subscribe((result: Author[] | undefined) => {
      if (result) {
        this.selectedAuthors.set(result);
        this.form.patchValue({authorIds: result.map(a => a.id)});
      }
    });
  }

  removeAuthor(authorId: number): void {
    this.selectedAuthors.update(list => list.filter(a => a.id !== authorId));
    this.form.patchValue({authorIds: this.selectedAuthors().map(a => a.id)});
  }

  save(): void {
    if (this.form.invalid) return;
    const dto: AdminBookDto = this.form.value;
    const obs = this.bookId
      ? this.adminService.updateBook(this.bookId, dto)
      : this.adminService.createBook(dto);

    obs.subscribe({
      next: () => {
        this.snackCommon.showSuccess(this.translocoService.translate('common.success.saved'));
        this.router.navigate(['/admin']);
      },
      error: (err) => this.snackCommon.showError(err)
    });
  }

  delete(): void {
    if (!this.bookId) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        message: this.translocoService.translate('common.confirmDelete'),
        confirmLabel: 'common.delete'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.adminService.deleteBook(this.bookId!).subscribe({
          next: () => {
            this.snackCommon.showSuccess(this.translocoService.translate('common.success.deleted'));
            this.router.navigate(['/admin']);
          },
          error: (err) => this.snackCommon.showError(err)
        });
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin']);
  }

}
