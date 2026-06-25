package com.upc.edufinservice.analytics.application.internal.eventhandlers;

import com.upc.edufinservice.analytics.domain.model.aggregates.ErrorPattern;
import com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories.ErrorPatternRepository;
import com.upc.edufinservice.analytics.infrastructure.external.fastapi.FastAPIClient;
import com.upc.edufinservice.analytics.infrastructure.external.fastapi.dto.SolicitudPrediccionDto;
import com.upc.edufinservice.assessment.domain.model.events.QuestionAnsweredCorrectlyEvent;
import com.upc.edufinservice.assessment.domain.model.events.QuestionAnsweredIncorrectlyEvent;
import com.upc.edufinservice.learning.domain.model.queries.GetTopicByQuestionIdQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AssessmentEventsHandler {

    private final LearningQueryService learningQueryService;
    private final ErrorPatternRepository errorPatternRepository;
    private final FastAPIClient fastApiClient;

    public AssessmentEventsHandler(LearningQueryService learningQueryService,
                                   ErrorPatternRepository errorPatternRepository,
                                   FastAPIClient fastApiClient) {
        this.learningQueryService = learningQueryService;
        this.errorPatternRepository = errorPatternRepository;
        this.fastApiClient = fastApiClient;
    }

    // 1. ESCUCHAR CUANDO RESPONDE CORRECTAMENTE
    @EventListener
    public void on(QuestionAnsweredCorrectlyEvent event) {
        processInteraction(event.userId(), event.questionId(), 1);
    }

    // 2. ESCUCHAR CUANDO RESPONDE INCORRECTAMENTE
    @EventListener
    public void on(QuestionAnsweredIncorrectlyEvent event) {
        var topic = learningQueryService.handle(new GetTopicByQuestionIdQuery(event.questionId()));

        var errorPattern = errorPatternRepository.findByUserIdAndTopicId(event.userId(), topic.getId())
                .orElseGet(() -> new ErrorPattern(event.userId(), topic.getId()));

        errorPattern.incrementErrorCount();
        errorPatternRepository.save(errorPattern);

        processInteraction(event.userId(), event.questionId(), 0);
    }

    // MÈTODO AUXILIAR ACTUALIZADO PARA FASTAPI
    private void processInteraction(UUID userId, UUID questionId, Integer isCorrect) {
        var topic = learningQueryService.handle(new GetTopicByQuestionIdQuery(questionId));

        if (topic.getDktSkillId() != null) {

            // TODO: En el futuro consultaremos la base de datos para obtener el historial real del alumno.
            // Por ahora, usamos datos simulados para que la IA de Python no falle al recibir el JSON.
            List<Integer> historialSimulado = List.of(topic.getDktSkillId()); // Simulando que ya vio este tema antes
            Double diasInactividadSimulados = 0.5; // Simula medio día de inactividad

            // Armamos el DTO exactamente como lo pide tu modelo Pydantic
            var payload = new SolicitudPrediccionDto(
                    userId.toString(), // FastAPI espera un String, no un UUID
                    historialSimulado,
                    topic.getDktSkillId(),
                    diasInactividadSimulados
            );

            System.out.println("[ANALYTICS] Despachando evaluación a Python. Skill evaluado: " + topic.getName());

            // Enviamos la petición y capturamos la respuesta del modelo DKT
            var respuesta = fastApiClient.obtenerPrediccion(payload);

            if (respuesta != null) {
                System.out.println("[ANALYTICS] ¡Predicción recibida exitosamente!");
                System.out.println("-> Probabilidad DKT: " + respuesta.probabilidad_base_dkt());
                System.out.println("-> Probabilidad Final (Olvido): " + respuesta.probabilidad_final_dmma());
                System.out.println("-> Nivel Recomendado: " + respuesta.nivel_recommended());

                // TODO: Aquí guardaremos la respuesta en la tabla `ml_predictions`
            }
        }
    }
}