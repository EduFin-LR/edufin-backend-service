package com.upc.edufinservice.learning.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

public record TopicLessonsResponse(
        UUID topicId,
        String topicName,
        String category,
        List<LessonResource> lessons
) {
}
