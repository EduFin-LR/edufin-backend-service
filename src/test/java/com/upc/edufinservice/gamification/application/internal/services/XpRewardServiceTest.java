package com.upc.edufinservice.gamification.application.internal.services;

import com.upc.edufinservice.gamification.domain.model.commands.AddPointsCommand;
import com.upc.edufinservice.gamification.domain.services.GamificationCommandService;
import com.upc.edufinservice.gamification.infrastructure.persistence.jpa.repositories.XpRewardGrantRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class XpRewardServiceTest {

    @Test
    void firstRewardAddsPoints() {
        var repository = mock(XpRewardGrantRepository.class);
        var gamification = mock(GamificationCommandService.class);
        var service = new XpRewardService(repository, gamification);
        UUID userId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        when(repository.insertIfAbsent(any(UUID.class), eq(userId), eq(questionId),
                eq(XpRewardService.QUESTION_CORRECT), eq(10))).thenReturn(1);

        assertTrue(service.awardOnce(userId, questionId, XpRewardService.QUESTION_CORRECT, 10));
        ArgumentCaptor<AddPointsCommand> captor = ArgumentCaptor.forClass(AddPointsCommand.class);
        verify(gamification).handle(captor.capture());
        assertEquals(userId, captor.getValue().userId());
        assertEquals(10, captor.getValue().points());
    }

    @Test
    void duplicateRewardDoesNotAddPoints() {
        var repository = mock(XpRewardGrantRepository.class);
        var gamification = mock(GamificationCommandService.class);
        var service = new XpRewardService(repository, gamification);
        UUID userId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        when(repository.insertIfAbsent(any(UUID.class), eq(userId), eq(lessonId),
                eq(XpRewardService.LESSON_COMPLETED), eq(100))).thenReturn(0);

        assertFalse(service.awardOnce(userId, lessonId, XpRewardService.LESSON_COMPLETED, 100));
        verifyNoInteractions(gamification);
    }
}
