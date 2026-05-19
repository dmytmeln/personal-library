package org.example.library.quote.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.library.library_book.domain.LibraryBook;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "quotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "quotes_seq")
    @SequenceGenerator(name = "quotes_seq", sequenceName = "quotes_seq", allocationSize = 20)
    @Column(name = "quote_id")
    private Integer id;

    @Column(name = "text", nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "page", length = 50)
    private String page;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_book_id", nullable = false)
    private LibraryBook libraryBook;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Quote quote)) return false;
        return Objects.equals(id, quote.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
