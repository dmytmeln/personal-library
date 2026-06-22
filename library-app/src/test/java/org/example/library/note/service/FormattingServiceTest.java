package org.example.library.note.service;

import org.example.library.author.domain.Author;
import org.example.library.author.domain.AuthorTranslation;
import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookTranslation;
import org.example.library.library_book.domain.LibraryBook;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Uses real Gemini API. Enable manually and provide GEMINI_API_KEY environment variable.")
class FormattingServiceTest {

    @Test
    void formatTranscript_shouldReturnFormattedNote() throws IOException {
        var apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new AssertionError("GEMINI_API_KEY environment variable must be set to run this test");
        }
        var service = new FormattingService(
                apiKey,
                "gemini-2.5-flash",
                0.7);
        var author = Author.builder()
                .translations(Map.of("en", AuthorTranslation.builder()
                        .languageCode("en")
                        .fullName("George Orwell")
                        .build()))
                .build();
        var book = Book.builder()
                .authors(Set.of(author))
                .translations(Map.of("en", BookTranslation.builder()
                        .languageCode("en")
                        .title("1984")
                        .description("A dystopian social science fiction novel and cautionary tale.")
                        .build()))
                .build();
        var libraryBook = LibraryBook.builder()
                .book(book)
                .build();
        var rawTranscript = Files.readString(Path.of("src/test/resources/data/transcription/transcript.txt")).trim();

        var result = service.formatTranscript(rawTranscript, libraryBook);

        System.out.println(result);
        assertThat(result).isNotBlank();
        assertThat(result).isNotEqualTo(rawTranscript);
        assertThat(result).containsIgnoringCase("Winston");
        assertThat(result).containsIgnoringCase("Big Brother");
        assertThat(result).doesNotContainIgnoringCase("um");
    }

}
