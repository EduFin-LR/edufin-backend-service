package com.upc.edufinservice.gamification.domain.model.events;

import java.util.UUID;

public record LessonCompletedEvent(
        UUID userId,
        UUID lessonId,
        Float score
) {
}
