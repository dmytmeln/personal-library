package org.example.library.recommendation.service;

import lombok.RequiredArgsConstructor;
import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookStatus;
import org.example.library.book.repository.BookRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BatchEmbeddingProcessor {

    private final BookRepository bookRepository;
    private final EmbeddingService embeddingService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processBatch(List<Book> books) {
        var embeddings = embeddingService.generateEmbeddings(books);

        for (int i = 0; i < books.size(); i++) {
            var book = books.get(i);
            book.setEmbedding(embeddings.get(i));
            book.setStatus(BookStatus.SYNCED);
        }

        bookRepository.saveAll(books);
    }

}
