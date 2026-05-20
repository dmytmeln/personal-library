import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AdminService } from '../../services/admin.service';
import { AdminAuthorDto } from '../../interfaces/admin-author-dto';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSnackCommon } from '../../common/mat-snack-common';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../dialogs/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-admin-author-details',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    TranslocoDirective,
    MatProgressSpinnerModule,
    MatDialogModule,
  ],
  templateUrl: './admin-author-details.component.html',
  styleUrl: './admin-author-details.component.scss'
})
export class AdminAuthorDetailsComponent implements OnInit {

  authorId: number | null = null;
  form: FormGroup;
  languages = ['en', 'uk'];
  loading = signal(false);
  private snackCommon: MatSnackCommon;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private adminService: AdminService,
    private translocoService: TranslocoService,
    private dialog: MatDialog,
    matSnackBar: MatSnackBar
  ) {
    this.snackCommon = new MatSnackCommon(matSnackBar);
    this.form = this.fb.group({
      birthYear: [null, Validators.required],
      deathYear: [null],
      translations: this.fb.group({
        en: this.fb.group({
          fullName: ['', Validators.required],
          country: ['', Validators.required],
          biography: ['']
        }),
        uk: this.fb.group({
          fullName: ['', Validators.required],
          country: ['', Validators.required],
          biography: ['']
        })
      })
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.authorId = Number(id);
      this.loadAuthor();
    }
  }

  private loadAuthor(): void {
    if (!this.authorId) return;
    this.loading.set(true);
    this.adminService.getAuthor(this.authorId).subscribe({
      next: (author) => {
        this.form.patchValue({
          birthYear: author.birthYear,
          deathYear: author.deathYear,
          translations: author.translations
        });
        this.loading.set(false);
      },
      error: (err) => {
        this.snackCommon.showError(err);
        this.loading.set(false);
      }
    });
  }

  save(): void {
    if (this.form.invalid) return;
    const dto: AdminAuthorDto = this.form.value;
    const obs = this.authorId 
      ? this.adminService.updateAuthor(this.authorId, dto)
      : this.adminService.createAuthor(dto);

    obs.subscribe({
      next: () => {
        this.snackCommon.showSuccess(this.translocoService.translate('common.success.saved'));
        this.router.navigate(['/admin']);
      },
      error: (err) => this.snackCommon.showError(err)
    });
  }

  delete(): void {
    if (!this.authorId) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        message: this.translocoService.translate('common.confirmDelete'),
        confirmLabel: 'common.delete'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.adminService.deleteAuthor(this.authorId!).subscribe({
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
