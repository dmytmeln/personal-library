package org.example.library.recommendation.service;

import lombok.extern.slf4j.Slf4j;
import org.example.library.book.repository.BookRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class BookEmbeddingBackfillService {

    private final BookRepository bookRepository;
    private final BatchEmbeddingProcessor batchEmbeddingProcessor;

    private PageRequest batchPageRequest;

    @Value("${recommendations.rebuild.batch-size:100}")
    public void setBatchPageRequest(int batchSize) {
        this.batchPageRequest = PageRequest.of(0, batchSize);
    }

    public BookEmbeddingBackfillService(BookRepository bookRepository,
                                        BatchEmbeddingProcessor batchEmbeddingProcessor) {
        this.bookRepository = bookRepository;
        this.batchEmbeddingProcessor = batchEmbeddingProcessor;
    }

    public void backfillEmbeddings() {
        log.info("Starting embedding rebuild for all books missing embeddings...");

        long totalToUpdate = bookRepository.countBooksWithoutEmbedding();
        int iterations = calculateIterations(totalToUpdate);
        log.info("Found {} books that need embeddings. Iterations required: {}", totalToUpdate, iterations);

        for (int i = 0; i < iterations; i++) {
            int processedCount = batchEmbeddingProcessor.processBatch(batchPageRequest);
            if (processedCount == 0) {
                break;
            }
            log.info("Processed batch: {} books", processedCount);
        }

        log.info("Embedding rebuild completed.");
    }

    private int calculateIterations(double totalToUpdate) {
        return (int) Math.ceil(totalToUpdate / batchPageRequest.getPageSize());
    }

}
