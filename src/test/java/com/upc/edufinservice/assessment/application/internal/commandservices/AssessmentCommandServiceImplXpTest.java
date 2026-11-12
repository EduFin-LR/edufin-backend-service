package com.upc.edufinservice.assessment.application.internal.commandservices;

import com.upc.edufinservice.analytics.domain.model.entities.InteractionType;
import com.upc.edufinservice.analytics.domain.services.MasteryService;
import com.upc.edufinservice.assessment.domain.model.aggregates.QuestionAttempt;
import com.upc.edufinservice.assessment.domain.model.aggregates.UserLessonProgress;
import com.upc.edufinservice.assessment.domain.model.commands.CompleteLessonCommand;
import com.upc.edufinservice.assessment.domain.model.events.LessonCompletedEvent;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.QuestionAttemptRepository;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.UserLessonProgressRepository;
import com.upc.edufinservice.learning.domain.model.ValueObjetcts.LessonType;
import com.upc.edufinservice.learning.domain.model.aggregates.Lesson;
import com.upc.edufinservice.learning.domain.model.aggregates.Question;
import com.upc.edufinservice.learning.domain.model.aggregates.Topic;
import com.upc.edufinservice.learning.domain.model.queries.*;
import com.upc.edufinservice.learning.domain.services.LearningQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssessmentCommandServiceImplXpTest {
    private final QuestionAttemptRepository attempts = mock(QuestionAttemptRepository.class);
    private final UserLessonProgressRepository progressRepository = mock(UserLessonProgressRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final LearningQueryService learning = mock(LearningQueryService.class);
    private final MasteryService mastery = mock(MasteryService.class);
    private final AssessmentCommandServiceImpl service = new AssessmentCommandServiceImpl(
            attempts, progressRepository, events, learning, mastery);

    private final UUID userId = UUID.randomUUID();
    private final UUID lessonId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();
    private final UUID optionId = UUID.randomUUID();

    private UserLessonProgress configurePassingLesson(boolean completed) {
        var progress = new UserLessonProgress(userId, lessonId);
        if (completed) {
            progress.markAsCompleted(100f, 5);
        }
        when(progressRepository.findByUserIdAndLessonIdForUpdate(userId, lessonId))
                .thenReturn(Optional.of(progress));
        var topic = mock(Topic.class);
        var lesson = mock(Lesson.class);
        var question = mock(Question.class);

        when(topic.getId()).thenReturn(UUID.randomUUID());
        when(lesson.getId()).thenReturn(lessonId);
        when(lesson.getLessonType()).thenReturn(LessonType.QUIZ);
        when(question.getQuestionType()).thenReturn("MULTIPLE_CHOICE");
        when(learning.handle(any(GetQuestionByIdQuery.class))).thenReturn(Optional.of(question));
        when(learning.handle(any(GetAllTopicsQuery.class))).thenReturn(List.of(topic));
        when(learning.handle(any(GetLessonsByTopicIdQuery.class))).thenReturn(List.of(lesson));
        when(attempts.findByUserIdAndQuestionId(userId, questionId)).thenReturn(List.of(
                new QuestionAttempt(userId, questionId, optionId, null, true, 2f, InteractionType.QUIZ, null)
        ));
        return progress;
    }

    @Test
    void firstCompletionGrantsLessonEvent() {
        var progress = configurePassingLesson(false);
        var response = service.handle(new CompleteLessonCommand(userId, lessonId, 30, List.of(questionId)));

        assertEquals(100, response.lessonExperience());
        assertEquals(10, response.questionsExperience());
        assertEquals(110, response.totalExperience());
        assertNotNull(progress.getCompletedAt());
        verify(events).publishEvent(any(LessonCompletedEvent.class));
    }

    @Test
    void repeatCompletionAwardsNoXp() {
        var progress = configurePassingLesson(true);
        var originalCompletedAt = progress.getCompletedAt();
        var response = service.handle(new CompleteLessonCommand(userId, lessonId, 30, List.of(questionId)));

        assertEquals(0, response.lessonExperience());
        assertEquals(0, response.questionsExperience());
        assertEquals(0, response.totalExperience());
        assertEquals(originalCompletedAt, progress.getCompletedAt());
        verify(events, never()).publishEvent(any(LessonCompletedEvent.class));
    }
}
