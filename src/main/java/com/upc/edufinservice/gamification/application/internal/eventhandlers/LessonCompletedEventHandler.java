package com.upc.edufinservice.gamification.application.internal.eventhandlers;

import com.upc.edufinservice.assessment.domain.model.events.LessonCompletedEvent;
import com.upc.edufinservice.gamification.application.internal.services.XpRewardService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class LessonCompletedEventHandler {
    private final XpRewardService xpRewardService;

    public LessonCompletedEventHandler(XpRewardService xpRewardService) {
        this.xpRewardService = xpRewardService;
    }

    @EventListener
    public void on(LessonCompletedEvent event) {
        int pointsToAward = Math.round(event.score());
        xpRewardService.awardOnce(
                event.userId(), event.lessonId(), XpRewardService.LESSON_COMPLETED, pointsToAward
        );
    }
}
