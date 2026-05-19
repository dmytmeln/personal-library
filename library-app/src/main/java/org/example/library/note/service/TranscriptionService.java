package org.example.library.note.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.library.common.exception.TranscriptionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class TranscriptionService {

    private final WebClient webClient;
    @Getter
    private final String model;

    public TranscriptionService(
            @Value("${application.ai.groq.api-key}") String apiKey,
            @Value("${application.ai.groq.base-url}") String baseUrl,
            @Value("${application.ai.groq.model}") String model) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.model = model;
    }

    public String transcribeAudio(byte[] audioBytes) {
        var language = LocaleContextHolder.getLocale().getLanguage();

        log.info("Transcribing audio with Groq Whisper, language: {}, size: {} bytes", language, audioBytes.length);

        var builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return "audio.webm";
            }
        });
        builder.part("model", model);
        builder.part("language", language);

        var response = webClient.post()
                .uri("/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(TranscriptionResponse.class)
                .block();

        if (response == null || response.text() == null || response.text().isBlank()) {
            throw new TranscriptionException("Transcription returned empty result");
        }

        log.info("Transcription completed, length: {} characters", response.text().length());

        return response.text().trim();
    }

    private record TranscriptionResponse(@JsonProperty("text") String text) {
    }

}

