package com.upc.edufinservice.analytics.application.internal.eventhandlers;

import com.upc.edufinservice.analytics.domain.model.aggregates.ErrorPattern;
import com.upc.edufinservice.analytics.domain.model.aggregates.MlPrediction;
import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.analytics.domain.model.entities.SelectionReason;
import com.upc.edufinservice.analytics.domain.model.entities.StudentInteraction;
import com.upc.edufinservice.analytics.infrastructure.external.fastapi.FastAPIClient;
import com.upc.edufinservice.analytics.infrastructure.external.fastapi.dto.SolicitudPrediccionDto;
import com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories.ErrorPatternRepository;
import com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories.MlPredictionRepository;
import com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories.StudentInteractionRepository;
import com.upc.edufinservice.assessment.domain.model.events.QuestionAnsweredCorrectlyEvent;
import com.upc.edufinservice.assessment.domain.model.events.QuestionAnsweredIncorrectlyEvent;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.LessonType;
import com.upc.edufinservice.learning.domain.model.queries.GetQuestionByIdQuery;
import com.upc.edufinservice.learning.domain.model.queries.GetTopicByQuestionIdQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AssessmentEventsHandler {

    private static final int MIN_DKT_INTERACTIONS = 8;

    private final LearningQueryService learningQueryService;
    private final ErrorPatternRepository errorPatternRepository;
    private final MlPredictionRepository mlPredictionRepository;
    private final StudentInteractionRepository interactionRepository;
    private final FastAPIClient fastApiClient;

    public AssessmentEventsHandler(
            LearningQueryService learningQueryService,
            ErrorPatternRepository errorPatternRepository,
            MlPredictionRepository mlPredictionRepository,
            StudentInteractionRepository interactionRepository,
            FastAPIClient fastApiClient
    ) {
        this.learningQueryService = learningQueryService;
        this.errorPatternRepository = errorPatternRepository;
        this.mlPredictionRepository = mlPredictionRepository;
        this.interactionRepository = interactionRepository;
        this.fastApiClient = fastApiClient;
    }

    @EventListener
    public void on(QuestionAnsweredCorrectlyEvent event) {
        processInteraction(event.userId(), event.questionId(), 1);
    }

    @EventListener
    public void on(QuestionAnsweredIncorrectlyEvent event) {

        var topic = learningQueryService.handle(
                new GetTopicByQuestionIdQuery(event.questionId())
        );

        var errorPattern = errorPatternRepository
                .findByUserIdAndTopicId(event.userId(), topic.getId())
                .orElseGet(() -> new ErrorPattern(
                        event.userId(),
                        topic.getId()
                ));

        errorPattern.incrementErrorCount();
        errorPatternRepository.save(errorPattern);

        processInteraction(event.userId(), event.questionId(), 0);
    }

    private void processInteraction(
            UUID userId,
            UUID questionId,
            Integer isCorrect
    ) {

        // 1. Recuperar la pregunta.
        var question = learningQueryService
                .handle(new GetQuestionByIdQuery(questionId))
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Pregunta no encontrada para tracking de IA"
                        )
                );

        // 2. Solo QUIZ y FINAL participan en DKT-Forget.
        LessonType lessonType = question.getLesson().getLessonType();

        if (lessonType != LessonType.QUIZ
                && lessonType != LessonType.FINAL) {

            System.out.println(
                    "[DKT-FORGET] Interacción ignorada. "
                            + "lessonType=" + lessonType
                            + ", questionId=" + questionId
            );

            return;
        }

        // 3. Skill estable 1..30.
        Integer activeSkillId = question.getSkill().getId();

        if (activeSkillId == null) {
            System.out.println(
                    "[DKT-FORGET] Pregunta sin Skill. questionId=" + questionId
            );
            return;
        }

        /*
         * 4. Tipo de interacción.
         *
         * REINFORCEMENT todavía se marcará explícitamente más adelante,
         * cuando el flujo adaptativo sepa que una pregunta fue añadida como refuerzo.
         */
        InteractionType interactionType =
                lessonType == LessonType.FINAL
                        ? InteractionType.FINAL
                        : InteractionType.QUIZ;

        /*
         * 5. Razón de selección.
         *
         * Por ahora todo se registra como STANDARD.
         * Cuando implementemos la selección adaptativa:
         *
         * - preguntas normales -> STANDARD
         * - preguntas añadidas por bajo mastery -> LOW_MASTERY
         */
        SelectionReason selectionReason = SelectionReason.STANDARD;

        // 6. Guardar interacción válida.
        var currentInteraction = new StudentInteraction(
                userId,
                activeSkillId,
                isCorrect,
                interactionType,
                selectionReason
        );

        interactionRepository.save(currentInteraction);

        // 7. Historial cronológico real del estudiante.
        var historial =
                interactionRepository.findByUserIdOrderByInteractedAtAsc(userId);

        // 8. Umbral mínimo antes de consultar el modelo.
        if (historial.size() < MIN_DKT_INTERACTIONS) {

            System.out.println(
                    "[DKT-FORGET] Usuario " + userId
                            + ": " + historial.size()
                            + "/" + MIN_DKT_INTERACTIONS
                            + " interacciones válidas. "
                            + "Todavía no se consulta el modelo."
            );

            return;
        }

        // 9. Adaptar historial al contrato FastAPI.
        var interactions = historial.stream()
                .map(interaction ->
                        new SolicitudPrediccionDto.InteraccionDto(
                                interaction.getDktSkillId(),
                                interaction.getIsCorrect() == 1,
                                interaction.getInteractedAt()
                        )
                )
                .toList();

        var payload = new SolicitudPrediccionDto(
                userId.toString(),
                interactions
        );

        System.out.println(
                "[DKT-FORGET] Enviando "
                        + interactions.size()
                        + " interacciones de usuario "
                        + userId
        );

        // 10. Consultar DKT-Forget.
        var respuesta = fastApiClient.obtenerPrediccion(payload);

        if (respuesta == null
                || !Boolean.TRUE.equals(respuesta.modelReady())
                || respuesta.mastery() == null) {

            System.out.println(
                    "[DKT-FORGET] Modelo no disponible. "
                            + "La interacción quedó registrada para el piloto."
            );

            return;
        }

        // 11. Mastery correspondiente a la skill actual.
        Double mastery = respuesta.mastery()
                .get(String.valueOf(activeSkillId));

        if (mastery == null) {

            System.out.println(
                    "[DKT-FORGET] FastAPI no devolvió mastery para skill "
                            + activeSkillId
            );

            return;
        }

        Float nuevaProbabilidad = mastery.floatValue();

        // 12. Persistir predicción actual.
        var topic = learningQueryService.handle(
                new GetTopicByQuestionIdQuery(questionId)
        );

        var prediccionExistente =
                mlPredictionRepository.findByUserIdAndTopicId(
                        userId,
                        topic.getId()
                );

        if (prediccionExistente.isPresent()) {

            var prediccion = prediccionExistente.get();

            prediccion.updatePrediction(
                    nuevaProbabilidad,
                    null
            );

            mlPredictionRepository.save(prediccion);

        } else {

            var nuevaPrediccion = new MlPrediction(
                    userId,
                    topic.getId(),
                    nuevaProbabilidad,
                    null
            );

            mlPredictionRepository.save(nuevaPrediccion);
        }
    }
}
