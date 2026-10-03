package com.upc.edufinservice.assessment.domain.model.commands;

import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.analytics.domain.model.entities.SelectionReason;

import java.util.UUID;

public record SubmitQuestionAttemptCommand(
        UUID userId,
        UUID questionId,
        UUID selectedOptionId,
        String selectedMatchCategory,
        Float timeTakenSec,
        InteractionType interactionType,
        SelectionReason selectionReason
) {
    public SubmitQuestionAttemptCommand {
        if (userId == null) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio");
        }

        if (questionId == null) {
            throw new IllegalArgumentException("El ID de la pregunta es obligatorio");
        }
    }
}
