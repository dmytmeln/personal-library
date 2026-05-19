package org.example.library.note.service;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.library.author.domain.Author;
import org.example.library.author.domain.AuthorTranslation;
import org.example.library.book.domain.Book;
import org.example.library.common.exception.FormattingException;
import org.example.library.library_book.domain.LibraryBook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FormattingService {

    private final GoogleAiGeminiChatModel chatModel;
    @Getter
    private final String model;

    public FormattingService(
            @Value("${application.ai.gemini.api-key}") String apiKey,
            @Value("${application.ai.gemini.model}") String model,
            @Value("${application.ai.gemini.temperature}") double temperature) {
        this.chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temperature)
                .build();
        this.model = model;
    }

    public String formatTranscript(String rawTranscript, LibraryBook libraryBook) {
        var book = libraryBook.getBook();
        var translation = book.getDefaultTranslation();

        var title = Optional.ofNullable(libraryBook.getTitle())
                .orElse(translation.getTitle());
        var description = Optional.ofNullable(libraryBook.getDescription())
                .orElse(translation.getDescription());
        var authors = Optional.ofNullable(libraryBook.getCustomAuthorName())
                .orElseGet(() -> getBookAuthorNames(book));

        var prompt = buildPrompt(rawTranscript, title, authors, description);

        log.info("Formatting transcript with Gemini, book: {}, transcript length: {} characters",
                title, rawTranscript.length());

        var response = chatModel.chat(prompt);

        if (response == null || response.isBlank()) {
            throw new FormattingException("Formatting returned empty result");
        }

        log.info("Formatting completed, length: {} characters", response.length());
        return response;
    }

    private String buildPrompt(String rawTranscript, String bookTitle, String authorName, String bookDescription) {
        return String.format("""
                        You are an intelligent editor. Your job is to clean up voice transcriptions.
                        
                        Rules:
                        - Preserve the original meaning
                        - Remove filler words (um, uh, like, you know)
                        - Fix grammar and punctuation
                        - Structure text logically
                        - Do NOT invent information
                        - Do NOT summarize
                        - Do NOT add your own thoughts
                        
                        Book context: "%s" by %s. %s
                        
                        Transcription: %s
                        
                        Return only the polished note, no explanations.
                        """,
                bookTitle,
                authorName,
                bookDescription != null ? bookDescription : "",
                rawTranscript
        );
    }

    private String getBookAuthorNames(Book book) {
        return book.getAuthors().stream().map(Author::getDefaultTranslation).map(AuthorTranslation::getFullName).collect(Collectors.joining(", "));
    }

}
