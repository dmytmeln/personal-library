package org.example.library.recommendation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "genre_mapping")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GenreMapping {

    @Id
    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "vector_index", nullable = false, unique = true)
    private Integer vectorIndex;

}
