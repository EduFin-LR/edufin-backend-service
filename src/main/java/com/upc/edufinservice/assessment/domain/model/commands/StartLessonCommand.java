package com.upc.edufinservice.assessment.domain.model.commands;

import java.util.UUID;

public record StartLessonCommand(UUID userId, UUID lessonId) {
    public StartLessonCommand {
        if (userId == null) throw new IllegalArgumentException("El ID del usuario es obligatorio");
        if (lessonId == null) throw new IllegalArgumentException("El ID de la lección es obligatorio");
    }
}