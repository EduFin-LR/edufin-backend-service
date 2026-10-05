package com.upc.edufinservice.analytics.domain.model.entities;

/**
 * Origen pedagógico de una interacción que sí participa en DKT-Forget.
 *
 * QUIZ          -> pregunta respondida dentro de un quiz normal.
 * FINAL         -> pregunta respondida dentro de una evaluación final.
 * REINFORCEMENT -> pregunta reutilizada como refuerzo adaptativo.
 * PRE_TEST      -> respuesta del instrumento inicial; alimenta DKT, pero no habilita refuerzos.
 */
public enum InteractionType {
    QUIZ,
    FINAL,
    REINFORCEMENT,
    PRE_TEST
}