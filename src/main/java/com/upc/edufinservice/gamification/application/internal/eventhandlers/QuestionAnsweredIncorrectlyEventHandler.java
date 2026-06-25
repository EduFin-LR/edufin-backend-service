package com.upc.edufinservice.analytics.application.internal.eventhandlers;

import com.upc.edufinservice.assessment.domain.model.events.QuestionAnsweredIncorrectlyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class QuestionAnsweredIncorrectlyEventHandler {

    // Por ahora, solo pondremos un mensaje en consola para comprobar que el puente funciona.
    // Más adelante, inyectaremos aquí el servicio que actualiza el ErrorPattern y llama a FastAPI.

    @EventListener
    public void on(QuestionAnsweredIncorrectlyEvent event) {
        System.out.println("¡[ANALYTICS] Evento de error recibido! " +
                "El usuario: " + event.userId() +
                " falló en la pregunta: " + event.questionId() +
                ". Preparando datos para el modelo DKT en FastAPI...");
    }
}