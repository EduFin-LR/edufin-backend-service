package com.upc.edufinservice.learning.domain.model.queries;

import java.util.UUID;

public record GetTopicByQuestionIdQuery(UUID questionId) {
    public GetTopicByQuestionIdQuery {
        if (questionId == null) throw new IllegalArgumentException("El ID de la pregunta no puede ser nulo");
    }
}