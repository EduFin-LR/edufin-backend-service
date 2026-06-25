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
import com.upc.edufinservice.learning.domain.model.queries.GetTopicByQuestionIdQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        var topic = learningQueryService.handle(new GetTopicByQuestionIdQuery(event.questionId()));

        var errorPattern = errorPatternRepository.findByUserIdAndTopicId(event.userId(), topic.getId())
                .orElseGet(() -> new ErrorPattern(event.userId(), topic.getId()));

        errorPattern.incrementErrorCount();
        errorPatternRepository.save(errorPattern);

        processInteraction(event.userId(), event.questionId(), 0);
    }

    private void processInteraction(UUID userId, UUID questionId, Integer isCorrect) {
        var topic = learningQueryService.handle(new GetTopicByQuestionIdQuery(questionId));

        if (topic.getDktSkillId() != null) {

            // 1. Guardar la interacción actual en la bitácora
            var currentInteraction = new StudentInteraction(userId, topic.getDktSkillId(), isCorrect);
            interactionRepository.save(currentInteraction);

            // 2. Obtener todo el historial real cronológico del estudiante
            var historial = interactionRepository.findByUserIdOrderByInteractedAtAsc(userId);

            // 3. Calcular Días de Inactividad (DMMA)
            double diasInactividad = 0.0;
            if (historial.size() > 1) {
                var interaccionAnterior = historial.get(historial.size() - 2).getInteractedAt();
                var interaccionActual = currentInteraction.getInteractedAt();

                // Calculamos la diferencia en segundos y la convertimos a días (1 día = 86400 segundos)
                long segundos = Duration.between(interaccionAnterior, interaccionActual).getSeconds();
                diasInactividad = segundos / 86400.0;
            }

            // 4. Codificar la secuencia para DKT
            List<Integer> secuenciaReal = historial.stream()
                    .map(h -> (h.getDktSkillId() * 2) + h.getIsCorrect())
                    .collect(Collectors.toList());

            // 5. Armar el paquete y enviarlo a FastAPI
            var payload = new SolicitudPrediccionDto(
                    userId.toString(),
                    secuenciaReal,
                    topic.getDktSkillId(),
                    diasInactividad
            );

            System.out.println("[ANALYTICS] Secuencia DKT armada: " + secuenciaReal + " | Días inactividad: " + diasInactividad);

            var respuesta = fastApiClient.obtenerPrediccion(payload);

            if (respuesta != null) {
                Float nuevaProbabilidad = respuesta.probabilidad_final_dmma().floatValue();
                var prediccionExistente = mlPredictionRepository.findByUserIdAndTopicId(userId, topic.getId());

                if (prediccionExistente.isPresent()) {
                    var prediccion = prediccionExistente.get();
                    prediccion.updatePrediction(nuevaProbabilidad, null);
                    mlPredictionRepository.save(prediccion);
                } else {
                    var nuevaPrediccion = new MlPrediction(userId, topic.getId(), nuevaProbabilidad, null);
                    mlPredictionRepository.save(nuevaPrediccion);
                }
            }
        }
    }
}