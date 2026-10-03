package com.upc.edufinservice.assessment.application.internal.services;

import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.analytics.domain.model.entities.SelectionReason;
import com.upc.edufinservice.analytics.domain.services.MasteryService;
import com.upc.edufinservice.analytics.infrastructure.persistence.jpa.repositories.StudentInteractionRepository;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.LessonType;
import com.upc.edufinservice.learning.domain.model.aggregates.Question;
import com.upc.edufinservice.learning.domain.model.queries.GetQuestionsByLessonIdQuery;
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
public class AdaptiveQuizService {

    private static final int TOTAL_QUESTIONS = 10;
    private static final int TARGET_REINFORCEMENT_QUESTIONS = 2;

    private final LearningQueryService learningQueryService;
    private final MasteryService masteryService;
    private final StudentInteractionRepository interactionRepository;
    private final AdaptiveReinforcementService adaptiveReinforcementService;

    public AdaptiveQuizService(
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

    public AdaptiveQuizResult buildQuiz(
            UUID userId,
            UUID lessonId
    ) {
        List<Question> lessonQuestions =
                new ArrayList<>(
                        learningQueryService.handle(
                                new GetQuestionsByLessonIdQuery(lessonId)
                        )
                );

        if (lessonQuestions.isEmpty()) {
            throw new IllegalArgumentException(
                    "La lección no tiene preguntas disponibles."
            );
        }

        var lesson = lessonQuestions.get(0).getLesson();

        if (lesson.getLessonType() != LessonType.QUIZ) {
            throw new IllegalArgumentException(
                    "La lección solicitada no es de tipo QUIZ."
            );
        }

        Integer currentSkillId =
                lesson.getSkill() != null
                        ? lesson.getSkill().getId()
                        : null;

        if (currentSkillId == null) {
            throw new IllegalStateException(
                    "La lección QUIZ no tiene una Skill asociada."
            );
        }

        Map<String, Double> mastery =
                masteryService.getMasterySnapshot(userId);

        Set<Integer> observedSkillIds =
                new HashSet<>(
                        interactionRepository
                                .findDistinctSkillIdsByUserId(userId)
                );

        Set<Integer> excludedSkillIds = Set.of(currentSkillId);

        List<ReinforcementQuestionSelection> reinforcementSelections =
                adaptiveReinforcementService.selectReinforcementQuestions(
                        mastery,
                        observedSkillIds,
                        excludedSkillIds,
                        TARGET_REINFORCEMENT_QUESTIONS
                );

        int reinforcementCount = reinforcementSelections.size();
        int standardTarget = TOTAL_QUESTIONS - reinforcementCount;

        Collections.shuffle(lessonQuestions);

        List<AdaptiveQuizQuestionSelection> selections =
                new ArrayList<>();

        lessonQuestions.stream()
                .limit(standardTarget)
                .forEach(question ->
                        selections.add(
                                new AdaptiveQuizQuestionSelection(
                                        question,
                                        InteractionType.QUIZ,
                                        SelectionReason.STANDARD
                                )
                        )
                );

        reinforcementSelections.forEach(selection ->
                selections.add(
                        new AdaptiveQuizQuestionSelection(
                                selection.question(),
                                InteractionType.REINFORCEMENT,
                                SelectionReason.LOW_MASTERY
                        )
                )
        );

        Collections.shuffle(selections);

        int actualReinforcementCount =
                (int) selections.stream()
                        .filter(selection ->
                                selection.interactionType()
                                        == InteractionType.REINFORCEMENT
                        )
                        .count();

        int actualStandardCount =
                selections.size() - actualReinforcementCount;

        return new AdaptiveQuizResult(
                actualReinforcementCount > 0,
                actualStandardCount,
                actualReinforcementCount,
                List.copyOf(selections)
        );
    }
}
