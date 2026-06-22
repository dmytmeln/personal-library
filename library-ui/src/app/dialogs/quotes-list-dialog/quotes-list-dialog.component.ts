import {Component, Inject, OnInit, signal} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {QuoteService} from '../../services/quote.service';
import {Quote} from '../../interfaces/quote';
import {CommonModule} from '@angular/common';
import {TranslocoDirective, TranslocoService} from '@jsverse/transloco';
import {MatIconModule} from '@angular/material/icon';
import {QuoteFormDialogComponent} from '../quote-form-dialog/quote-form-dialog.component';
import {ConfirmationDialogComponent} from '../confirmation-dialog/confirmation-dialog.component';
import {filter} from 'rxjs';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatTooltipModule} from '@angular/material/tooltip';

export interface QuotesListDialogData {
  libraryBookId: number;
  bookTitle: string;
}

@Component({
  selector: 'app-quotes-list-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    TranslocoDirective,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './quotes-list-dialog.component.html',
  styleUrl: './quotes-list-dialog.component.scss'
})
export class QuotesListDialogComponent implements OnInit {
  quotes = signal<Quote[]>([]);
  loading = signal(false);

  constructor(
    public dialogRef: MatDialogRef<QuotesListDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: QuotesListDialogData,
    private quoteService: QuoteService,
    private dialog: MatDialog,
    private translocoService: TranslocoService,
  ) {}

  ngOnInit(): void {
    this.loadQuotes();
  }

  loadQuotes(): void {
    this.loading.set(true);
    this.quoteService.getByLibraryBookId(this.data.libraryBookId).subscribe({
      next: (quotes) => {
        this.quotes.set(quotes);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  openAddQuoteDialog(): void {
    const dialogRef = this.dialog.open(QuoteFormDialogComponent, {
      data: { libraryBookId: this.data.libraryBookId },
      width: '500px'
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe(() => {
      this.loadQuotes();
    });
  }

  editQuote(quote: Quote): void {
    const dialogRef = this.dialog.open(QuoteFormDialogComponent, {
      data: { libraryBookId: this.data.libraryBookId, quote },
      width: '500px'
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe(() => {
      this.loadQuotes();
    });
  }

  deleteQuote(quote: Quote): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        message: this.translocoService.translate('dialogs.quotes.deleteConfirm'),
        confirmLabel: 'common.delete'
      }
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe(() => {
      this.quoteService.delete(quote.id).subscribe(() => {
        this.loadQuotes();
      });
    });
  }
}
