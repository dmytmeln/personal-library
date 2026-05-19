package org.example.library.note.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Uses real Groq API. Enable manually and provide GROQ_API_KEY environment variable.")
class TranscriptionServiceTest {

    @BeforeAll
    static void beforeAll() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @Test
    void transcribeAudio_shouldReturnTranscription() throws IOException {
        var apiKey = System.getenv("GROQ_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new AssertionError("GROQ_API_KEY environment variable must be set to run this test");
        }
        var service = new TranscriptionService(
                apiKey,
                "https://api.groq.com/openai/v1",
                "whisper-large-v3-turbo");
        var audioPath = Path.of("src/test/resources/data/transcription/speech.mp3");
        var audioBytes = Files.readAllBytes(audioPath);
        var expectedTranscript = Files.readString(Path.of("src/test/resources/data/transcription/transcript.txt")).trim();

        var result = service.transcribeAudio(audioBytes);

        assertThat(result).isNotBlank();
        assertThat(result).isEqualToIgnoringWhitespace(expectedTranscript);
    }

}
