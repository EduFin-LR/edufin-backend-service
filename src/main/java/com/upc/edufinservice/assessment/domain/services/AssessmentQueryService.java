package com.upc.edufinservice.assessment.domain.services;

import java.util.List;
import java.util.UUID;

public interface AssessmentQueryService {
    int getCompletedLessonsCount(UUID userId, List<UUID> lessonIds);

    String getLessonStatus(UUID userId, UUID lessonId, boolean isFirstLessonOfApp);

    boolean hasCompletedDiagnostic(UUID userId);
}
