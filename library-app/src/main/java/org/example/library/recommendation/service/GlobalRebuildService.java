package org.example.library.recommendation.service;

import lombok.extern.slf4j.Slf4j;
import org.example.library.book.repository.BookRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GlobalRebuildService {

    private final BookRepository bookRepository;
    private final BatchEmbeddingProcessor batchEmbeddingProcessor;

    private PageRequest batchPageRequest;

    @Value("${recommendations.rebuild.batch-size:100}")
    public void setBatchPageRequest(int batchSize) {
        this.batchPageRequest = PageRequest.of(0, batchSize);
    }

    public GlobalRebuildService(BookRepository bookRepository,
                                BatchEmbeddingProcessor batchEmbeddingProcessor) {
        this.bookRepository = bookRepository;
        this.batchEmbeddingProcessor = batchEmbeddingProcessor;
    }


    public void executeFullRebuild() {
        log.info("Starting embedding rebuild for all books missing embeddings...");

        long totalToUpdate = bookRepository.countBooksWithoutEmbedding();
        int iterations = calculateIterations(totalToUpdate);
        log.info("Found {} books that need embeddings. Iterations required: {}", totalToUpdate, iterations);

        for (int i = 0; i < iterations; i++) {
            var booksWithoutEmbedding = bookRepository.findBooksWithoutEmbedding(batchPageRequest)
                    .getContent();

            if (booksWithoutEmbedding.isEmpty()) {
                break;
            }

            batchEmbeddingProcessor.processBatch(booksWithoutEmbedding);
            log.info("Processed batch: {} books", booksWithoutEmbedding.size());
        }

        log.info("Embedding rebuild completed.");
    }

    private int calculateIterations(double totalToUpdate) {
        return (int) Math.ceil(totalToUpdate / batchPageRequest.getPageSize());
    }

}
