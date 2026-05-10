package org.example.library.recommendation.repository;

import org.example.library.recommendation.domain.UserProfileVector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileVectorRepository extends JpaRepository<UserProfileVector, Integer> {
}
