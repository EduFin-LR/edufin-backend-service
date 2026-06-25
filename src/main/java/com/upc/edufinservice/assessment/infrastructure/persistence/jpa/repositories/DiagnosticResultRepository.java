package com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.assessment.domain.model.aggregates.DiagnosticResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiagnosticResultRepository extends JpaRepository<DiagnosticResult, UUID> {

    // Útil para saber si un alumno ya dio el examen inicial y no mostrárselo de nuevo
    Optional<DiagnosticResult> findByUserId(UUID userId);
}