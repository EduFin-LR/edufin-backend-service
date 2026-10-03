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
        processInteraction(
                event.userId(),
                event.questionId(),
                1,
                event.interactionType(),
                event.selectionReason()
        );
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

        processInteraction(
                event.userId(),
                event.questionId(),
                0,
                event.interactionType(),
                event.selectionReason()
        );
    }

    private void processInteraction(
            UUID userId,
            UUID questionId,
            Integer isCorrect,
            InteractionType requestedInteractionType,
            SelectionReason requestedSelectionReason
    ) {

        var question = learningQueryService
                .handle(new GetQuestionByIdQuery(questionId))
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Pregunta no encontrada para tracking de IA"
                        )
                );

        /*
         * Solo preguntas provenientes de bancos evaluativos participan en DKT.
         *
         * Una pregunta de QUIZ puede presentarse como:
         * - QUIZ normal
         * - REINFORCEMENT
         * - FINAL dinámico
         */
        LessonType sourceLessonType = question.getLesson().getLessonType();

        if (sourceLessonType != LessonType.QUIZ
                && sourceLessonType != LessonType.FINAL) {

            System.out.println(
                    "[DKT-FORGET] Interacción ignorada. "
                            + "sourceLessonType=" + sourceLessonType
                            + ", questionId=" + questionId
            );
            return;
        }

        Integer activeSkillId = question.getSkill().getId();

        if (activeSkillId == null) {
            System.out.println(
                    "[DKT-FORGET] Pregunta sin Skill. questionId=" + questionId
            );
            return;
        }

        /*
         * Fallback para flujos antiguos/no adaptativos:
         * si el frontend no mandó metadata, inferimos el contexto básico.
         */
        InteractionType interactionType = requestedInteractionType;

        if (interactionType == null) {
            interactionType =
                    sourceLessonType == LessonType.FINAL
                            ? InteractionType.FINAL
                            : InteractionType.QUIZ;
        }

        SelectionReason selectionReason =
                requestedSelectionReason != null
                        ? requestedSelectionReason
                        : SelectionReason.STANDARD;

        /*
         * Consistencia pedagógica:
         * si la actividad fue REINFORCEMENT, la razón debe ser LOW_MASTERY.
         */
        if (interactionType == InteractionType.REINFORCEMENT) {
            selectionReason = SelectionReason.LOW_MASTERY;
        }

        var currentInteraction = new StudentInteraction(
                userId,
                activeSkillId,
                isCorrect,
                interactionType,
                selectionReason
        );

        interactionRepository.save(currentInteraction);

        var historial =
                interactionRepository.findByUserIdOrderByInteractedAtAsc(userId);

        if (historial.size() < MIN_DKT_INTERACTIONS) {
            System.out.println(
                    "[DKT-FORGET] Usuario " + userId
                            + ": " + historial.size()
                            + "/" + MIN_DKT_INTERACTIONS
                            + " interacciones válidas."
            );
            return;
        }

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
