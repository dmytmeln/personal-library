import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogModule, MatDialogRef} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {Quote, QuoteRequest} from '../../interfaces/quote';
import {CommonModule} from '@angular/common';
import {TranslocoDirective} from '@jsverse/transloco';
import {QuoteService} from '../../services/quote.service';

export interface QuoteFormDialogData {
  libraryBookId: number;
  quote?: Quote;
}

@Component({
  selector: 'app-quote-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    TranslocoDirective,
  ],
  templateUrl: './quote-form-dialog.component.html',
  styleUrl: './quote-form-dialog.component.scss'
})
export class QuoteFormDialogComponent implements OnInit {
  form: FormGroup;
  isEditMode: boolean;
  saving = false;

  constructor(
    private fb: FormBuilder,
    private quoteService: QuoteService,
    public dialogRef: MatDialogRef<QuoteFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: QuoteFormDialogData,
  ) {
    this.isEditMode = !!data.quote;
    this.form = this.fb.group({
      text: [data.quote?.text || '', [Validators.required]],
      page: [data.quote?.page || ''],
      comment: [data.quote?.comment || ''],
    });
  }

  ngOnInit(): void {}

  onSave(): void {
    if (this.form.invalid) return;
    
    this.saving = true;
    const request: QuoteRequest = this.form.value;

    if (this.isEditMode && this.data.quote) {
      this.quoteService.update(this.data.quote.id, request).subscribe({
        next: (result) => this.dialogRef.close(result),
        error: () => this.saving = false
      });
    } else {
      this.quoteService.create(this.data.libraryBookId, request).subscribe({
        next: (result) => this.dialogRef.close(result),
        error: () => this.saving = false
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
