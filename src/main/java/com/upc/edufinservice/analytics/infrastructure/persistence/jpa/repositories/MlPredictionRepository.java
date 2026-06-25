package com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.analytics.domain.model.aggregates.MlPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MlPredictionRepository extends JpaRepository<MlPrediction, UUID> {
    // Para buscar la predicción más reciente de un usuario en un tema específico
    Optional<MlPrediction> findByUserIdAndTopicId(UUID userId, UUID topicId);
}