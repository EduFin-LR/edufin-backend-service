package com.upc.edufinservice.assessment.interfaces.rest.resources;

import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.analytics.domain.model.entities.SelectionReason;
import com.upc.edufinservice.learning.interfaces.rest.resources.QuestionResource;

public record AdaptiveQuizQuestionResource(
        QuestionResource question,
        InteractionType interactionType,
        SelectionReason selectionReason
) {}
