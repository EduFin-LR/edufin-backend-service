package com.upc.edufinservice.assessment.application.internal.services;

import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.analytics.domain.model.entities.SelectionReason;
import com.upc.edufinservice.analytics.domain.services.MasteryService;
import com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories.StudentInteractionRepository;
import com.upc.edufinservice.learning.domain.model.aggregates.Question;
import com.upc.edufinservice.learning.domain.model.queries.GetRandomQuizQuestionsByTopicQuery;
import com.upc.edufinservice.learning.domain.model.queries.GetTopicByIdQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DynamicFinalService {

    private static final int TOTAL_QUESTIONS = 10;
    private static final int TARGET_ADAPTIVE_QUESTIONS = 2;

    private final LearningQueryService learningQueryService;
    private final MasteryService masteryService;
    private final StudentInteractionRepository interactionRepository;
    private final AdaptiveReinforcementService adaptiveReinforcementService;

    public DynamicFinalService(
            LearningQueryService learningQueryService,
            MasteryService masteryService,
            StudentInteractionRepository interactionRepository,
            AdaptiveReinforcementService adaptiveReinforcementService
    ) {
        this.learningQueryService = learningQueryService;
        this.masteryService = masteryService;
        this.interactionRepository = interactionRepository;
        this.adaptiveReinforcementService = adaptiveReinforcementService;
    }

    /**
     * Construye el FINAL dinámico de un Topic/módulo.
     *
     * Sin mastery disponible:
     *   10 preguntas STANDARD del banco QUIZ del módulo.
     *
     * Con mastery disponible:
     *   intenta 8 STANDARD + 2 LOW_MASTERY.
     *
     * Las preguntas LOW_MASTERY pueden provenir de cualquier skill ya observada
     * por el estudiante, incluyendo skills del módulo actual o módulos previos.
     *
     * Importante:
     * TODAS las preguntas del final se presentan con InteractionType.FINAL.
     */
    public DynamicFinalResult buildFinal(
            UUID userId,
            UUID topicId
    ) {

        learningQueryService.handle(
                new GetTopicByIdQuery(topicId)
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Topic/módulo no encontrado."
                )
        );

        Map<String, Double> mastery =
                masteryService.getMasterySnapshot(userId);

        Set<Integer> observedSkillIds =
                new HashSet<>(
                        interactionRepository
                                .findDistinctSkillIdsByUserId(userId)
                );

        /*
         * Para FINAL no excluimos la skill actual:
         * una debilidad puede pertenecer al mismo módulo o a módulos previos.
         */
        List<ReinforcementQuestionSelection> adaptiveSelections =
                adaptiveReinforcementService.selectReinforcementQuestions(
                        mastery,
                        observedSkillIds,
                        Set.of(),
                        TARGET_ADAPTIVE_QUESTIONS
                );

        int adaptiveCount = adaptiveSelections.size();
        int standardTarget = TOTAL_QUESTIONS - adaptiveCount;

        /*
         * Pedimos unas pocas candidatas adicionales para poder eliminar
         * cualquier duplicado con las preguntas LOW_MASTERY sin traer todo el banco.
         */
        int standardCandidateLimit =
                Math.min(
                        15,
                        standardTarget + (adaptiveCount * 2) + 1
                );

        List<Question> standardCandidates =
                learningQueryService.handle(
                        new GetRandomQuizQuestionsByTopicQuery(
                                topicId,
                                standardCandidateLimit
                        )
                );

        if (standardCandidates.isEmpty()) {
            throw new IllegalStateException(
                    "El módulo no tiene preguntas QUIZ disponibles para construir el FINAL."
            );
        }

        Set<UUID> adaptiveQuestionIds = new HashSet<>();

        adaptiveSelections.forEach(selection -> {
            if (selection.question() != null
                    && selection.question().getId() != null) {
                adaptiveQuestionIds.add(
                        selection.question().getId()
                );
            }
        });

        List<DynamicFinalQuestionSelection> selections =
                new ArrayList<>();

        standardCandidates.stream()
                .filter(question ->
                        question != null
                                && question.getId() != null
                                && !adaptiveQuestionIds.contains(
                                        question.getId()
                                )
                )
                .limit(standardTarget)
                .forEach(question ->
                        selections.add(
                                new DynamicFinalQuestionSelection(
                                        question,
                                        InteractionType.FINAL,
                                        SelectionReason.STANDARD
                                )
                        )
                );

        adaptiveSelections.forEach(selection ->
                selections.add(
                        new DynamicFinalQuestionSelection(
                                selection.question(),
                                InteractionType.FINAL,
                                SelectionReason.LOW_MASTERY
                        )
                )
        );

        /*
         * Si después de eliminar duplicados faltaran preguntas estándar,
         * hacemos una segunda consulta pequeña y completamos sin repetir IDs.
         */
        if (selections.size() < TOTAL_QUESTIONS) {
            Set<UUID> selectedIds = new HashSet<>();

            selections.forEach(selection ->
                    selectedIds.add(
                            selection.question().getId()
                    )
            );

            int missing = TOTAL_QUESTIONS - selections.size();

            List<Question> extraCandidates =
                    learningQueryService.handle(
                            new GetRandomQuizQuestionsByTopicQuery(
                                    topicId,
                                    Math.min(15, missing + 5)
                            )
                    );

            extraCandidates.stream()
                    .filter(question ->
                            question != null
                                    && question.getId() != null
                                    && selectedIds.add(
                                            question.getId()
                                    )
                    )
                    .limit(missing)
                    .forEach(question ->
                            selections.add(
                                    new DynamicFinalQuestionSelection(
                                            question,
                                            InteractionType.FINAL,
                                            SelectionReason.STANDARD
                                    )
                            )
                    );
        }

        Collections.shuffle(selections);

        int actualAdaptiveCount =
                (int) selections.stream()
                        .filter(selection ->
                                selection.selectionReason()
                                        == SelectionReason.LOW_MASTERY
                        )
                        .count();

        int actualStandardCount =
                selections.size() - actualAdaptiveCount;

        return new DynamicFinalResult(
                topicId,
                actualAdaptiveCount > 0,
                actualStandardCount,
                actualAdaptiveCount,
                List.copyOf(selections)
        );
    }
}
