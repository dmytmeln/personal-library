import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatToolbar} from '@angular/material/toolbar';
import {MatAnchor, MatButton, MatIconButton} from '@angular/material/button';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import {AuthService} from '../services/auth.service';
import {LangService} from '../services/lang.service';
import {MatMenu, MatMenuItem, MatMenuTrigger} from '@angular/material/menu';
import {MatIcon} from '@angular/material/icon';
import {TranslocoDirective, TranslocoService} from '@jsverse/transloco';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatSnackCommon} from '../common/mat-snack-common';

@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbar,
    RouterLink,
    MatAnchor,
    RouterLinkActive,
    MatButton,
    MatMenu,
    MatMenuItem,
    MatMenuTrigger,
    MatIcon,
    MatIconButton,
    TranslocoDirective
  ],
  templateUrl: './menu.component.html',
  styleUrl: './menu.component.scss'
})
export class MenuComponent {

  private snackCommon: MatSnackCommon;

  constructor(
    public authService: AuthService,
    private langService: LangService,
    private router: Router,
    private translocoService: TranslocoService,
    matSnackBar: MatSnackBar,
  ) {
    this.snackCommon = new MatSnackCommon(matSnackBar);
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

  changeLang(lang: string): void {
    this.langService.setLang(lang);
  }

}
