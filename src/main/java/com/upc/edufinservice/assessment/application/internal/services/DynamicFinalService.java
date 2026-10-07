package com.upc.edufinservice.assessment.application.internal.services;

import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.analytics.domain.model.entities.SelectionReason;
import com.upc.edufinservice.analytics.domain.services.MasteryService;
import com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories.StudentInteractionRepository;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.LessonType;
import com.upc.edufinservice.learning.domain.model.aggregates.Question;
import com.upc.edufinservice.learning.domain.model.queries.GetLessonsByTopicIdQuery;
import com.upc.edufinservice.learning.domain.model.queries.GetQuizQuestionsBySkillQuery;
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

    /**
     * Cada LESSON del módulo aporta exactamente 3 preguntas base.
     */
    private static final int BASE_QUESTIONS_PER_LESSON = 3;

    /**
     * Pedimos más candidatas LOW_MASTERY de las que finalmente usaremos,
     * porque alguna puede coincidir con una pregunta ya usada en la base.
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

        // 2. Obtener solamente las LESSON del módulo.
        // Cada LESSON representa un concepto/skill que debe estar cubierto
        // uniformemente en el FINAL.
        var topicLessons = learningQueryService.handle(
                new GetLessonsByTopicIdQuery(topicId)
        );

        var contentLessons = topicLessons.stream()
                .filter(lesson ->
                        lesson.getLessonType() == LessonType.LESSON
                )
                .filter(lesson ->
                        lesson.getSkill() != null
                                && lesson.getSkill().getId() != null
                )
                .toList();

        if (contentLessons.isEmpty()) {
            throw new IllegalStateException(
                    "El módulo no tiene LESSON con skill disponibles para construir el FINAL."
            );
        }

        // 3. Construir la parte base:
        // exactamente 3 preguntas QUIZ por cada LESSON.
        List<DynamicFinalQuestionSelection> selections =
                new ArrayList<>();

        Set<UUID> selectedQuestionIds =
                new HashSet<>();

        for (var lesson : contentLessons) {

            int skillId = lesson.getSkill().getId();

            List<Question> skillQuestions =
                    learningQueryService.handle(
                            new GetQuizQuestionsBySkillQuery(
                                    skillId,
                                    BASE_QUESTIONS_PER_LESSON
                            )
                    );

            if (skillQuestions == null
                    || skillQuestions.size() < BASE_QUESTIONS_PER_LESSON) {

                throw new IllegalStateException(
                        "La skill " + skillId
                                + " no tiene suficientes preguntas QUIZ para aportar "
                                + BASE_QUESTIONS_PER_LESSON
                                + " preguntas al FINAL."
                );
            }

            int addedForLesson = 0;

            for (Question question : skillQuestions) {

                if (addedForLesson >= BASE_QUESTIONS_PER_LESSON) {
                    break;
                }

                if (question == null
                        || question.getId() == null
                        || !selectedQuestionIds.add(question.getId())) {
                    continue;
                }

                selections.add(
                        new DynamicFinalQuestionSelection(
                                question,
                                InteractionType.FINAL,
                                SelectionReason.STANDARD
                        )
                );

                addedForLesson++;
            }

            if (addedForLesson < BASE_QUESTIONS_PER_LESSON) {
                throw new IllegalStateException(
                        "No fue posible seleccionar "
                                + BASE_QUESTIONS_PER_LESSON
                                + " preguntas distintas para la skill "
                                + skillId + "."
                );
            }
        }

        int baseQuestionCount =
                contentLessons.size() * BASE_QUESTIONS_PER_LESSON;

        if (selections.size() != baseQuestionCount) {
            throw new IllegalStateException(
                    "La cantidad de preguntas base del FINAL no coincide con "
                            + "3 preguntas por cada LESSON."
            );
        }

        // 4. Determinar el máximo de preguntas de refuerzo.
        //
        // 3 LESSON -> hasta 3
        // 4 LESSON -> hasta 3
        // 5 LESSON -> hasta 4
        // 6 o más -> hasta 5
        int reinforcementLimit =
                determineReinforcementLimit(contentLessons.size());

        // 5. Buscar preguntas LOW_MASTERY
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

            if (adaptiveAdded >= reinforcementLimit) {
                break;
            }

            Question question = candidate.question();

            if (question == null
                    || question.getId() == null
                    || !selectedQuestionIds.add(question.getId())) {
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

        /*
         * Ya no se rellena con preguntas STANDARD adicionales.
         *
         * El tamaño final queda:
         *
         * (3 x cantidad de LESSON)
         * +
         * LOW_MASTERY realmente disponibles
         */

        Collections.shuffle(selections);

        // 6. Metadata de respuesta
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

    /**
     * Cantidad máxima de preguntas de refuerzo según
     * la cantidad de LESSON del módulo.
     */
    private int determineReinforcementLimit(int lessonCount) {

        if (lessonCount <= 4) {
            return 3;
        }

        if (lessonCount == 5) {
            return 4;
        }

        return 5;
    }
}