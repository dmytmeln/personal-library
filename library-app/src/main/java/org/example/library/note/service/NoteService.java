package org.example.library.note.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.library.common.exception.NotFoundException;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.note.domain.Note;
import org.example.library.note.dto.NoteDto;
import org.example.library.note.dto.NoteRequest;
import org.example.library.note.dto.VoiceNoteResponse;
import org.example.library.note.mapper.NoteMapper;
import org.example.library.note.repository.NoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteService {

    private final NoteRepository repository;
    private final NoteMapper mapper;
    private final LibraryBookRepository libraryBookRepository;
    private final TranscriptionService transcriptionService;
    private final FormattingService formattingService;


    @Transactional(readOnly = true)
    public NoteDto getByLibraryBookId(Integer libraryBookId, Integer userId) {
        return repository.findByLibraryBookIdAndLibraryBookUserId(libraryBookId, userId)
                .map(mapper::toDto)
                .orElseThrow(() -> new NotFoundException("error.note.not_found"));
    }

    @Transactional
    public NoteDto createOrUpdate(NoteRequest request, Integer userId) {
        return repository.findByLibraryBookIdAndLibraryBookUserId(request.libraryBookId(), userId)
                .map(existingNote -> updateExisting(existingNote, request))
                .orElseGet(() -> createNew(request, userId));
    }

    @Transactional
    public void delete(Integer libraryBookId, Integer userId) {
        repository.deleteByLibraryBookIdAndLibraryBookUserId(libraryBookId, userId);
        log.info("[NOTE_DELETE] User ID: {}, Library Book ID: {}", userId, libraryBookId);
    }

    @Transactional
    public VoiceNoteResponse uploadVoiceNote(Integer libraryBookId, MultipartFile audioFile, Integer userId) {
        var libraryBook = libraryBookRepository.findByIdAndUserId(libraryBookId, userId)
                .orElseThrow(() -> new NotFoundException("error.library_book.not_found"));

        var audioBytes = getAudioBytes(audioFile);

        var rawTranscript = transcriptionService.transcribeAudio(audioBytes);
        log.debug("Raw Transcript: {}", rawTranscript);
        var formattedNote = formattingService.formatTranscript(rawTranscript, libraryBook);
        log.debug("Formatted Note: {}", formattedNote);

        var note = repository.findByLibraryBookIdAndLibraryBookUserId(libraryBookId, userId)
                .orElse(Note.builder()
                        .libraryBook(libraryBook)
                        .build());
        note.setNoteType(Note.NoteType.VOICE);
        note.setRawTranscript(rawTranscript);
        note.setContent(formattedNote);
        note.setTranscriptionModel(transcriptionService.getModel());
        note.setFormattingModel(formattingService.getModel());
        note.setVoiceCreatedAt(LocalDateTime.now());

        var savedNote = repository.saveAndFlush(note);

        log.info("[VOICE_NOTE_CREATE] User ID: {}, Library Book ID: {}, Transcript length: {}, Formatted length: {}",
                userId, libraryBookId, rawTranscript.length(), formattedNote.length());

        return mapper.toVoiceNoteResponse(savedNote);
    }

    private NoteDto createNew(NoteRequest request, Integer userId) {
        var libraryBook = libraryBookRepository.findByIdAndUserId(request.libraryBookId(), userId)
                .orElseThrow(() -> new NotFoundException("error.library_book.not_found"));
        var note = mapper.toEntity(request);
        note.setLibraryBook(libraryBook);
        var savedNote = repository.saveAndFlush(note);
        log.info("[NOTE_CREATE] User ID: {}, Library Book ID: {}", userId, request.libraryBookId());
        return mapper.toDto(savedNote);
    }

    private NoteDto updateExisting(Note existingNote, NoteRequest request) {
        mapper.update(existingNote, request);
        var savedNote = repository.saveAndFlush(existingNote);
        log.info("[NOTE_UPDATE] User ID: {}, Library Book ID: {}", existingNote.getLibraryBook().getUser().getId(), existingNote.getLibraryBook().getId());
        return mapper.toDto(savedNote);
    }

    private byte[] getAudioBytes(MultipartFile audioFile) {
        try {
            return audioFile.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Error reading audio file", e);
        }
    }

}
