package com.upc.edufinservice.assessment.domain.model.commands;

public record LessonCompletionResponse(
        int totalQuestions,
        int correctAnswers,
        int incorrectAnswers,
        int lessonExperience, // El bono por terminar (Math.round)
        int questionsExperience, // Lo que ganó por responder bien
        int totalExperience // La suma de todo
) {
}
