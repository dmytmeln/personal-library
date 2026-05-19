package org.example.library.category.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.library.book.domain.Book;
import org.example.library.book.domain.Book_;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "categories")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "categories_seq")
    @SequenceGenerator(name = "categories_seq", sequenceName = "categories_seq", allocationSize = 20, initialValue = 11)
    @Column(name = "category_id")
    private Integer id;

    @Column(name = "popularity_count", nullable = false)
    private Integer popularityCount;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @MapKey(name = "languageCode")
    @Builder.Default
    private Map<String, CategoryTranslation> translations = new HashMap<>();

    @OneToMany(mappedBy = Book_.CATEGORY)
    private List<Book> books;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Category category)) return false;
        return Objects.equals(id, category.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public CategoryTranslation getDefaultTranslation() {
        if (translations == null) {
            throw new NullPointerException("Category translations must not be null");
        }
        return translations.get("en");
    }

}
