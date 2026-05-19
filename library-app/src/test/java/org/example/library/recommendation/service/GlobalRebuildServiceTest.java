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
class GlobalRebuildServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BatchEmbeddingProcessor batchEmbeddingProcessor;

    private GlobalRebuildService globalRebuildService;


    @BeforeEach
    void setUp() {
        globalRebuildService = new GlobalRebuildService(bookRepository, batchEmbeddingProcessor);
        globalRebuildService.setBatchPageRequest(2);
    }

    @Test
    void shouldExecuteFullRebuildSuccessfully() {
        var book1 = new Book();
        var book2 = new Book();
        var book3 = new Book();
        var batch1 = List.of(book1, book2);
        var batch2 = List.of(book3);

        when(bookRepository.countBooksWithoutEmbedding()).thenReturn(3L);
        when(bookRepository.findBooksWithoutEmbedding(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(batch1))
                .thenReturn(new PageImpl<>(batch2));

        globalRebuildService.executeFullRebuild();

        verify(batchEmbeddingProcessor, times(1)).processBatch(batch1);
        verify(batchEmbeddingProcessor, times(1)).processBatch(batch2);
        verify(bookRepository).countBooksWithoutEmbedding();
    }

    @Test
    void shouldDoNothingWhenNoBooksNeedEmbedding() {
        when(bookRepository.countBooksWithoutEmbedding()).thenReturn(0L);

        globalRebuildService.executeFullRebuild();

        verify(bookRepository, never()).findBooksWithoutEmbedding(any());
        verify(batchEmbeddingProcessor, never()).processBatch(any());
    }

}
