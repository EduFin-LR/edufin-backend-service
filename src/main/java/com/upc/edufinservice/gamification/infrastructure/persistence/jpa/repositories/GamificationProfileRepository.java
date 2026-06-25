package com.upc.edufinservice.gamification.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.gamification.domain.model.aggregates.GamificationProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GamificationProfileRepository extends JpaRepository<GamificationProfile, UUID> {
    Optional<GamificationProfile> findByUserId(UUID userid);

    boolean existsByUserId(UUID userId);

    // Trae a los jugadores con más puntos en orden descendente
    @Query("SELECT u FROM GamificationProfile u ORDER BY u.totalPoints DESC")
    List<GamificationProfile> getTopPlayers(Pageable pageable);
}
