package org.example.library.recommendation.service;

import org.example.library.book.domain.Book;
import org.example.library.book.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookEmbeddingBackfillServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BatchEmbeddingProcessor batchEmbeddingProcessor;

    private BookEmbeddingBackfillService bookEmbeddingBackfillService;


    @BeforeEach
    void setUp() {
        bookEmbeddingBackfillService = new BookEmbeddingBackfillService(bookRepository, batchEmbeddingProcessor);
        bookEmbeddingBackfillService.setBatchPageRequest(2);
    }

    @Test
    void shouldBackfillEmbeddingsSuccessfully() {
        when(bookRepository.countBooksWithoutEmbedding()).thenReturn(3L);
        when(batchEmbeddingProcessor.processBatch(any(PageRequest.class))).thenReturn(2).thenReturn(1);

        bookEmbeddingBackfillService.backfillEmbeddings();

        verify(batchEmbeddingProcessor, times(2)).processBatch(any(PageRequest.class));
        verify(bookRepository).countBooksWithoutEmbedding();
    }

    @Test
    void shouldDoNothingWhenNoBooksNeedEmbedding() {
        when(bookRepository.countBooksWithoutEmbedding()).thenReturn(0L);

        bookEmbeddingBackfillService.backfillEmbeddings();

        verify(bookRepository, never()).findBooksWithoutEmbedding(any());
        verify(batchEmbeddingProcessor, never()).processBatch(any());
    }

}
