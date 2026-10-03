package com.upc.edufinservice.assessment.application.internal.services;

import com.upc.edufinservice.learning.domain.model.aggregates.Question;

/**
 * Representa una pregunta seleccionada como refuerzo adaptativo.
 *
 * skillId y mastery permiten conservar por qué fue elegida la pregunta.
 */
public record ReinforcementQuestionSelection(
        Question question,
        Integer skillId,
        Double mastery
) {}
