package org.example.library.recommendation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "tf_idf_vocabulary")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TfIdfVocabulary {

    @Id
    @Column(name = "word", length = 100, nullable = false)
    private String word;

    @Column(name = "vector_index", nullable = false, unique = true)
    private Integer vectorIndex;

    @Column(name = "idf_score", nullable = false)
    private Double idfScore;

    @Column(name = "version", nullable = false)
    private Integer version;

}
