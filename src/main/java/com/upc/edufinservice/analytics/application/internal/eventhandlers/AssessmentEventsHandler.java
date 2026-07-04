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
import com.upc.edufinservice.learning.domain.model.queries.GetSideQuestQuestionsBySkillQuery;
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
        // El patrón de errores macro (Alcancías) se mantiene a nivel de Tema (Topic)
        var topic = learningQueryService.handle(new GetTopicByQuestionIdQuery(event.questionId()));

        var errorPattern = errorPatternRepository.findByUserIdAndTopicId(event.userId(), topic.getId())
                .orElseGet(() -> new ErrorPattern(event.userId(), topic.getId()));

        errorPattern.incrementErrorCount();
        errorPatternRepository.save(errorPattern);

        processInteraction(event.userId(), event.questionId(), 0);
    }

    private void processInteraction(UUID userId, UUID questionId, Integer isCorrect) {
        // Extraemos la pregunta para obtener su habilidad granular e individual
        var question = learningQueryService.handle(new GetQuestionByIdQuery(questionId))
                .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada para tracking de IA"));

        var topic = learningQueryService.handle(new GetTopicByQuestionIdQuery(questionId));
        Integer activeSkillId = question.getDktSkillId();

        if (activeSkillId != null) {

            // 1. Guardar la interacción actual en la bitácora con la habilidad exacta de la pregunta
            var currentInteraction = new StudentInteraction(userId, activeSkillId, isCorrect);
            interactionRepository.save(currentInteraction);

            // 2. Obtener todo el historial real cronológico del estudiante
            var historial = interactionRepository.findByUserIdOrderByInteractedAtAsc(userId);

            // 2.1 Filtrar el historial de esta habilidad específica para el cálculo temporal (DMMA)
            var historialDelTema = historial.stream()
                    .filter(h -> h.getDktSkillId().equals(activeSkillId))
                    .collect(Collectors.toList());

            // 3. Calcular Días de Inactividad ESPECÍFICOS (DMMA)
            double diasInactividad = 0.0;
            if (historialDelTema.size() > 1) {
                var interaccionAnterior = historialDelTema.get(historialDelTema.size() - 2).getInteractedAt();
                var interaccionActual = currentInteraction.getInteractedAt();

                long segundos = Duration.between(interaccionAnterior, interaccionActual).getSeconds();
                diasInactividad = segundos / 86400.0;
            }

            // 4. Codificar la secuencia matemática tradicional DKT: (Skill * 2) + isCorrect
            List<Integer> secuenciaReal = historial.stream()
                    .map(h -> (h.getDktSkillId() * 2) + h.getIsCorrect())
                    .collect(Collectors.toList());

            // 5. Armar el contrato de inferencia enviando la habilidad objetivo real
            var payload = new SolicitudPrediccionDto(
                    userId.toString(),
                    secuenciaReal,
                    activeSkillId,
                    diasInactividad
            );

            System.out.println("[ANALYTICS] Secuencia granular enviada a FastAPI: " + secuenciaReal + " | Skill Objetivo: " + activeSkillId);

            var respuesta = fastApiClient.obtenerPrediccion(payload);

            if (respuesta != null) {
                Float nuevaProbabilidad = respuesta.probabilidad_final_dmma().floatValue();
                String nivelRecomendado = respuesta.nivel_recomendado(); // 🔥 Capturamos el String de la IA [Nivel 1, 2 o 3]

                UUID recommendedLessonId = null;

                // 🔥 2. INTERCEPTOR DE CRISIS PEDAGÓGICA (SIDE QUEST TRIGGER)
                if ("Nivel 1 (Repaso / Fácil)".equalsIgnoreCase(nivelRecomendado)) {
                    System.out.println("⚠️ [SIDE QUEST] Crisis de retención detectada para la habilidad: " + activeSkillId + ". Generando misión de reforzamiento...");

                    // Ejecutamos el Query aleatorio que creamos en el paso anterior (Limitado a 3 preguntas de tipo QUIZ)
                    var preguntasRefuerzo = learningQueryService.handle(new GetSideQuestQuestionsBySkillQuery(activeSkillId, 3));

                    if (!preguntasRefuerzo.isEmpty()) {
                        // Extraemos la lección de origen de estas preguntas para activar el flag en el mapa
                        recommendedLessonId = preguntasRefuerzo.get(0).getLesson().getId();
                        System.out.println("🎯 [SIDE QUEST] Misión asociada exitosamente a la lección ID: " + recommendedLessonId);
                    }
                }

                // 3. Persistimos los resultados cruzados en PostgreSQL
                var prediccionExistente = mlPredictionRepository.findByUserIdAndTopicId(userId, topic.getId());

                if (prediccionExistente.isPresent()) {
                    var prediccion = prediccionExistente.get();
                    // Actualizamos la nota predictiva e inyectamos el ID de la lección si hubo Side Quest (o null si aprobó)
                    prediccion.updatePrediction(nuevaProbabilidad, recommendedLessonId);
                    mlPredictionRepository.save(prediccion);
                } else {
                    var nuevaPrediccion = new MlPrediction(userId, topic.getId(), nuevaProbabilidad, recommendedLessonId);
                    mlPredictionRepository.save(nuevaPrediccion);
                }
            }
        }
    }
}