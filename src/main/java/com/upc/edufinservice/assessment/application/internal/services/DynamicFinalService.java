package com.upc.edufinservice.assessment.application.internal.services;

import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.analytics.domain.model.entities.SelectionReason;
import com.upc.edufinservice.analytics.domain.services.MasteryService;
import com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories.StudentInteractionRepository;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.LessonType;
import com.upc.edufinservice.learning.domain.model.aggregates.Question;
import com.upc.edufinservice.learning.domain.model.queries.GetLessonsByTopicIdQuery;
import com.upc.edufinservice.learning.domain.model.queries.GetQuizQuestionsBySkillQuery;
import com.upc.edufinservice.learning.domain.model.queries.GetRandomQuizQuestionsByTopicQuery;
import com.upc.edufinservice.learning.domain.model.queries.GetTopicByIdQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DynamicFinalService {

    private static final int BASE_QUESTIONS = 10;
    private static final int MIN_EXTRA_QUESTIONS = 3;
    private static final int MAX_EXTRA_QUESTIONS = 5;

    /*
     * Pedimos más candidatas de las que finalmente usaremos porque
     * alguna podría coincidir con una pregunta ya usada en las 10 base.
     */
    private static final int ADAPTIVE_CANDIDATE_LIMIT = 10;

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

    public DynamicFinalResult buildFinal(
            UUID userId,
            UUID topicId
    ) {

        // 1. Validar Topic
        learningQueryService.handle(
                new GetTopicByIdQuery(topicId)
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Topic/módulo no encontrado."
                )
        );

        // 2. Obtener las skills QUIZ del módulo
        var topicLessons = learningQueryService.handle(
                new GetLessonsByTopicIdQuery(topicId)
        );

        Set<Integer> moduleSkillIds = new LinkedHashSet<>();

        topicLessons.stream()
                .filter(lesson ->
                        lesson.getLessonType() == LessonType.QUIZ
                )
                .filter(lesson ->
                        lesson.getSkill() != null
                                && lesson.getSkill().getId() != null
                )
                .forEach(lesson ->
                        moduleSkillIds.add(
                                lesson.getSkill().getId()
                        )
                );

        if (moduleSkillIds.isEmpty()) {
            throw new IllegalStateException(
                    "El módulo no tiene skills QUIZ disponibles para construir el FINAL."
            );
        }

        // 3. Construir las 10 preguntas base equilibradas por skill
        List<DynamicFinalQuestionSelection> selections =
                new ArrayList<>();

        Set<UUID> selectedQuestionIds =
                new HashSet<>();

        List<Integer> skills =
                new ArrayList<>(moduleSkillIds);

        int totalSkills = skills.size();

        int basePerSkill =
                BASE_QUESTIONS / totalSkills;

        int remainder =
                BASE_QUESTIONS % totalSkills;

        for (int i = 0; i < totalSkills; i++) {

            int skillId = skills.get(i);

            int quota =
                    basePerSkill
                            + (i < remainder ? 1 : 0);

            if (quota <= 0) {
                continue;
            }

            List<Question> skillQuestions =
                    learningQueryService.handle(
                            new GetQuizQuestionsBySkillQuery(
                                    skillId,
                                    quota
                            )
                    );

            if (skillQuestions == null) {
                continue;
            }

            for (Question question : skillQuestions) {

                if (question == null
                        || question.getId() == null
                        || !selectedQuestionIds.add(
                        question.getId()
                )) {
                    continue;
                }

                selections.add(
                        new DynamicFinalQuestionSelection(
                                question,
                                InteractionType.FINAL,
                                SelectionReason.STANDARD
                        )
                );
            }
        }

        /*
         * Si alguna skill no tenía suficientes preguntas,
         * completar las 10 base desde cualquier QUIZ del módulo.
         */
        fillWithStandardQuestions(
                topicId,
                BASE_QUESTIONS,
                selections,
                selectedQuestionIds
        );

        if (selections.size() < BASE_QUESTIONS) {
            throw new IllegalStateException(
                    "No existen suficientes preguntas QUIZ para construir "
                            + "las " + BASE_QUESTIONS
                            + " preguntas base del FINAL."
            );
        }

        // 4. Buscar preguntas extra LOW_MASTERY
        Map<String, Double> mastery =
                masteryService.getMasterySnapshot(userId);

        Set<Integer> observedSkillIds =
                new HashSet<>(
                        interactionRepository
                                .findDistinctSkillIdsByUserIdAndInteractionTypeIn(
                                        userId,
                                        List.of(
                                                InteractionType.QUIZ,
                                                InteractionType.FINAL,
                                                InteractionType.REINFORCEMENT
                                        )
                                )
                );

        /*
         * En el FINAL pueden reforzarse skills del módulo actual
         * o de módulos anteriores.
         */
        List<ReinforcementQuestionSelection> adaptiveCandidates =
                adaptiveReinforcementService
                        .selectReinforcementQuestions(
                                mastery,
                                observedSkillIds,
                                Set.of(),
                                ADAPTIVE_CANDIDATE_LIMIT
                        );

        int adaptiveAdded = 0;

        for (ReinforcementQuestionSelection candidate : adaptiveCandidates) {

            if (adaptiveAdded >= MAX_EXTRA_QUESTIONS) {
                break;
            }

            Question question = candidate.question();

            if (question == null
                    || question.getId() == null
                    || !selectedQuestionIds.add(
                    question.getId()
            )) {
                continue;
            }

            selections.add(
                    new DynamicFinalQuestionSelection(
                            question,
                            InteractionType.FINAL,
                            SelectionReason.LOW_MASTERY
                    )
            );

            adaptiveAdded++;
        }

        // 5. Determinar tamaño objetivo entre 13 y 15
        int desiredExtraCount =
                Math.max(
                        MIN_EXTRA_QUESTIONS,
                        Math.min(
                                adaptiveAdded,
                                MAX_EXTRA_QUESTIONS
                        )
                );

        int targetQuestionCount =
                BASE_QUESTIONS + desiredExtraCount;

        // 6. Si faltan preguntas, completar con FINAL + STANDARD
        fillWithStandardQuestions(
                topicId,
                targetQuestionCount,
                selections,
                selectedQuestionIds
        );

        /*
         * Seguridad: nunca devolver más de 15.
         */
        int maxAllowed =
                BASE_QUESTIONS + MAX_EXTRA_QUESTIONS;

        if (selections.size() > maxAllowed) {
            selections =
                    new ArrayList<>(
                            selections.subList(
                                    0,
                                    maxAllowed
                            )
                    );
        }

        Collections.shuffle(selections);

        // 7. Metadata de respuesta
        int actualAdaptiveCount =
                (int) selections.stream()
                        .filter(selection ->
                                selection.selectionReason()
                                        == SelectionReason.LOW_MASTERY
                        )
                        .count();

        int actualStandardCount =
                selections.size()
                        - actualAdaptiveCount;

        return new DynamicFinalResult(
                topicId,
                actualAdaptiveCount > 0,
                actualStandardCount,
                actualAdaptiveCount,
                List.copyOf(selections)
        );
    }

    /**
     * Completa la lista con preguntas FINAL + STANDARD del Topic,
     * evitando IDs ya seleccionados.
     */
    private void fillWithStandardQuestions(
            UUID topicId,
            int targetCount,
            List<DynamicFinalQuestionSelection> selections,
            Set<UUID> selectedQuestionIds
    ) {

        if (selections.size() >= targetCount) {
            return;
        }

        int missing =
                targetCount - selections.size();

        int candidateLimit =
                Math.min(
                        30,
                        missing + 10
                );

        List<Question> candidates =
                learningQueryService.handle(
                        new GetRandomQuizQuestionsByTopicQuery(
                                topicId,
                                candidateLimit
                        )
                );

        if (candidates == null
                || candidates.isEmpty()) {
            return;
        }

        for (Question question : candidates) {

            if (selections.size() >= targetCount) {
                break;
            }

            if (question == null
                    || question.getId() == null
                    || !selectedQuestionIds.add(
                    question.getId()
            )) {
                continue;
            }

            selections.add(
                    new DynamicFinalQuestionSelection(
                            question,
                            InteractionType.FINAL,
                            SelectionReason.STANDARD
                    )
            );
        }
    }
}
