package org.example.library.recommendation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vocabulary_metadata")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VocabularyMetadata {

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion;

    @Column(name = "last_rebuild_at")
    private LocalDateTime lastRebuildAt;

}
