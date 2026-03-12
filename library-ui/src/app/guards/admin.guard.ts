import {Injectable} from '@angular/core';
import {CanActivate, Router, UrlTree} from '@angular/router';
import {AuthService} from '../services/auth.service';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatSnackCommon} from '../common/mat-snack-common';
import {TranslocoService} from '@jsverse/transloco';

@Injectable({
  providedIn: 'root'
})
export class AdminGuard implements CanActivate {

  private matSnackCommon: MatSnackCommon;

  constructor(
    private authService: AuthService,
    private router: Router,
    matSnackBar: MatSnackBar,
    private translocoService: TranslocoService
  ) {
    this.matSnackCommon = new MatSnackCommon(matSnackBar);
  }

  canActivate(): boolean | UrlTree {
    if (!this.authService.isAuthenticated()) {
      this.matSnackCommon.showError(this.translocoService.translate('auth.login.required'));
      return this.router.createUrlTree(['/login']);
    }

    if (this.authService.isUser()) {
      this.matSnackCommon.showError(this.translocoService.translate('auth.admin.required'));
      return this.router.createUrlTree(['/']);
    }

    return this.authService.isAdmin();
  }

}
