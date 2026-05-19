package org.example.library.quote.repository;

import org.example.library.quote.domain.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Integer> {

    List<Quote> findByLibraryBookIdAndLibraryBookUserIdOrderByCreatedAtDesc(Integer libraryBookId, Integer userId);

    Optional<Quote> findByIdAndLibraryBookUserId(Integer quoteId, Integer userId);

}
