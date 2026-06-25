package com.upc.edufinservice.gamification.application.internal.eventhandlers;

import com.upc.edufinservice.assessment.domain.model.events.QuestionAnsweredCorrectlyEvent;
import com.upc.edufinservice.gamification.domain.model.commands.AddPointsCommand;
import com.upc.edufinservice.gamification.domain.services.GamificationCommandService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class QuestionAnsweredCorrectlyEventHandler {
    private final GamificationCommandService _gamificationCommandService;

    public QuestionAnsweredCorrectlyEventHandler(GamificationCommandService gamificationCommandService){
        _gamificationCommandService = gamificationCommandService;
    }

    @EventListener
    public void on(QuestionAnsweredCorrectlyEvent event){
        System.out.println("Evento recibio, sumando puntos al usuario: " + event.userId());

        var command = new AddPointsCommand(event.userId(), 10);
        _gamificationCommandService.handle(command);
    }
}
