package com.upc.edufinservice.assessment.interfaces.rest.resources;

import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.analytics.domain.model.entities.SelectionReason;

import java.util.UUID;

public record SubmitQuestionAttemptResource(
        UUID questionId,
        UUID selectedOptionId,
        Float timeTakenSec,
        String selectedMatchCategory,

        /*
         * Contexto en que la pregunta fue presentada.
         *
         * Pueden venir null para flujos antiguos/no adaptativos.
         * El backend aplicará un fallback seguro.
         */
        InteractionType interactionType,
        SelectionReason selectionReason
) {}
