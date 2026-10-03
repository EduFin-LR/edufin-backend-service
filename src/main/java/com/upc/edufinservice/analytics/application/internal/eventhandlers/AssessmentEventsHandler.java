package com.upc.edufinservice.analytics.application.internal.eventhandlers;

import com.upc.edufinservice.analytics.domain.model.aggregates.ErrorPattern;
import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.analytics.domain.model.entities.SelectionReason;
import com.upc.edufinservice.analytics.domain.model.entities.StudentInteraction;
import com.upc.edufinservice.analytics.domain.services.MasteryService;
import com.upc.edufinservice.analytics.infrastructure.external.fastapi.FastAPIClient;
import com.upc.edufinservice.analytics.infrastructure.external.fastapi.dto.SolicitudPrediccionDto;
import com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories.ErrorPatternRepository;
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
    private final StudentInteractionRepository interactionRepository;
    private final FastAPIClient fastApiClient;
    private final MasteryService masteryService;

    public AssessmentEventsHandler(
            LearningQueryService learningQueryService,
            ErrorPatternRepository errorPatternRepository,
            StudentInteractionRepository interactionRepository,
            FastAPIClient fastApiClient,
            MasteryService masteryService
    ) {
        this.learningQueryService = learningQueryService;
        this.errorPatternRepository = errorPatternRepository;
        this.interactionRepository = interactionRepository;
        this.fastApiClient = fastApiClient;
        this.masteryService = masteryService;
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

        // 4. Contexto inicial de la interacción.
        InteractionType interactionType =
                lessonType == LessonType.FINAL
                        ? InteractionType.FINAL
                        : InteractionType.QUIZ;

        // Más adelante el flujo adaptativo enviará LOW_MASTERY/REINFORCEMENT.
        SelectionReason selectionReason = SelectionReason.STANDARD;

        // 5. Guardar interacción válida.
        var currentInteraction = new StudentInteraction(
                userId,
                activeSkillId,
                isCorrect,
                interactionType,
                selectionReason
        );

        interactionRepository.save(currentInteraction);

        // 6. Historial cronológico del estudiante.
        var historial =
                interactionRepository.findByUserIdOrderByInteractedAtAsc(userId);

        // 7. No consultar DKT hasta tener suficiente evidencia.
        if (historial.size() < MIN_DKT_INTERACTIONS) {
            System.out.println(
                    "[DKT-FORGET] Usuario " + userId
                            + ": " + historial.size()
                            + "/" + MIN_DKT_INTERACTIONS
                            + " interacciones válidas."
            );
            return;
        }

        // 8. Adaptar historial al contrato FastAPI.
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

        // 9. Consultar DKT-Forget.
        var respuesta = fastApiClient.obtenerPrediccion(payload);

        if (respuesta == null
                || !Boolean.TRUE.equals(respuesta.modelReady())
                || respuesta.mastery() == null
                || respuesta.mastery().isEmpty()) {

            System.out.println(
                    "[DKT-FORGET] Modelo no disponible. "
                            + "La interacción quedó registrada para el piloto."
            );
            return;
        }

        /*
         * 10. Persistir el snapshot COMPLETO de mastery.
         *
         * FastAPI devuelve una probabilidad para cada una de las 30 skills.
         * Ya no descartamos 29 probabilidades para conservar solo la skill actual.
         */
        masteryService.updateMasterySnapshot(
                userId,
                respuesta.mastery()
        );

        System.out.println(
                "[DKT-FORGET] Snapshot de mastery actualizado para usuario "
                        + userId
        );
    }
}
