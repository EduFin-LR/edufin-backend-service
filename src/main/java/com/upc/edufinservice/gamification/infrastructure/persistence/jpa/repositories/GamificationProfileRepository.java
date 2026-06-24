package com.upc.edufinservice.gamification.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.gamification.domain.model.aggregates.GamificationProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GamificationProfileRepository extends JpaRepository<GamificationProfile, UUID> {
    Optional<GamificationProfile> findByUserId(UUID userid);

    boolean existByUserId(UUID userId);
}
