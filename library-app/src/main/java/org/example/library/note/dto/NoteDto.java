package org.example.library.note.dto;

import org.example.library.note.domain.Note;

import java.time.LocalDateTime;

public record NoteDto(
    Integer id,
    String content,
    Note.NoteType noteType,
    String rawTranscript,
    String transcriptionModel,
    String formattingModel,
    LocalDateTime voiceCreatedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
