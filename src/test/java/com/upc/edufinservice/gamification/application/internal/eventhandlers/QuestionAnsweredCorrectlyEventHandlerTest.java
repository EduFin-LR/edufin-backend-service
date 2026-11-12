package com.upc.edufinservice.gamification.application.internal.eventhandlers;

import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.assessment.domain.model.aggregates.QuestionAttempt;
import com.upc.edufinservice.assessment.domain.model.aggregates.UserLessonProgress;
import com.upc.edufinservice.assessment.domain.model.events.QuestionAnsweredCorrectlyEvent;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.QuestionAttemptRepository;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.UserLessonProgressRepository;
import com.upc.edufinservice.gamification.application.internal.services.XpRewardService;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.ProgressStatus;
import com.upc.edufinservice.learning.domain.model.aggregates.Lesson;
import com.upc.edufinservice.learning.domain.model.aggregates.Question;
import com.upc.edufinservice.learning.domain.model.queries.GetQuestionByIdQuery;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QuestionAnsweredCorrectlyEventHandlerTest {
    private final XpRewardService rewards = mock(XpRewardService.class);
    private final LearningQueryService learning = mock(LearningQueryService.class);
    private final UserLessonProgressRepository progressRepository = mock(UserLessonProgressRepository.class);
    private final QuestionAttemptRepository attempts = mock(QuestionAttemptRepository.class);
    private final QuestionAnsweredCorrectlyEventHandler handler = new QuestionAnsweredCorrectlyEventHandler(
            rewards, learning, progressRepository, attempts);
    private final UUID userId = UUID.randomUUID();
    private final UUID lessonId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();
    private final UUID optionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        var question = mock(Question.class);
        var lesson = mock(Lesson.class);
        when(question.getLesson()).thenReturn(lesson);
        when(lesson.getId()).thenReturn(lessonId);
        when(question.getQuestionType()).thenReturn("MULTIPLE_CHOICE");
        when(learning.handle(any(GetQuestionByIdQuery.class))).thenReturn(Optional.of(question));
        when(progressRepository.findByUserIdAndLessonId(userId, lessonId)).thenReturn(Optional.empty());
    }

    private QuestionAnsweredCorrectlyEvent event() {
        return new QuestionAnsweredCorrectlyEvent(userId, questionId, optionId, InteractionType.QUIZ, null);
    }

    private QuestionAttempt correctAttempt() {
        return new QuestionAttempt(userId, questionId, optionId, null, true, 2f, InteractionType.QUIZ, null);
    }

    @Test
    void awardsFirstCorrectAnswer() {
        when(attempts.findByUserIdAndQuestionId(userId, questionId))
                .thenReturn(List.of(correctAttempt()));
        handler.on(event());
        verify(rewards).awardOnce(userId, questionId, XpRewardService.QUESTION_CORRECT, 10);
    }

    @Test
    void skipsCorrectAnswersAfterLessonIsCompleted() {
        var progress = new UserLessonProgress(userId, lessonId);
        progress.setStatus(ProgressStatus.COMPLETED);
        when(progressRepository.findByUserIdAndLessonId(userId, lessonId)).thenReturn(Optional.of(progress));

        handler.on(event());
        verifyNoInteractions(rewards);
    }

    @Test
    void skipsPreviouslyCorrectQuestionOnRetake() {
        when(attempts.findByUserIdAndQuestionId(userId, questionId))
                .thenReturn(List.of(correctAttempt(), correctAttempt()));

        handler.on(event());
        verifyNoInteractions(rewards);
    }
}
