package com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories;

import com.upc.edufinservice.assessment.domain.model.aggregates.UserLessonProgress;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.ProgressStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository("assessmentUserLessonProgressRepository")
public interface UserLessonProgressRepository extends JpaRepository<UserLessonProgress, UUID> {

    // Busca si el alumno ya tiene un registro de progreso para una lección específica
    Optional<UserLessonProgress> findByUserIdAndLessonId(UUID userId, UUID lessonId);

    // NUEVO: Cuenta cuántas lecciones de una lista específica ya completó el alumno
    int countByUserIdAndLessonIdInAndStatus(UUID userId, List<UUID> lessonIds, ProgressStatus status);
}
