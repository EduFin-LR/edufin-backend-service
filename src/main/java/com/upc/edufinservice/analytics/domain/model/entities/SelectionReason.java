package com.upc.edufinservice.analytics.domain.model.entities;

/**
 * Motivo por el cual una pregunta fue seleccionada para el estudiante.
 *
 * STANDARD    -> pregunta incluida por el flujo normal de la actividad.
 * LOW_MASTERY -> pregunta incluida porque DKT-Forget detectó una skill
 *                con bajo mastery estimado.
 */
public enum SelectionReason {
    STANDARD,
    LOW_MASTERY
}
