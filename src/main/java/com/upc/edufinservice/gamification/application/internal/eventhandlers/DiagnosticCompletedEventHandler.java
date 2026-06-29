package com.upc.edufinservice.gamification.application.internal.eventhandlers;

import com.upc.edufinservice.assessment.domain.model.events.DiagnosticCompletedEvent;
import com.upc.edufinservice.gamification.domain.model.commands.AddPointsCommand;
import com.upc.edufinservice.gamification.domain.services.GamificationCommandService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class DiagnosticCompletedEventHandler {
    private final GamificationCommandService _gamificationCommandService;

    public DiagnosticCompletedEventHandler (GamificationCommandService gamificationCommandService){
        _gamificationCommandService = gamificationCommandService;
    }

    @EventListener
    public void on(DiagnosticCompletedEvent event){
        //System.out.println("🎉 [GAMIFICACIÓN] ¡Examen de Diagnóstico detectado! Otorgando bono de 100 XP al usuario: " + event.userId());
        //DIsparamos el comando de sumar puntos.
        var command = new AddPointsCommand(event.userId(), 100);
        _gamificationCommandService.handle(command);
    }
}
