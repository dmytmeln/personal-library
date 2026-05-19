import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AdminService } from '../../services/admin.service';
import { AdminCategoryDto } from '../../interfaces/admin-category-dto';
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
  selector: 'app-admin-category-details',
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
  templateUrl: './admin-category-details.component.html',
  styleUrl: './admin-category-details.component.scss'
})
export class AdminCategoryDetailsComponent implements OnInit {

  categoryId: number | null = null;
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
      translations: this.fb.group({
        en: this.fb.group({
          name: ['', Validators.required],
          description: ['']
        }),
        uk: this.fb.group({
          name: ['', Validators.required],
          description: ['']
        })
      })
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.categoryId = Number(id);
      this.loadCategory();
    }
  }

  private loadCategory(): void {
    if (!this.categoryId) return;
    this.loading.set(true);
    this.adminService.getCategory(this.categoryId).subscribe({
      next: (cat) => {
        this.form.patchValue({
          translations: cat.translations
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
    const dto: AdminCategoryDto = this.form.value;
    const obs = this.categoryId 
      ? this.adminService.updateCategory(this.categoryId, dto)
      : this.adminService.createCategory(dto);

    obs.subscribe({
      next: () => {
        this.snackCommon.showSuccess(this.translocoService.translate('common.success.saved'));
        this.router.navigate(['/admin']);
      },
      error: (err) => this.snackCommon.showError(err)
    });
  }

  delete(): void {
    if (!this.categoryId) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        message: this.translocoService.translate('common.confirmDelete'),
        confirmLabel: 'common.delete'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.adminService.deleteCategory(this.categoryId!).subscribe({
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
