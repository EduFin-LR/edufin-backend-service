package com.upc.edufinservice.learning.domain.model.queries;

import java.util.UUID;

public record GetLessonsByTopicIdQuery(UUID topicId) {
    public GetLessonsByTopicIdQuery {
        if (topicId == null) throw new IllegalArgumentException("El ID del tema no puede ser nulo");
    }
}
