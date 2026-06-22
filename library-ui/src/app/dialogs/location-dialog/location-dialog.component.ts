import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogModule, MatDialogRef} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {FormControl, ReactiveFormsModule, Validators} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {TranslocoDirective} from '@jsverse/transloco';
import {LibraryBookService} from '../../services/library-book.service';
import {MatIconModule} from '@angular/material/icon';

@Component({
  selector: 'app-location-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    TranslocoDirective,
    MatIconModule
  ],
  templateUrl: './location-dialog.component.html',
  styleUrl: './location-dialog.component.scss'
})
export class LocationDialogComponent implements OnInit {

  locationControl = new FormControl('', [Validators.maxLength(255)]);
  saving = false;

  constructor(
    public dialogRef: MatDialogRef<LocationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { libraryBookId: number, location: string | null },
    private libraryBookService: LibraryBookService
  ) {
  }

  ngOnInit(): void {
    if (this.data.location) {
      this.locationControl.setValue(this.data.location);
    }
  }

  onSave(): void {
    if (this.locationControl.invalid || this.saving) return;

    this.saving = true;
    const location = this.locationControl.value;

    this.libraryBookService.updateLocation(this.data.libraryBookId, location).subscribe({
      next: (updatedBook) => this.dialogRef.close(updatedBook),
      error: () => this.saving = false
    });
  }

  onClear(): void {
    this.locationControl.setValue('');
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
