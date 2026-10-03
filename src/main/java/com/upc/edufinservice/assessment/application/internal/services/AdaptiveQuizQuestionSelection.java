package com.upc.edufinservice.assessment.application.internal.services;

import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.analytics.domain.model.entities.SelectionReason;
import com.upc.edufinservice.learning.domain.model.aggregates.Question;

public record AdaptiveQuizQuestionSelection(
        Question question,
        InteractionType interactionType,
        SelectionReason selectionReason
) {}
