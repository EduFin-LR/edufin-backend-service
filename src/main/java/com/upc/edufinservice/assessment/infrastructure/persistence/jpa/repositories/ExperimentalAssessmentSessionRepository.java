package com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalAssessmentPhase;
import com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalAssessmentSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExperimentalAssessmentSessionRepository extends JpaRepository<ExperimentalAssessmentSession, UUID> {
    Optional<ExperimentalAssessmentSession> findByUserIdAndPhase(UUID userId, ExperimentalAssessmentPhase phase);
    boolean existsByUserIdAndPhase(UUID userId, ExperimentalAssessmentPhase phase);
}
