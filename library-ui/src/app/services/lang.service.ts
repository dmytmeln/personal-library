import {Injectable} from '@angular/core';
import {TranslocoService} from '@jsverse/transloco';

@Injectable({
  providedIn: 'root'
})
export class LangService {

  constructor(private transloco: TranslocoService) {}

  setLang(lang: string) {
    localStorage.setItem('lang', lang);
    this.transloco.setActiveLang(lang);
  }

  loadSaved() {
    const saved = localStorage.getItem('lang') || 'en';
    this.transloco.setActiveLang(saved);
  }

}
