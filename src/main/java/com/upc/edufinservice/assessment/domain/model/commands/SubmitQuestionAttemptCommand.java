package com.upc.edufinservice.assessment.domain.model.commands;

import java.util.UUID;

public record SubmitQuestionAttemptCommand(
        UUID userId,
        UUID questionId,
        UUID selectedOptionId,        // Puede ser null si la lógica frontend solo envía el match textual
        String selectedMatchCategory, // Nuevo: Recibe la caja destino ("Ingreso", "Gasto Fijo", etc.)
        Float timeTakenSec
) {
    public SubmitQuestionAttemptCommand {
        if (userId == null) throw new IllegalArgumentException("El ID del usuario es obligatorio");
        if (questionId == null) throw new IllegalArgumentException("El ID de la pregunta es obligatorio");
    }
}