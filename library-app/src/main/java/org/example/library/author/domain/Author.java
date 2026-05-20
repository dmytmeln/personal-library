package org.example.library.author.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.library.book.domain.Book;
import org.example.library.book.domain.Book_;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "authors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "authors_seq")
    @SequenceGenerator(name = "authors_seq", sequenceName = "authors_seq", allocationSize = 20, initialValue = 20)
    @Column(name = "author_id")
    private Integer id;

    @Column(name = "birth_year", nullable = false)
    private Short birthYear;

    @Column(name = "death_year")
    private Short deathYear;

    @Column(name = "popularity_count", nullable = false)
    private Integer popularityCount;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @MapKey(name = "languageCode")
    @Builder.Default
    private Map<String, AuthorTranslation> translations = new HashMap<>();

    @ManyToMany(mappedBy = Book_.AUTHORS)
    private List<Book> books;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Author author)) return false;
        return Objects.equals(id, author.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public AuthorTranslation getDefaultTranslation() {
        if (translations == null) {
            throw new NullPointerException("Author translations must not be null");
        }
        return translations.get("en");
    }

}
