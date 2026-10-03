package com.upc.edufinservice.assessment.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

public record DynamicFinalResource(
        UUID topicId,
        boolean adaptive,
        int standardQuestionCount,
        int adaptiveQuestionCount,
        List<DynamicFinalQuestionResource> questions
) {}
