package org.example.library.book.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.library.author.domain.Author;
import org.example.library.category.domain.Category;
import org.example.library.user.domain.User;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "books")
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "books_seq")
    @SequenceGenerator(name = "books_seq", sequenceName = "books_seq", allocationSize = 20, initialValue = 56)
    @Column(name = "book_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "publish_year")
    private Short publishYear;

    @Column(name = "pages")
    private Short pages;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "embedding", columnDefinition = "vector(384)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 384)
    private float[] embedding;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BookStatus status = BookStatus.NEW;

    @Column(name = "popularity_count", nullable = false)
    private Integer popularityCount;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @MapKey(name = "languageCode")
    private Map<String, BookTranslation> translations;

    @ManyToMany
    @JoinTable(
            name = "book_authors",
            joinColumns = {@JoinColumn(name = "book_id", nullable = false)},
            inverseJoinColumns = {@JoinColumn(name = "author_id", nullable = false)}
    )
    private Set<Author> authors;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Book book)) return false;
        return Objects.equals(id, book.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public BookTranslation getDefaultTranslation() {
        return translations.get("en");
    }

}
