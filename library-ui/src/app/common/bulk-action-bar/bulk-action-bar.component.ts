import {Component, input, output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatTooltip} from '@angular/material/tooltip';
import {TranslocoModule} from '@jsverse/transloco';

@Component({
  selector: 'app-bulk-action-bar',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatToolbarModule,
    MatTooltip,
    TranslocoModule,
  ],
  templateUrl: './bulk-action-bar.component.html',
  styleUrl: './bulk-action-bar.component.scss'
})
export class BulkActionBarComponent {
  selectedCount = input.required<number>();
  clear = output<void>();
}
