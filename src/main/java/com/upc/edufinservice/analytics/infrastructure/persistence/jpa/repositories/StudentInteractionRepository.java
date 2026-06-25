package com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.analytics.domain.model.entities.StudentInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentInteractionRepository extends JpaRepository<StudentInteraction, UUID> {
    // Trae el historial de un usuario ordenado por fecha ascendente
    List<StudentInteraction> findByUserIdOrderByInteractedAtAsc(UUID userId);
}