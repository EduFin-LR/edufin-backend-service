package com.upc.edufinservice.assessment.domain.services;

import java.util.List;
import java.util.UUID;

public interface AssessmentQueryService {
    int getCompletedLessonsCount(UUID userId, List<UUID> lessonIds);

    String getLessonStatus(UUID userId, UUID lessonId, boolean isFirstLessonOfApp);

    /**
     * Devuelve las estrellas obtenidas en una lección evaluativa a partir
     * del mejor score persistido. Si todavía no hubo intentos, devuelve 0.
     */
    int getLessonStars(UUID userId, UUID lessonId);

    boolean hasCompletedDiagnostic(UUID userId);

    boolean hasPassedTopicFinal(UUID userId, UUID topicId);
}
