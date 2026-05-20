package org.example.library.note.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.library.note.dto.NoteDto;
import org.example.library.note.dto.NoteRequest;
import org.example.library.note.dto.VoiceNoteResponse;
import org.example.library.note.service.NoteService;
import org.example.library.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService service;


    @GetMapping
    public NoteDto getByLibraryBookId(@RequestParam Integer libraryBookId,
                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return service.getByLibraryBookId(libraryBookId, userPrincipal.getId());
    }

    @PutMapping
    public NoteDto createOrUpdate(@Valid @RequestBody NoteRequest request,
                                  @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return service.createOrUpdate(request, userPrincipal.getId());
    }

    @DeleteMapping
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(@RequestParam Integer libraryBookId,
                       @AuthenticationPrincipal UserPrincipal userPrincipal) {
        service.delete(libraryBookId, userPrincipal.getId());
    }

    @PostMapping("/{libraryBookId}/voice")
    public VoiceNoteResponse uploadVoiceNote(@PathVariable Integer libraryBookId,
                                             @RequestParam("audio") MultipartFile audioFile,
                                             @AuthenticationPrincipal UserPrincipal userPrincipal) throws IOException {
        return service.uploadVoiceNote(libraryBookId, audioFile, userPrincipal.getId());
    }

}
