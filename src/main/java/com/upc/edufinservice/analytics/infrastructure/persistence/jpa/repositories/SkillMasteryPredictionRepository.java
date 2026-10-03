package com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.analytics.domain.model.entities.SkillMasteryPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillMasteryPredictionRepository
        extends JpaRepository<SkillMasteryPrediction, UUID> {

    Optional<SkillMasteryPrediction> findByUserIdAndSkillId(
            UUID userId,
            Integer skillId
    );

    List<SkillMasteryPrediction> findByUserId(UUID userId);
}
