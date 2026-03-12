import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import {MatCardModule} from '@angular/material/card';
import {MatInputModule} from '@angular/material/input';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {AuthService} from '../services/auth.service';

import {TranslocoDirective, TranslocoPipe, TranslocoService} from '@jsverse/transloco';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatSnackCommon} from '../common/mat-snack-common';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatInputModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    RouterLink,
    TranslocoDirective,
    TranslocoPipe,
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {

  registerForm: FormGroup;
  hidePassword: boolean = true;
  private snackCommon: MatSnackCommon;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private translocoService: TranslocoService,
    matSnackBar: MatSnackBar,
  ) {
    this.registerForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      fullName: ['', [Validators.required, Validators.minLength(2)]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
    this.snackCommon = new MatSnackCommon(matSnackBar);
  }

  onSubmit(): void {
    if (!this.registerForm.valid) {
      return;
    }

    this.authService.register(this.registerForm.value).subscribe({
      next: () => this.router.navigate(['/login']),
      error: (error) => {
        this.snackCommon.showError(error.error?.message || this.translocoService.translate('auth.register.error'));
        console.error('Registration error:', error);
      }
    });
  }

}
