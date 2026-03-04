package org.example.library.library_book.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.library.author.domain.Author;
import org.example.library.category.domain.Category;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "library_books_view")
@Immutable
@Getter
public class LibraryBookView {

    @Id
    @Column(name = "library_book_id")
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "book_id")
    private Integer bookId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private LibraryBookStatus status;

    @Column(name = "added_at")
    private LocalDateTime addedAt;

    @Column(name = "rating")
    private Byte rating;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "publish_year")
    private Short publishYear;

    @Column(name = "pages")
    private Short pages;

    @Column(name = "language_code")
    private String languageCode;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "category_id")
    private Integer categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;

    @Column(name = "book_language")
    private String bookLanguage;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "custom_author_name")
    private String customAuthorName;

    @Column(name = "owner_user_id")
    private Integer ownerUserId;

    @ManyToMany
    @JoinTable(
            name = "book_authors",
            joinColumns = @JoinColumn(name = "book_id", referencedColumnName = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private Set<Author> authors;

}
