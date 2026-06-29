package com.upc.edufinservice.assessment.domain.model.commands;

public record LessonCompletionResponse(
        int totalQuestions,
        int correctAnswers,
        int incorrectAnswers,
        Float score
) {
}
