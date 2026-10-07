package com.upc.edufinservice.assessment.application.internal.services;

import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.analytics.domain.model.entities.SelectionReason;
import com.upc.edufinservice.assessment.domain.model.aggregates.QuestionAttempt;
import com.upc.edufinservice.assessment.domain.model.aggregates.TopicFinalResult;
import com.upc.edufinservice.assessment.domain.model.aggregates.UserLessonProgress;
import com.upc.edufinservice.assessment.domain.model.events.FinalCompletedEvent;
import com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalAssessmentPhase;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.QuestionAttemptRepository;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.ExperimentalAssessmentSessionRepository;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.TopicFinalResultRepository;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.UserLessonProgressRepository;
import com.upc.edufinservice.assessment.interfaces.rest.resources.FinalCompletionResponse;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.LessonType;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.ProgressStatus;
import com.upc.edufinservice.learning.domain.model.queries.GetAllTopicsQuery;
import com.upc.edufinservice.learning.domain.model.queries.GetLessonsByTopicIdQuery;
import com.upc.edufinservice.learning.domain.model.queries.GetQuestionByIdQuery;
import com.upc.edufinservice.learning.domain.model.queries.GetTopicByIdQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FinalCompletionService {

    /*
     * El FINAL se aprueba con 70% sobre las preguntas base STANDARD.
     * Las preguntas LOW_MASTERY se registran y alimentan DKT, pero no
     * modifican el porcentaje utilizado para aprobar el módulo.
     */
    private static final float PASSING_SCORE = 70.0f;

    private final LearningQueryService learningQueryService;
    private final QuestionAttemptRepository questionAttemptRepository;
    private final TopicFinalResultRepository finalResultRepository;
    private final UserLessonProgressRepository lessonProgressRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ExperimentalAssessmentSessionRepository experimentalSessionRepository;

    public FinalCompletionService(
            LearningQueryService learningQueryService,
            QuestionAttemptRepository questionAttemptRepository,
            TopicFinalResultRepository finalResultRepository,
            UserLessonProgressRepository lessonProgressRepository,
            ApplicationEventPublisher eventPublisher,
            ExperimentalAssessmentSessionRepository experimentalSessionRepository
    ) {
        this.learningQueryService = learningQueryService;
        this.questionAttemptRepository = questionAttemptRepository;
        this.finalResultRepository = finalResultRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.eventPublisher = eventPublisher;
        this.experimentalSessionRepository = experimentalSessionRepository;
    }

    @Transactional
    public FinalCompletionResponse completeFinal(
            UUID userId,
            UUID topicId,
            List<UUID> questionIds,
            Integer timeSpentSec
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("userId es obligatorio.");
        }

        if (topicId == null) {
            throw new IllegalArgumentException("topicId es obligatorio.");
        }

        learningQueryService.handle(
                new GetTopicByIdQuery(topicId)
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Topic/módulo no encontrado."
                )
        );

        if (questionIds == null || questionIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "questionIds no puede estar vacío."
            );
        }

        /*
         * Evitamos que un ID duplicado cuente dos veces.
         */
        Set<UUID> uniqueQuestionIds =
                new LinkedHashSet<>(questionIds);

        /*
         * El score del FINAL se calcula únicamente con las preguntas
         * STANDARD. Las LOW_MASTERY siguen siendo parte del FINAL y sus
         * intentos ya fueron registrados, pero no afectan la aprobación.
         */
        int standardQuestions = 0;
        int standardCorrectAnswers = 0;

        for (UUID questionId : uniqueQuestionIds) {

            var question = learningQueryService
                    .handle(new GetQuestionByIdQuery(questionId))
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Pregunta no encontrada: " + questionId
                            )
                    );

            LessonType sourceType =
                    question.getLesson().getLessonType();

            if (sourceType != LessonType.QUIZ
                    && sourceType != LessonType.FINAL) {
                throw new IllegalArgumentException(
                        "La pregunta " + questionId
                                + " no es evaluativa."
                );
            }

            List<QuestionAttempt> finalAttempts =
                    questionAttemptRepository
                            .findByUserIdAndQuestionIdAndInteractionType(
                                    userId,
                                    questionId,
                                    InteractionType.FINAL
                            )
                            .stream()
                            .sorted(Comparator.comparing(QuestionAttempt::getAttemptedAt).reversed())
                            .toList();

            if (finalAttempts.isEmpty()) {
                throw new IllegalStateException(
                        "No existe un intento FINAL registrado para la pregunta "
                                + questionId + "."
                );
            }

            /*
             * La metadata guardada en QuestionAttempt indica por qué fue
             * seleccionada la pregunta. Los intentos antiguos pueden tener
             * null; en ese caso se consideran STANDARD por compatibilidad.
             */
            SelectionReason selectionReason =
                    finalAttempts.stream()
                            .map(QuestionAttempt::getSelectionReason)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(SelectionReason.STANDARD);

            boolean lowMastery =
                    selectionReason == SelectionReason.LOW_MASTERY;

            /*
             * Las preguntas STANDARD deben pertenecer al módulo que se está
             * evaluando. Una LOW_MASTERY sí puede venir de un módulo anterior,
             * porque el selector adaptativo permite reforzar skills ya trabajadas.
             */
            if (!lowMastery
                    && !question.getLesson().getTopic().getId().equals(topicId)) {
                throw new IllegalArgumentException(
                        "La pregunta STANDARD " + questionId
                                + " no pertenece al módulo del FINAL."
                );
            }

            boolean questionCorrect = false;

            if ("DRAG_AND_DROP".equalsIgnoreCase(question.getQuestionType())) {
                var expectedOptions = learningQueryService.handle(
                        new com.upc.edufinservice.learning.domain.model.queries.GetOptionsByQuestionIdQuery(questionId)
                );

                Map<UUID, QuestionAttempt> latestByOption = finalAttempts.stream()
                        .filter(a -> a.getSelectedOptionId() != null)
                        .collect(Collectors.toMap(
                                QuestionAttempt::getSelectedOptionId,
                                Function.identity(),
                                (newer, older) -> newer,
                                LinkedHashMap::new
                        ));

                questionCorrect = !expectedOptions.isEmpty()
                        && latestByOption.size() == expectedOptions.size()
                        && expectedOptions.stream().allMatch(option -> {
                    var attempt = latestByOption.get(option.getId());
                    return attempt != null && Boolean.TRUE.equals(attempt.getIsCorrect());
                });
            } else {
                // Para reintentos del FINAL solo cuenta la respuesta FINAL más reciente.
                questionCorrect = Boolean.TRUE.equals(finalAttempts.get(0).getIsCorrect());
            }

            /*
             * LOW_MASTERY no participa en el score de aprobación.
             * Sus intentos ya permanecen registrados para analytics/DKT.
             */
            if (lowMastery) {
                continue;
            }

            standardQuestions++;

            if (questionCorrect) {
                standardCorrectAnswers++;
            }
        }

        if (standardQuestions <= 0) {
            throw new IllegalStateException(
                    "El FINAL no contiene preguntas STANDARD para calcular la aprobación."
            );
        }

        int standardIncorrectAnswers =
                standardQuestions - standardCorrectAnswers;

        float score =
                ((float) standardCorrectAnswers / standardQuestions) * 100.0f;

        boolean passed = score >= PASSING_SCORE;

        var existingResult =
                finalResultRepository.findByUserIdAndTopicId(
                        userId,
                        topicId
                );

        TopicFinalResult result;

        if (existingResult.isPresent()) {
            result = existingResult.get();

            result.updateResult(
                    score,
                    passed,
                    standardCorrectAnswers,
                    standardQuestions,
                    timeSpentSec != null ? timeSpentSec : 0
            );

        } else {
            result = new TopicFinalResult(
                    userId,
                    topicId,
                    score,
                    passed,
                    standardCorrectAnswers,
                    standardQuestions,
                    timeSpentSec != null ? timeSpentSec : 0
            );
        }

        finalResultRepository.save(result);

        boolean postTestAvailable = hasCompletedAllTopicFinals(userId)
                && !experimentalSessionRepository.existsByUserIdAndPhase(
                userId, ExperimentalAssessmentPhase.POST_TEST);

        boolean nextTopicUnlocked = false;

        if (passed) {
            nextTopicUnlocked =
                    unlockNextTopic(
                            userId,
                            topicId
                    );
        }

        /*
         * XP del FINAL se mantiene basado en el score evaluativo del módulo.
         * Como LOW_MASTERY no participa en la aprobación, tampoco altera este bono.
         */
        int finalExperience =
                Math.round(score);

        eventPublisher.publishEvent(
                new FinalCompletedEvent(
                        userId,
                        topicId,
                        score,
                        passed
                )
        );

        /*
         * La respuesta expone los conteos evaluativos STANDARD, porque son los
         * que explican directamente el score y el estado passed.
         */
        return new FinalCompletionResponse(
                standardQuestions,
                standardCorrectAnswers,
                standardIncorrectAnswers,
                score,
                passed,
                finalExperience,
                nextTopicUnlocked,
                postTestAvailable
        );
    }


    private boolean hasCompletedAllTopicFinals(UUID userId) {
        var allTopics = learningQueryService.handle(new GetAllTopicsQuery());
        if (allTopics == null || allTopics.isEmpty()) {
            return false;
        }

        var completedTopicIds = finalResultRepository.findByUserId(userId).stream()
                .map(TopicFinalResult::getTopicId)
                .collect(Collectors.toSet());

        return allTopics.stream()
                .allMatch(topic -> completedTopicIds.contains(topic.getId()));
    }

    private boolean unlockNextTopic(
            UUID userId,
            UUID currentTopicId
    ) {
        var allTopics =
                learningQueryService.handle(
                        new GetAllTopicsQuery()
                );

        for (int i = 0; i < allTopics.size(); i++) {

            if (!allTopics.get(i).getId()
                    .equals(currentTopicId)) {
                continue;
            }

            if (i + 1 >= allTopics.size()) {
                return false;
            }

            var nextTopic =
                    allTopics.get(i + 1);

            var nextTopicLessons =
                    learningQueryService.handle(
                            new GetLessonsByTopicIdQuery(
                                    nextTopic.getId()
                            )
                    );

            if (nextTopicLessons.isEmpty()) {
                return false;
            }

            var firstLesson =
                    nextTopicLessons.get(0);

            var progress =
                    lessonProgressRepository
                            .findByUserIdAndLessonId(
                                    userId,
                                    firstLesson.getId()
                            )
                            .orElseGet(() -> {
                                UserLessonProgress newProgress =
                                        new UserLessonProgress(
                                                userId,
                                                firstLesson.getId()
                                        );

                                newProgress.setStatus(
                                        ProgressStatus.LOCKED
                                );

                                return newProgress;
                            });

            if (progress.getStatus()
                    == ProgressStatus.LOCKED) {

                progress.setStatus(
                        ProgressStatus.UNLOCKED
                );

                lessonProgressRepository.save(
                        progress
                );

                return true;
            }

            return false;
        }

        return false;
    }
}
