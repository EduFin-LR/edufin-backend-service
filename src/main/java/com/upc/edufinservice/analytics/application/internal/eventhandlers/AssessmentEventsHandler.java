package com.upc.edufinservice.analytics.application.internal.eventhandlers;

import com.upc.edufinservice.analytics.domain.model.aggregates.ErrorPattern;
import com.upc.edufinservice.analytics.domain.model.aggregates.MlPrediction;
import com.upc.edufinservice.analytics.domain.model.entities.StudentInteraction;
import com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories.ErrorPatternRepository;
import com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories.MlPredictionRepository;
import com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories.StudentInteractionRepository;
import com.upc.edufinservice.analytics.infrastructure.external.fastapi.FastAPIClient;
import com.upc.edufinservice.analytics.infrastructure.external.fastapi.dto.SolicitudPrediccionDto;
import com.upc.edufinservice.assessment.domain.model.events.QuestionAnsweredCorrectlyEvent;
import com.upc.edufinservice.assessment.domain.model.events.QuestionAnsweredIncorrectlyEvent;
import com.upc.edufinservice.learning.domain.model.queries.GetQuestionByIdQuery;
import com.upc.edufinservice.learning.domain.model.queries.GetTopicByQuestionIdQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
public class AssessmentEventsHandler {

    private final LearningQueryService learningQueryService;
    private final ErrorPatternRepository errorPatternRepository;
    private final MlPredictionRepository mlPredictionRepository;
    private final StudentInteractionRepository interactionRepository; // Nueva bitácora
    private final FastAPIClient fastApiClient;

    public AssessmentEventsHandler(LearningQueryService learningQueryService,
                                   ErrorPatternRepository errorPatternRepository,
                                   MlPredictionRepository mlPredictionRepository,
                                   StudentInteractionRepository interactionRepository,
                                   FastAPIClient fastApiClient) {
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
        // El patrón de errores macro (Alcancías) se mantiene a nivel de Tema (Topic)
        var topic = learningQueryService.handle(new GetTopicByQuestionIdQuery(event.questionId()));

        var errorPattern = errorPatternRepository.findByUserIdAndTopicId(event.userId(), topic.getId())
                .orElseGet(() -> new ErrorPattern(event.userId(), topic.getId()));

        errorPattern.incrementErrorCount();
        errorPatternRepository.save(errorPattern);

        processInteraction(event.userId(), event.questionId(), 0);
    }


    private static final int MIN_DKT_INTERACTIONS = 8;
    private void processInteraction(UUID userId, UUID questionId, Integer isCorrect) {
        // Extraemos la pregunta para obtener su habilidad granular e individual
        var question = learningQueryService
                .handle(new GetQuestionByIdQuery(questionId))
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Pregunta no encontrada para tracking de IA"
                        )
                );

        Integer activeSkillId = question.getSkill().getId();

        if (activeSkillId == null) {
            return;
        }

        // 1. Guardamos la interacción real.
        var currentInteraction = new StudentInteraction(
                userId,
                activeSkillId,
                isCorrect
        );

        interactionRepository.save(currentInteraction);

        // 2. Recuperamos el historial cronológico.
        var historial =
                interactionRepository.findByUserIdOrderByInteractedAtAsc(userId);

        /*
         * Durante el piloto simplemente acumulamos datos.
         * No consultamos DKT hasta tener suficiente historial.
         */
        if (historial.size() < MIN_DKT_INTERACTIONS) {

            System.out.println(
                    "[DKT-FORGET] Usuario " + userId
                            + " tiene " + historial.size()
                            + "/" + MIN_DKT_INTERACTIONS
                            + " interacciones. Todavía no se consulta el modelo."
            );

            return;
        }

        // 3. Transformamos nuestro historial al contrato nuevo de FastAPI.
        var interactions = historial.stream()
                .map(interaction ->
                        new SolicitudPrediccionDto.InteraccionDto(
                                interaction.getDktSkillId(),
                                interaction.getIsCorrect() == 1,
                                interaction.getInteractedAt()
                                        .atOffset(java.time.ZoneOffset.UTC)
                        )
                )
                .toList();

        // 4. Request DKT-Forget.
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

        /*
         * Si todavía no existe checkpoint, FastAPI devuelve 503.
         * FastAPIClient lo transforma en null.
         */
        var respuesta = fastApiClient.obtenerPrediccion(payload);

        if (respuesta == null
                || respuesta.mastery() == null
                || !Boolean.TRUE.equals(respuesta.modelReady())) {

            System.out.println(
                    "[DKT-FORGET] Modelo no disponible. "
                            + "La interacción quedó registrada para el piloto."
            );

            return;
        }

        // 5. Obtenemos la estimación correspondiente a la skill actual.
        Double mastery = respuesta.mastery()
                .get(String.valueOf(activeSkillId));

        if (mastery == null) {

            System.out.println(
                    "[DKT-FORGET] No se recibió predicción para skill "
                            + activeSkillId
            );

            return;
        }

        Float nuevaProbabilidad = mastery.floatValue();

        // 6. Guardamos la predicción actual.
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