package com.upc.edufinservice.gamification.application.internal.eventhandlers;

import com.upc.edufinservice.assessment.domain.model.events.FinalCompletedEvent;
import com.upc.edufinservice.gamification.domain.model.commands.AddPointsCommand;
import com.upc.edufinservice.gamification.domain.services.GamificationCommandService;
import com.upc.edufinservice.learning.infrastructure.persistence.jpa.repositories.TopicRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class FinalCompletedEventHandler {

    private final GamificationCommandService gamificationCommandService;
    private final TopicRepository topicRepository;

    public FinalCompletedEventHandler(
            GamificationCommandService gamificationCommandService,
            TopicRepository topicRepository
    ) {
        this.gamificationCommandService = gamificationCommandService;
        this.topicRepository = topicRepository;
    }

    @EventListener
    public void on(FinalCompletedEvent event) {

        int pointsToAward = Math.round(event.score());

        if (pointsToAward <= 0) {
            return;
        }

        if (pointsToAward > 0) {
            gamificationCommandService.handle(
                    new AddPointsCommand(
                            event.userId(),
                            pointsToAward
                    )
            );
        }

        if (Boolean.TRUE.equals(event.passed())) {

            topicRepository.findById(event.topicId())
                    .ifPresent(topic ->
                            gamificationCommandService.evaluateModuleCompleted(
                                    event.userId(),
                                    topic.getTopicOrder()
                            )
                    );
        }
    }
}
