import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';
import { ReadingGoalService } from '../../services/reading-goal.service';
import { ReadingGoal } from '../../interfaces/reading-goal';
import { ConfirmationDialogComponent } from '../../dialogs/confirmation-dialog/confirmation-dialog.component';
import { filter } from 'rxjs';

export interface SetGoalDialogData {
  goal: ReadingGoal | null;
  year: number;
}

@Component({
  selector: 'app-set-goal-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    TranslocoDirective
  ],
  templateUrl: './set-goal-dialog.component.html',
  styleUrl: './set-goal-dialog.component.scss'
})
export class SetGoalDialogComponent {

  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<SetGoalDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SetGoalDialogData,
    private goalService: ReadingGoalService,
    private dialog: MatDialog,
    private translocoService: TranslocoService
  ) {
    this.form = this.fb.group({
      targetBooks: [data.goal?.targetBooks || 0, [Validators.required, Validators.min(0)]],
      targetPages: [data.goal?.targetPages || 0, [Validators.min(0)]]
    });
  }

  save(): void {
    if (this.form.valid) {
      const goal: ReadingGoal = {
        ...this.data.goal,
        year: this.data.year,
        targetBooks: this.form.value.targetBooks,
        targetPages: this.form.value.targetPages
      };

      this.goalService.saveOrUpdateGoal(goal).subscribe(() => {
        this.dialogRef.close('saved');
      });
    }
  }

  delete(): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        message: this.translocoService.translate('dashboard.deleteConfirm', { year: this.data.year }),
        confirmLabel: 'common.delete'
      }
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe(() => {
      this.goalService.deleteGoal(this.data.year).subscribe(() => {
        this.dialogRef.close('deleted');
      });
    });
  }
  
}
