package com.upc.edufinservice.assessment.application.internal.services;

import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.analytics.domain.model.entities.SelectionReason;
import com.upc.edufinservice.learning.domain.model.aggregates.Question;

/**
 * Pregunta seleccionada para un FINAL dinámico.
 *
 * Todas las preguntas del FINAL se registran como InteractionType.FINAL.
 * selectionReason indica si fue base o añadida por bajo mastery.
 */
public record DynamicFinalQuestionSelection(
        Question question,
        InteractionType interactionType,
        SelectionReason selectionReason
) {}
