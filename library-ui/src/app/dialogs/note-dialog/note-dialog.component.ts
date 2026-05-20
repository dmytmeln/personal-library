import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {FormControl, ReactiveFormsModule} from '@angular/forms';
import {NoteService} from '../../services/note.service';
import {Note} from '../../interfaces/note';
import {CommonModule} from '@angular/common';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {TranslocoDirective, TranslocoService} from '@jsverse/transloco';
import {ConfirmationDialogComponent} from '../confirmation-dialog/confirmation-dialog.component';
import {filter} from 'rxjs';
import {AudioRecorderService} from '../../services/audio-recorder.service';
import {MatIconModule} from '@angular/material/icon';
import {MatProgressBarModule} from '@angular/material/progress-bar';

export interface NoteDialogData {
  libraryBookId: number;
  bookTitle: string;
}

@Component({
  selector: 'app-note-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    MatProgressSpinnerModule,
    TranslocoDirective,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './note-dialog.component.html',
  styleUrl: './note-dialog.component.scss'
})
export class NoteDialogComponent implements OnInit {
  noteControl = new FormControl('');
  note: Note | null = null;
  loading = false;
  saving = false;
  
  isProcessing = false;
  rawTranscript = '';
  showTranscript = false;

  constructor(
    public dialogRef: MatDialogRef<NoteDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: NoteDialogData,
    private noteService: NoteService,
    private dialog: MatDialog,
    private translocoService: TranslocoService,
    public audioRecorder: AudioRecorderService,
  ) {
  }

  ngOnInit(): void {
    this.loadNote();
  }

  private loadNote(): void {
    this.loading = true;
    this.noteService.getByLibraryBookId(this.data.libraryBookId).subscribe({
      next: (note) => {
        this.note = note;
        this.noteControl.setValue(note.content);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  async startRecording(): Promise<void> {
    if (!this.audioRecorder.canRecord()) {
      alert('Microphone not available');
      return;
    }
    
    try {
      await this.audioRecorder.startRecording();
    } catch (error) {
      alert('Failed to start recording. Please allow microphone access.');
    }
  }

  async stopAndUpload(): Promise<void> {
    const duration = this.audioRecorder.duration();
    const audioBlob = await this.audioRecorder.stopRecording();
    
    if (!audioBlob) {
      return;
    }

    if (duration < this.audioRecorder.MIN_DURATION) {
      alert('Recording too short (minimum 1 second)');
      return;
    }

    this.isProcessing = true;
    this.noteService.uploadVoiceNote(this.data.libraryBookId, audioBlob).subscribe({
      next: (response) => {
        this.rawTranscript = response.rawTranscript;
        this.noteControl.setValue(response.formattedNote);
        this.showTranscript = true;
        this.isProcessing = false;
      },
      error: () => {
        alert('Failed to process voice note');
        this.isProcessing = false;
      }
    });
  }

  onSave(): void {
    const content = this.noteControl.value?.trim() || '';

    if (content === '' && !this.note) {
      this.dialogRef.close();
      return;
    }

    if (content === '' && this.note) {
      this.onDelete();
      return;
    }

    this.saving = true;
    this.noteService.upsert({
      libraryBookId: this.data.libraryBookId,
      content: content
    }).subscribe({
      next: () => this.dialogRef.close('saved'),
      error: () => this.saving = false
    });
  }

  onDelete(): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        message: this.translocoService.translate('common.confirmDelete'),
        confirmLabel: 'common.delete'
      }
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe(() => {
      this.saving = true;
      this.noteService.delete(this.data.libraryBookId).subscribe({
        next: () => this.dialogRef.close('deleted'),
        error: () => this.saving = false
      });
    });
  }

  onClear(): void {
    this.noteControl.setValue('');
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
