package com.upc.edufinservice.learning.domain.model.queries;

import java.util.UUID;

public record GetTopicByIdQuery(
        UUID topicId
) {
}
