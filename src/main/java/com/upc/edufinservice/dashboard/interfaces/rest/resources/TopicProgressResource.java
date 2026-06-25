package com.upc.edufinservice.dashboard.interfaces.rest.resources;

import java.util.UUID;

public record TopicProgressResource(
        UUID topicId,
        String topicName,
        Integer completedLessons,
        Integer totalLessons,
        Integer progressPercentage,
        String status, // "IN_PROGRESS", "PENDING", "COMPLETED"
        Boolean isAiRecommended // ¡El toque de la tesis!
) {}