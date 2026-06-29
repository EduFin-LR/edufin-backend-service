package com.upc.edufinservice.assessment.domain.model.commands;

public record DiagnosticResponse(
        int totalQuestions,
        int correctAnswers,
        int incorrectAnswers,
        Float score,
        String financialLevel,
        String feedbackMessage
) {
}
