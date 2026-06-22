package org.example.library.collection_book.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.library.collection.domain.Collection;
import org.example.library.library_book.domain.LibraryBook;
import org.example.library.library_book.domain.LibraryBookView;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "collection_books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionBook implements Persistable<CollectionBookId> {

    @EmbeddedId
    private CollectionBookId id;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("libraryBookId")
    @JoinColumn(name = "library_book_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private LibraryBook libraryBook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_book_id", insertable = false, updatable = false)
    private LibraryBookView libraryBookView;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("collectionId")
    @JoinColumn(name = "collection_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Collection collection;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CollectionBook collectionBook)) return false;
        return Objects.equals(id, collectionBook.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

}
