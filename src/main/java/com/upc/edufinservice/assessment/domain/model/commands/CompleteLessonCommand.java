package com.upc.edufinservice.assessment.domain.model.commands;

import java.util.List;
import java.util.UUID;

public record CompleteLessonCommand(
        UUID userId,
        UUID lessonId,
        Integer timeSpentSec,
        List<UUID> questionIds
) {
    public CompleteLessonCommand {
        if (userId == null || lessonId == null) {
            throw new IllegalArgumentException("Usuario y Lección son parámetros obligatorios");
        }
        if (questionIds == null || questionIds.isEmpty()) {
            throw new IllegalArgumentException("questionIds no puede estar vacío");
        }
    }
}
