package com.upc.edufinservice.assessment.domain.model.events;

import java.util.UUID;

public record FinalCompletedEvent(
        UUID userId,
        UUID topicId,
        Float score,
        Boolean passed
) {}
