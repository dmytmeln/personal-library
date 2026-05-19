package org.example.library.note.dto;

import java.time.LocalDateTime;

public record VoiceNoteResponse(
    Integer noteId,
    String rawTranscript,
    String formattedNote,
    LocalDateTime createdAt
) {}
