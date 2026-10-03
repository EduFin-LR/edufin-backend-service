package com.upc.edufinservice.assessment.interfaces.rest.resources;

import java.util.List;

public record AdaptiveQuizResource(
        boolean adaptive,
        int standardQuestionCount,
        int reinforcementQuestionCount,
        List<AdaptiveQuizQuestionResource> questions
) {}
