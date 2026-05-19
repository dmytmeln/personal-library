package org.example.library.note.mapper;

import org.example.library.common.exception.MappingException;
import org.example.library.note.domain.Note;
import org.example.library.note.dto.NoteDto;
import org.example.library.note.dto.NoteRequest;
import org.example.library.note.dto.VoiceNoteResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface NoteMapper {

    NoteDto toDto(Note note);

    @Mapping(target = "noteId", source = "id")
    @Mapping(target = "formattedNote", source = "content")
    VoiceNoteResponse toVoiceNoteResponse(Note note);

    @BeforeMapping
    default void validateVoiceNote(Note note, @TargetType Class<?> targetType) {
        if (targetType.equals(VoiceNoteResponse.class) && note.getNoteType() != Note.NoteType.VOICE) {
            throw new MappingException("Cannot map non-VOICE note to VoiceNoteResponse");
        }
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "libraryBook", ignore = true)
    @Mapping(target = "noteType", ignore = true)
    @Mapping(target = "rawTranscript", ignore = true)
    @Mapping(target = "transcriptionModel", ignore = true)
    @Mapping(target = "formattingModel", ignore = true)
    @Mapping(target = "voiceCreatedAt", ignore = true)
    Note toEntity(NoteRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "libraryBook", ignore = true)
    @Mapping(target = "noteType", ignore = true)
    @Mapping(target = "rawTranscript", ignore = true)
    @Mapping(target = "transcriptionModel", ignore = true)
    @Mapping(target = "formattingModel", ignore = true)
    @Mapping(target = "voiceCreatedAt", ignore = true)
    void update(@MappingTarget Note note, NoteRequest request);
}
