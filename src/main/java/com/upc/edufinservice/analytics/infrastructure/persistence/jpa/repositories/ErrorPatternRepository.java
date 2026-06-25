package com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.analytics.domain.model.aggregates.ErrorPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ErrorPatternRepository extends JpaRepository<ErrorPattern, UUID> {
    // Muy útil para buscar si ya existe un patrón de error y sumarle +1
    Optional<ErrorPattern> findByUserIdAndTopicId(UUID userId, UUID topicId);
}