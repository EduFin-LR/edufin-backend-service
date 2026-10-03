package com.upc.edufinservice.assessment.application.internal.services;

import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.assessment.domain.model.aggregates.QuestionAttempt;
import com.upc.edufinservice.assessment.domain.model.aggregates.TopicFinalResult;
import com.upc.edufinservice.assessment.domain.model.aggregates.UserLessonProgress;
import com.upc.edufinservice.assessment.domain.model.events.FinalCompletedEvent;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.QuestionAttemptRepository;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FinalCompletionService {

    /*
     * El frontend ya venía usando 60% como criterio visual de aprobación.
     * Se centraliza aquí para que el backend sea la fuente de verdad.
     */
    private static final float PASSING_SCORE = 60.0f;

    private final LearningQueryService learningQueryService;
    private final QuestionAttemptRepository questionAttemptRepository;
    private final TopicFinalResultRepository finalResultRepository;
    private final UserLessonProgressRepository lessonProgressRepository;
    private final ApplicationEventPublisher eventPublisher;

    public FinalCompletionService(
            LearningQueryService learningQueryService,
            QuestionAttemptRepository questionAttemptRepository,
            TopicFinalResultRepository finalResultRepository,
            UserLessonProgressRepository lessonProgressRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.learningQueryService = learningQueryService;
        this.questionAttemptRepository = questionAttemptRepository;
        this.finalResultRepository = finalResultRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.eventPublisher = eventPublisher;
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

        int correctAnswers = 0;

        for (UUID questionId : uniqueQuestionIds) {

            var question = learningQueryService
                    .handle(new GetQuestionByIdQuery(questionId))
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Pregunta no encontrada: " + questionId
                            )
                    );

            /*
             * El FINAL dinámico se construye a partir de bancos QUIZ.
             * Validamos que la pregunta presentada pertenezca al Topic
             * solicitado y provenga de un banco evaluativo.
             */
            if (!question.getLesson().getTopic().getId().equals(topicId)) {
                throw new IllegalArgumentException(
                        "La pregunta " + questionId
                                + " no pertenece al módulo del FINAL."
                );
            }

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
                            );

            boolean hasCorrectFinalAttempt =
                    finalAttempts.stream()
                            .anyMatch(QuestionAttempt::getIsCorrect);

            if (hasCorrectFinalAttempt) {
                correctAnswers++;
            }
        }

        int totalQuestions = uniqueQuestionIds.size();
        int incorrectAnswers =
                totalQuestions - correctAnswers;

        float score =
                totalQuestions > 0
                        ? ((float) correctAnswers / totalQuestions) * 100.0f
                        : 0.0f;

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
                    correctAnswers,
                    totalQuestions,
                    timeSpentSec != null ? timeSpentSec : 0
            );

        } else {
            result = new TopicFinalResult(
                    userId,
                    topicId,
                    score,
                    passed,
                    correctAnswers,
                    totalQuestions,
                    timeSpentSec != null ? timeSpentSec : 0
            );
        }

        finalResultRepository.save(result);

        boolean nextTopicUnlocked = false;

        if (passed) {
            nextTopicUnlocked =
                    unlockNextTopic(
                            userId,
                            topicId
                    );
        }

        /*
         * XP del FINAL: un punto por cada porcentaje obtenido,
         * igual al bono actual por completar una lección.
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

        return new FinalCompletionResponse(
                totalQuestions,
                correctAnswers,
                incorrectAnswers,
                score,
                passed,
                finalExperience,
                nextTopicUnlocked
        );
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
