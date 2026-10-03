package com.upc.edufinservice.assessment.application.internal.services;

import java.util.List;

public record AdaptiveQuizResult(
        boolean adaptive,
        int standardQuestionCount,
        int reinforcementQuestionCount,
        List<AdaptiveQuizQuestionSelection> questions
) {}
