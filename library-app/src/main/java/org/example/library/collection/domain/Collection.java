package org.example.library.collection.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.library.collection_book.domain.CollectionBook;
import org.example.library.user.domain.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "collections")
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder(toBuilder = true)
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "collections_seq")
    @SequenceGenerator(name = "collections_seq", sequenceName = "collections_seq", allocationSize = 20)
    @Column(name = "collection_id")
    private Integer id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Collection parent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "collection", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @Builder.Default
    private List<CollectionBook> collectionBooks = new ArrayList<>();

    @OneToMany(mappedBy = "parent")
    @Builder.Default
    private List<Collection> children = new ArrayList<>();

    public void addChildrenCollection(Collection subCollection) {
        children.add(subCollection);
        subCollection.setParent(this);
    }

    public void removeChildrenCollection(Collection subCollection) {
        children.remove(subCollection);
        subCollection.setParent(null);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Collection collection)) return false;
        return Objects.equals(id, collection.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
