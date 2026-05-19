export interface Note {
  id: number;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface NoteRequest {
  libraryBookId: number;
  content: string;
}

export interface VoiceNoteResponse {
  noteId: number;
  rawTranscript: string;
  formattedNote: string;
  createdAt: string;
}
