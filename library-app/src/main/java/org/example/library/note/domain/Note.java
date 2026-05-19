package org.example.library.note.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.library.library_book.domain.LibraryBook;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notes_seq")
    @SequenceGenerator(name = "notes_seq", sequenceName = "notes_seq", allocationSize = 20)
    @Column(name = "note_id")
    private Integer id;

    @Column(name = "content", nullable = true, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, length = 20)
    @Builder.Default
    private NoteType noteType = NoteType.TEXT;

    @Column(name = "raw_transcript", nullable = true, columnDefinition = "text")
    private String rawTranscript;

    @Column(name = "transcription_model", nullable = true)
    private String transcriptionModel;

    @Column(name = "formatting_model", nullable = true)
    private String formattingModel;

    @Column(name = "voice_created_at", nullable = true)
    private LocalDateTime voiceCreatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_book_id", nullable = false, unique = true)
    private LibraryBook libraryBook;

    public enum NoteType {
        TEXT, VOICE
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Note note)) return false;
        return Objects.equals(id, note.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
