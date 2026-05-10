package org.example.library.recommendation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profile_vectors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileVector {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "vector", nullable = false, columnDefinition = "vector(1100)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1100)
    private float[] vector;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

}
