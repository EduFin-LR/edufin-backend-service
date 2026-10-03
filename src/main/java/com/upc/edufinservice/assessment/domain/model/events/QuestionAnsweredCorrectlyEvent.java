package com.upc.edufinservice.assessment.domain.model.events;

import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.analytics.domain.model.entities.SelectionReason;

import java.util.UUID;

public record QuestionAnsweredCorrectlyEvent(
        UUID userId,
        UUID questionId,
        InteractionType interactionType,
        SelectionReason selectionReason
) {}
