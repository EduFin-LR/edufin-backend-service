package com.upc.edufinservice.assessment.interfaces.rest.resources;

public record FinalCompletionResponse(
        int totalQuestions,
        int correctAnswers,
        int incorrectAnswers,
        float score,
        boolean passed,
        int finalExperience,
        boolean nextTopicUnlocked
) {}
