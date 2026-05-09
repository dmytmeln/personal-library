import {Component, OnInit} from '@angular/core';
import {RouterOutlet} from '@angular/router';
import {MenuComponent} from './menu/menu.component';
import {LangService} from './services/lang.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, MenuComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {

  constructor(private langService: LangService) {
  }

  ngOnInit() {
    this.langService.loadSaved();
  }

}
