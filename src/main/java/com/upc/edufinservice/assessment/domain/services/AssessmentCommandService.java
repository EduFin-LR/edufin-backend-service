package com.upc.edufinservice.assessment.domain.services;

import com.upc.edufinservice.assessment.domain.model.aggregates.QuestionAttempt;
import com.upc.edufinservice.assessment.domain.model.aggregates.UserLessonProgress;
import com.upc.edufinservice.assessment.domain.model.commands.CompleteLessonCommand;
import com.upc.edufinservice.assessment.domain.model.commands.LessonCompletionResponse;
import com.upc.edufinservice.assessment.domain.model.commands.StartLessonCommand;
import com.upc.edufinservice.assessment.domain.model.commands.SubmitQuestionAttemptCommand;

import java.util.Optional;

public interface AssessmentCommandService {
    // 1. Evalúa e introduce el intento de respuesta individual (MC / Drag and Drop)
    Optional<QuestionAttempt> handle(SubmitQuestionAttemptCommand command);

    // 2. Inicializa el estatus del alumno en un nivel como IN_PROGRESS
    UserLessonProgress handle(StartLessonCommand command);

    // 3. Cierra la lección, actualiza notas e inyecta el estado UNLOCKED a la siguiente lección
    LessonCompletionResponse handle(CompleteLessonCommand command);
}