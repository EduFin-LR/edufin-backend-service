package com.upc.edufinservice.analytics.domain.model.entities;

/**
 * Origen pedagógico de una interacción que sí participa en DKT-Forget.
 *
 * QUIZ          -> pregunta respondida dentro de un quiz normal.
 * FINAL         -> pregunta respondida dentro de una evaluación final.
 * REINFORCEMENT -> pregunta reutilizada como refuerzo adaptativo.
 */
public enum InteractionType {
    QUIZ,
    FINAL,
    REINFORCEMENT
}
