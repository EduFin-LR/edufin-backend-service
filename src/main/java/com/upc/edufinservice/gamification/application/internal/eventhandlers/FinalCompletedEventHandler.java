package com.upc.edufinservice.gamification.application.internal.eventhandlers;

import com.upc.edufinservice.assessment.domain.model.events.FinalCompletedEvent;
import com.upc.edufinservice.gamification.domain.model.commands.AddPointsCommand;
import com.upc.edufinservice.gamification.domain.services.GamificationCommandService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class FinalCompletedEventHandler {

    private final GamificationCommandService gamificationCommandService;

    public FinalCompletedEventHandler(
            GamificationCommandService gamificationCommandService
    ) {
        this.gamificationCommandService =
                gamificationCommandService;
    }

    @EventListener
    public void on(FinalCompletedEvent event) {

        int pointsToAward =
                Math.round(event.score());

        if (pointsToAward <= 0) {
            return;
        }

        gamificationCommandService.handle(
                new AddPointsCommand(
                        event.userId(),
                        pointsToAward
                )
        );
    }
}
