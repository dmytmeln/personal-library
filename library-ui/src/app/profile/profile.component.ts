import {Component, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatListModule} from '@angular/material/list';
import {MatDividerModule} from '@angular/material/divider';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router} from '@angular/router';
import {AuthService} from '../services/auth.service';
import {UserResponse} from '../interfaces/user-response';
import {TranslocoDirective, TranslocoService} from '@jsverse/transloco';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatSnackCommon} from '../common/mat-snack-common';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    TranslocoDirective,
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {

  user = signal<UserResponse | null>(null);
  isEditing = signal<boolean>(false);
  saving = signal<boolean>(false);

  profileForm = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email]
    }),
    fullName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(2)]
    })
  });

  private snackCommon: MatSnackCommon;

  constructor(
    private authService: AuthService,
    private router: Router,
    private translocoService: TranslocoService,
    matSnackBar: MatSnackBar,
  ) {
    this.snackCommon = new MatSnackCommon(matSnackBar);
  }

  ngOnInit(): void {
    this.user.set(this.authService.currentUser());
  }

  toggleEdit(): void {
    const user = this.user();
    if (user) {
      this.profileForm.patchValue({
        email: user.email,
        fullName: user.fullName
      });
      this.isEditing.set(true);
    }
  }

  cancelEdit(): void {
    this.isEditing.set(false);
    this.profileForm.reset();
  }

  saveProfile(): void {
    if (this.profileForm.invalid || this.saving()) return;

    this.saving.set(true);
    const request = this.profileForm.getRawValue();

    this.authService.updateProfile(request).subscribe({
      next: (updatedUser) => {
        this.user.set(updatedUser);
        this.isEditing.set(false);
        this.saving.set(false);
        this.snackCommon.showSuccess(this.translocoService.translate('profile.updateSuccess'));
      },
      error: (error) => {
        this.snackCommon.showError(error);
        this.saving.set(false);
      }
    });
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.snackCommon.showSuccess(this.translocoService.translate('common.success.logout'));
        this.router.navigate(['/login']);
      },
      error: (error) => {
        this.snackCommon.showError(error);
        this.router.navigate(['/login']);
      }
    });
  }

}
