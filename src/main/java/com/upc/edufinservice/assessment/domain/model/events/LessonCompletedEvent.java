package com.upc.edufinservice.assessment.domain.model.events;

import java.util.UUID;

public record LessonCompletedEvent(
        UUID userId,
        UUID lessonId,
        Float score,
        Integer attempts
) {}