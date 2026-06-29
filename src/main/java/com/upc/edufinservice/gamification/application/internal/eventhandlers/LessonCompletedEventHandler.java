package com.upc.edufinservice.gamification.application.internal.eventhandlers;

import com.upc.edufinservice.assessment.domain.model.events.LessonCompletedEvent;
import com.upc.edufinservice.gamification.domain.model.commands.AddPointsCommand;
import com.upc.edufinservice.gamification.domain.services.GamificationCommandService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class LessonCompletedEventHandler {
    private final GamificationCommandService _gamificationCommandService;

    public LessonCompletedEventHandler(GamificationCommandService gamificationCommandService) {
        _gamificationCommandService = gamificationCommandService;
    }

    @EventListener
    public void on(LessonCompletedEvent event) {
        // El score viene del back de 0.0 a 100.0f (porcentaje de respuestas correctas)
        // Lo redondeamos para otorgar 1 punto de XP por cada 1% de nota obtenida en el nivel
        int pointsToAward = Math.round(event.score());

        // Tu AddPointsCommand exige que los puntos sean mayores a cero, así evitamos registrar 0 XP
        if (pointsToAward > 0) {
            System.out.println("🎉 [GAMIFICACIÓN] Lección completada. Sumando " + pointsToAward + " puntos de XP al usuario: " + event.userId());

            var command = new AddPointsCommand(event.userId(), pointsToAward);
            _gamificationCommandService.handle(command);
        }
    }
}
