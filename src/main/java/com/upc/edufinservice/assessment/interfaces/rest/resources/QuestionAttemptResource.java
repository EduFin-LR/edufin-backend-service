package com.upc.edufinservice.assessment.interfaces.rest.resources;

import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.analytics.domain.model.entities.SelectionReason;

import java.time.LocalDateTime;
import java.util.UUID;

public record QuestionAttemptResource(
        UUID id,
        UUID userId,
        UUID questionId,
        UUID selectedOptionId,
        String selectedMatchCategory,
        Boolean isCorrect,
        Float timeTakenSec,
        InteractionType interactionType,
        SelectionReason selectionReason,
        LocalDateTime attemptedAt
) {}
