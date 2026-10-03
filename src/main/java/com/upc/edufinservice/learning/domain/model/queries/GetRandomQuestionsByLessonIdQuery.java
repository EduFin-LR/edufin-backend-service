package com.upc.edufinservice.learning.domain.model.queries;

import java.util.UUID;

/**
 * Solicita una cantidad limitada de preguntas aleatorias
 * pertenecientes a una lección concreta.
 */
public record GetRandomQuestionsByLessonIdQuery(
        UUID lessonId,
        int limit
) {
    public GetRandomQuestionsByLessonIdQuery {
        if (lessonId == null) {
            throw new IllegalArgumentException("lessonId es obligatorio");
        }

        if (limit <= 0) {
            throw new IllegalArgumentException("limit debe ser mayor que 0");
        }
    }
}
