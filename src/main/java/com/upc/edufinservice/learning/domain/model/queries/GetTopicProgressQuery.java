package com.upc.edufinservice.learning.domain.model.queries;

import java.util.UUID;

public record GetTopicProgressQuery(UUID userId, UUID topicId) {}
