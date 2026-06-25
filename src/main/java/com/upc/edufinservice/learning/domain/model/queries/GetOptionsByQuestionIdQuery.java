package com.upc.edufinservice.learning.domain.model.queries;

import java.util.UUID;

public record GetOptionsByQuestionIdQuery(UUID questionId) {
    public GetOptionsByQuestionIdQuery {
        if (questionId == null) throw new IllegalArgumentException("El ID de la pregunta no puede ser nulo");
    }
}