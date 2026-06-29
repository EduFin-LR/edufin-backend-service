package com.upc.edufinservice.assessment.domain.model.commands;

import java.util.UUID;

public record CompleteLessonCommand(
        UUID userId,
        UUID lessonId,
        Integer timeSpentSec
) {
    public CompleteLessonCommand {
        if (userId == null || lessonId == null) {
            throw new IllegalArgumentException("Usuario y Lección son parámetros obligatorios");
        }
    }
}