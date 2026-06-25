package com.upc.edufinservice.assessment.application.internal.commandservices;

import com.upc.edufinservice.assessment.domain.model.aggregates.DiagnosticResult;
import com.upc.edufinservice.assessment.domain.model.aggregates.QuestionAttempt;
import com.upc.edufinservice.assessment.domain.model.commands.EvaluateDiagnosticCommand;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.DiagnosticResultRepository;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.QuestionAttemptRepository;
import com.upc.edufinservice.assessment.interfaces.rest.resources.SubmitDiagnosticResource;
import com.upc.edufinservice.learning.domain.model.queries.GetOptionsByQuestionIdQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiagnosticCommandServiceImpl {

    private final DiagnosticResultRepository diagnosticResultRepository;
    private final QuestionAttemptRepository questionAttemptRepository;
    private final LearningQueryService learningQueryService;

    public DiagnosticCommandServiceImpl(DiagnosticResultRepository diagnosticResultRepository,
                                        QuestionAttemptRepository questionAttemptRepository,
                                        LearningQueryService learningQueryService) {
        this.diagnosticResultRepository = diagnosticResultRepository;
        this.questionAttemptRepository = questionAttemptRepository;
        this.learningQueryService = learningQueryService;
    }

    @Transactional
    public Float evaluateDiagnostic(EvaluateDiagnosticCommand command) {
        int correctAnswers = 0;
        int totalQuestions = command.answers().size();

        if (totalQuestions == 0) return 0.0f;

        // 1. Evaluar cada respuesta
        for (var answer : command.answers()) {
            boolean isCorrect = false;

            // Le pedimos a Learning las opciones reales de esta pregunta para verificar
            var realOptions = learningQueryService.handle(new GetOptionsByQuestionIdQuery(answer.questionId()));

            for (var option : realOptions) {
                if (option.getId().equals(answer.selectedOptionId()) && option.getIsCorrect()) {
                    isCorrect = true;
                    correctAnswers++;
                    break;
                }
            }

            // 2. Guardar el intento para el modelo DKT (Analytics)
            var attempt = new QuestionAttempt(
                    command.userId(),
                    answer.questionId(),
                    answer.selectedOptionId(),
                    isCorrect,
                    answer.timeTakenSec() != null ? answer.timeTakenSec() : 0.0f
            );
            questionAttemptRepository.save(attempt);
        }

        // 3. Calcular la nota sobre 100
        float score = ((float) correctAnswers / totalQuestions) * 100;

        // 4. Guardar el resultado final del diagnóstico
        var result = new DiagnosticResult(command.userId(), score);
        diagnosticResultRepository.save(result);

        return score;
    }
}