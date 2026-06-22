package org.example.library.recommendation.service;

import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookStatus;
import org.example.library.book.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchEmbeddingProcessorTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private BatchEmbeddingProcessor batchEmbeddingProcessor;

    @Test
    void shouldProcessBatchSuccessfully() {
        var book1 = new Book();
        var book2 = new Book();
        var books = List.of(book1, book2);
        float[] vector1 = new float[]{0.1f};
        float[] vector2 = new float[]{0.2f};
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        when(bookRepository.findBooksWithoutEmbedding(pageable)).thenReturn(new PageImpl<>(books));
        when(embeddingService.generateEmbeddings(books)).thenReturn(List.of(vector1, vector2));

        batchEmbeddingProcessor.processBatch(pageable);

        assertThat(book1.getEmbedding()).isEqualTo(vector1);
        assertThat(book1.getStatus()).isEqualTo(BookStatus.SYNCED);
        assertThat(book2.getEmbedding()).isEqualTo(vector2);
        assertThat(book2.getStatus()).isEqualTo(BookStatus.SYNCED);

        verify(bookRepository).saveAll(books);
    }

}
