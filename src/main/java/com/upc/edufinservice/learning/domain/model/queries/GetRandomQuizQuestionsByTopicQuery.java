package com.upc.edufinservice.learning.domain.model.queries;

import java.util.UUID;

/**
 * Solicita preguntas evaluativas QUIZ de un Topic/módulo.
 * Se usa como banco base para construir el FINAL dinámico.
 */
public record GetRandomQuizQuestionsByTopicQuery(
        UUID topicId,
        int limit
) {
    public GetRandomQuizQuestionsByTopicQuery {
        if (topicId == null) {
            throw new IllegalArgumentException("topicId es obligatorio");
        }

        if (limit <= 0) {
            throw new IllegalArgumentException("limit debe ser mayor que 0");
        }
    }
}
