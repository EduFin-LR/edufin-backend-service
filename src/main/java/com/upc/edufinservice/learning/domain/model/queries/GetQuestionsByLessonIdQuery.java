package com.upc.edufinservice.learning.domain.model.queries;

import java.util.UUID;

public record GetQuestionsByLessonIdQuery(UUID lessonId) {
    public GetQuestionsByLessonIdQuery {
        if (lessonId == null) throw new IllegalArgumentException("El ID de la lección no puede ser nulo");
    }
}
