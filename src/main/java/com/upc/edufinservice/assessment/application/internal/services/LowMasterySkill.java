package com.upc.edufinservice.assessment.application.internal.services;

/**
 * Resultado ordenable de una skill junto con su mastery estimado por DKT-Forget.
 *
 * mastery representa la probabilidad estimada de respuesta correcta para la skill.
 */
public record LowMasterySkill(
        Integer skillId,
        Double mastery
) {}
