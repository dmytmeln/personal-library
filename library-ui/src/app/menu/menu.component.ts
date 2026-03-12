import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatToolbar} from '@angular/material/toolbar';
import {MatAnchor, MatButton, MatIconButton} from '@angular/material/button';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import {AuthService} from '../services/auth.service';
import {LangService} from '../services/lang.service';
import {MatMenu, MatMenuItem, MatMenuTrigger} from '@angular/material/menu';
import {MatIcon} from '@angular/material/icon';
import {TranslocoDirective} from '@jsverse/transloco';

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

  constructor(
    public authService: AuthService,
    private langService: LangService,
    private router: Router,
  ) {
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  changeLang(lang: string): void {
    this.langService.setLang(lang);
  }

}
