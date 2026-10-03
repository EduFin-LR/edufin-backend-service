package com.upc.edufinservice.assessment.application.internal.services;

import java.util.List;
import java.util.UUID;

public record DynamicFinalResult(
        UUID topicId,
        boolean adaptive,
        int standardQuestionCount,
        int adaptiveQuestionCount,
        List<DynamicFinalQuestionSelection> questions
) {}
