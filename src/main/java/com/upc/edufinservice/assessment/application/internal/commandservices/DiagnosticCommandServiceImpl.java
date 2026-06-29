package com.upc.edufinservice.assessment.application.internal.commandservices;

import com.upc.edufinservice.assessment.domain.model.aggregates.DiagnosticResult;
import com.upc.edufinservice.assessment.domain.model.aggregates.QuestionAttempt;
import com.upc.edufinservice.assessment.domain.model.commands.EvaluateDiagnosticCommand;
import com.upc.edufinservice.assessment.domain.model.commands.DiagnosticResponse;
import com.upc.edufinservice.assessment.domain.model.events.DiagnosticCompletedEvent;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.DiagnosticResultRepository;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.QuestionAttemptRepository;
import com.upc.edufinservice.learning.domain.model.queries.GetOptionsByQuestionIdQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiagnosticCommandServiceImpl {

    private final DiagnosticResultRepository diagnosticResultRepository;
    private final QuestionAttemptRepository questionAttemptRepository;
    private final LearningQueryService learningQueryService;
    private final ApplicationEventPublisher eventPublisher;

    public DiagnosticCommandServiceImpl(DiagnosticResultRepository diagnosticResultRepository,
                                        QuestionAttemptRepository questionAttemptRepository,
                                        LearningQueryService learningQueryService,
                                        ApplicationEventPublisher eventPublisher) {
        this.diagnosticResultRepository = diagnosticResultRepository;
        this.questionAttemptRepository = questionAttemptRepository;
        this.learningQueryService = learningQueryService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public DiagnosticResponse evaluateDiagnostic(EvaluateDiagnosticCommand command) {
        int correctAnswers = 0;
        int totalQuestions = command.answers().size();

        if (totalQuestions == 0) {
            return new DiagnosticResponse(0, 0, 0, 0.0f, "Sin Nivel", "No respondiste ninguna pregunta.");
        }

        // 1. Evaluar cada respuesta de forma híbrida (Múltiple y Arrastre)
        for (var answer : command.answers()) {
            boolean isCorrect = false;

            // Jalamos las opciones correctas y categorías mapeadas desde el catálogo de Learning
            var realOptions = learningQueryService.handle(new GetOptionsByQuestionIdQuery(answer.questionId()));

            for (var option : realOptions) {
                if (option.getId().equals(answer.selectedOptionId())) {
                    // CASO A: Es una pregunta de Drag & Drop (evalúa por coincidencia de texto en la caja destino)
                    if (option.getMatchCategory() != null) {
                        if (answer.selectedMatchCategory() != null &&
                                option.getMatchCategory().trim().equalsIgnoreCase(answer.selectedMatchCategory().trim())) {
                            isCorrect = true;
                            correctAnswers++;
                        }
                    }
                    // CASO B: Es una pregunta clásica de Opción Múltiple
                    else {
                        if (Boolean.TRUE.equals(option.getIsCorrect())) {
                            isCorrect = true;
                            correctAnswers++;
                        }
                    }
                    break;
                }
            }

            // 2. Guardar el intento unitario en la base de datos
            var attempt = new QuestionAttempt(
                    command.userId(),
                    answer.questionId(),
                    answer.selectedOptionId(),
                    answer.selectedMatchCategory(), // Registramos la caja si la hubo
                    isCorrect,
                    answer.timeTakenSec() != null ? answer.timeTakenSec() : 0.0f
            );
            questionAttemptRepository.save(attempt);
        }

        // 3. Calcular métricas finales
        int incorrectAnswers = totalQuestions - correctAnswers;
        float score = ((float) correctAnswers / totalQuestions) * 100;

        // 4. Determinar dinámicamente el Nivel y Mensaje Gamificado
        String financialLevel;
        String feedbackMessage;

        if (score >= 80.0f) {
            financialLevel = "Nivel Avanzado / Finanzas Pro";
            feedbackMessage = "¡Espectacular! Tienes una excelente salud financiera. Estás listo para perfeccionar tus habilidades y optimizar tus recursos al máximo.";
        } else if (score >= 50.0f) {
            financialLevel = "Nivel Estable / Intermedio";
            feedbackMessage = "¡Tienes un nivel estable en finanzas, excelente! Conoces lo básico, pero estás listo para hackear el sistema y dominar las tarjetas de crédito.";
        } else {
            financialLevel = "Nivel Inicial / Básico";
            feedbackMessage = "¡Estás empezando tu viaje! Es un gran momento para aprender a cuidar tu bolsillo y ganarle a los destructivos gastos hormiga.";
        }

        // 5. Guardar el resultado consolidado del diagnóstico del alumno
        var result = new DiagnosticResult(command.userId(), score);
        diagnosticResultRepository.save(result);

        //¡Nuevo!: Publicamos el evento cruzado hacia gamificación
        eventPublisher.publishEvent(new DiagnosticCompletedEvent(command.userId(), score));

        // Retornamos el objeto enriquecido para que el controlador lo mande limpio a React
        return new DiagnosticResponse(
                totalQuestions,
                correctAnswers,
                incorrectAnswers,
                score,
                financialLevel,
                feedbackMessage
        );
    }
}